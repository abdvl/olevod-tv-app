# v0.1 release verification

- App version0.1.0/code1, package com.olevod.tv, minSdk26, targetSdk35, leanback required.
- Dedicated RSA3072 release certificate, SHA-256 db2d039b4b6685678c5c71e397df7146d5e3bac038d438d55f496db1cdd060f4.
- APK Signature Scheme v2 verification passed; release APK is not debuggable and excludes debug intent screen/preview behavior.
- assembleRelease, testReleaseUnitTest (23 tests, no failures/errors/skips), lintRelease passed. Keystore/config paths are ignored; package inventory contains no private signing configuration or test credentials.
- Fresh API34 TV emulator Olevod_Release_v01/emulator-5556 was created for release verification; install and actual app launch succeeded without deleting any previous device data.
- Captured live release home, current-year hot/score category sections, catalog, MN suggestion→魔女→first result, normal/fullscreen player, generated test watch history and exit confirmation. Screenshots are under docs/screenshots/; no real account login was used in this clean environment.
- Existing Chromecast debug installation retained. Its previously confirmed audio/video and UI regressions are evidence for the same feature implementation, not proof of a release-signature upgrade.
- Install migration docs explicitly describe differing debug/release signatures, local-data loss on uninstall, and no automatic migration/export.
