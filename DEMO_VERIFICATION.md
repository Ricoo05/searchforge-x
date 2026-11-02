# Reproducible demonstration

Verified demonstration type: real local application plus genuine screenshots. No public
hosted backend is deployed. Public hosting was evaluated against the current security
boundary: unrestricted administrative APIs lack auth; public deployment requires a separate
restricted mode, operational review and approved provider/cost. GitHub Pages cannot run
these Java workers. The selected zero-hosting-cost option is loopback Docker Compose.

From a clean checkout with Docker/Compose: `docker compose up --build`; open
http://127.0.0.1:8090. On a disposable index choose Load sample corpus. Search `search`,
then `"inverted index"`, then `search AND ranking`. Inspect returned document IDs/BM25
scores and the real worker counts. Select a worker with the keyboard buttons; close the
inspector and pause/resume the 3D view. In Document studio add an original test document
with a new stable ID, then find its unique term. Stable IDs replace existing documents.

For a safe failure demo on disposable data: `docker compose stop worker1`, repeat the
query and inspect PARTIAL/missing shard 1, then `docker compose start worker1` and verify
COMPLETE after recovery. Stop normally with `docker compose down`; do not remove volumes
unless discarding that explicit disposable data. CI executes these real transitions.

Evidence: evidence/ui-after/production-search.jpg, partial-worker.jpg, index-success.jpg,
transient-states.json and fault-states.json; exact API parity is in production-preservation.json.
New automated Chromium suite: 10 passing tests against isolated port 8490, real backend,
six widths, indexing/retrieval, parser/empty states and pagination. No mocked responses.
It does not prove public availability, no-GPU fallback, every browser or all failure states.
For a recording, follow the sequence above and narrate worker loss/recovery while showing
the actual query and health labels; existing screenshots are not a video recording.
