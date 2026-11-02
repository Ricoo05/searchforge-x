"""Macro-average standard binary relevance metrics. Unjudged documents count nonrelevant."""
import argparse, csv, json, math, pathlib, statistics

def metrics(run, judgments):
    per_query = {}
    for query, relevant in judgments.items():
        hits = [str(hit["id"]) for hit in run.get(query, [])]
        if len(hits) != len(set(hits)): raise ValueError("Duplicate run document")
        relevant = {doc for doc, grade in relevant.items() if grade > 0}
        if not relevant: raise ValueError("Query has no positive judgment")
        gains = [int(doc in relevant) for doc in hits]
        dcg = sum(gain / math.log2(rank + 2) for rank, gain in enumerate(gains[:10]))
        ideal = sum(1 / math.log2(rank + 2) for rank in range(min(10, len(relevant))))
        per_query[query] = {"nDCG@10": dcg / ideal, "Recall@10": sum(gains[:10]) / len(relevant),
                            "Recall@100": sum(gains[:100]) / len(relevant), "Precision@10": sum(gains[:10]) / 10,
                            "MRR@10": next((1 / (i + 1) for i, gain in enumerate(gains[:10]) if gain), 0)}
    return {"queries": len(per_query), "macro": {key: statistics.mean(row[key] for row in per_query.values()) for key in next(iter(per_query.values()))}, "perQuery": per_query}

if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("run", type=pathlib.Path); parser.add_argument("qrels", type=pathlib.Path); parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args(); judgments = {}
    for row in csv.DictReader(args.qrels.open(), delimiter="\t"):
        grade = int(row["score"])
        if grade not in (0, 1): raise ValueError("This evaluator currently supports binary judgments only")
        judgments.setdefault(row["query-id"], {})[row["corpus-id"]] = grade
    result = metrics(json.loads(args.run.read_text()), judgments)
    args.output.write_text(json.dumps(result, indent=2) + "\n"); print(json.dumps(result["macro"], indent=2))
