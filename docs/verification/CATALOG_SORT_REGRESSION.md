# Movie catalog sort regression

## Reproduced

On emulator-5554, the movie directory initially rendered 4,123 films / 20 loaded. Selecting Latest Upload persistently rendered 0 / no films. A temporary state diagnostic showed the new feed had successfully loaded 20 movies while the lazy content still displayed its earlier empty snapshot. Diagnostics were removed after investigation.

A separate unauthenticated read-only API check returned code 0, total 4,123, and 20 movies for update, desc, hot, and score. No credential or session data was read for this investigation.

## Fix

Replace `key(filter) { rememberLazyListState() }` with `rememberSaveable(filter, saver = LazyListState.Saver) { LazyListState() }`. This resets scrolling when the filter changes while keeping the saveable state/effect structure stable and retaining scroll restoration when returning to the screen. Apply the same correction to the equivalent query-keyed search list. No API sort mapping was changed.

## Validation

- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
- Opt-in `CatalogSortUiTest` on emulator-5554, `liveCatalogUi=true`: OK (1 test), 4.275 seconds. Fetches each sort's expected first title from the live API and waits for that title to be displayed after changing the sort. Checks all four sorts, switching back to a cached sort, and automatic loading from 20 to 40 items at the end of the list.
- Actual ADB D-pad selection of Latest Upload, Most Popular, and Highest Rated: each rendered 4,123 films / 20 loaded; focus remained on the chosen sort chip after the response.
- Screenshot: `screenshots/catalog-score-fixed.png`.
- Search uses the same corrected state pattern. Its query-change result coverage was not expanded in this report; an `A` search returned no films and is not treated as proof of positive search-result rendering.
- Chromecast was not operated during this task. Final integrated installation is handled by the coordinating agent.
