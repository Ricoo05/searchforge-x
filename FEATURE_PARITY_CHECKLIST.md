# Before/after feature and content inventory

Baseline: production UI at 8090, source copies in work/ui-audit-baseline, screenshots in
evidence/ui-before. This inventory was established before editing presentation. Verification
status below reflects executed checks or explicitly identified source review.

## Routes, navigation and content

- [x] `/` and `#cluster` remain intact; Search explorer and Document studio remain available.
- [x] Product identity, DISTRIBUTED SEARCH LAB, LOCAL ENVIRONMENT and existing status terms.
- [x] SEARCH INFRASTRUCTURE, EXPOSED; Every query. A deeper connection.; original description.
- [x] Explore the engine action; JAVA 21 × CUSTOM BM25; all three existing live readouts.
- [x] LIVE CLUSTER TOPOLOGY, COORDINATOR, interaction instructions and topology disclaimer.
- [x] Search workspace tabs, CONNECTED TO REAL SERVICES, 01 / RETRIEVAL and original heading.
- [x] Existing query help, three existing example buttons and BM25 · TOP 10 label.
- [x] Document studio: 02 / INGESTION, original heading/introduction, labels, route hint,
      sample description and local administration/atomicity warning.
- [x] Cluster pulse/Shard headings, LIVE TELEMETRY, all worker values and generation labels.
- [x] Session activity and actual history; diagnostics-refresh/count/round-trip explanation.
- [x] Original footer identity/description and process-crash-recovery scope label.

## Actions and data contracts

- [x] Enter and Run search submit unchanged query/offset/limit=10/timeoutMillis=5000 payload.
- [x] `/` keyboard shortcut focuses the existing search input without intercepting text entry.
- [x] Three existing example-query actions keep their exact query strings and behavior.
- [x] Results preserve order, IDs, shard metadata, BM25 formatting, titles and full returned text.
- [x] Previous/Next retain the existing bounded page behavior and disabled states.
- [x] Document ID validation, title/text limits, modulo route hint and stable-ID indexing.
- [x] Sample loading preserves all six original documents and replacement warning.
- [x] Acknowledged/unknown shard write outcomes and uncertainty warnings are preserved.
- [x] Diagnostics still use POST /diagnostics and refresh every 10 seconds (hash/source review
      plus executed refresh and repeated visible diagnostics).
- [x] Health refresh, selectors, inspector close and pause/resume execute successfully.
- [x] 3D orbit/zoom handlers and limits preserved by source review; canvas renders in browser.
- [x] Keyboard alternatives to pointer-driven 3D selection remain available.

## Existing states

- [x] Connecting/initial, healthy populated and empty index; unknown metrics remain unknown.
- [x] Query loading with existing copy, no fabricated percentage or stage timing.
- [x] COMPLETE, PARTIAL, FAILED, zero hits, invalid-query error and genuine network error.
- [x] Ingestion pending, validation failure, COMPLETE/PARTIAL/FAILED and uncertain outcome.
- [x] Worker ready/unavailable, selected ready/unavailable and diagnostic connection failure.
- [x] WebGL fallback and reduced-motion implementation retained and source-reviewed.
- [ ] Force actual request timeout (network failure is verified separately).
- [ ] Force WebGL loss/no-GPU fallback and change OS reduced-motion preference.
- [ ] Formal screen-reader and cross-browser testing.

## Protected behavior

- [x] Backend and test source hashes unchanged during this UI transformation.
- [x] engine.ts (including samples and API contracts) unchanged.
- [x] Existing production storage contents/generations and query results unchanged.
- [x] Tests which write fixtures run only against isolated test data at port 8290.
- [x] No new backend endpoints, filters, routes, metrics, content substitutions or fake data.

Executed evidence: 18 browser checks, 12 final responsive screen captures, real fault-state
and transient-state captures, 13 final axe reports with zero violations/incomplete rules,
29 protected-file hashes and six full production-result comparisons. See
UI_UX_TRANSFORMATION_REPORT.md for the exact evidence files and limitations. The test-only
latency preview delays actual requests by 1,500 ms; its response data is never mocked.

No existing autocomplete, filters, previews, result links or dialogs were found; none are
invented for this redesign. Accessibility-only helpers may add labels/skip navigation,
while all existing product text and controls remain available.
