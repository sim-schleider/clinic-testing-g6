#!/usr/bin/env python3
"""Generates the informe appendices from what is on disk. Never edit the generated files by hand.

Usage (from the project root):
    build_appendix.py [--informe docs/informe]

Writes:
    secciones/90-apendice-resultados.md  results tables of every suite, red then green
    secciones/91-apendice-capturas.md    every screenshot in capturas/, red then green
    secciones/93-apendice-codigo.md      the test classes in src/test/java with their tags and case counts
"""
import argparse
import pathlib
import re
import sys

SUITES = [
    ("unit", "Pruebas unitarias (AsociadoDTO)"),
    ("integration", "Pruebas de integración con MySQL"),
    ("mock", "Pruebas con mocks (Ambulancia y State)"),
    ("concurrency", "Pruebas de integración y concurrencia (máquina de estados)"),
    ("all", "Todas las suites"),
]
FASES = [("rojo", "en rojo: código original"), ("verde", "en verde: código corregido")]
GENERATED = "<!-- Archivo generado por build_appendix.py: no editar a mano. -->\n\n"
TEST_ANNOTATION = re.compile(r"@(Test|ParameterizedTest|RepeatedTest)\b")
TAG = re.compile(r'@Tag\("([^"]+)"\)')


def results_appendix(informe: pathlib.Path) -> str:
    parts = [GENERATED, "# Apéndice A: Resultados de ejecución {.unnumbered}\n\n",
             "Tablas generadas a partir de los reportes XML de Maven Surefire de cada ejecución. "
             "El encabezado de cada tabla indica la fecha y el commit del código ejecutado.\n"]
    for suite, title in SUITES:
        for fase, label in FASES:
            summary = informe / "resultados" / f"{suite}-{fase}.md"
            if not summary.exists():
                continue
            lines = summary.read_text(encoding="utf-8").splitlines()
            body = [line for line in lines if not line.startswith("# Resultados")]
            parts.append(f"\n## {title}, {label} {{.unnumbered}}\n\n" + "\n".join(body).strip() + "\n")
    return "".join(parts)


def captures_appendix(informe: pathlib.Path) -> str:
    parts = [GENERATED, "# Apéndice B: Capturas de ejecución {.unnumbered}\n\n",
             "Capturas de cada suite en rojo (detección de los errores sobre el código original) y en verde "
             "(funcionamiento correcto tras las correcciones).\n"]
    captures = informe / "capturas"
    known = set()
    for suite, title in SUITES:
        for fase, label in FASES:
            png = captures / f"{suite}-{fase}.png"
            if png.exists():
                known.add(png.name)
                parts.append(f"\n![{title}, {label}](../capturas/{png.name})\n")
    others = sorted(p.name for p in captures.glob("*.png") if p.name not in known) if captures.is_dir() else []
    for name in others:
        parts.append(f"\n![{pathlib.Path(name).stem}](../capturas/{name})\n")
    return "".join(parts)


def code_appendix(root: pathlib.Path) -> str:
    rows = []
    for java in sorted((root / "src" / "test" / "java").rglob("*.java")):
        text = java.read_text(encoding="utf-8", errors="replace")
        tests = len(TEST_ANNOTATION.findall(text))
        if not tests:
            continue
        tags = ", ".join(sorted(set(TAG.findall(text)))) or "—"
        rows.append(f"| `{java.relative_to(root).as_posix()}` | {tags} | {tests} |")
    table = ["| Clase de prueba | Tags | Métodos de prueba |", "|-----------------|------|-------------------|", *rows]
    return (GENERATED + "# Apéndice D: Código fuente de las suites {.unnumbered}\n\n"
            "El código completo de las suites se entrega en el repositorio, en `src/test/java`. "
            "Los métodos parametrizados o repetidos cuentan una vez aunque generen varias ejecuciones.\n\n"
            + ("\n".join(table) if rows else "_Todavía no hay clases de prueba._") + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--informe", default="docs/informe")
    args = parser.parse_args()

    informe = pathlib.Path(args.informe)
    sections = informe / "secciones"
    if not sections.is_dir():
        print(f"No existe {sections}: inicializar el informe primero (ver SKILL.md, paso 1)", file=sys.stderr)
        return 1
    outputs = {
        "90-apendice-resultados.md": results_appendix(informe),
        "91-apendice-capturas.md": captures_appendix(informe),
        "93-apendice-codigo.md": code_appendix(pathlib.Path.cwd()),
    }
    for name, content in outputs.items():
        (sections / name).write_text(content, encoding="utf-8")
        print(sections / name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
