#!/usr/bin/env python3
"""Create a local release key once. Never commit or upload .secrets/."""
import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parents[1]
folder = root / '.secrets'
folder.mkdir(mode=0o700, exist_ok=True)
props = folder / 'release-signing.properties'
key = folder / 'olevod-release.jks'
if props.exists() or key.exists():
    raise SystemExit('Signing files already exist; refusing to replace a release identity.')
password = secrets.token_urlsafe(36)
env = {**os.environ, 'OLE_RELEASE_KEY_PASSWORD': password}
subprocess.run(['keytool', '-genkeypair', '-keystore', str(key), '-storetype', 'JKS',
                '-alias', 'olevod-tv', '-keyalg', 'RSA', '-keysize', '3072',
                '-validity', '10000', '-dname', 'CN=Olevod TV Personal Release',
                '-storepass:env', 'OLE_RELEASE_KEY_PASSWORD',
                '-keypass:env', 'OLE_RELEASE_KEY_PASSWORD', '-noprompt'], env=env, check=True,
               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
key.chmod(0o600)
with props.open('x') as stream:
    stream.write('storeFile=.secrets/olevod-release.jks\nkeyAlias=olevod-tv\n'
                 f'storePassword={password}\nkeyPassword={password}\n')
props.chmod(0o600)
print('Created private release signing files in .secrets/. Back up both files securely.')
