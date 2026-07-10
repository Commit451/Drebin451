#!/usr/bin/env bash
set -euo pipefail

mode="${1:-optional}"
case "$mode" in
  optional|server|web|android-release) ;;
  *)
    echo "Usage: $0 [optional|server|web|android-release]" >&2
    exit 2
    ;;
esac

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
runner_temp="${RUNNER_TEMP:-$repo_root/.secrets/tmp}"
mkdir -p "$runner_temp"

is_required() {
  local target="$1"
  case "$mode:$target" in
    server:deploy|server:admin|web:deploy|web:web|android-release:android|android-release:signing) return 0 ;;
    *) return 1 ;;
  esac
}

decode_env() {
  local variable="$1"
  local destination="$2"
  local label="$3"
  local required="$4"
  local value="${!variable:-}"

  if [[ -z "$value" ]]; then
    if [[ "$required" == "true" ]]; then
      echo "Required GitHub Actions secret $variable is missing." >&2
      exit 1
    fi
    return 0
  fi

  mkdir -p "$(dirname "$destination")"
  VARIABLE="$variable" DESTINATION="$destination" python3 - <<'PY'
import base64
import os
from pathlib import Path

name = os.environ["VARIABLE"]
value = os.environ.get(name, "")
try:
    decoded = base64.b64decode(value, validate=True)
except Exception as error:
    raise SystemExit(f"{name} is not valid base64: {error}")
if not decoded:
    raise SystemExit(f"{name} decoded to an empty value")
Path(os.environ["DESTINATION"]).write_bytes(decoded)
PY
  chmod 600 "$destination"
  echo "Prepared $label."
}

deploy_required=false
admin_required=false
web_required=false
android_required=false
signing_required=false
is_required deploy && deploy_required=true
is_required admin && admin_required=true
is_required web && web_required=true
is_required android && android_required=true
is_required signing && signing_required=true

deploy_path="$runner_temp/drebin451-github-actions-deploy.json"
decode_env GCP_DEPLOY_SERVICE_ACCOUNT_JSON_BASE64 "$deploy_path" "GCP deploy credentials" "$deploy_required"
if [[ -f "$deploy_path" ]]; then
  DEPLOY_PATH="$deploy_path" python3 - <<'PY'
import json
import os
from pathlib import Path

value = json.loads(Path(os.environ["DEPLOY_PATH"]).read_text())
expected_email = "github-actions-deploy@drebin451.iam.gserviceaccount.com"
if (
    value.get("type") != "service_account"
    or value.get("project_id") != "drebin451"
    or value.get("client_email") != expected_email
    or not value.get("private_key")
):
    raise SystemExit("Decoded GCP deploy credentials failed structure/account validation")
PY
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    printf 'GOOGLE_APPLICATION_CREDENTIALS=%s\n' "$deploy_path" >> "$GITHUB_ENV"
  fi
fi

admin_path="$runner_temp/drebin451-firebase-adminsdk.json"
decode_env FIREBASE_ADMIN_JSON_BASE64 "$admin_path" "Firebase Admin credentials" "$admin_required"
if [[ -f "$admin_path" ]]; then
  ADMIN_PATH="$admin_path" python3 - <<'PY'
import json
import os
from pathlib import Path

value = json.loads(Path(os.environ["ADMIN_PATH"]).read_text())
if value.get("type") != "service_account" or value.get("project_id") != "drebin451" or not value.get("private_key"):
    raise SystemExit("Decoded Firebase Admin credentials failed structure/project validation")
PY
fi

android_path="$repo_root/app/androidApp/google-services.json"
decode_env FIREBASE_ANDROID_CONFIG_BASE64 "$android_path" "Android Firebase configuration" "$android_required"
if [[ -f "$android_path" ]]; then
  ANDROID_CONFIG_PATH="$android_path" python3 - <<'PY'
import json
import os
from pathlib import Path

value = json.loads(Path(os.environ["ANDROID_CONFIG_PATH"]).read_text())
packages = {
    client.get("client_info", {}).get("android_client_info", {}).get("package_name")
    for client in value.get("client", [])
}
if "com.commit451.drebin451" not in packages:
    raise SystemExit("Decoded Android Firebase config is not for com.commit451.drebin451")
PY
fi

web_path="$repo_root/app/shared/firebase-web-config.properties"
decode_env FIREBASE_WEB_CONFIG_BASE64 "$web_path" "web Firebase configuration" "$web_required"
if [[ -f "$web_path" ]]; then
  WEB_CONFIG_PATH="$web_path" python3 - <<'PY'
import os
from pathlib import Path

required = {
    "applicationId", "apiKey", "projectId", "storageBucket",
    "gcmSenderId", "authDomain", "webClientId",
}
values = {}
for raw_line in Path(os.environ["WEB_CONFIG_PATH"]).read_text().splitlines():
    line = raw_line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    key, value = line.split("=", 1)
    values[key.strip()] = value.strip()
missing = sorted(key for key in required if not values.get(key))
if missing:
    raise SystemExit("Decoded web Firebase config is missing: " + ", ".join(missing))
PY
fi

keystore_path="$runner_temp/drebin451-release.jks"
decode_env ANDROID_KEYSTORE_BASE64 "$keystore_path" "Android release keystore" "$signing_required"
if [[ -f "$keystore_path" ]]; then
  signing_names=(ANDROID_KEYSTORE_ALIAS ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_PASSWORD)
  for name in "${signing_names[@]}"; do
    if [[ -z "${!name:-}" ]]; then
      if [[ "$signing_required" == "true" ]]; then
        echo "Required GitHub Actions secret $name is missing." >&2
        exit 1
      fi
      continue
    fi
  done
  if [[ -n "${ANDROID_KEYSTORE_ALIAS:-}" && -n "${ANDROID_KEYSTORE_PASSWORD:-}" ]]; then
    keytool -list \
      -keystore "$keystore_path" \
      -storepass "$ANDROID_KEYSTORE_PASSWORD" \
      -alias "$ANDROID_KEYSTORE_ALIAS" >/dev/null 2>&1
  fi
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    printf 'ANDROID_KEYSTORE_PATH=%s\n' "$keystore_path" >> "$GITHUB_ENV"
  fi
fi
