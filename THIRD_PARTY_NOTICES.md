# Third-party material and license scope

Original project source is MIT-licensed; see LICENSE. This inventory does not replace
upstream licenses or relicense datasets/dependencies. Preserve notices when distributing binaries.

Runtime components: Jackson core/databind/annotations (Apache-2.0, FasterXML), React and
React DOM (MIT, Meta and contributors), scheduler (MIT), Three.js (MIT, three.js authors).
Development tooling includes TypeScript (Apache-2.0), Vite (MIT), Playwright (Apache-2.0),
axe-core (MPL-2.0), and their transitive dependencies. Consult each installed package's
LICENSE/package.json and Maven POM; this summary does not relicense their code.

The separate Lucene comparison uses Apache Lucene (Apache-2.0), not the core serving engine.
Synthetic examples are original AI-assisted project text. SciFact/BEIR data attribution and
component-specific permissions are in DATA_PROVENANCE.md. The corpus itself is not tracked;
retrieval IDs, scores, judgments-derived metrics and provenance are retained as evidence.

GitHub Action source remains upstream; workflows pin verified official repository commits.
This repository references those actions rather than copying their source. Before shipping
an executable release, inspect its actual dependency notices and include the required texts.
