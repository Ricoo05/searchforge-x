# Browser evidence reproduction

These checks use the documented CUA browser API, with an isolated backend at 8290.
They do not require browser access to production data and refuse writes through another
preview origin. The normal preview at 5273 points to production and must not be used for
fixture writes.

Start an isolated ClusterMain using a copied corpus and a separate data root. Before
changing that copy, run scripts/check_ui_software_parity.py with --seed-test-copy and an
--output path to compare all six baseline queries and seed 25 ranking fixtures. Once the
copy contains those fixtures, use scripts/capture_ui_reference.py before subsequent runs.

Start Vite from web with --config vite.test.config.ts (port 5274), bind the browser tab
through CUA and call runBrowserRegression(tab, browser, absoluteRepositoryRoot) from
browser-regression.mjs. It writes evidence/ui-after/browser-regression.json and screenshots.
The module checks exact current API rows, native validation, genuine writes, examples,
pagination, keyboard behavior, worker controls and six viewport widths. Scoring correctness
against an independent oracle is covered by the Java suite and benchmark/evaluation runs.

For transient-state inspection, run the test Vite config with --mode latency --port 5275.
It delays actual search, ingestion and diagnostics requests by 1,500 ms. It does not mock
responses or alter production serving. Use a fresh isolated data root for the empty-index
screen. Actual worker/coordinator termination is used for failure-state evidence; never
stop processes without verifying their exact isolated data root and command line.
