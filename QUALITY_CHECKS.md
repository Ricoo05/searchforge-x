# Security and quality gates

The main workflow enforces Java formatting with Spotless/google-java-format, frontend
formatting with locked Prettier, and Python formatting and fatal lint checks with locked
Ruff. Java formatting covers the engine, tests and independent Lucene reference. Raw
benchmark results and historical evidence are preserved rather than reformatted.

Reproduce the checks with Java 21, Maven, Node 24 and Python 3.12:

```sh
mvn spotless:check
cd web
npm ci
npm run format:check
npm run build
cd ..
python -m pip install -r scripts/requirements-quality.txt
python -m ruff format --check scripts
python -m ruff check scripts
python -m unittest discover -s scripts -p 'test_*.py'
mvn clean verify
```

Apply formatting with `mvn spotless:apply`, `npm run format` inside web, and
`python -m ruff format scripts`. CI checks formatting without rewriting contributions.

CodeQL analyzes Java, JavaScript/TypeScript and Python using security-extended queries
on pushes, PRs and weekly schedules. Only its analysis job receives security-events write
permission for GitHub code scanning. Actions use immutable official commit references.

The image-security job builds the actual final runtime image, runs Trivy and uploads the
complete JSON report, including low and medium findings. A separate fail-closed gate rejects
every HIGH/CRITICAL vulnerability, including those without fixes. Scanner errors fail the
job. There are no CVE suppressions or ignored unfixed findings. Weekly scans refresh advisory
coverage. This is a time-specific advisory check, not a zero-vulnerability guarantee.

Browser tests run against disposable real Compose services in Chromium, Firefox and WebKit.
They cover retrieval, ingestion, parser errors, pagination and six responsive widths.
Automated axe WCAG A/AA checks cover desktop/mobile search, results, studio and validation;
keyboard skip navigation, reduced-motion CSS and WebGL-loss fallback are also exercised.
These checks do not replace manual assistive-technology testing or certify accessibility.
Never point fixture-writing browser tests at a cluster containing valuable documents.

```sh
docker compose up --build -d --wait
cd web
npx playwright install --with-deps chromium firefox webkit
# CI sets CI=true; for a disposable local cluster set SFX_BROWSER_URL explicitly.
SFX_BROWSER_URL=http://127.0.0.1:8090 npm run test:browser
```

Executed run evidence is recorded in QUALITY_VALIDATION.md after remote completion.
The storage/distribution and deployment limits in KNOWN_LIMITATIONS.md remain applicable.
