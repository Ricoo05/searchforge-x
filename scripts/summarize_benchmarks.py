import argparse, json, math, pathlib, statistics


def percentile(values, q):
    return sorted(values)[max(0, math.ceil(q * len(values)) - 1)]


def summarize(directory):
    results = []
    for size in (10000, 100000):
        for label in ("baseline", "optimized"):
            trials = [
                json.loads(path.read_text())
                for path in sorted(directory.glob(f"{label}-{size}-*.json"))
            ]
            if len(trials) != 3:
                raise ValueError(f"Require exactly three trials: {label}/{size}")
            med = lambda fn: statistics.median(fn(row) for row in trials)
            row = {
                "variant": label,
                "documents": size,
                "trials": len(trials),
                "samplesPerTrial": 350,
                "p50Ms": med(lambda r: percentile(r["queryLatenciesNanos"], 0.50) / 1e6),
                "p95Ms": med(lambda r: percentile(r["queryLatenciesNanos"], 0.95) / 1e6),
                "p99Ms": med(lambda r: percentile(r["queryLatenciesNanos"], 0.99) / 1e6),
                "queriesPerSecond": med(lambda r: r["samples"] * 1e9 / r["queryElapsedNanos"]),
                "fourThreadQueriesPerSecond": med(
                    lambda r: r["concurrentQueries"] * 1e9 / r["concurrentElapsedNanos"]
                ),
                "indexBuildMs": med(lambda r: r["indexBuildNanos"] / 1e6),
                "segmentBytes": med(lambda r: r["segmentBytes"]),
                "heapUsedAfterQueriesBytes": med(lambda r: r["heapUsedAfterQueriesBytes"]),
                "recoveryMs": med(lambda r: r["recoveryNanos"] / 1e6),
                "forcedCommitMs": med(lambda r: r["forcedCommitNanos"] / 1e6),
                "postingsCompression": {
                    codec: {
                        field: med(lambda r: r["postingsCompression"][codec][field])
                        for field in ("bytes", "encodeNanos", "decodeNanos")
                    }
                    for codec in ("fixedWidth", "deltaVarint")
                },
            }
            results.append(row)
    return {
        "aggregation": "median of three independent JVM trial statistics; nearest-rank latency percentiles",
        "results": results,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=pathlib.Path)
    args = parser.parse_args()
    report = summarize(args.directory)
    (args.directory / "summary.json").write_text(json.dumps(report, indent=2) + "\n")
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    fig, axes = plt.subplots(1, 2, figsize=(10, 4), layout="constrained")
    for variant, color in (("baseline", "#7b8a85"), ("optimized", "#245d45")):
        rows = [row for row in report["results"] if row["variant"] == variant]
        axes[0].plot(
            [row["documents"] for row in rows],
            [row["p50Ms"] for row in rows],
            marker="o",
            label=variant,
            color=color,
        )
        axes[1].plot(
            [row["documents"] for row in rows],
            [row["queriesPerSecond"] for row in rows],
            marker="o",
            label=variant,
            color=color,
        )
    for axis in axes:
        axis.set_xscale("log")
        axis.set_xticks([10000, 100000], labels=["10,000", "100,000"])
        axis.set_xlabel("Synthetic documents")
        axis.spines[["top", "right"]].set_visible(False)
        axis.grid(axis="y", alpha=0.2)
    axes[0].set_ylabel("Median query latency (ms)")
    axes[1].set_ylabel("Sequential queries / second")
    axes[0].legend(frameon=False)
    fig.suptitle(
        "SearchForge X — observed local trials\n32 tokens/document · fixed mixed workload · 3 JVM trials · no general performance claim",
        fontsize=10,
    )
    fig.savefig(args.directory / "comparison.png", dpi=180)
    fig.savefig(args.directory / "comparison.svg")
    print(json.dumps(report, indent=2))
