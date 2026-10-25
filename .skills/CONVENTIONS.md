# Clinic testing: shared conventions for the testing skills

Testing strategy over the "Clínica" Java app (TP final de Calidad de Software). The assignment is
`docs/Enunciado_Trabajo_final.md`:
1. Isolated unit tests of `AsociadoDTO` (equivalence classes and boundary values).
2. Integration tests of `AsociadoDTO` + `DataAccessObject` + `BaseDeDatos` against a real MySQL.
3. Mock tests of `Ambulancia` through the `State` interface, plus integration and concurrency
   tests of the state machine with `RetornoAutomatico` and other threads.

Deliverables: the test suites, red and green screenshots, and a report (informe) with the formal
scenario design, test cases (equivalence classes and boundary values), results and conclusions.

## Skills

| Skill | Use it for |
|-------|-----------|
| `unit-testing` | One class in isolation from its spec (AsociadoDTO, Domicilio) |
| `db-integration-testing` | DTO → DAO → BaseDeDatos against real MySQL |
| `mock-testing` | Ambulancia with a mocked `State`, and states with a mocked `Ambulancia` |
| `concurrency-testing` | Real state machine, `synchronized`/`wait`/`notifyAll`, `RetornoAutomatico` and other threads |
| `test-report` | Running suites for red/green evidence, screenshots, defect registry, assembling and exporting the informe |

**Setup:** Claude Code discovers project skills in `.claude/skills/`. Link that folder to this one
once per clone, `mkdir -p .claude && ln -s ../.skills .claude/skills`, and import these
conventions from a local `CLAUDE.md` with the line `@.skills/CONVENTIONS.md`. All paths inside
the skills are relative to the project root (`.skills/...`), so they work with or without the
link.

## Stack and how to run

- Java 17, Maven, JUnit 5.10 (Jupiter), Mockito 5.11, MySQL 8 (`docker-compose.yml`).
- Surefire runs with `-ea`: production code validates with Java `assert`, so invalid-input tests
  expect `AssertionError`. Without `-ea` those tests give false reds.
- Run everything through `.skills/test-report/scripts/run_tests.sh <suite> <fase>`
  (suite: `unit` | `integration` | `mock` | `concurrency` | `all`; fase: `rojo` | `verde` | `dev`).
  It uses local `mvn` if present, otherwise `docker compose`. Under WSL, Docker needs
  "WSL integration" enabled in Docker Desktop. If neither works, the user runs `mvn test` from
  IntelliJ and the script is called with `--no-run` to summarize `target/surefire-reports`.
- The DB connection is hardcoded in `BaseDeDatos` (`localhost:3306`, `progra_c`/`progra_c`, DB
  `Grupo_7`). Adapt the environment to the code, never the code to the environment.

## Rules every skill follows

- **The oracle is the specification**: SRS (`docs/SRS_Persistencia_Clinica.md`), the assignment,
  UML diagrams (removed from `docs/`; recover them from git history, see `git log --diff-filter=D -- docs/UML.png`), Javadoc contracts and the design report (`docs/Informe.md`).
  Never the current behaviour of the code. A test that fails because the code disagrees with
  the spec is a finding, not a broken test. Gaps in the spec are written down as *supuestos*.
- **Production code changes only with the user's OK**, one defect per commit
  (`fix(<scope>): ...`), so each red→green transition maps to a commit in the report.
- **Known build blocker**: `EnTallerState.java` declares `package model...` instead of
  `modelo...`, and `TrasladandoPacienteClinicasState.java` holds the public class
  `TrasladandoPacienteClinicaState`. Nothing compiles until both are fixed (with the user's OK);
  register them as the first defects.
- **Red/green cycle**: (1) write the suite, (2) run `rojo` on the original code (only the build
  fixes applied), (3) fix defects with the user's OK, (4) run `verde`, (5) seed faults on the
  green code to prove the suite catches them. Never commit a seeded fault.
- **No commits or pushes** unless the user asks for them.

## Conventions

- Test classes in `src/test/java`, same package as the class under test, name ends in `Test`,
  class-level `@Tag("unit" | "integration" | "mock" | "concurrency")`.
- Case IDs: `CU` unit, `CI` DB integration, `CM` mocks, `CC` concurrency; `D` defects;
  `FU`/`FI`/`FM`/`FC` seeded faults. Every test method starts with its ID (`cu3_numeroCeroValido`)
  and has `@DisplayName("CU3 – ...")`; parameterized rows put the ID in the first argument and use
  `name = "{0} – ..."`. That is how results are matched back to the design tables.
- Deliverables (tables, report text) are written in Spanish; code identifiers follow the existing
  Spanish naming.

## Report layout (`docs/informe/`)

The report is written as the work happens: each skill owns its section file, and `test-report`
concatenates them in file-name order.

| File | Owner |
|------|-------|
| `caratula.html`, `secciones/01-introduccion.md`, `02-estrategia.md`, `08-conclusiones.md`, `92-apendice-entorno.md` | test-report |
| `secciones/03-unitarias.md` | unit-testing |
| `secciones/04-integracion-bd.md` | db-integration-testing |
| `secciones/05-mocks.md` | mock-testing |
| `secciones/06-concurrencia.md` | concurrency-testing |
| `secciones/07-defectos.md` (defect registry, every skill appends) | shared |
| `secciones/90-…`, `91-…`, `93-…` (appendices), `resultados/`, `capturas/` | generated by test-report scripts |

Defect registry row (`07-defectos.md`):

| ID | Componente | Descripción | Oráculo violado | Detectado por | Esperado | Obtenido | Severidad | Estado |
|----|------------|-------------|-----------------|---------------|----------|----------|-----------|--------|

`Estado` is `Abierto`, `Corregido (<commit>)` or `No se corrige: <motivo>`. `Detectado por` lists
case IDs.
