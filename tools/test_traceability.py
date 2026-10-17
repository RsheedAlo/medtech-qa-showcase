"""Tests for the gate itself: proves that the pipeline turns red when traceability has a gap.

Run with: python tools/test_traceability.py
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import traceability as t  # noqa: E402

REQS = {"REQ-001": "Create", "REQ-002": "Upload"}
RISKS = {
    "RISK-01": {"title": "Wrong approval", "severity": "hoch", "reqs": ["REQ-001"]},
    "RISK-02": {"title": "Bad file", "severity": "mittel", "reqs": ["REQ-002"]},
}
CLASS = "com.medtechqa.unit.FooTest"


def test(*reqs, level="unit"):
    return {"reqs": set(reqs), "display": " ".join(reqs) + " does something", "level": level}


class GateTest(unittest.TestCase):

    def run_gate(self, sources, results, risks=RISKS):
        return t.analyse(REQS, risks, sources, results)

    def test_complete_and_passing_has_no_errors(self):
        sources = {(CLASS, "a"): test("REQ-001"), (CLASS, "b"): test("REQ-002")}
        _, _, errors = self.run_gate(sources, {(CLASS, "a"): ["pass"], (CLASS, "b"): ["pass"]})
        self.assertEqual([], errors)

    def test_requirement_without_test_fails(self):
        sources = {(CLASS, "a"): test("REQ-001")}
        _, _, errors = self.run_gate(sources, {(CLASS, "a"): ["pass"]})
        self.assertIn("REQ-002 hat keinen Test", errors)

    def test_high_risk_without_tested_control_fails(self):
        sources = {(CLASS, "b"): test("REQ-002")}
        _, risk_rows, errors = self.run_gate(sources, {(CLASS, "b"): ["pass"]})
        self.assertTrue(any(e.startswith("RISK-01 (Schweregrad hoch)") for e in errors))
        self.assertEqual("LÜCKE", dict((r[0], r[4]) for r in risk_rows)["RISK-01"])

    def test_medium_risk_gap_is_only_reported_through_the_requirement(self):
        sources = {(CLASS, "a"): test("REQ-001")}
        _, _, errors = self.run_gate(sources, {(CLASS, "a"): ["pass"]})
        self.assertFalse(any(e.startswith("RISK-02") for e in errors))

    def test_failing_test_fails(self):
        sources = {(CLASS, "a"): test("REQ-001"), (CLASS, "b"): test("REQ-002")}
        rows, _, errors = self.run_gate(sources, {(CLASS, "a"): ["pass", "fail"], (CLASS, "b"): ["pass"]})
        self.assertIn("Test fehlgeschlagen: FooTest.a", errors)
        self.assertEqual("FEHLGESCHLAGEN", dict((r[0], r[4]) for r in rows)["REQ-001"])

    def test_test_without_req_tag_fails(self):
        sources = {(CLASS, "a"): test("REQ-001"), (CLASS, "b"): test("REQ-002"), (CLASS, "c"): test()}
        results = {k: ["pass"] for k in sources}
        _, _, errors = self.run_gate(sources, results)
        self.assertIn("Test ohne REQ-Tag: FooTest.c", errors)

    def test_tag_naming_unknown_requirement_fails(self):
        sources = {(CLASS, "a"): test("REQ-001"), (CLASS, "b"): test("REQ-002", "REQ-099")}
        results = {k: ["pass"] for k in sources}
        _, _, errors = self.run_gate(sources, results)
        self.assertTrue(any("REQ-099" in e for e in errors))

    def test_test_that_was_not_executed_fails(self):
        sources = {(CLASS, "a"): test("REQ-001"), (CLASS, "b"): test("REQ-002")}
        _, _, errors = self.run_gate(sources, {(CLASS, "a"): ["pass"]})
        self.assertTrue(any("wurde nicht ausgeführt" in e for e in errors))

    def test_display_name_must_name_the_tagged_requirement(self):
        wrong = {"reqs": {"REQ-001"}, "display": "no id here", "level": "unit"}
        sources = {(CLASS, "a"): wrong, (CLASS, "b"): test("REQ-002")}
        results = {k: ["pass"] for k in sources}
        _, _, errors = self.run_gate(sources, results)
        self.assertTrue(any("@DisplayName" in e for e in errors))

    def test_parsers_read_the_real_documents(self):
        root = Path(__file__).resolve().parent.parent
        reqs = t.parse_requirements((root / "docs/requirements.md").read_text(encoding="utf-8"))
        risks = t.parse_risks((root / "docs/risk-analysis.md").read_text(encoding="utf-8"))
        self.assertEqual(10, len(reqs))
        self.assertEqual(6, len(risks))
        self.assertTrue(all(r["severity"] and r["reqs"] for r in risks.values()))


if __name__ == "__main__":
    unittest.main()
