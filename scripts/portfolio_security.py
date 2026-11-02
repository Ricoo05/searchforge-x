"""Fail-closed dependency advisory and reachable-history credential-pattern checks.

Findings print locations/IDs only, never matching secret values. This is a bounded
pattern scan, not a security certification. OSV receives only public package coordinates.
"""

import argparse
import json
from pathlib import Path
import re
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def secrets():
    patterns = [
        rb"gh[pousr]_[A-Za-z0-9]{30,}",
        rb"github_pat_[A-Za-z0-9_]{40,}",
        rb"sk-(?:proj-)?[A-Za-z0-9_-]{40,}",
        rb"AKIA[0-9A-Z]{16}",
        rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
    ]
    rows = subprocess.check_output(["git", "rev-list", "--objects", "--all"], cwd=ROOT).splitlines()
    findings = []
    blobs = 0
    for row in rows:
        oid = row.split(b" ", 1)[0].decode("ascii")
        if subprocess.check_output(["git", "cat-file", "-t", oid], cwd=ROOT).strip() != b"blob":
            continue
        blobs += 1
        data = subprocess.check_output(["git", "cat-file", "blob", oid], cwd=ROOT)
        if any(re.search(pattern, data) for pattern in patterns):
            findings.append({"object": oid})
    paths = (
        subprocess.check_output(
            ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=ROOT
        )
        .decode()
        .split("\0")
    )
    for relative in filter(None, paths):
        file = ROOT / relative
        if file.is_file() and any(re.search(pattern, file.read_bytes()) for pattern in patterns):
            findings.append({"path": relative})
    print(json.dumps({"historyBlobs": blobs, "currentFiles": len(paths) - 1, "findings": findings}))
    if findings:
        raise SystemExit(1)


def dependencies(file):
    packages = []
    for line in Path(file).read_text().splitlines():
        match = re.match(
            r"\s*([^ :]+):([^ :]+):jar:([^ :]+):(compile|runtime|test|provided)\b", line
        )
        if match:
            group, artifact, version, _scope = match.groups()
            packages.append(
                {
                    "package": {"ecosystem": "Maven", "name": group + ":" + artifact},
                    "version": version,
                }
            )
    if not packages:
        raise SystemExit(
            "No resolved dependencies; run the documented Maven dependency:list command first"
        )
    request = urllib.request.Request(
        "https://api.osv.dev/v1/querybatch",
        data=json.dumps({"queries": packages}).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        results = json.load(response)["results"]
    if len(results) != len(packages):
        raise SystemExit("Incomplete advisory response")
    findings = [
        {"package": package, "advisories": result["vulns"]}
        for package, result in zip(packages, results)
        if result.get("vulns")
    ]
    print(json.dumps({"packages": packages, "findings": findings}, indent=2))
    if findings:
        raise SystemExit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("check", choices=["secrets", "dependencies"])
    parser.add_argument("--resolved", default=str(ROOT / "target/dependency-audit.txt"))
    args = parser.parse_args()
    if args.check == "secrets":
        secrets()
    else:
        dependencies(args.resolved)
