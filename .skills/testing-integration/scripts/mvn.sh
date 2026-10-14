#!/usr/bin/env bash
# ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. Ver sync_common.py.
#
# Ejecuta Maven sobre un proyecto Java 8. Si hay `mvn` instalado lo usa; si no, corre
# Maven dentro de Docker (imagen con JDK 8). Devuelve el código de salida de Maven.
#
# Uso:
#   mvn.sh [--proyecto DIR] [--mysql CONTENEDOR] [--log ARCHIVO] [--docker] -- <args de mvn>
#
#   --proyecto DIR      raíz del proyecto (donde está pom.xml). Por defecto, el directorio actual.
#   --mysql CONTENEDOR  comparte la red del contenedor MySQL (ver mysql.sh), así
#                       `localhost:3306` dentro de los tests llega a esa base.
#   --log ARCHIVO       además de mostrar la salida, la guarda en ARCHIVO.
#   --docker            fuerza Docker aunque haya `mvn` local.
#
# Ejemplos:
#   mvn.sh -- test
#   mvn.sh --mysql tpe-mysql --log docs/pruebas/resultados/2026-10-08-integracion.log -- -Dtest='*IntegracionTest' test
#   mvn.sh -- -Dtest=AsociadoDTOCajaNegraTest#cn3_numeroNegativo test
set -euo pipefail

readonly IMAGEN_MAVEN="${TESTING_MAVEN_IMAGE:-maven:3.9.9-eclipse-temurin-8}"
readonly M2_HOST="${HOME}/.m2"
readonly M2_CONTENEDOR="/var/maven/.m2"
readonly DIR_CONTENEDOR="/proyecto"

proyecto="$PWD"
red_mysql=""
archivo_log=""
forzar_docker="false"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --proyecto) proyecto="$2"; shift 2 ;;
        --mysql)    red_mysql="$2"; shift 2 ;;
        --log)      archivo_log="$2"; shift 2 ;;
        --docker)   forzar_docker="true"; shift ;;
        --)         shift; break ;;
        *)          break ;;
    esac
done

if [[ $# -eq 0 ]]; then
    echo "Faltan los argumentos de Maven (por ejemplo: -- test)" >&2
    exit 2
fi

proyecto="$(cd "$proyecto" && pwd)"
if [[ ! -f "$proyecto/pom.xml" ]]; then
    echo "No hay pom.xml en $proyecto" >&2
    exit 2
fi

construir_comando() {
    if [[ "$forzar_docker" == "false" ]] && command -v mvn >/dev/null 2>&1; then
        if [[ -n "$red_mysql" ]]; then
            echo "Aviso: Maven local no comparte la red de '$red_mysql'. El contenedor tiene" \
                 "que publicar el puerto (mysql.sh up --publicar 3306)." >&2
        fi
        comando=(mvn -B -f "$proyecto/pom.xml" "$@")
        return
    fi

    mkdir -p "$M2_HOST"
    comando=(docker run --rm
        -u "$(id -u):$(id -g)"
        -v "$proyecto:$DIR_CONTENEDOR" -w "$DIR_CONTENEDOR"
        -v "$M2_HOST:$M2_CONTENEDOR" -e "MAVEN_CONFIG=$M2_CONTENEDOR")
    if [[ -n "$red_mysql" ]]; then
        comando+=(--network "container:$red_mysql")
    fi
    comando+=("$IMAGEN_MAVEN" mvn -B "-Duser.home=/var/maven" "$@")
}

construir_comando "$@"

if [[ -n "$archivo_log" ]]; then
    mkdir -p "$(dirname "$archivo_log")"
    set +e
    "${comando[@]}" 2>&1 | tee "$archivo_log"
    codigo=${PIPESTATUS[0]}
    set -e
    exit "$codigo"
fi

exec "${comando[@]}"
