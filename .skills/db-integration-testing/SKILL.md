---
name: db-integration-testing
description: Design, write and run integration tests (JUnit 5) of the persistence layer against a real MySQL in Docker. Covers AsociadoDTO → DataAccessObject → BaseDeDatos, the Singleton connection, automatic schema creation and seed data, parameterized CRUD, SQL exceptions and mapping errors, and writes the design, results and defects into the report section. Use this whenever the user mentions integration tests with the database, "pruebas de integración", MySQL, JDBC, the DAO, BaseDeDatos, CRUD, "datos semilla", "iniciarBD", the Singleton connection, SQLExceptions, DNI duplicados, or wants to prove the suite catches SQL or mapping errors, even if they don't say "integration". Not for one class in isolation (unit-testing), mocked collaborators (mock-testing), threads (concurrency-testing) or assembling the report (test-report).
---

# DB integration testing: DTO, DAO and BaseDeDatos against real MySQL

This skill produces:
- sequence diagrams of each operation,
- the integration strategy,
- the table of element states and representative values,
- the integration case table,
- JUnit 5 tests against the real MySQL,
- red/green results and the defects found,
- the report section `docs/informe/secciones/04-integracion-bd.md`, written in Spanish.

The project conventions are in `.skills/CONVENTIONS.md`.

## Why the procedure is shaped this way

The assignment asks for "comunicación real con el motor MySQL", so nothing on the
DTO → DAO → BaseDeDatos → MySQL path is mocked. Two failure modes make integration suites look
green while hiding defects:

- **Verifying a write with the component that wrote it.** If `guardar` puts `calle` in the
  `ciudad` column and `cargar` reads it back the same wrong way, the round trip passes.
  Verify every write through an **independent JDBC connection** with your own SQL.
- **Shared state between tests.** `BaseDeDatos` is a Singleton, so its connection survives from
  one test to the next, and from one test class to the next in the same JVM. If one test closes
  the connection, every later test fails with misleading errors. Each test starts from a known
  database state, and tests that close the connection clean up after themselves.

## Inputs

- **Oracle**: `docs/SRS_Persistencia_Clinica.md` §3.2 (connection, Singleton, `iniciarBD`) and §3.3
  (DAO operations), the assignment item 2, the Javadoc of `DataAccessObject` and `BaseDeDatos`, and
  the schema created in `iniciarBD`. The schema is part of the design: column types and lengths
  are boundaries.
- **Code**: the three classes plus `Asociado` and `Domicilio`, which the DAO takes as input. Read
  them to trace the real calls for the sequence diagrams and to find injection points. Expected
  results still come from the spec.
- **Environment**: `docker-compose.yml` (service `mysql`). Credentials and URL are hardcoded in
  `BaseDeDatos`.

## Procedure

Read `references/integration-method.md` before step 1. It covers the states-by-values method,
the table format and the sequence diagram template.

### 1. Delimit the integration

List the components, the external resource and each operation to test:

- `getInstancia` + `iniciarBD`,
- `guardar`,
- `cargar(id)`,
- `cargarConLimite`,
- `cargarBD`,
- `actualizar`,
- `eliminar`,
- `cerrarConexion`.

For each operation, draw a Mermaid `sequenceDiagram` traced over the real code.

The assignment item 2 starts with "corregir si es necesario el código del ítem 1". Check
`docs/informe/secciones/07-defectos.md` for open AsociadoDTO defects, because they cross into this
integration. For example, a DTO rule that rejects valid rows read from the database breaks
`cargar`.

### 2. Strategy

Use bottom-up integration with everything real. Build the table
`Componente | Real / Mock | Motivo`; here every row is "Real". Say why: the assignment requires
the real engine, and a mocked JDBC would hide exactly the SQL and mapping errors under test.

### 3. Element states

Build the table `Elemento | Estados posibles | Cómo se provoca en el test`. Elements include:

- the MySQL server (up, or down: a separate run),
- the `Asociados` table (seed data, extra row, missing row),
- the Singleton (not created, created, connection closed),
- the target row (exists, does not exist),
- the DNI (new, duplicate).

### 4. Representative values

For each method in the chain, find the values that **cross** into the next component: they
reach SQL or come back from it. Values handled entirely inside the DTO were already covered by
`unit-testing`. The schema gives boundaries the SRS doesn't:

| Column | Type in schema | Values to try |
|--------|----------------|---------------|
| nombre, apellido, calle | VARCHAR(30) | lengths 30 and 31 |
| ciudad | VARCHAR(35) | lengths 35 and 36 |
| DNI | VARCHAR(10) UNIQUE | lengths 10 and 11; duplicate |
| telefono | VARCHAR(15) | lengths 15 and 16 |
| numero | INT | 0, 1, −1 |

The DAO parameters also cross:
- `id`: 1, 20 (last seed row), 21 (missing), 0;
- `limite`: 1, 20, 21, 0;
- `dni`: existing, missing, `""`, `null`.

### 5. Combine into cases

Build the table:

| Id | Entrada | Condiciones de entrada | Salida esperada | Condiciones de salida | Resultado |
|----|---------|------------------------|-----------------|-----------------------|-----------|

Use IDs `CI1…`. Combine as the method says: each failure state with normal values, each
representative value with everything in its normal state, plus the critical combinations. Every
*condición de salida* names how it is checked independently (which SQL query).

Write steps 1–5 into `docs/informe/secciones/04-integracion-bd.md` in Spanish:

```
# Testing de integración con MySQL
## Objetivo y alcance
## Oráculo y supuestos
## Diagramas de secuencia
## Estrategia de integración
## Estados de los elementos y valores representativos
## Casos de prueba
## Resultados
## Defectos detectados y correcciones
## Fallas sembradas
## Conclusiones
```

### 6. Code the tests

Read `references/jdbc-patterns.md`. It covers:
- the independent JDBC helper,
- resetting to the seed data,
- the Singleton reset,
- exception assertions,
- the "server down" run.

- Classes with `@Tag("integration")`, for example `BaseDeDatosIntegrationTest` (Singleton,
  schema, seed data) and `DataAccessObjectIntegrationTest` (CRUD, errors, mapping).
- One method per case (`ci4_guardarPersisteTodasLasColumnas`) with `@DisplayName("CI4 – ...")`.
- `@BeforeEach` restores the seed data. Every test that touches the DB has `@Timeout`, so a hung
  connection shows up as a red and not as a stuck build.
- Assert on specific exception types (`SQLIntegrityConstraintViolationException`,
  `MysqlDataTruncation`, `SQLException`), not on `Exception`.

### 7. Run

```bash
.skills/test-report/scripts/run_tests.sh integration dev    # starts MySQL via docker compose
.skills/test-report/scripts/run_tests.sh integration rojo   # evidence on the original code
```

The "server down" cases need a separate run with MySQL stopped (see `jdbc-patterns.md` §6).
Record that run in the section as a special execution.

### 8. Analyze every red

Classify each red first:

- **Environment**: `Communications link failure`, `Access denied`,
  `Public Key Retrieval is not allowed`. This is not a defect. Fix the environment
  (`jdbc-patterns.md` §7) and rerun.
- **Wrong test**: bad setup or wrong expectation. Fix the test.
- **Defect**: register it in `07-defectos.md` with component, quoted requirement, detecting cases,
  expected, obtained and severity. Fill in `Resultado` in the case table.

Then propose fixes to the user. The assignment explicitly allows correcting the code, but each
fix needs the user's OK and its own commit (`fix(persistencia): ...`). Capture `rojo` **before**
fixing anything, then `verde` after all approved fixes.

### 9. Seed faults (after green)

Introduce one fault at a time, run `integration dev`, record what caught it, then restore with
`git checkout -- <file>`. Good faults here:

- swap two `setString` indexes in an insert,
- drop `UNIQUE` from the DNI column in `iniciarBD`,
- change the `LIMIT` parameter,
- remove one seed row,
- read a column by the wrong name in the mapping.

Record them in the "Fallas sembradas" table with IDs `FI1…`.

### 10. Results and conclusions

"Resultados" gives the counts for red and green, the commits of the fixes, and a pointer to the
appendix. "Conclusiones" covers:
- the defects found, separated into SQL exceptions, mapping errors and inconsistencies (the
  three kinds the assignment names),
- what each fix changed,
- what the suite cannot see. Two examples: statements that are never closed (a resource leak, a
  code-review finding), and Singleton thread-safety, which is concurrency and not covered here.

## Never

- Mock JDBC, the DAO or BaseDeDatos in this suite: the assignment asks for real communication.
- Verify a write by reading it back with the same DAO.
- Let a test depend on another test's leftovers or on execution order.
- Call `cerrarConexion()` without restoring the Singleton afterwards. It breaks every following
  test in the JVM.
- Change the URL, user or password in `BaseDeDatos` to make the tests connect. Adapt the
  container instead.
- Simulate "server down" by pausing the container mid-run (tests hang). Use the separate run.
- Fix production code without the user's OK, or before the red evidence is captured.

## References

| File | When |
|------|------|
| `references/integration-method.md` | Before step 1 |
| `references/jdbc-patterns.md` | Step 6; §6 for the server-down run; §7 when the environment fails |
