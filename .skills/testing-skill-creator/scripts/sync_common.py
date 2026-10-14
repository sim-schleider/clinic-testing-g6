#!/usr/bin/env python3
"""Copia los archivos comunes (assets/common) a las skills de testing, o verifica que estén iguales.

Uso:
    python3 -I sync_common.py [--verificar] [CARPETA_SKILL ...]

Sin carpetas, actúa sobre todas las skills hermanas cuyo nombre empieza con "testing-"
(salvo testing-skill-creator). Con --verificar no copia nada: informa las diferencias y
sale con código 1 si hay alguna.
"""
import argparse
import pathlib
import shutil
import sys

RAIZ_CREADOR = pathlib.Path(__file__).resolve().parent.parent
DIR_COMUN = RAIZ_CREADOR / "assets" / "common"
PREFIJO_SKILLS = "testing-"


def skills_hermanas() -> list:
    return sorted(
        d for d in RAIZ_CREADOR.parent.iterdir()
        if d.is_dir() and d.name.startswith(PREFIJO_SKILLS) and d != RAIZ_CREADOR and (d / "SKILL.md").exists()
    )


def archivos_comunes() -> list:
    return sorted(p.relative_to(DIR_COMUN) for p in DIR_COMUN.rglob("*") if p.is_file())


def procesar(skill: pathlib.Path, verificar: bool) -> int:
    diferencias = 0
    for relativo in archivos_comunes():
        origen, objetivo = DIR_COMUN / relativo, skill / relativo
        igual = objetivo.exists() and objetivo.read_bytes() == origen.read_bytes()
        if igual:
            continue
        diferencias += 1
        if verificar:
            estado = "falta" if not objetivo.exists() else "distinto"
            print(f"  ✘ {skill.name}/{relativo}: {estado}")
        else:
            objetivo.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(origen, objetivo)
            print(f"  ↻ {skill.name}/{relativo}: actualizado")
    return diferencias


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--verificar", action="store_true")
    parser.add_argument("carpetas", nargs="*")
    args = parser.parse_args()

    skills = [pathlib.Path(c).resolve() for c in args.carpetas] or skills_hermanas()
    if not skills:
        print("No hay skills de testing para sincronizar.")
        return 0

    total = 0
    for skill in skills:
        diferencias = procesar(skill, args.verificar)
        total += diferencias
        if diferencias == 0:
            print(f"  ✔ {skill.name}: comunes al día")
    return 1 if (args.verificar and total) else 0


if __name__ == "__main__":
    sys.exit(main())
