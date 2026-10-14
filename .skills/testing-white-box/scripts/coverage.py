#!/usr/bin/env python3
"""Resume la cobertura de JaCoCo por método y lista las líneas y ramas sin cubrir.

Uso:
    python3 -I coverage.py [--xml target/site/jacoco/jacoco.xml] [--clase Nombre] [--metodo nombre]
                            [--fuentes src/main/java] [--salida ARCHIVO.md]

Para cada método muestra: líneas cubiertas, ramas cubiertas y V(G) según JaCoCo
(contador COMPLEXITY). Con --fuentes muestra además el texto de cada línea sin cubrir o
con ramas parciales, para decidir qué caso de caja blanca falta.
"""
import argparse
import pathlib
import sys
import xml.etree.ElementTree as ET

XML_DEFECTO = "target/site/jacoco/jacoco.xml"
FUENTES_DEFECTO = "src/main/java"
CONTADORES = ("LINE", "BRANCH", "COMPLEXITY")
METODOS_SINTETICOS = ("<clinit>",)


def contador(nodo: ET.Element, tipo: str) -> tuple:
    for c in nodo.findall("counter"):
        if c.get("type") == tipo:
            cubiertos, perdidos = int(c.get("covered")), int(c.get("missed"))
            return cubiertos, cubiertos + perdidos
    return 0, 0


def porcentaje(cubiertos: int, total: int) -> str:
    return "—" if total == 0 else f"{cubiertos}/{total} ({100 * cubiertos // total}%)"


def lineas_metodos(clase: ET.Element) -> list:
    metodos = []
    for m in clase.findall("method"):
        if m.get("name") in METODOS_SINTETICOS:
            continue
        metodos.append((int(m.get("line", "0")), m))
    return sorted(metodos, key=lambda par: par[0])


def rango_de(metodos: list, indice: int) -> tuple:
    inicio = metodos[indice][0]
    fin = metodos[indice + 1][0] - 1 if indice + 1 < len(metodos) else sys.maxsize
    return inicio, fin


def leer_fuente(dir_fuentes: pathlib.Path, paquete: str, archivo: str) -> list:
    ruta = dir_fuentes / paquete / archivo
    return ruta.read_text(encoding="utf-8", errors="replace").splitlines() if ruta.exists() else []


def describir_linea(linea: ET.Element) -> str:
    mi, ci = int(linea.get("mi")), int(linea.get("ci"))
    mb, cb = int(linea.get("mb")), int(linea.get("cb"))
    if ci == 0 and mi > 0:
        return "no ejecutada"
    if mb > 0:
        return f"ramas {cb}/{cb + mb}"
    return ""


def resumir(args: argparse.Namespace) -> str:
    raiz = ET.parse(args.xml).getroot()
    dir_fuentes = pathlib.Path(args.fuentes)
    salida = ["# Cobertura JaCoCo", ""]
    for paquete in raiz.findall("package"):
        lineas_por_archivo = {sf.get("name"): sf.findall("line") for sf in paquete.findall("sourcefile")}
        for clase in paquete.findall("class"):
            nombre_clase = clase.get("name").rsplit("/", 1)[-1]
            if args.clase and nombre_clase != args.clase:
                continue
            archivo = clase.get("sourcefilename", "")
            fuente = leer_fuente(dir_fuentes, paquete.get("name"), archivo)
            lineas = lineas_por_archivo.get(archivo, [])
            metodos = lineas_metodos(clase)
            salida += [f"## {nombre_clase}", "",
                       "| Método | Líneas | Ramas | V(G) JaCoCo |",
                       "|--------|--------|-------|-------------|"]
            detalle = []
            for i, (_, metodo) in enumerate(metodos):
                if args.metodo and metodo.get("name") != args.metodo:
                    continue
                lc, lt = contador(metodo, "LINE")
                bc, bt = contador(metodo, "BRANCH")
                _, complejidad = contador(metodo, "COMPLEXITY")
                salida.append(f"| `{metodo.get('name')}` | {porcentaje(lc, lt)} | {porcentaje(bc, bt)} | {complejidad} |")
                inicio, fin = rango_de(metodos, i)
                for linea in lineas:
                    numero = int(linea.get("nr"))
                    estado = describir_linea(linea)
                    if inicio <= numero <= fin and estado:
                        texto = fuente[numero - 1].strip() if 0 < numero <= len(fuente) else ""
                        detalle.append(f"- `{metodo.get('name')}` L{numero} ({estado}): `{texto}`")
            salida.append("")
            if detalle:
                salida += ["Pendiente de cubrir:", ""] + detalle + [""]
    return "\n".join(salida) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--xml", default=XML_DEFECTO)
    parser.add_argument("--clase")
    parser.add_argument("--metodo")
    parser.add_argument("--fuentes", default=FUENTES_DEFECTO)
    parser.add_argument("--salida")
    args = parser.parse_args()

    if not pathlib.Path(args.xml).exists():
        print(f"No existe {args.xml}. Ejecutar los tests con JaCoCo (ver execution-environment.md).", file=sys.stderr)
        return 1
    markdown = resumir(args)
    if args.salida:
        pathlib.Path(args.salida).parent.mkdir(parents=True, exist_ok=True)
        pathlib.Path(args.salida).write_text(markdown, encoding="utf-8")
    print(markdown)
    return 0


if __name__ == "__main__":
    sys.exit(main())
