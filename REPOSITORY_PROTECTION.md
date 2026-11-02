# Repository ownership and change protection

Verified GitHub configuration for Ricoo05/searchforge-x:

- Ricoo05 is the only listed collaborator with write/admin permission.
- `.github/CODEOWNERS` names Ricoo05 for every path.
- Outside contributions require a pull request and one approving code-owner review.
- New commits dismiss stale approvals; review conversations must be resolved.
- Main requires strict passing Windows, Ubuntu, container/browser and security checks.
- Force pushes and deletion of main are disabled.
- GitHub secret scanning and secret push protection are enabled.

The owner retains administrator bypass to maintain their own project without requiring
self-approval, which GitHub does not allow. Nobody else is listed as an administrator.
Dependabot may propose upgrades in PRs; it is not configured to merge them automatically.
No strangers have been invited or granted write access. No account-level setting was changed.

Public visibility permits viewing and forking copies. MIT licensing permits modifications
to copies under its terms; it does not grant anyone GitHub write access to the original
repository. These protections control changes to this original repository and cannot
prevent copying, misuse of a compromised owner account, or authorized owner overrides.
