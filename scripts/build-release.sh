#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/android-env.sh
release_tag="${1:?Usage: bash scripts/build-release.sh v0.2}"
if [[ ! "$release_tag" =~ ^v[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
  echo 'Expected a release tag such as v0.2 or v0.2.1.' >&2
  exit 1
fi
release_dir="artifacts/releases/$release_tag"
if [ -e "$release_dir" ]; then
  echo "Release artifacts already exist: $release_dir. Refusing to overwrite them." >&2
  exit 1
fi
python3 - "$release_tag" <<'PY'
import re
import sys
from pathlib import Path
version = re.search(r'versionName\s*=\s*"([^"]+)"', Path('app/build.gradle.kts').read_text()).group(1)
tag_version = sys.argv[1][1:]
if version not in (tag_version, tag_version + '.0'):
    sys.exit('Release tag does not match app versionName')
PY
if [ ! -f .secrets/release-signing.properties ]; then
  echo 'Missing release signing configuration. Read docs/BUILD_AND_TEST.md before creating or restoring a key.' >&2
  exit 1
fi
./gradlew assembleRelease testReleaseUnitTest lintRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
mkdir -p artifacts/releases
release_staging=$(mktemp -d artifacts/releases/.staging.XXXXXX)
trap 'rm -rf "$release_staging"' EXIT
cp app/build/outputs/apk/release/app-release.apk "$release_staging/olevod-tv-$release_tag.apk"
python3 - "$release_staging" "$release_tag" <<'PY'
from pathlib import Path
import hashlib
import sys
apk = Path(sys.argv[1]) / ('olevod-tv-' + sys.argv[2] + '.apk')
(apk.parent / 'SHA256SUMS.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest() + '  ' + apk.name + '\n')
PY
mv "$release_staging" "$release_dir"
trap - EXIT
echo "Release artifacts: $release_dir"
