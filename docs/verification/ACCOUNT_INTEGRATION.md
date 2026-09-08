# Real account integration validation

Credentials are deliberately excluded.

- Native same-client CAPTCHA login and cross-process encrypted session passed.
- VIP 80632 played with Media3 and actual video frames.
- Actual locally watched VIP record synced and read back: episode matched, position 117 seconds, duration 1050 seconds. HistorySyncIntegrationTest passed.
- Movie favorite add/list/remove passed and original state was restored.
- Channel cancellation returned code 0 but did not converge after 60 seconds. FavoritesIntegrationTest FAILED; this is not an all-tests-passed release.
- Final read-only account check showed exactly the original CCTV13 favorite, with no extra channels.
- User explicitly authorized new viewing-record uploads; no bulk migration was performed.

Cloud state can update asynchronously. Code 0 alone is not proof of persisted state. Physical Chromecast validation is tracked in PROGRESS.md.
