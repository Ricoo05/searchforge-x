"""HTTP smoke probe for a disposable Compose cluster in CI; --seed writes three test docs."""

import argparse
import json
import time
import urllib.request


def post(path, body):
    request = urllib.request.Request(
        "http://127.0.0.1:8090" + path,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=12) as response:
        return json.load(response)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", action="store_true")
    parser.add_argument("--missing-shard", type=int)
    args = parser.parse_args()
    expected_workers = 3 if args.missing_shard is None else 2
    deadline = time.monotonic() + 90
    while True:
        try:
            health = post("/diagnostics", {})
            assert sum(worker["status"] == "ready" for worker in health) == expected_workers
            break
        except Exception:
            if time.monotonic() >= deadline:
                raise
            time.sleep(1)
    if args.seed:
        result = post(
            "/ingest",
            {
                "upserts": [
                    {
                        "id": value,
                        "text": "containerprobe20261102 search recovery",
                        "metadata": {"title": f"Container document {value}"},
                    }
                    for value in [1, 2, 3]
                ],
                "deletes": [],
            },
        )
        assert result["status"] == "COMPLETE", result
    result = post(
        "/search",
        {"query": "containerprobe20261102", "offset": 0, "limit": 10, "timeoutMillis": 5000},
    )
    expected_ids = [value for value in [1, 2, 3] if value % 3 != args.missing_shard]
    assert result["status"] == ("COMPLETE" if args.missing_shard is None else "PARTIAL"), result
    assert sorted(hit["id"] for hit in result["hits"]) == expected_ids, result
    assert result["missingShards"] == (
        [] if args.missing_shard is None else [args.missing_shard]
    ), result
    with urllib.request.urlopen("http://127.0.0.1:8090/", timeout=10) as response:
        assert b'<div id="root">' in response.read(), "Built frontend not served"
    print(
        json.dumps(
            {
                "workers": expected_workers,
                "status": result["status"],
                "ids": expected_ids,
                "frontend": "served",
            }
        )
    )
