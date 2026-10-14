#!/usr/bin/env python3
"""Valida una o más skills de testing generadas con testing-skill-creator.

Uso:
    python3 -I validate_skill.py CARPETA_SKILL [CARPETA_SKILL ...]

Controla: frontmatter (name y description), largo del SKILL.md, secciones obligatorias,
marcadores de plantilla sin completar, rutas citadas que existan, archivos comunes
sincronizados y scripts ejecutables. Sale con código 1 si hay errores.
"""
import os
import pathlib
import re
import sys

RAIZ_CREADOR = pathlib.Path(__file__).resolve().parent.parent
DIR_COMUN = RAIZ_CREADOR / "assets" / "common"
PATRON_NOMBRE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
PATRON_RUTA_CITADA = re.compile(r"`((?:references|scripts)/[A-Za-z0-9_.\-/]+)`")
LARGO_MAXIMO_NOMBRE = 64
LARGO_MAXIMO_DESCRIPCION = 1024
LINEAS_MAXIMAS = 500
LINEAS_RECOMENDADAS = 250
SECCIONES_OBLIGATORIAS = ("## Cuándo usar", "## Entradas", "## Procedimiento", "## Salidas", "## NUNCA", "## Referencias")
MARCADOR_PLANTILLA = "<!--"
FRASES_DESCRIPCION = ("Usar cuando", "NO usar")


class Resultado:
    def __init__(self) -> None:
        self.errores = 0

    def ok(self, mensaje: str) -> None:
        print(f"  ✔ {mensaje}")

    def aviso(self, mensaje: str) -> None:
        print(f"  ⚠ {mensaje}")

    def error(self, mensaje: str) -> None:
        self.errores += 1
        print(f"  ✘ {mensaje}")


def leer_frontmatter(texto: str) -> tuple:
    lineas = texto.splitlines()
    if not lineas or lineas[0].strip() != "---":
        return {}, lineas
    try:
        fin = lineas.index("---", 1)
    except ValueError:
        return {}, lineas
    campos = {}
    for linea in lineas[1:fin]:
        coincidencia = re.match(r"^(\w+):\s*(.*)$", linea)
        if coincidencia:
            campos[coincidencia.group(1)] = coincidencia.group(2).strip().strip('"').strip("'")
    return campos, lineas[fin + 1:]


def validar_frontmatter(skill: pathlib.Path, campos: dict, r: Resultado) -> None:
    nombre = campos.get("name", "")
    if not PATRON_NOMBRE.match(nombre) or len(nombre) > LARGO_MAXIMO_NOMBRE:
        r.error(f"name inválido: '{nombre}'")
    elif nombre != skill.name:
        r.error(f"name '{nombre}' no coincide con la carpeta '{skill.name}'")
    else:
        r.ok(f"name: {nombre}")

    descripcion = campos.get("description", "")
    if not descripcion or descripcion.startswith("PENDIENTE"):
        r.error("description vacía o pendiente")
        return
    if len(descripcion) > LARGO_MAXIMO_DESCRIPCION:
        r.error(f"description de {len(descripcion)} caracteres (máx. {LARGO_MAXIMO_DESCRIPCION})")
    faltantes = [f for f in FRASES_DESCRIPCION if f not in descripcion]
    if faltantes:
        r.error(f"a la description le falta: {', '.join(faltantes)} (cuándo usar y cuándo no)")
    else:
        r.ok(f"description: {len(descripcion)} caracteres, con cuándo usar y cuándo no")


def validar_cuerpo(skill: pathlib.Path, cuerpo: list, r: Resultado) -> None:
    total = len(cuerpo)
    if total > LINEAS_MAXIMAS:
        r.error(f"SKILL.md tiene {total} líneas (máx. {LINEAS_MAXIMAS}): mover detalle a references/")
    elif total > LINEAS_RECOMENDADAS:
        r.aviso(f"SKILL.md tiene {total} líneas (recomendado ≤ {LINEAS_RECOMENDADAS})")
    else:
        r.ok(f"SKILL.md: {total} líneas")

    texto = "\n".join(cuerpo)
    faltan = [s for s in SECCIONES_OBLIGATORIAS if not re.search(rf"^{re.escape(s)}\b", texto, re.MULTILINE)]
    if faltan:
        r.error(f"faltan secciones: {', '.join(faltan)}")
    else:
        r.ok("secciones obligatorias presentes")

    if MARCADOR_PLANTILLA in texto:
        r.error("quedan marcadores de plantilla <!-- … --> sin completar")

    citadas = sorted(set(PATRON_RUTA_CITADA.findall(texto)))
    inexistentes = [c for c in citadas if not (skill / c.rstrip("/.")).exists()]
    if inexistentes:
        r.error(f"rutas citadas que no existen: {', '.join(inexistentes)}")
    else:
        r.ok(f"{len(citadas)} rutas citadas, todas existen")

    for relativo in (p.relative_to(skill) for p in (skill / "references").glob("*.md")):
        if str(relativo) not in texto:
            r.aviso(f"{relativo} no se menciona en SKILL.md (no se va a cargar nunca)")


def validar_comunes(skill: pathlib.Path, r: Resultado) -> None:
    desincronizados = []
    for origen in (p for p in DIR_COMUN.rglob("*") if p.is_file()):
        objetivo = skill / origen.relative_to(DIR_COMUN)
        if not objetivo.exists() or objetivo.read_bytes() != origen.read_bytes():
            desincronizados.append(str(origen.relative_to(DIR_COMUN)))
    if desincronizados:
        r.error(f"comunes desincronizados: {', '.join(desincronizados)} → correr sync_common.py")
    else:
        r.ok("archivos comunes sincronizados")

    no_ejecutables = [p.name for p in (skill / "scripts").glob("*") if p.suffix in (".sh", ".py") and not os.access(p, os.X_OK)]
    if no_ejecutables:
        r.error(f"scripts sin permiso de ejecución: {', '.join(no_ejecutables)}")


def validar(skill: pathlib.Path) -> int:
    print(f"\n{skill.name}")
    r = Resultado()
    archivo = skill / "SKILL.md"
    if not archivo.exists():
        r.error("no existe SKILL.md")
        return r.errores
    campos, cuerpo = leer_frontmatter(archivo.read_text(encoding="utf-8"))
    if not campos:
        r.error("frontmatter ausente o mal cerrado (--- … ---)")
        return r.errores
    validar_frontmatter(skill, campos, r)
    validar_cuerpo(skill, cuerpo, r)
    validar_comunes(skill, r)
    return r.errores


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    errores = sum(validar(pathlib.Path(c).resolve()) for c in sys.argv[1:])
    print(f"\n{'✘' if errores else '✔'} {errores} error(es)")
    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
