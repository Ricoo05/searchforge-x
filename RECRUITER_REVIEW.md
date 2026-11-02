# Recruiter acceptance review — SearchForge X only

This is an evidence-based self-review, not an independent hiring decision.

| Acceptance question | Status | Evidence |
|---|---|---|
| Find repository quickly | BLOCKED publicly | Existing private repo; profile pin requires authorization |
| Public access | BLOCKED | Private visibility, pending approval |
| Understand README | PASS | Problem, stack, quick start, diagram, honest status |
| Identify implemented features | PASS | README, algorithms/storage/distribution docs |
| Inspect source | PASS for authorized reviewers | Java engine, tests and web source tracked |
| See passing CI | PASS for baseline, BLOCKED for new CI | Actual run 41424775458; new workflow unpushed |
| Reproduce tests | PASS baseline / pending final public clone | OS matrix and clean Maven checkout |
| Run or try application | PASS locally | Compose startup/recovery and real browser evidence |
| Real demonstrations | PASS locally | Genuine screenshots and reproducible instructions |
| Architectural decisions | PASS | ARCHITECTURE.md, TRADEOFFS.md, diagrams |
| Backed performance numbers | PASS within scope | Retained trials/profiles/SciFact comparison |
| Honest limitations | PASS | KNOWN_LIMITATIONS.md, SECURITY.md |
| No obvious security concerns | BLOCKED for public service | Unauthenticated admin API; local-only boundary |
| Application works | PASS tested scenarios | 29 Java, two metric, 10 new Chromium tests |
| Meaningful depth | PASS | Custom retrieval/storage, actual processes, recovery and oracles |

**Recruiter-ready for anonymous public review: FAIL until publication, licensing and new
remote checks are complete.** That does not negate the executed engineering evidence.

Provisional requested rubric: correctness 22/25; tests/CI 17/20; reproducibility 11/15;
documentation/evidence 13/15; security/hygiene 6/10; demo accessibility 6/10; presentation
3/5 = **78/100**. Scores are judgments, not measured facts. Private access, missing selected
license, pending new CI/public clone, finite security scanning and no restricted hosted demo
prevent a higher readiness score. Personal interview competence is unassessed.
