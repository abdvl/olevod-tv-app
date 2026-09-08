#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/android-env.sh
if [ ! -f .secrets/release-signing.properties ]; then
  echo 'Missing release signing configuration. Read docs/BUILD_AND_TEST.md before creating or restoring a key.' >&2
  exit 1
fi
./gradlew assembleRelease testReleaseUnitTest lintRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
mkdir -p artifacts/releases/v0.1
cp app/build/outputs/apk/release/app-release.apk artifacts/releases/v0.1/olevod-tv-v0.1.apk
python3 - <<'PY'
from pathlib import Path
import hashlib
apk = Path('artifacts/releases/v0.1/olevod-tv-v0.1.apk')
(apk.parent / 'SHA256SUMS.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest() + '  ' + apk.name + '\n')
print('Release artifacts:', apk.parent)
PY
