#!/usr/bin/env python3
"""Crea el esqueleto de una skill de testing a partir de la plantilla y los archivos comunes.

Uso:
    python3 -I new_skill.py <nombre> --titulo "Título" [--descripcion "..."] [--destino DIR]

<nombre> en minúsculas con guiones (por ejemplo, testing-black-box). Por defecto la skill se
crea al lado de testing-skill-creator (la misma carpeta .claude/skills del proyecto).
"""
import argparse
import pathlib
import re
import shutil
import sys

RAIZ_CREADOR = pathlib.Path(__file__).resolve().parent.parent
DIR_COMUN = RAIZ_CREADOR / "assets" / "common"
PLANTILLA = RAIZ_CREADOR / "assets" / "template" / "SKILL.md.tmpl"
DESTINO_DEFECTO = RAIZ_CREADOR.parent
PATRON_NOMBRE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
LARGO_MAXIMO_NOMBRE = 64
DESCRIPCION_PENDIENTE = "PENDIENTE: qué hace. Usar cuando … NO usar para … (usar …)."


def copiar_comun(destino: pathlib.Path) -> list:
    copiados = []
    for origen in sorted(DIR_COMUN.rglob("*")):
        if origen.is_dir():
            continue
        relativo = origen.relative_to(DIR_COMUN)
        objetivo = destino / relativo
        objetivo.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(origen, objetivo)
        copiados.append(str(relativo))
    return copiados


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("nombre")
    parser.add_argument("--titulo", required=True)
    parser.add_argument("--descripcion", default=DESCRIPCION_PENDIENTE)
    parser.add_argument("--destino", default=str(DESTINO_DEFECTO))
    args = parser.parse_args()

    if not PATRON_NOMBRE.match(args.nombre) or len(args.nombre) > LARGO_MAXIMO_NOMBRE:
        print(f"Nombre inválido: '{args.nombre}' (minúsculas, números y guiones; máx. {LARGO_MAXIMO_NOMBRE})", file=sys.stderr)
        return 2

    carpeta = pathlib.Path(args.destino).resolve() / args.nombre
    if carpeta.exists():
        print(f"Ya existe {carpeta}. Para actualizar los archivos comunes usar sync_common.py.", file=sys.stderr)
        return 1

    (carpeta / "references").mkdir(parents=True)
    copiados = copiar_comun(carpeta)
    contenido = (
        PLANTILLA.read_text(encoding="utf-8")
        .replace("{{NOMBRE}}", args.nombre)
        .replace("{{TITULO}}", args.titulo)
        .replace("{{DESCRIPCION}}", args.descripcion.replace('"', "'"))
    )
    (carpeta / "SKILL.md").write_text(contenido, encoding="utf-8")

    print(f"Skill creada en {carpeta}")
    print("Archivos comunes copiados:", ", ".join(copiados))
    print("Siguiente: completar SKILL.md (reemplazar los <!-- … -->), agregar references/ de la técnica y validar:")
    print(f"  python3 -I {RAIZ_CREADOR / 'scripts' / 'validate_skill.py'} {carpeta}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
