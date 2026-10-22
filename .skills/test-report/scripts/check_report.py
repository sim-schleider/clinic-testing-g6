#!/usr/bin/env python3
"""Checks the informe against the assignment's "Criterios de Entrega" and its own traceability.

Usage (from the project root):
    check_report.py [--informe docs/informe]

✅ ok · ⚠ worth a look · ❌ blocks the delivery (exit code 1 if any).
"""
import argparse
import pathlib
import re
import sys

SUITES = {"unit": "CU", "integration": "CI", "mock": "CM", "concurrency": "CC"}
SECTION_OF = {"unit": "03-unitarias.md", "integration": "04-integracion-bd.md",
              "mock": "05-mocks.md", "concurrency": "06-concurrencia.md"}
REQUIRED = ["01-introduccion.md", "02-estrategia.md", *SECTION_OF.values(), "07-defectos.md",
            "08-conclusiones.md", "92-apendice-entorno.md"]
GENERATED = ["90-apendice-resultados.md", "91-apendice-capturas.md", "93-apendice-codigo.md"]
CASE_ROW = re.compile(r"^\|\s*(C[UIMC]\d+)\s*\|", re.MULTILINE)
DEFECT_ROW = re.compile(r"^\|\s*(D\d+)\s*\|(.*)\|\s*$", re.MULTILINE)
RESULT_ROW = re.compile(r"^\| [^|]+ \| `.+` \| (?P<id>[^|]*) \| \S+ (?P<status>\w+) \|", re.MULTILINE)
PLACEHOLDER = re.compile(r"TODO|\{\{[^}]*\}\}")
IMAGE = re.compile(r"!\[[^\]]*\]\(([^)\s]+)")
CASE_REF = re.compile(r"\bC[UIMC]\d+\b")
DEFECT_REF = re.compile(r"\bD\d+\b")


class Report:
    def __init__(self):
        self.lines, self.blocking = [], 0

    def add(self, level: str, text: str):
        self.lines.append(f"  {level} {text}")
        self.blocking += level == "❌"

    def title(self, text: str):
        self.lines.append(f"\n{text}")


def read(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def words_after(text: str, heading: str) -> int:
    match = re.search(rf"^##\s+{heading}\s*$(.*?)(?=^#{{1,2}}\s|\Z)", text, re.MULTILINE | re.DOTALL)
    return len(re.findall(r"\w+", match.group(1))) if match else -1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--informe", default="docs/informe")
    args = parser.parse_args()
    informe = pathlib.Path(args.informe)
    sections = informe / "secciones"
    r = Report()

    r.title("Estructura")
    for name in REQUIRED:
        r.add("✅" if (sections / name).exists() else "❌", f"secciones/{name}")
    r.add("✅" if (informe / "caratula.html").exists() else "❌", "caratula.html")
    for name in GENERATED:
        if not (sections / name).exists():
            r.add("⚠", f"secciones/{name} no generado: correr build_appendix.py")
    texts = {p.name: read(p) for p in sorted(sections.glob("*.md"))} if sections.is_dir() else {}
    pending = [n for n, t in {**texts, "caratula.html": read(informe / "caratula.html")}.items() if PLACEHOLDER.search(t)]
    r.add("❌" if pending else "✅", "marcadores TODO/{{…}} sin completar: " + ", ".join(pending) if pending else "sin marcadores pendientes")

    r.title("Código fuente de las suites")
    tests_dir = pathlib.Path("src/test/java")
    java = "\n".join(read(p) for p in tests_dir.rglob("*.java")) if tests_dir.is_dir() else ""
    for suite in SUITES:
        r.add("✅" if f'@Tag("{suite}")' in java else "❌", f'clases con @Tag("{suite}")')

    r.title("Diseño de escenarios y casos (clases de equivalencia y valores límite)")
    unit = texts.get("03-unitarias.md", "")
    has_partition = "Clases válidas" in unit and "Clases inválidas" in unit and "Valores límite" in unit
    r.add("✅" if has_partition else "❌", "tabla de clases de equivalencia y valores límite en 03-unitarias.md")
    designed = {}
    for suite, section in SECTION_OF.items():
        ids = set(CASE_ROW.findall(texts.get(section, "")))
        designed[suite] = ids
        r.add("✅" if ids else "❌", f"{section}: {len(ids)} casos {SUITES[suite]} en tablas")
        if section in texts and "Escenario" not in texts[section] and "escenario" not in texts[section]:
            r.add("⚠", f"{section}: no menciona escenarios")

    r.title("Resultados y capturas en verde y en rojo")
    executed = {}
    for suite in SUITES:
        for fase in ("rojo", "verde"):
            summary = informe / "resultados" / f"{suite}-{fase}.md"
            png = informe / "capturas" / f"{suite}-{fase}.png"
            text = read(summary)
            if not text:
                r.add("❌", f"falta resultados/{suite}-{fase}.md (run_tests.sh {suite} {fase})")
                continue
            rows = [m.groupdict() for m in RESULT_ROW.finditer(text)]
            failing = [x for x in rows if x["status"] in ("fail", "error")]
            executed.setdefault(suite, {})[fase] = rows
            if "CON cambios sin commitear" in text:
                r.add("❌", f"{suite}-{fase}: se ejecutó con cambios sin commitear en src/main (¿falla sembrada?)")
            if fase == "rojo" and not failing:
                r.add("⚠", f"{suite}-rojo no tiene tests fallidos: no demuestra detección de errores")
            if fase == "verde" and failing:
                ids = sorted({x["id"] or "?" for x in failing})
                r.add("⚠", f"{suite}-verde tiene {len(failing)} fallidos ({', '.join(ids)}): justificarlos en 07-defectos.md")
            r.add("✅" if png.exists() else "❌", f"capturas/{suite}-{fase}.png")

    r.title("Trazabilidad diseño ↔ ejecución")
    for suite, ids in designed.items():
        ran = {x["id"] for rows in executed.get(suite, {}).values() for x in rows if x["id"]}
        if not ran:
            continue
        missing, extra = sorted(ids - ran), sorted(ran - ids)
        r.add("⚠" if missing else "✅", f"{SUITES[suite]}: casos diseñados sin ejecutar: {', '.join(missing) or 'ninguno'}")
        r.add("⚠" if extra else "✅", f"{SUITES[suite]}: tests ejecutados sin fila en el diseño: {', '.join(extra) or 'ninguno'}")
        no_id = sum(1 for rows in executed[suite].values() for x in rows if not x["id"])
        if no_id:
            r.add("⚠", f"{SUITES[suite]}: {no_id} test(s) sin Id de caso en el nombre")

    r.title("Registro de defectos")
    registry = texts.get("07-defectos.md", "")
    defects = {m.group(1): [c.strip() for c in m.group(2).split("|")] for m in DEFECT_ROW.finditer(registry)}
    r.add("✅" if defects else "⚠", f"{len(defects)} defectos registrados")
    all_designed = set().union(*designed.values()) if designed else set()
    for did, cols in defects.items():
        detected_by = set(CASE_REF.findall(cols[3])) if len(cols) > 3 else set()
        if not detected_by:
            r.add("⚠", f"{did}: sin caso en 'Detectado por'")
        elif detected_by - all_designed:
            r.add("⚠", f"{did}: 'Detectado por' cita casos inexistentes: {', '.join(sorted(detected_by - all_designed))}")
        if not cols or not cols[-1]:
            r.add("❌", f"{did}: sin Estado (Abierto / Corregido (commit) / No se corrige: motivo)")
    cited = {d for name, t in texts.items() if name != "07-defectos.md" for d in DEFECT_REF.findall(t)}
    for did in sorted(cited - set(defects)):
        r.add("⚠", f"{did} se cita en el informe pero no está en el registro")

    r.title("Conclusiones")
    for section in SECTION_OF.values():
        n = words_after(texts.get(section, ""), "Conclusiones")
        r.add("✅" if n >= 40 else "❌", f"{section}: conclusiones ({max(n, 0)} palabras)")
    general = len(re.findall(r"\w+", texts.get("08-conclusiones.md", "")))
    r.add("✅" if general >= 120 else "❌", f"08-conclusiones.md ({general} palabras)")

    r.title("Imágenes referenciadas")
    broken = [f"{name}: {src}" for name, t in texts.items() for src in IMAGE.findall(t)
              if not src.startswith("http") and not (sections / src).exists()]
    r.add("❌" if broken else "✅", "imágenes inexistentes: " + "; ".join(broken) if broken else "todas las imágenes existen")

    print("\n".join(r.lines).lstrip("\n"))
    print(f"\n{'❌ Hay ' + str(r.blocking) + ' problema(s) que bloquean la entrega.' if r.blocking else '✅ Sin problemas bloqueantes.'}")
    return 1 if r.blocking else 0


if __name__ == "__main__":
    sys.exit(main())
