"""Inject locally supplied login input without exposing credentials in process arguments."""
import json
import os
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
if len(sys.argv) not in (2,3):
    raise SystemExit("Usage: python3 scripts/prepare-login-test.py CURRENT_CAPTCHA [--native]")
values = {}
for line in (root / ".secrects").read_text().splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value
state = {} if "--native" in sys.argv else json.loads((root / ".tools/captcha-state.json").read_text())
payload = {"username": values["username"], "password": values["password"],
           "captcha": sys.argv[1], "captcha_id": state.get("captchaId", "")}
adb = root / ".tools/android-sdk/platform-tools/adb"
target=".native-login-input.json" if "--native" in sys.argv else ".login-test.json"
subprocess.run([str(adb), "-s", os.environ.get("ANDROID_SERIAL", "emulator-5554"), "shell", "run-as", "com.olevod.tv", "sh", "-c",
                "'mkdir -p files && cat > files/"+target+".tmp && mv files/"+target+".tmp files/"+target+"'"],
               input=json.dumps(payload).encode(), check=True, stdout=subprocess.DEVNULL)
print("Private login test input prepared; no credentials printed.")
