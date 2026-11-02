# Expanded quality validation — 2026-11-02

Verified application/test/workflow revision: `92b43cd`.

- [Correctness, formatting, dependencies, image and browser run](https://github.com/Ricoo05/searchforge-x/actions/runs/41476169720): all five jobs passed.
- [CodeQL run](https://github.com/Ricoo05/searchforge-x/actions/runs/41476169838): Java, JavaScript/TypeScript and Python analyses passed. The open-alert API returned an empty list after analysis.
- Windows and Ubuntu each passed 30 Java tests and five Python tests, frontend production
  compilation and formatting gates. Ruff checks passed; Spotless covers 29 Java files,
  including the independent Lucene reference. Prettier covers frontend source and tests.
- Real Docker services passed ingestion/retrieval and COMPLETE → PARTIAL → COMPLETE
  worker-loss/recovery probes. All 42 Playwright tests passed: 14 each in Chromium,
  Firefox and WebKit. No mock search responses or skipped browser tests are used.
- axe WCAG A/AA checks cover desktop/mobile search, results, studio and validation.
  Keyboard navigation, reduced-motion CSS and WebGL-loss fallback also passed.
- Trivy scanned the actual runtime image: zero HIGH/CRITICAL findings, 53 MEDIUM and
  four LOW findings. Lower-severity findings are retained, not suppressed. The complete
  JSON report is a downloadable image-vulnerability-report workflow artifact.
- Main now requires eight checks, including all CodeQL analyses and the image gate.
  Code-owner review, conversation resolution and force-push/deletion restrictions remain.

Machine-readable job/step results and image severity counts are retained in
[evidence/quality/verified-checks-92b43cd.json](evidence/quality/verified-checks-92b43cd.json).
Subsequent documentation-only commits are validated by the live main-branch workflows;
these historical run links continue to identify the exact tested application revision.

The new tests found a visible/accessibility-name mismatch on the brand link and a WebKit
keyboard-navigation issue. Both were fixed. The first audit harness also tried inline
script injection, which the application's strict CSP correctly blocked; audits now use
DevTools evaluation without weakening the served CSP. Earlier failed/cancelled runs are
retained in Actions and are not represented as passing runs.

Local Windows verification passed the build, all 30 Java tests, all five Python tests,
Chromium accessibility checks and WebKit checks. Local Firefox could not launch
(`spawn UNKNOWN`); its complete coverage passed on the remote Linux runner.
Formatting changes source hashes and line numbers; older preservation hashes describe
their original revisions. Raw benchmark and historical evidence files were preserved.

Manual screen-reader/user research, hosted-service security, real disk-full/power-loss
validation, replication and production-scale claims remain outside this verified local
portfolio scope. Automated accessibility checks are not accessibility certification,
and passing scanners do not establish that a system has no vulnerabilities.
