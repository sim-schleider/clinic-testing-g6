#!/usr/bin/env python3
# ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. Ver sync_common.py.
"""Convierte un resumen de surefire_summary.py (.md) en una página HTML estilo panel de JUnit, para
tomar las capturas de pantalla en verde o en rojo que pide la cátedra.

Uso:
    python3 -I results_html.py RESUMEN.md [RESUMEN2.md ...] --salida-dir DIR

Por cada resumen genera DIR/<nombre>.html. Para la captura: abrir el HTML en un navegador
(o con Playwright) y guardar un PNG de la página completa.
"""
import argparse
import html
import pathlib
import re
import sys

PATRON_FILA = re.compile(r"^\| (?P<clase>[^|]+) \| `(?P<test>.+)` \| (?P<id>[^|]*) \| (?P<icono>\S+) (?P<estado>\w+) \| (?P<detalle>.*) \|$")
PATRON_TITULO = re.compile(r"^# Resultados: (?P<titulo>.+)$")
PATRON_FECHA = re.compile(r"^Fecha: (?P<fecha>[0-9-]+ [0-9:]+)")
COLOR_VERDE = "#2e9e4f"
COLOR_ROJO = "#d64545"
COLOR_GRIS = "#8a8f98"
ESTADO_PASS = "pass"
ESTADO_SKIP = "skipped"

ESTILO = """
body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; background: #f4f5f7; margin: 0; padding: 24px; color: #1f2328; }
.panel { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; max-width: 1180px; margin: 0 auto; overflow: hidden; }
.cabecera { padding: 14px 18px; border-bottom: 1px solid #d0d7de; }
.cabecera h1 { font-size: 17px; margin: 0 0 4px 0; }
.cabecera .meta { font-size: 12px; color: #57606a; }
.barra { height: 22px; }
.resumen { padding: 10px 18px; font-size: 14px; font-weight: 600; border-bottom: 1px solid #d0d7de; }
table { border-collapse: collapse; width: 100%; font-size: 12.5px; }
td { padding: 4px 10px; border-bottom: 1px solid #eef0f2; vertical-align: top; }
td.icono { width: 18px; font-weight: 700; text-align: center; }
td.clase { color: #57606a; white-space: nowrap; }
td.test { font-family: Consolas, 'DejaVu Sans Mono', monospace; }
tr.fila-fail td.test { color: #b42318; font-weight: 600; }
td.detalle { color: #b42318; font-family: Consolas, 'DejaVu Sans Mono', monospace; font-size: 11.5px; }
tr.grupo td { background: #f6f8fa; font-weight: 600; color: #24292f; padding-top: 6px; }
"""


def leer_resumen(archivo: pathlib.Path) -> dict:
    datos = {"titulo": archivo.stem, "fecha": "", "filas": []}
    for linea in archivo.read_text(encoding="utf-8").splitlines():
        titulo, fecha, fila = PATRON_TITULO.match(linea), PATRON_FECHA.match(linea), PATRON_FILA.match(linea)
        if titulo:
            datos["titulo"] = titulo.group("titulo")
        elif fecha:
            datos["fecha"] = fecha.group("fecha")
        elif fila and fila.group("clase").strip() != "Clase":
            datos["filas"].append({k: v.strip() for k, v in fila.groupdict().items()})
    return datos


def armar_html(datos: dict) -> str:
    filas = datos["filas"]
    total = len(filas)
    pasaron = sum(1 for f in filas if f["estado"] == ESTADO_PASS)
    omitidos = sum(1 for f in filas if f["estado"] == ESTADO_SKIP)
    fallaron = total - pasaron - omitidos
    color = COLOR_ROJO if fallaron else COLOR_VERDE
    if fallaron:
        texto = f"✗ Tests fallidos: {fallaron}, pasados: {pasaron} de {total}"
    else:
        texto = f"✓ Tests pasados: {pasaron} de {total}"
    if omitidos:
        texto += f" · omitidos: {omitidos}"

    cuerpo, clase_actual = [], None
    for f in filas:
        if f["clase"] != clase_actual:
            clase_actual = f["clase"]
            cuerpo.append(f'<tr class="grupo"><td colspan="3">{html.escape(clase_actual)}</td></tr>')
        if f["estado"] == ESTADO_PASS:
            icono, color_icono = "✓", COLOR_VERDE
        elif f["estado"] == ESTADO_SKIP:
            icono, color_icono = "–", COLOR_GRIS
        else:
            icono, color_icono = "✗", COLOR_ROJO
        detalle = f'<div class="detalle">{html.escape(f["detalle"].replace(chr(92) + "|", "|"))}</div>' if f["detalle"] else ""
        clase_fila = "fila-pass" if f["estado"] == ESTADO_PASS else "fila-fail"
        cuerpo.append(
            f'<tr class="{clase_fila}"><td class="icono" style="color:{color_icono}">{icono}</td>'
            f'<td class="test">{html.escape(f["test"])}{detalle}</td><td class="clase">{html.escape(f["id"])}</td></tr>')

    return f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>{html.escape(datos['titulo'])}</title><style>{ESTILO}</style></head>
<body><div class="panel">
<div class="cabecera"><h1>{html.escape(datos['titulo'])}</h1><div class="meta">JUnit 4 · Maven Surefire · {html.escape(datos['fecha'])}</div></div>
<div class="barra" style="background:{color}"></div>
<div class="resumen" style="color:{color}">{texto}</div>
<table>{''.join(cuerpo)}</table>
</div></body></html>
"""


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("resumenes", nargs="+")
    parser.add_argument("--salida-dir", required=True)
    args = parser.parse_args()

    destino = pathlib.Path(args.salida_dir)
    destino.mkdir(parents=True, exist_ok=True)
    for ruta in args.resumenes:
        archivo = pathlib.Path(ruta)
        datos = leer_resumen(archivo)
        if not datos["filas"]:
            print(f"Sin filas de resultados en {archivo}", file=sys.stderr)
            continue
        salida = destino / (archivo.stem + ".html")
        salida.write_text(armar_html(datos), encoding="utf-8")
        print(salida)
    return 0


if __name__ == "__main__":
    sys.exit(main())
