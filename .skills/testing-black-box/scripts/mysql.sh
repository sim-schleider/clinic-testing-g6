#!/usr/bin/env bash
# ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. Ver sync_common.py.
#
# Levanta un MySQL 8.0 descartable en Docker para pruebas de integración.
#
# Uso:
#   mysql.sh up     [--nombre N] [--usuario U] [--clave C] [--publicar PUERTO]
#   mysql.sh down   [--nombre N]
#   mysql.sh status [--nombre N]
#   mysql.sh sql    [--nombre N] -- "SENTENCIAS SQL"      (se ejecutan como root)
#   mysql.sh lock / unlock [--nombre N] [--usuario U]      (bloquea/desbloquea el usuario: simula BD inaccesible)
#
# Valores por defecto: nombre tpe-mysql, usuario/clave test/test, sin publicar puertos.
# El usuario y la clave tienen que ser los que usa el código (buscarlos en la clase de conexión).
# Los tests llegan a la base con `mvn.sh --mysql <nombre>` (comparten la red del contenedor,
# así `localhost:3306` funciona sin publicar puertos). `--publicar` solo hace falta para
# correr los tests desde el IDE o con Maven local.
set -euo pipefail

readonly IMAGEN_MYSQL="${TESTING_MYSQL_IMAGE:-mysql:8.0}"
readonly CLAVE_ROOT="root"
readonly ESPERA_MAXIMA_SEG=90

accion="${1:-}"
[[ -n "$accion" ]] && shift
nombre="tpe-mysql"
usuario="test"
clave="test"
puerto_publicado=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --nombre)   nombre="$2"; shift 2 ;;
        --usuario)  usuario="$2"; shift 2 ;;
        --clave)    clave="$2"; shift 2 ;;
        --publicar) puerto_publicado="$2"; shift 2 ;;
        --)         shift; break ;;
        *)          echo "Opción desconocida: $1" >&2; exit 2 ;;
    esac
done

sql_root() {
    docker exec -e "MYSQL_PWD=$CLAVE_ROOT" "$nombre" mysql -uroot -h127.0.0.1 --protocol=TCP -e "$1"
}

esperar_listo() {
    # El entrypoint arranca primero un servidor temporal sin red; por eso se espera por TCP.
    local inicio=$SECONDS
    until sql_root "SELECT 1" >/dev/null 2>&1; do
        if (( SECONDS - inicio > ESPERA_MAXIMA_SEG )); then
            echo "MySQL no respondió en ${ESPERA_MAXIMA_SEG}s. Ver: docker logs $nombre" >&2
            exit 1
        fi
        sleep 2
    done
}

case "$accion" in
    up)
        if [[ -z "$(docker ps -aq -f "name=^${nombre}$")" ]]; then
            args=(docker run -d --name "$nombre" -e "MYSQL_ROOT_PASSWORD=$CLAVE_ROOT")
            [[ -n "$puerto_publicado" ]] && args+=(-p "$puerto_publicado:3306")
            "${args[@]}" "$IMAGEN_MYSQL" >/dev/null
        else
            docker start "$nombre" >/dev/null
        fi
        esperar_listo
        # mysql_native_password evita "Public Key Retrieval is not allowed" con useSSL=false.
        # Privilegios globales porque el código suele crear el esquema (CREATE DATABASE).
        sql_root "CREATE USER IF NOT EXISTS '$usuario'@'%' IDENTIFIED WITH mysql_native_password BY '$clave';
                  ALTER USER '$usuario'@'%' IDENTIFIED WITH mysql_native_password BY '$clave';
                  GRANT ALL PRIVILEGES ON *.* TO '$usuario'@'%' WITH GRANT OPTION;
                  FLUSH PRIVILEGES;"
        echo "MySQL listo: contenedor=$nombre usuario=$usuario${puerto_publicado:+ puerto_host=$puerto_publicado}"
        ;;
    down)
        # -v: la imagen de MySQL declara un VOLUME anónimo; sin -v queda huérfano en el disco.
        docker rm -f -v "$nombre" >/dev/null 2>&1 || true
        echo "Contenedor $nombre y su volumen eliminados"
        ;;
    status)
        docker ps -a -f "name=^${nombre}$" --format '{{.Names}} {{.Status}} {{.Ports}}'
        ;;
    sql)
        sql_root "$*"
        ;;
    lock)
        # No se detiene ni se pausa el contenedor: los tests comparten su red y quedarían
        # colgados. Con la cuenta bloqueada, la conexión falla enseguida con SQLException.
        sql_root "ALTER USER '$usuario'@'%' ACCOUNT LOCK;" && echo "Usuario $usuario bloqueado"
        ;;
    unlock)
        sql_root "ALTER USER '$usuario'@'%' ACCOUNT UNLOCK;" && echo "Usuario $usuario desbloqueado"
        ;;
    *)
        sed -n '4,17p' "$0"
        exit 2
        ;;
esac
