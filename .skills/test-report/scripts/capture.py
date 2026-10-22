#!/usr/bin/env python3
"""Renders a results table (output of surefire_summary.py) as a JUnit-style panel and screenshots it.

Usage:
    capture.py docs/informe/resultados/unit-rojo.md --out docs/informe/capturas/unit-rojo.png [--html-only]

The PNG is a faithful rendering of the Surefire XML of a real run: same tests, same outcomes, same
failure messages, plus the date and commit from the summary header. Screenshots taken from the IDE
are equally valid: save them in docs/informe/capturas/ with the same <suite>-<fase>.png name.

Needs a Chromium-family browser: $CHROME, chromium/google-chrome in PATH, or Playwright's build in
~/.cache/ms-playwright.
"""
import argparse
import glob
import html
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile

ROW = re.compile(r"^\| (?P<cls>[^|]+) \| `(?P<test>(?:[^`])+)` \| (?P<id>[^|]*) \| (?P<icon>\S+) (?P<status>\w+) \| (?P<detail>.*) \|$")
GREEN, RED, GREY = "#2e9e4f", "#d64545", "#8a8f98"
WIDTH = 1200
STYLE = """
body { font-family: 'Segoe UI', Roboto, 'DejaVu Sans', Arial, sans-serif; background: #f4f5f7; margin: 0; padding: 20px; color: #1f2328; }
.panel { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; overflow: hidden; }
.head { padding: 12px 16px; border-bottom: 1px solid #d0d7de; }
.head h1 { font-size: 16px; margin: 0 0 4px 0; }
.meta { font-size: 12px; color: #57606a; }
.bar { height: 20px; }
.total { padding: 8px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid #d0d7de; }
table { border-collapse: collapse; width: 100%; font-size: 12.5px; }
td { padding: 3px 10px; border-bottom: 1px solid #eef0f2; vertical-align: top; }
td.icon { width: 16px; font-weight: 700; text-align: center; }
td.id { width: 60px; color: #57606a; white-space: nowrap; }
td.test { font-family: Consolas, 'DejaVu Sans Mono', monospace; }
tr.bad td.test { color: #b42318; font-weight: 600; }
.detail { color: #b42318; font-size: 11.5px; font-weight: 400; margin-top: 2px; }
tr.group td { background: #f6f8fa; font-weight: 600; padding-top: 5px; }
"""


def parse(summary: pathlib.Path) -> dict:
    data = {"title": summary.stem, "meta": [], "rows": []}
    for line in summary.read_text(encoding="utf-8").splitlines():
        if line.startswith("# Resultados: "):
            data["title"] = line[len("# Resultados: "):]
        elif line.startswith(("Fecha:", "Código:")):
            data["meta"].append(line)
        elif (row := ROW.match(line)) and row.group("cls").strip() != "Clase":
            data["rows"].append({k: v.strip() for k, v in row.groupdict().items()})
    return data


def render(data: dict) -> str:
    rows = data["rows"]
    passed = sum(r["status"] == "pass" for r in rows)
    skipped = sum(r["status"] == "skipped" for r in rows)
    failed = len(rows) - passed - skipped
    color = RED if failed else GREEN
    total = (f"✗ Tests fallidos: {failed} · pasados: {passed} de {len(rows)}" if failed
             else f"✓ Tests pasados: {passed} de {len(rows)}")
    if skipped:
        total += f" · omitidos: {skipped}"

    body, current = [], None
    for r in rows:
        if r["cls"] != current:
            current = r["cls"]
            body.append(f'<tr class="group"><td colspan="3">{html.escape(current)}</td></tr>')
        icon, icon_color = {"pass": ("✓", GREEN), "skipped": ("–", GREY)}.get(r["status"], ("✗", RED))
        detail = r["detail"].replace("\\|", "|")
        detail_html = f'<div class="detail">{html.escape(detail)}</div>' if detail else ""
        test = r["test"].replace("\\|", "|")
        body.append(f'<tr class="{"ok" if r["status"] == "pass" else "bad"}">'
                    f'<td class="icon" style="color:{icon_color}">{icon}</td>'
                    f'<td class="test">{html.escape(test)}{detail_html}</td>'
                    f'<td class="id">{html.escape(r["id"])}</td></tr>')

    meta = " · ".join(html.escape(m) for m in data["meta"])
    return (f'<!doctype html><html lang="es"><head><meta charset="utf-8"><title>{html.escape(data["title"])}</title>'
            f'<style>{STYLE}</style></head><body><div class="panel">'
            f'<div class="head"><h1>{html.escape(data["title"])}</h1><div class="meta">JUnit 5 · Maven Surefire · {meta}</div></div>'
            f'<div class="bar" style="background:{color}"></div>'
            f'<div class="total" style="color:{color}">{total}</div>'
            f'<table>{"".join(body)}</table></div></body></html>\n')


def find_chrome() -> str | None:
    candidates = [os.environ.get("CHROME", "")]
    candidates += [shutil.which(n) or "" for n in ("chromium", "chromium-browser", "google-chrome", "google-chrome-stable")]
    home = pathlib.Path.home() / ".cache" / "ms-playwright"
    candidates += sorted(glob.glob(str(home / "chromium_headless_shell-*" / "chrome-headless-shell-linux64" / "chrome-headless-shell")), reverse=True)
    candidates += sorted(glob.glob(str(home / "chromium-*" / "chrome-linux64" / "chrome")), reverse=True)
    return next((c for c in candidates if c and os.access(c, os.X_OK)), None)


def estimated_height(data: dict) -> int:
    groups = len({r["cls"] for r in data["rows"]})
    details = sum(1 for r in data["rows"] if r["detail"])
    return 150 + 24 * (len(data["rows"]) + groups) + 18 * details


def screenshot(html_file: pathlib.Path, png: pathlib.Path, height: int) -> None:
    chrome = find_chrome()
    if not chrome:
        raise RuntimeError("No se encontró Chromium/Chrome (definir $CHROME)")
    png.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([chrome, "--headless", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
                    f"--window-size={WIDTH},{height}", f"--screenshot={png.resolve()}",
                    html_file.resolve().as_uri()], check=True, capture_output=True, timeout=120)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("summary")
    parser.add_argument("--out", required=True, help="PNG de salida (o .html con --html-only)")
    parser.add_argument("--html-only", action="store_true")
    args = parser.parse_args()

    data = parse(pathlib.Path(args.summary))
    if not data["rows"]:
        print(f"Sin filas de resultados en {args.summary}", file=sys.stderr)
        return 1
    page = render(data)
    out = pathlib.Path(args.out)
    if args.html_only:
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(page, encoding="utf-8")
        print(out)
        return 0
    with tempfile.TemporaryDirectory() as tmp:
        html_file = pathlib.Path(tmp) / "panel.html"
        html_file.write_text(page, encoding="utf-8")
        try:
            screenshot(html_file, out, estimated_height(data))
        except (RuntimeError, subprocess.SubprocessError) as error:
            print(f"No se pudo capturar: {error}", file=sys.stderr)
            return 1
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
