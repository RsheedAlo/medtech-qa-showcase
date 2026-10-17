#!/usr/bin/env python3
"""Builds docs/traceability.md and fails when traceability has a gap.

Inputs
  docs/requirements.md      '## REQ-001 – Title' headings
  docs/risk-analysis.md     '## RISK-01 – Title' headings with '- **Schweregrad:** hoch' and
                            '- **Umgesetzt durch:** REQ-005, REQ-004'
  src/test/java/**.java     @Tag("REQ-xxx") on each test method; the test level is the package name
  target/surefire-reports   TEST-*.xml with the results

The link between test and requirement is read from the source (the Tag), because the Surefire XML
contains neither tags nor display names. The result is read from the XML. The two are joined by
class name and method name.

Exit code 1 if any of these holds (the report is still written, so the gap is visible):
  - a requirement has no test
  - a risk with severity 'hoch' has a control (REQ) without a passing test
  - a test fails, or a test has no REQ tag, or a tag names an unknown REQ
"""
import argparse
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

LEVELS = ["unit", "integration", "system", "acceptance"]
REQ_HEADING = re.compile(r"^##\s+(REQ-\d{3})\s+[–-]\s+(.+?)\s*$", re.M)
RISK_HEADING = re.compile(r"^##\s+(RISK-\d{2})\s+[–-]\s+(.+?)\s*$", re.M)
REQ_ID = re.compile(r"REQ-\d{3}")


def parse_requirements(text):
    return {m.group(1): m.group(2) for m in REQ_HEADING.finditer(text)}


def parse_risks(text):
    """Returns {risk_id: {title, severity, reqs}}. Missing fields stay None / empty and are reported."""
    risks = {}
    matches = list(RISK_HEADING.finditer(text))
    for i, m in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        body = text[m.end():end]
        severity = re.search(r"\*\*Schweregrad:\*\*\s*(\S+)", body)
        reqs = re.search(r"\*\*Umgesetzt durch:\*\*\s*(.+)", body)
        risks[m.group(1)] = {
            "title": m.group(2),
            "severity": severity.group(1).lower() if severity else None,
            "reqs": REQ_ID.findall(reqs.group(1)) if reqs else [],
        }
    return risks


def parse_test_sources(test_root):
    """Returns {(class_name, method): {'reqs': set, 'display': str, 'level': str}} for every test method."""
    tests = {}
    for path in sorted(Path(test_root).rglob("*.java")):
        package = class_name = None
        tags, display, is_test = set(), "", False
        for line in path.read_text(encoding="utf-8").splitlines():
            if (m := re.match(r"\s*package\s+([\w.]+);", line)):
                package = m.group(1)
            elif (m := re.match(r"\s*(?:public\s+|abstract\s+|final\s+)*class\s+(\w+)", line)) and not class_name:
                class_name = m.group(1)
            elif (m := re.search(r'@Tag\("(REQ-\d{3})"\)', line)):
                tags.add(m.group(1))
            elif (m := re.search(r'@DisplayName\("(.*)"\)', line)):
                display = m.group(1)
            elif re.match(r"\s*@(Test|ParameterizedTest)\b", line):
                is_test = True
            elif (m := re.match(r"\s*(?:public\s+|private\s+|protected\s+)?(?:static\s+)?[\w<>\[\]]+\s+(\w+)\s*\(", line)) \
                    and (is_test or tags or display) and not line.strip().startswith("@"):
                if is_test and package and class_name:
                    level = package.rsplit(".", 1)[-1]
                    tests[(f"{package}.{class_name}", m.group(1))] = {
                        "reqs": set(tags), "display": display, "level": level}
                tags, display, is_test = set(), "", False
    return tests


def parse_results(report_dir):
    """Returns {(class_name, method): [status, ...]} with status in pass/fail/skipped, one per invocation."""
    results = defaultdict(list)
    for path in sorted(Path(report_dir).glob("TEST-*.xml")):
        for case in ET.parse(path).getroot().iter("testcase"):
            method = case.get("name").split("(")[0].split("[")[0]
            if case.find("failure") is not None or case.find("error") is not None:
                status = "fail"
            elif case.find("skipped") is not None:
                status = "skipped"
            else:
                status = "pass"
            results[(case.get("classname"), method)].append(status)
    return results


def analyse(requirements, risks, sources, results):
    """Pure function: returns (rows, risk_rows, errors)."""
    errors = []
    per_req = {r: {"levels": defaultdict(int), "statuses": []} for r in requirements}

    for key, info in sources.items():
        label = f"{key[0].split('.')[-1]}.{key[1]}"
        if not info["reqs"]:
            errors.append(f"Test ohne REQ-Tag: {label}")
        for req in info["reqs"]:
            if req not in requirements:
                errors.append(f"Test {label} verweist auf unbekannte Anforderung {req}")
            elif req not in info["display"]:
                errors.append(f"Test {label}: @DisplayName nennt {req} nicht (Tag und Name müssen übereinstimmen)")
        statuses = results.get(key)
        if statuses is None:
            errors.append(f"Test {label} wurde nicht ausgeführt (kein Eintrag in den Surefire-Berichten)")
            statuses = ["skipped"]
        if "fail" in statuses:
            errors.append(f"Test fehlgeschlagen: {label}")
        for req in info["reqs"]:
            if req in per_req:
                per_req[req]["levels"][info["level"]] += len(statuses)
                per_req[req]["statuses"].extend(statuses)

    for key in results:
        if key not in sources:
            errors.append(f"Ausgeführter Test ohne Quelle gefunden: {key[0]}.{key[1]}")

    rows = []
    req_ok = {}
    for req, title in requirements.items():
        data = per_req[req]
        if not data["statuses"]:
            result = "KEIN TEST"
            errors.append(f"{req} hat keinen Test")
        elif "fail" in data["statuses"]:
            result = "FEHLGESCHLAGEN"
        elif "skipped" in data["statuses"]:
            result = "ÜBERSPRUNGEN"
            errors.append(f"{req} hat übersprungene Tests")
        else:
            result = "BESTANDEN"
        req_ok[req] = result == "BESTANDEN"
        tests = ", ".join(f"{lvl} ({data['levels'][lvl]})" for lvl in LEVELS if data["levels"][lvl])
        linked = [rid for rid, r in risks.items() if req in r["reqs"]]
        rows.append((req, title, ", ".join(linked) or "–", tests or "–", result))

    risk_rows = []
    for rid, risk in risks.items():
        if risk["severity"] is None or not risk["reqs"]:
            errors.append(f"{rid}: Schweregrad oder 'Umgesetzt durch' fehlt in risk-analysis.md")
        for req in risk["reqs"]:
            if req not in requirements:
                errors.append(f"{rid} verweist auf unbekannte Anforderung {req}")
        verified = bool(risk["reqs"]) and all(req_ok.get(req, False) for req in risk["reqs"])
        if risk["severity"] == "hoch" and not verified:
            errors.append(f"{rid} (Schweregrad hoch) hat keine vollständig getestete Maßnahme: "
                          + ", ".join(f"{r}={'ok' if req_ok.get(r) else 'Lücke'}" for r in risk["reqs"]))
        risk_rows.append((rid, risk["title"], risk["severity"] or "?", ", ".join(risk["reqs"]) or "–",
                          "getestet" if verified else "LÜCKE"))
    return rows, risk_rows, errors


def render(rows, risk_rows, errors):
    out = ["# Traceability-Matrix", "",
           "Automatisch erzeugt von `tools/traceability.py`. Nicht von Hand ändern.", "",
           "## Anforderungen", "",
           "| REQ-ID | Kurzbeschreibung | Risiken | Tests (Ebene) | Ergebnis |", "|---|---|---|---|---|"]
    out += [f"| {a} | {b} | {c} | {d} | {e} |" for a, b, c, d, e in rows]
    out += ["", "## Risiken", "",
            "Ein Risiko gilt als getestet, wenn alle Anforderungen seiner Risikokontrolle mindestens einen Test haben und alle bestehen.", "",
            "| Risiko | Titel | Schweregrad | Maßnahme (REQ) | Status |", "|---|---|---|---|---|"]
    out += [f"| {a} | {b} | {c} | {d} | {e} |" for a, b, c, d, e in risk_rows]
    out += ["", "## Prüfergebnis", ""]
    out += [f"- FEHLER: {e}" for e in errors] if errors else ["Keine Lücken gefunden."]
    return "\n".join(out) + "\n"


def main(argv=None):
    root = Path(__file__).resolve().parent.parent
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--requirements", default=root / "docs/requirements.md")
    p.add_argument("--risks", default=root / "docs/risk-analysis.md")
    p.add_argument("--tests", default=root / "src/test/java")
    p.add_argument("--reports", default=root / "target/surefire-reports")
    p.add_argument("--out", default=root / "docs/traceability.md")
    args = p.parse_args(argv)

    requirements = parse_requirements(Path(args.requirements).read_text(encoding="utf-8"))
    risks = parse_risks(Path(args.risks).read_text(encoding="utf-8"))
    errors = []
    if not requirements:
        errors.append("Keine Anforderungen in requirements.md gefunden")
    if not Path(args.reports).is_dir() or not list(Path(args.reports).glob("TEST-*.xml")):
        errors.append(f"Keine Testergebnisse unter {args.reports} (erst 'mvnw test' ausführen)")
    rows, risk_rows, more = analyse(requirements, risks, parse_test_sources(args.tests),
                                    parse_results(args.reports) if not errors else {})
    errors += more
    Path(args.out).write_text(render(rows, risk_rows, errors), encoding="utf-8", newline="\n")
    print(f"{args.out} geschrieben: {len(rows)} Anforderungen, {len(risk_rows)} Risiken, {len(errors)} Fehler")
    for e in errors:
        print(f"FEHLER: {e}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
