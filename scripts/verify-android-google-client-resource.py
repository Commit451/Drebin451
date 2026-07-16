#!/usr/bin/env python3
"""Verify that a release APK retains a non-empty Google web client ID resource."""

from __future__ import annotations

import os
from pathlib import Path
import re
import subprocess
import sys

RESOURCE_NAME = "string/default_web_client_id"
DEFAULT_APK = Path("app/androidApp/build/outputs/apk/release/androidApp-release.apk")


def find_aapt2() -> Path:
    sdk_roots = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        str(Path.home() / "Library/Android/sdk"),
        str(Path.home() / "Android/Sdk"),
    ]
    candidates: list[Path] = []
    for root in dict.fromkeys(filter(None, sdk_roots)):
        candidates.extend(Path(root).glob("build-tools/*/aapt2"))
    executable_candidates = [path for path in candidates if os.access(path, os.X_OK)]
    if not executable_candidates:
        raise SystemExit(
            "Could not find aapt2 under ANDROID_HOME, ANDROID_SDK_ROOT, or a standard SDK path."
        )
    return max(executable_candidates, key=lambda path: path.parent.name)


def main() -> None:
    apk_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_APK
    if not apk_path.is_file():
        raise SystemExit(f"APK not found: {apk_path}")

    result = subprocess.run(
        [str(find_aapt2()), "dump", "resources", str(apk_path)],
        check=True,
        capture_output=True,
        text=True,
    )
    lines = result.stdout.splitlines()
    for index, line in enumerate(lines):
        if RESOURCE_NAME not in line:
            continue
        value_lines = lines[index + 1 : index + 4]
        if any(re.search(r'^\s*\(\)\s+".+"$', value) for value in value_lines):
            print(f"Verified non-empty {RESOURCE_NAME} in {apk_path}.")
            return
        raise SystemExit(f"{RESOURCE_NAME} is present but empty in {apk_path}")

    raise SystemExit(f"{RESOURCE_NAME} is missing from {apk_path}")


if __name__ == "__main__":
    main()
