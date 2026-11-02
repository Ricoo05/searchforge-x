"""Read-only comparison with the pre-redesign production evidence."""

import json
from pathlib import Path
from check_ui_software_parity import post

if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    baseline = json.loads((root / "evidence/ui-software-parity.json").read_text(encoding="utf-8"))
    checks = []
    for entry in baseline["readOnlyComparisons"]:
        result = post(
            "http://127.0.0.1:8090",
            "/search",
            {"query": entry["query"], "offset": 0, "limit": 10, "timeoutMillis": 5000},
        )
        assert result == entry["result"], entry["query"]
        checks.append({"query": entry["query"], "identical": True})
    health = post("http://127.0.0.1:8090", "/diagnostics", {})
    assert health == baseline["productionDiagnostics"], "Production diagnostics changed"
    report = {
        "readOnly": True,
        "queries": checks,
        "diagnosticsIdentical": True,
        "productionDiagnostics": health,
    }
    (root / "evidence/ui-after/production-preservation.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8"
    )
    print("Original production results and generations preserved for all six queries")
