# Clean-clone verification

Authenticated clean checkout: `git clone https://github.com/Ricoo05/searchforge-x.git
work/portfolio-clean-clone`, revision 8861c82f5aa04319ce45d927a88a1750c800ecfd.
This directory contains no original workspace source or application data.

Java 21/Maven 3.9.9/Node 24 were supplied explicitly as prerequisites. Maven used an ordinary
dependency cache. `mvn --batch-mode --no-transfer-progress clean verify` passed all 29 tests
with zero skips. Python metric tests are separately exercised in the baseline remote jobs.

Normal `npm ci` encountered a registry download stall for a Three.js type-tooling dependency;
offline retry correctly failed ENOTCACHED for @dimforge/rapier3d-compat 0.12.0. This is not
reported as a clean first-attempt install. A mirror tarball is checked against the committed
official SHA-512 integrity before cache priming; an offline retry/build result is recorded
in evidence/portfolio/clean-clone.txt when completed. No dependency files are copied from
the working project's node_modules. The application source and lockfile are unchanged.

The baseline remote Ubuntu/Windows checkouts and Docker build also rebuilt from GitHub with
fresh dependency resolution. That evidence covers the baseline revision, not unpublished
portfolio additions. Anonymous clone/public-access and a clean clone of the final public
revision remain BLOCKED until publication approval. Do not label those checks PASS.
