# Dataset provenance and permissions

The synthetic benchmark generator in BenchmarkMain.java and the six UI example documents
are original project material produced with AI assistance. The synthetic corpus uses seed
20261102, 32 tokens/document, and a skewed 512-term vocabulary plus two common terms.
It models simple posting workloads; it does not approximate an Internet-scale corpus.

Relevance evaluation uses SciFact corpus/claims from the authors' official archive and
unaltered BEIR SciFact test judgments. The standard BEIR archive host failed DNS resolution
in this environment, so `scripts/prepare_scifact.py` joins equivalent official inputs:

- Corpus/claims: [official SciFact repository](https://github.com/allenai/scifact), archive
  `https://scifact.s3-us-west-2.amazonaws.com/release/latest/data.tar.gz`.
- Qrels: [BEIR SciFact judgments](https://huggingface.co/datasets/BeIR/scifact-qrels), pinned
  revision `2938d17dc3b09882fdb8c12bbbe2e2dc0e75a029`, test.tsv.
- [Authors' license statement](https://github.com/allenai/scifact/blob/master/LICENSE.md):
  claims/evidence annotations are CC BY 4.0; abstracts are under ODC-By 1.0.
- Attribution: David Wadden and coauthors, *Fact or Fiction: Verifying Scientific Claims*,
  EMNLP 2020; Nandan Thakur and coauthors, *BEIR*, NeurIPS 2021; Semantic Scholar S2ORC
  for the scientific abstracts. Retain upstream attribution when redistributing data.

The preparation script pins SHA-256 hashes, checks all 5,183 corpus IDs, selects all
300 official dev claims referenced by the BEIR test split and validates all 339 judgments.
Input text is title + one space + abstract sentences joined with spaces. The qrels are
not inferred from empty/nonempty evidence: they come directly from the pinned BEIR file.
Natural-language queries bypass Boolean grammar and use analyzed distinct bag-of-words.

Hashes, counts and the transformation are in `evidence/scifact-provenance.json`. Data is
downloaded into workspace work/ and is not committed. Retrieval runs and aggregate metrics
are committed. No websites were scraped and no paid service was used.

Metadata on mirrors can differ from the authors' component-specific licenses. This
evaluation records the official source statement and does not treat a mirror's generic
dataset-card label as permission to remove original attribution.
