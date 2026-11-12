# Ad and tracker protection tasks

- [x] Inspect current implementation, capture failing reproductions, and independently review plan.
- [x] 1. Implement supported immutable host rules and focused tests. Verify :core:adblock:testDebugUnitTest. Depends: none.
- [x] 2a. Implement bounded per-source atomic cache with failure/corruption/cancellation tests. Depends: 1.
- [x] 2b. Connect immediate seed, serialized refresh/status and worker retry. Verify focused tests/build. Depends: 2a.
- [x] 3a. Persist/manage exact-host protection defaults and independent resets; test preference behavior. Depends: none.
- [x] 3b. Implement TV Settings protection status/update/override controls and remote focus behavior. Depends: 2b, 3a.
- [x] 3c. Implement Browse shield, document counts and per-visit pause; verify site/tab scope. Depends: 1, 2b, 3a.
- [x] 4a. Run full relevant unit/lint/build checks and deterministic WebView/emulator acceptance flows. Depends: 3.
- [x] 4b. Independent implementation review, resolve findings and record verification/limitations. Depends: 4a.
- [x] 4c. Update user documentation/changelog and save reviewed commits. Depends: 4b.
