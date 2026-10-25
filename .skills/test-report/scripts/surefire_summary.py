#!/usr/bin/env python3
"""Summarizes Surefire XML reports into the Markdown results table used by the informe.

Usage:
    surefire_summary.py --suite unit --fase rojo [--reports target/surefire-reports] [--out FILE.md]

The case ID (CU3, CI12, CM4, CC7) is taken from the test name: either the method prefix
(`cu3_...`) or anywhere in a display name ("CU3 – ..."). The header records the date and the
commit, and whether production code (src/main) had uncommitted changes, so a red run on seeded
or half-fixed code can't pass as the original code.
"""
import argparse
import datetime
import pathlib
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

SUITE_TITLES = {
    "unit": "Pruebas unitarias",
    "integration": "Pruebas de integración con MySQL",
    "mock": "Pruebas con mocks",
    "concurrency": "Pruebas de integración y concurrencia",
    "all": "Todas las suites",
}
# Method names "cu3_..." or "testCu3...": the ID is the prefix, optionally after "test".
PREFIX_ID = re.compile(r"^(?:test_?)?(cu|ci|cm|cc)(\d+)(?=_|[a-z])", re.IGNORECASE)
ANYWHERE_ID = re.compile(r"\b(CU|CI|CM|CC)(\d+)\b", re.IGNORECASE)
MAX_DETAIL = 160
ICONS = {"pass": "✅", "fail": "❌", "error": "💥", "skipped": "⏭"}


def git(*args: str) -> str:
    try:
        return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return ""


def code_state() -> str:
    commit = git("rev-parse", "--short", "HEAD")
    if not commit:
        return "commit desconocido (sin repositorio git)"
    dirty = git("status", "--porcelain", "--", "src/main")
    return f"commit {commit} · " + ("src/main CON cambios sin commitear" if dirty else "src/main sin cambios")


COMMENT_ID = re.compile(r"^\s*//\s*(CU|CI|CM|CC)(\d+)\b")
METHOD = re.compile(r"\bvoid\s+(\w+)\s*\(")


def ids_from_comments(root: pathlib.Path = pathlib.Path("src/test/java")) -> dict:
    """Map (test class, method) to the case ID written in the comment right above the method."""
    ids = {}
    for path in root.rglob("*.java"):
        pending = ""
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            comment = COMMENT_ID.match(line)
            if comment:
                pending = (comment.group(1) + comment.group(2)).upper()
                continue
            method = METHOD.search(line)
            if method:
                if pending:
                    ids[(path.stem, method.group(1))] = pending
                pending = ""
    return ids


SOURCE_IDS = ids_from_comments()


def case_id(name: str, test_class: str = "") -> str:
    match = PREFIX_ID.match(name) or ANYWHERE_ID.search(name)
    if match:
        return (match.group(1) + match.group(2)).upper()
    # Method without the ID in its name: take it from the "// CU3 – ..." comment above it.
    return SOURCE_IDS.get((test_class.split("$")[0], name.split("(")[0]), "")


def clean(text: str) -> str:
    line = " ".join(text.split())
    if len(line) > MAX_DETAIL:
        line = line[:MAX_DETAIL] + "…"
    return line.replace("|", "\\|")


def classify(case: ET.Element) -> dict:
    status, detail = "pass", ""
    for tag in ("failure", "error", "skipped"):
        node = case.find(tag)
        if node is not None:
            status = "fail" if tag == "failure" else tag
            detail = node.get("message") or node.get("type") or (node.text or "").strip()
            break
    name = case.get("name", "")
    test_class = case.get("classname", "").rsplit(".", 1)[-1].replace("$", " › ")
    return {"class": test_class, "test": name, "id": case_id(name, case.get("classname", "").rsplit(".", 1)[-1]), "status": status, "detail": clean(detail)}


def natural_key(case: dict) -> tuple:
    match = re.match(r"([A-Z]+)(\d+)", case["id"])
    return (case["class"], match.group(1) if match else "~", int(match.group(2)) if match else sys.maxsize, case["test"])


def read_cases(reports: pathlib.Path) -> list:
    cases = []
    for report in sorted(reports.glob("TEST-*.xml")):
        cases.extend(classify(c) for c in ET.parse(report).getroot().iter("testcase"))
    return sorted(cases, key=natural_key)


def to_markdown(cases: list, suite: str, fase: str) -> str:
    counts = {status: sum(1 for c in cases if c["status"] == status) for status in ICONS}
    title = SUITE_TITLES.get(suite, suite) + (f" ({fase})" if fase != "dev" else "")
    lines = [
        f"# Resultados: {title}",
        "",
        f"Fecha: {datetime.datetime.now():%Y-%m-%d %H:%M}",
        f"Código: {code_state()}",
        f"Total: {len(cases)} · " + " · ".join(f"{ICONS[s]} {s}: {n}" for s, n in counts.items()),
        "",
        "| Clase | Test | Id | Resultado | Detalle |",
        "|-------|------|----|-----------|---------|",
    ]
    for c in cases:
        test = c["test"].replace("|", "\\|")
        lines.append(f"| {c['class']} | `{test}` | {c['id']} | {ICONS[c['status']]} {c['status']} | {c['detail']} |")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--reports", default="target/surefire-reports")
    parser.add_argument("--suite", required=True)
    parser.add_argument("--fase", required=True)
    parser.add_argument("--out")
    args = parser.parse_args()

    reports = pathlib.Path(args.reports)
    cases = read_cases(reports) if reports.is_dir() else []
    if not cases:
        print(f"No hay reportes TEST-*.xml en {reports}", file=sys.stderr)
        return 1

    markdown = to_markdown(cases, args.suite, args.fase)
    if args.out:
        out = pathlib.Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(markdown, encoding="utf-8")
        print(f"Resultados guardados en {out}", file=sys.stderr)
    print(markdown)
    missing = [c["test"] for c in cases if not c["id"]]
    if missing:
        print(f"Aviso: {len(missing)} test(s) sin Id de caso (CU/CI/CM/CC): {missing[:5]}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
