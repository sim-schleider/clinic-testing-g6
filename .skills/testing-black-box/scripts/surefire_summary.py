#!/usr/bin/env python3
# ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. Ver sync_common.py.
"""Resume los reportes XML de Surefire en una tabla Markdown (pass/fail por test).

Uso:
    python3 -I surefire_summary.py [--reportes DIR] [--clase PATRON] [--titulo TEXTO] [--salida ARCHIVO.md]

Por defecto lee target/surefire-reports del directorio actual. El Id de la tabla de diseño
(CN3, T2, CI5...) se toma del prefijo del nombre del método (cn3_..., t2_..., ci5_...: 1 a 3
letras, número y guion bajo) o, en
tests parametrizados, del inicio del nombre entre corchetes ([CN6: ...]).
--clase filtra por nombre simple de la clase de test (admite comodines: '*CajaNegraTest').
Surefire NO borra reportes de corridas anteriores: ejecutar con `clean test` o usar --clase.
"""
import argparse
import datetime
import fnmatch
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

DIR_REPORTES_DEFECTO = "target/surefire-reports"
PATRON_REPORTE = "TEST-*.xml"
# Id = prefijo de 1 a 3 letras + número (CN3, T2, CI15, CE16...), al inicio del método o entre corchetes.
PATRON_ID = re.compile(r"^([a-z]{1,3})(\d+)_", re.IGNORECASE)
PATRON_ID_PARAMETRIZADO = re.compile(r"\[([a-z]{1,3})(\d+)\b", re.IGNORECASE)
LARGO_MAXIMO_DETALLE = 160
ESTADO_PASS = "pass"
ESTADO_FAIL = "fail"
ESTADO_ERROR = "error"
ESTADO_SKIP = "skipped"
ICONOS = {ESTADO_PASS: "✅", ESTADO_FAIL: "❌", ESTADO_ERROR: "💥", ESTADO_SKIP: "⏭"}


def leer_casos(dir_reportes: pathlib.Path, patron_clase: str) -> list:
    casos = []
    for archivo in sorted(dir_reportes.glob(PATRON_REPORTE)):
        raiz = ET.parse(archivo).getroot()
        for caso in raiz.iter("testcase"):
            datos = clasificar(caso)
            if fnmatch.fnmatchcase(datos["clase"], patron_clase):
                casos.append(datos)
    return sorted(casos, key=orden_natural)


def buscar_id(nombre: str):
    return PATRON_ID.match(nombre) or PATRON_ID_PARAMETRIZADO.search(nombre)


def orden_natural(caso: dict) -> tuple:
    coincidencia = buscar_id(caso["test"])
    numero = int(coincidencia.group(2)) if coincidencia else sys.maxsize
    return caso["clase"], numero, caso["test"]


def clasificar(caso: ET.Element) -> dict:
    estado, detalle = ESTADO_PASS, ""
    for etiqueta, valor in (("failure", ESTADO_FAIL), ("error", ESTADO_ERROR), ("skipped", ESTADO_SKIP)):
        nodo = caso.find(etiqueta)
        if nodo is not None:
            estado = valor
            detalle = nodo.get("message") or nodo.get("type") or (nodo.text or "").strip()
            break
    nombre = caso.get("name", "")
    coincidencia = buscar_id(nombre)
    return {
        "clase": caso.get("classname", "").rsplit(".", 1)[-1],
        "test": nombre,
        "id": (coincidencia.group(1) + coincidencia.group(2)).upper() if coincidencia else "",
        "estado": estado,
        "detalle": limpiar(detalle),
    }


def limpiar(texto: str) -> str:
    linea = " ".join(texto.split())
    if len(linea) > LARGO_MAXIMO_DETALLE:
        linea = linea[:LARGO_MAXIMO_DETALLE] + "…"
    return linea.replace("|", "\\|")


def armar_markdown(casos: list, titulo: str) -> str:
    conteo = {estado: sum(1 for c in casos if c["estado"] == estado) for estado in ICONOS}
    lineas = [
        f"# Resultados: {titulo}",
        "",
        f"Fecha: {datetime.datetime.now():%Y-%m-%d %H:%M} · Total: {len(casos)} · "
        + " · ".join(f"{ICONOS[e]} {e}: {n}" for e, n in conteo.items()),
        "",
        "| Clase | Test | Id | Resultado | Detalle |",
        "|-------|------|----|-----------|---------|",
    ]
    for c in casos:
        lineas.append(f"| {c['clase']} | `{c['test']}` | {c['id']} | {ICONOS[c['estado']]} {c['estado']} | {c['detalle']} |")
    return "\n".join(lineas) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--reportes", default=DIR_REPORTES_DEFECTO)
    parser.add_argument("--clase", default="*")
    parser.add_argument("--titulo", default="suite de tests")
    parser.add_argument("--salida")
    args = parser.parse_args()

    dir_reportes = pathlib.Path(args.reportes)
    casos = leer_casos(dir_reportes, args.clase) if dir_reportes.is_dir() else []
    if not casos:
        print(f"No hay reportes {PATRON_REPORTE} en {dir_reportes}. ¿Se ejecutó `mvn test`?", file=sys.stderr)
        return 1

    markdown = armar_markdown(casos, args.titulo)
    if args.salida:
        salida = pathlib.Path(args.salida)
        salida.parent.mkdir(parents=True, exist_ok=True)
        salida.write_text(markdown, encoding="utf-8")
    print(markdown)
    return 0


if __name__ == "__main__":
    sys.exit(main())
