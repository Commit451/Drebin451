#!/usr/bin/env bash
set -euo pipefail

version="8.30.1"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
scan_dir="$work_dir/tracked"
mkdir -p "$scan_dir"

# Keep the Cloud Run service within the intended Secret Manager resource budget. Also ensure every
# secret version synced during deploy is actually bound to Cloud Run, and vice versa.
WORKFLOW_PATH="$repo_root/.github/workflows/deploy.yml" python3 - <<'PY'
import os
import re
from pathlib import Path

workflow = Path(os.environ["WORKFLOW_PATH"]).read_text()
synced = set(re.findall(r"^\s*sync_secret\s+([a-z0-9][a-z0-9-]*)\s+\S+\s*$", workflow, re.MULTILINE))
bound = set(re.findall(r"=([a-z0-9][a-z0-9-]*):latest", workflow))

if synced != bound:
    raise SystemExit(
        "Secret Manager sync/binding mismatch: "
        f"synced-only={sorted(synced - bound)}, bound-only={sorted(bound - synced)}"
    )
if len(bound) > 6:
    raise SystemExit(f"Cloud Run binds {len(bound)} Secret Manager secrets; maximum is 6")
print(f"Cloud Run Secret Manager binding count: {len(bound)}/6")
PY

# Scan only files that would be committed. Ignored local credential fallbacks deliberately remain
# on developer machines and must not produce false positives.
REPO_ROOT="$repo_root" SCAN_DIR="$scan_dir" python3 - <<'PY'
import os
from pathlib import Path
import shutil
import subprocess

root = Path(os.environ["REPO_ROOT"])
destination = Path(os.environ["SCAN_DIR"])
paths = subprocess.check_output(
    ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
    cwd=root,
).split(b"\0")
for raw_path in paths:
    if not raw_path:
        continue
    relative = Path(os.fsdecode(raw_path))
    source = root / relative
    if not source.is_file():
        continue
    target = destination / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
PY

if command -v gitleaks >/dev/null 2>&1; then
  gitleaks_bin="$(command -v gitleaks)"
else
  os_name="$(uname -s | tr '[:upper:]' '[:lower:]')"
  arch_name="$(uname -m)"
  case "$arch_name" in
    arm64|aarch64) arch_name="arm64" ;;
    x86_64|amd64) arch_name="x64" ;;
    *) echo "Unsupported architecture for Gitleaks: $arch_name" >&2; exit 1 ;;
  esac
  case "$os_name" in
    darwin|linux) ;;
    *) echo "Unsupported operating system for Gitleaks: $os_name" >&2; exit 1 ;;
  esac

  asset="gitleaks_${version}_${os_name}_${arch_name}.tar.gz"
  checksums="gitleaks_${version}_checksums.txt"
  base_url="https://github.com/gitleaks/gitleaks/releases/download/v${version}"
  curl --fail --silent --show-error --location "$base_url/$asset" --output "$work_dir/$asset"
  curl --fail --silent --show-error --location "$base_url/$checksums" --output "$work_dir/$checksums"

  ASSET="$asset" CHECKSUMS="$checksums" WORK_DIR="$work_dir" python3 - <<'PY'
import hashlib
import os
from pathlib import Path

work = Path(os.environ["WORK_DIR"])
asset = os.environ["ASSET"]
checksums = os.environ["CHECKSUMS"]
expected = None
for line in (work / checksums).read_text().splitlines():
    parts = line.split()
    if len(parts) == 2 and parts[1].lstrip("*") == asset:
        expected = parts[0]
        break
if expected is None:
    raise SystemExit(f"No checksum published for {asset}")
actual = hashlib.sha256((work / asset).read_bytes()).hexdigest()
if actual != expected:
    raise SystemExit(f"Checksum mismatch for {asset}")
PY

  tar -xzf "$work_dir/$asset" -C "$work_dir" gitleaks
  gitleaks_bin="$work_dir/gitleaks"
fi

"$gitleaks_bin" dir "$scan_dir" --redact=100 --no-banner
