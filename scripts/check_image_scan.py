"""Fail closed on missing reports and high/critical Trivy findings, including unfixed CVEs."""

import json
import sys
from pathlib import Path


def findings(report):
    if report.get("SchemaVersion") != 2 or not report.get("Results"):
        raise ValueError("Expected a complete Trivy v2 image report with scan results")
    return [
        (result.get("Target"), item["VulnerabilityID"], item["Severity"])
        for result in report["Results"]
        for item in result.get("Vulnerabilities") or []
        if item["Severity"] in ("HIGH", "CRITICAL")
    ]


if __name__ == "__main__":
    blocked = findings(json.loads(Path(sys.argv[1]).read_text(encoding="utf-8")))
    for target, identifier, severity in blocked:
        print(f"{severity}: {identifier} in {target}")
    print(f"Image scan: {len(blocked)} HIGH/CRITICAL findings")
    sys.exit(bool(blocked))
