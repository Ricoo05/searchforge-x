import unittest
from check_image_scan import findings


class ImageGateTest(unittest.TestCase):
    def test_clean_report(self):
        self.assertEqual([], findings({"SchemaVersion": 2, "Results": [{"Target": "os"}]}))

    def test_blocks_unfixed_high_and_critical(self):
        report = {
            "SchemaVersion": 2,
            "Results": [
                {
                    "Target": "jar",
                    "Vulnerabilities": [
                        {"VulnerabilityID": "high", "Severity": "HIGH"},
                        {"VulnerabilityID": "critical", "Severity": "CRITICAL"},
                        {"VulnerabilityID": "low", "Severity": "LOW"},
                    ],
                }
            ],
        }
        self.assertEqual(
            [("jar", "high", "HIGH"), ("jar", "critical", "CRITICAL")], findings(report)
        )

    def test_missing_report_fails(self):
        for report in ({}, {"SchemaVersion": 2, "Results": []}):
            with self.assertRaises(ValueError):
                findings(report)
