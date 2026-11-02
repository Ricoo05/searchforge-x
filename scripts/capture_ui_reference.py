"""Capture current expected API rows before a repeat of the isolated browser checks."""

import json
from pathlib import Path
from check_ui_software_parity import post

if __name__ == "__main__":
    origin = "http://127.0.0.1:8290"
    result = post(
        origin,
        "/search",
        {"query": "parityfixture20261102", "offset": 0, "limit": 100, "timeoutMillis": 5000},
    )
    assert result["status"] == "COMPLETE" and result["totalHits"] == 25, (
        "Seed the isolated fixtures first"
    )
    destination = Path(__file__).resolve().parents[1] / "evidence/ui-after/browser-reference.json"
    destination.write_text(
        json.dumps({"origin": origin, "fixtureResult": result}, indent=2, ensure_ascii=False)
        + "\n",
        encoding="utf-8",
    )
    print("Captured 25 expected rows from isolated backend")
