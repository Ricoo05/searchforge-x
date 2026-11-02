import math, unittest
from evaluate import metrics


class MetricsTest(unittest.TestCase):
    def test_manually_derived_two_query_example(self):
        result = metrics(
            {"q1": [{"id": "x"}, {"id": "a"}, {"id": "b"}], "q2": []},
            {"q1": {"a": 1, "b": 1}, "q2": {"c": 1}},
        )
        self.assertAlmostEqual(
            result["macro"]["nDCG@10"],
            ((1 / math.log2(3) + 1 / math.log2(4)) / (1 + 1 / math.log2(3))) / 2,
        )
        self.assertEqual(result["macro"]["Recall@100"], 0.5)
        self.assertEqual(result["macro"]["Precision@10"], 0.1)
        self.assertEqual(result["macro"]["MRR@10"], 0.25)

    def test_duplicates_rejected(self):
        with self.assertRaises(ValueError):
            metrics({"q": [{"id": "a"}, {"id": "a"}]}, {"q": {"a": 1}})


if __name__ == "__main__":
    unittest.main()
