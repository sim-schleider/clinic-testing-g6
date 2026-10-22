#!/usr/bin/env bash
# Runs one test suite and, for evidence runs, saves the log, the results table and the screenshot.
#
# Usage (from the project root):
#   run_tests.sh <suite> <fase> [--no-run] [-- <extra mvn args>]
#
#   suite   unit | integration | mock | concurrency | all   (JUnit 5 @Tag; "all" runs everything)
#   fase    rojo | verde   evidence run: writes docs/informe/resultados/<suite>-<fase>.{log,md}
#                          and docs/informe/capturas/<suite>-<fase>.png
#           dev            iteration run: prints the results table, saves nothing
#   --no-run  do not run Maven; summarize the existing target/surefire-reports
#             (for runs made from IntelliJ's Maven panel)
#
# Maven is taken from, in order: local `mvn`, `docker compose` (services in docker-compose.yml),
# or plain `docker run`. Test failures do not stop the script (they are the evidence).
set -uo pipefail

readonly SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly RESULTS_DIR="docs/informe/resultados"
readonly CAPTURES_DIR="docs/informe/capturas"
readonly REPORTS_DIR="target/surefire-reports"
readonly MAVEN_IMAGE="maven:3.9-eclipse-temurin-17"

usage() { sed -n '2,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 2; }

[[ $# -ge 2 ]] || usage
suite="$1"; fase="$2"; shift 2
run_maven="true"
extra_args=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-run) run_maven="false"; shift ;;
        --) shift; extra_args=("$@"); break ;;
        *) usage ;;
    esac
done

case "$suite" in unit|integration|mock|concurrency|all) ;; *) echo "Suite desconocida: $suite" >&2; usage ;; esac
case "$fase" in rojo|verde|dev) ;; *) echo "Fase desconocida: $fase" >&2; usage ;; esac
[[ -f pom.xml ]] || { echo "Ejecutar desde la raíz del proyecto (donde está pom.xml)" >&2; exit 2; }

mvn_args=(-B -Dmaven.test.failure.ignore=true)
[[ "$suite" != "all" ]] && mvn_args+=("-Dgroups=$suite")
mvn_args+=("${extra_args[@]}" clean test)

docker_ok() { command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; }

run_mvn() {
    if command -v mvn >/dev/null 2>&1; then
        echo ">> mvn local" >&2
        mvn "${mvn_args[@]}"
    elif docker_ok && docker compose version >/dev/null 2>&1 && [[ -f docker-compose.yml ]]; then
        local deps=(--no-deps)
        if [[ "$suite" == "integration" || "$suite" == "all" ]]; then
            echo ">> levantando MySQL (docker compose)" >&2
            docker compose up -d --wait mysql >&2 || return 1
            deps=()
        fi
        echo ">> mvn en docker compose (servicio maven)" >&2
        docker compose --profile test run --rm "${deps[@]}" maven mvn "${mvn_args[@]}"
    elif docker_ok; then
        echo ">> mvn en docker run ($MAVEN_IMAGE)" >&2
        docker run --rm --network host -v "$PWD:/app" -w /app -v "$HOME/.m2:/root/.m2" "$MAVEN_IMAGE" mvn "${mvn_args[@]}"
    else
        cat >&2 <<'EOF'
No hay Maven local ni Docker disponible.
  - Docker Desktop: Settings > Resources > WSL integration > habilitar esta distro.
  - O ejecutar `mvn test` desde IntelliJ (panel Maven) y volver a llamar con --no-run.
EOF
        return 3
    fi
}

if [[ "$run_maven" == "true" ]]; then
    if [[ "$fase" == "dev" ]]; then
        run_mvn || { [[ $? -eq 3 ]] && exit 3; }
    else
        mkdir -p "$RESULTS_DIR"
        run_mvn 2>&1 | tee "$RESULTS_DIR/$suite-$fase.log"
        [[ ${PIPESTATUS[0]} -eq 3 ]] && exit 3
    fi
fi

if ! compgen -G "$REPORTS_DIR/TEST-*.xml" >/dev/null; then
    echo "No hay reportes en $REPORTS_DIR: ¿falló la compilación? Revisar el log de Maven." >&2
    exit 1
fi

summary_args=(--reports "$REPORTS_DIR" --suite "$suite" --fase "$fase")
if [[ "$fase" == "dev" ]]; then
    python3 -I "$SKILL_DIR/scripts/surefire_summary.py" "${summary_args[@]}"
    exit $?
fi

summary="$RESULTS_DIR/$suite-$fase.md"
python3 -I "$SKILL_DIR/scripts/surefire_summary.py" "${summary_args[@]}" --out "$summary" || exit 1
python3 -I "$SKILL_DIR/scripts/capture.py" "$summary" --out "$CAPTURES_DIR/$suite-$fase.png" \
    || echo "Aviso: no se pudo generar la captura (ver capture.py)" >&2
