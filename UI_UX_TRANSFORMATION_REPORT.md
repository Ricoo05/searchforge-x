# SearchForge X UI/UX transformation

The UI is rebuilt around a compact paper, charcoal and copper instrument aesthetic.
Search and document creation are the primary work areas; live health leads the diagnostics
rail, followed by the functional Three.js cluster, worker rows and real query history.
All original product copy, features and API behavior remain available.

## Scope and preservation

UI_UX_AUDIT.md and FEATURE_PARITY_CHECKLIST.md were established before presentation edits.
Before screenshots are in evidence/ui-before; after screenshots are in evidence/ui-after.
Earlier blue-interface evidence remains historical and is not the final design.

The presentation phase changes web/src/main.tsx, style.css and ClusterScene.tsx. It adds
development-only axe/performance instrumentation, isolated Vite test configuration and
a browser regression module. axe-core is a development dependency and is removed from
the production bundle. No production dependency was added during this presentation phase.
main.tsx also disposes its React root during development hot replacement.

The source-hash report protects 29 files: the established Java/backend/test sources and
engine.ts. All hashes match the pre-transformation snapshot. The quoted-string inventory
reports no removed original literals; the content inventory and browser checks cover
headings, prose and controls. This claim concerns the UI phase. Earlier software work in
this continuation includes the executor fix, stronger reliability tests, ingestion,
diagnostics, packaging and benchmark/evaluation work described elsewhere.

The preserved UI-phase snapshot is commit 00debad. After GitHub upload, Windows CI exposed
locked temporary logs in DistributedTest cleanup. A subsequent, separate test-harness fix
uses pipe capture and parent-owned logs after process/reader termination. That later change
does not alter the UI, production backend or API; it is outside the UI hash-preservation claim.

Six production queries were compared as full JSON objects after installing the packaged
redesign, including IDs, scores, document text, metadata, ordering, statuses and generations.
All match. Diagnostics still show seven documents and generations 1, 1, 2. Fixture writes
and failure injection used isolated data at port 8290; no UI test wrote to port 8090.

## Visual and interaction decisions

- Neutral surfaces, a restrained copper action color and semantic green/amber/red states.
  No blue gradient, glow, decorative glass or invented metric stream.
- Smaller introduction, shared alignment, typographic hierarchy, explicit spacing tokens,
  restrained radii, editorial result separators and readable supporting values.
- Ingestion warning before the form, shorter initial textarea, and acknowledgment feedback
  immediately after the indexing action and before sample loading.
- Health summary before topology; selectable worker rows have hover/focus/selected states.
- The actual 3D topology retains orbit, zoom, selection, pause and keyboard alternatives.
  Decorative packet meshes and idle floating were removed. Rendering is invalidated by
  interaction, resize, status/selection changes or active requests, instead of an idle loop.
- Skip navigation, visible keyboard focus, pressed-state navigation/selection, search
  landmark, live result summary, persistent labels and existing errors/status messages.
- At small widths content wraps into a single column. Existing supporting labels remain
  available. Reduced-motion CSS and the existing no-WebGL fallback remain implemented.

## Executed verification

The final repeatable browser run passed 18 checks with no console errors. It verified:
exact first/second/third page rows, deterministic IDs and four-decimal BM25 display, long
titles, Unicode and literal HTML text, previous/next disabled boundaries, Enter submission,
all three original query examples, zero hits, actual parser errors, ID validation,
modulo routing, real indexing and retrieval, original sample loading, inspection/close,
pause/resume, health refresh and slash-shortcut behavior inside and outside text fields.

Both search and document studio were checked at 1440, 1280, 1024, 768, 390 and 320 pixels:
12 screen checks, no horizontal document overflow. Full-page screenshots are retained.
The final contrast correction was followed by another 12-screen geometry/a11y capture.

Real isolated worker termination verified PARTIAL queries, a selected unavailable worker,
PARTIAL ingestion and UNKNOWN acknowledgment warnings. Stopping all isolated workers
verified FAILED queries and ingestion; stopping the coordinator verified network errors,
unknown readouts and uncertain-write messaging. The test cluster was restarted afterward.
An empty isolated data root verified the blank-index screen. A documented test-only
1,500 ms network delay captured connecting, indexing-pending and query-pending screens
using actual backend responses; no product response or metric was mocked.

Evidence: browser-regression.json, final-accessibility.json, fault-states.json,
transient-states.json and production-preservation.json in evidence/ui-after.
The browser module uses Codex CUA's documented browser API; it is not a standalone
Playwright CLI test. Capture the current isolated API reference with
scripts/capture_ui_reference.py before repeating its ranking checks. Seed fixtures with
scripts/check_ui_software_parity.py --seed-test-copy only when the copied cluster still
matches production; that script intentionally rejects a mismatched copy.

Final Java verification: 29 tests, zero failures/errors/skips. Python evaluator: two tests
pass. TypeScript and Vite production build pass. The final JAR serves the built frontend
and was checked at port 8090. The later packaging pass skips tests explicitly and does not
replace the retained full-test evidence.

## Accessibility and performance observations

Final axe captures across the six widths report zero violations and zero incomplete rules
for the sampled states. Earlier canvas-overlay contrast checks were incomplete; opaque
instrument-colored label backgrounds resolved them. Keyboard focus, native validation,
labels, status announcements and selection alternatives were checked through DOM/AX state.
This is not screen-reader user research or a complete WCAG conformance assessment.

The baseline local development capture observed LCP 392 ms and CLS 0.01025. The final local
capture observed LCP 188 ms and CLS 0; maximum recorded interaction duration was 48 ms.
These single-machine development observations have different cache/session conditions,
include automated interactions, and do not establish a causal speedup, field Core Web
Vitals or formal INP. Raw observations are retained, including unavailable measurements.

Production output: main JavaScript 243.08 kB / 75.81 kB gzip; lazy Three.js scene chunk
587.18 kB / 146.19 kB gzip; CSS 25.60 kB / 5.79 kB gzip. Vite's >500 kB chunk warning remains
visible. The topology is lazy-loaded but appears on the initial screen, so its bytes are
still downloaded when that screen opens. The meaningful runtime change is eliminating
continuous idle scene rendering; battery/GPU savings have not been benchmarked.

## Independent reviews and disposition

Three separately delegated, read-only reviewers provided distinct perspectives:

- Principal design review: 7.2/10. Requested a shorter introduction, more readable small
  metadata and health before the graph. Applied all three changes.
- First-user perspective: 7.5/10. Requested indexing feedback beside the action, the stable-ID
  warning before entry, a shorter textarea and clearer worker-row affordances. Applied.
- Staff frontend review: 8/10. Identified placeholder contrast just below 4.5:1, continuous
  idle requestAnimationFrame scheduling and unfinished parity evidence. Fixed the contrast
  and rendering behavior; completed the executed-check inventory and retained limitations.

These are source/screenshot expert reviews, not live usability studies. The scores describe
the reviewed intermediate version; no perfect or independently re-scored final result is claimed.

## Remaining verification limits

WebGL context-loss/no-GPU fallback and an operating-system reduced-motion setting were
preserved and source-reviewed but were not forcibly exercised in this browser session.
The actual request-timeout branch was not forced; parser and genuine network-failure
branches were exercised. Formal screen-reader use, cross-browser coverage, field INP,
large-device performance and accessibility user research remain future checks.

Docker is unavailable locally. CI includes actual Compose startup, ingestion/search,
worker-loss partial results and persistent recovery checks; its observed outcome belongs
in CHECKPOINT.md. No production-readiness or hiring guarantee follows from this project.
