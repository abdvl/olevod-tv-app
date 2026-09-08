# Chromecast player regression verification

Device: Google Chromecast with Google TV, Android 14, 1920×1080 UI override. Installed player fixes through `2a1cd7d`.

- Normal player starts focused on Fullscreen. Up focuses the outlined video; Down returns to Fullscreen. Further Up reaches header and Down returns to video. Confirm on video enters fullscreen.
- Episode groups and episode buttons are below controls in both modes. Fullscreen group 21–30 renders all ten buttons. Down focuses21; Up returns to group then controls. Changing the group does not change the playing episode.
- Controls Up hides the overlay; Down wakes it and focuses Exit fullscreen. Physical QA caught hidden-root key routing regression, fixed in `2a1cd7d` and covered by PlayerKeyInputTest.
- Video content frame bounds were `[0,0][1920,1080]` with overlay both visible and hidden. Current stream reported1280×720, peak2.58Mbps. Official website logo appears in header.
- Playback stayed on 牧神记, episode1. Installed update resumed the saved position; final QA returned to normal playback and group1–10.
- Build/lint and23 unit tests passed. Three integrated Android UI tests passed: PlayerKeyInputTest, EpisodePickerTest, PlayerVideoStageTest. The separate real API catalog sorting regression also passed.

Screenshots: `screenshots/chromecast-video-focus-episodes.png`, `screenshots/chromecast-full-overlay-quality-episodes.png`. ADB captures exclude the hardware video layer; black video in these captures is not evidence of playback failure. The user previously confirmed normal audio and video on this device.
