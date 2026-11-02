"""Reconstruct BEIR SciFact test input from official corpus/claims and pinned BEIR qrels."""

import argparse, csv, hashlib, io, json, pathlib, tarfile, urllib.request

CORPUS_URL = "https://scifact.s3-us-west-2.amazonaws.com/release/latest/data.tar.gz"
CORPUS_SHA = "11c621288d41ac144d29b13b0f8503b3820b7d6e8b1f6ff24dff335c196d76be"
QRELS_URL = "https://huggingface.co/datasets/BeIR/scifact-qrels/resolve/2938d17dc3b09882fdb8c12bbbe2e2dc0e75a029/test.tsv"
QRELS_SHA = "0864bb985e0ca2367ba217977e72004d549054b2b06666ed9d4825ac7c21284c"


def checked(path, url, checksum):
    if not path.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            path.write_bytes(response.read())
    if hashlib.sha256(path.read_bytes()).hexdigest() != checksum:
        raise ValueError(
            f"Dataset hash mismatch: {path}; retain file and investigate upstream change"
        )


def prepare(output, cache):
    output.mkdir(parents=True, exist_ok=True)
    cache.mkdir(parents=True, exist_ok=True)
    archive = cache / "scifact-original.tar.gz"
    qrels = cache / "scifact-test.tsv"
    checked(archive, CORPUS_URL, CORPUS_SHA)
    checked(qrels, QRELS_URL, QRELS_SHA)
    with tarfile.open(archive) as data:
        corpus = [json.loads(line) for line in data.extractfile("data/corpus.jsonl")]
        claims = [json.loads(line) for line in data.extractfile("data/claims_dev.jsonl")]
    rows = list(csv.DictReader(io.StringIO(qrels.read_text()), delimiter="\t"))
    wanted = {row["query-id"] for row in rows}
    ids = {str(row["doc_id"]) for row in corpus}
    queries = [
        {"_id": str(row["id"]), "text": row["claim"]} for row in claims if str(row["id"]) in wanted
    ]
    assert len(corpus) == 5183 and len(queries) == len(wanted) == 300
    assert all(row["corpus-id"] in ids for row in rows)
    for name, values in (
        (
            "corpus.jsonl",
            (
                {
                    "_id": str(row["doc_id"]),
                    "title": row["title"],
                    "text": " ".join(row["abstract"]),
                }
                for row in corpus
            ),
        ),
        ("queries.jsonl", queries),
    ):
        with (output / name).open("w", encoding="utf-8") as stream:
            for row in values:
                stream.write(json.dumps(row, ensure_ascii=False) + "\n")
    (output / "test.tsv").write_bytes(qrels.read_bytes())
    provenance = {
        "corpus_url": CORPUS_URL,
        "corpus_sha256": CORPUS_SHA,
        "qrels_url": QRELS_URL,
        "qrels_sha256": QRELS_SHA,
        "documents": len(corpus),
        "queries": len(queries),
        "judgments": len(rows),
        "claims_license": "CC BY 4.0",
        "abstracts_license": "ODC-By 1.0",
        "license_source": "https://github.com/allenai/scifact/blob/master/LICENSE.md",
        "attribution": "Wadden et al., Fact or Fiction: Verifying Scientific Claims (EMNLP 2020); Thakur et al., BEIR (NeurIPS 2021)",
        "transform": "official corpus title + joined abstract; official dev claims filtered to all pinned BEIR test query IDs; BEIR qrels unchanged",
    }
    (output / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    print(json.dumps(provenance, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    parser.add_argument("--cache", type=pathlib.Path, required=True)
    args = parser.parse_args()
    prepare(args.output, args.cache)
