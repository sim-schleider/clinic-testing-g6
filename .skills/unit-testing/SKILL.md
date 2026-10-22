---
name: unit-testing
description: Design, write and run isolated unit tests (JUnit 5) for a single Java class against its specification using equivalence partitioning and boundary value analysis, then write the design, results and defects into the report section. Use this whenever the user wants unit tests or "pruebas unitarias", "testing atómico", "clases de equivalencia", "valores límite", "particiones", test cases for setters, getters or integrity validations of a DTO or value object (AsociadoDTO, Domicilio, …), or wants to prove that a class's validations catch bad data without starting MySQL or any other service. Not for code that needs the database (db-integration-testing), mocked collaborators (mock-testing), threads or the ambulance state machine (concurrency-testing), or assembling and exporting the final report (test-report).
---

# Unit testing: equivalence classes and boundary values

For one class this skill produces: the equivalence-class and boundary-value table, the case
table, a JUnit 5 test class, the red/green results, the defects found, and the report section
`docs/informe/secciones/03-unitarias.md` with all of it written up in Spanish.

The project conventions (IDs, tags, red/green cycle, report layout) are in `.skills/CONVENTIONS.md`.

## Why the procedure is shaped this way

The assignment asks for a suite that "detects the faults or logical inconsistencies that may
exist in the provided code". A suite can only do that if its expected results come from the
**specification**. If you read the implementation first, the tests end up asserting whatever the
code does, bugs included, and stay green. So: design from the spec and the public signatures,
and only open the method bodies when a test is red and you need to explain why.

## Inputs

- **Oracle**: `docs/SRS_Persistencia_Clinica.md` (§3.1 has the AsociadoDTO rules),
  `docs/Enunciado_Trabajo_final.md`, and the Javadoc of the class (`pre:`/`post:`).
- **Code**: only the public interface while designing (constructors, setters, getters).

## Procedure

### 1. Pin down the unit and its oracle

For each method under test, quote the requirement that governs it (SRS section or Javadoc
line). Then list the **supuestos**: what the spec does not say but the tests need. Typical ones
for a DTO:

- Only `null` is forbidden. Is `""` valid? Is `"   "` valid?
- Is there an upper bound for `numero`? (The SRS gives only `>= 0`.)
- How is invalid data rejected? The project validates with Java `assert`, so the assumed
  mechanism is `AssertionError` with `-ea`. Write this down as an assumption.
- The SRS rules belong to the component, not only to the setters. Does the parameterized
  constructor have to enforce them too?

Write the supuestos in the section and ask the user about any that change expected results.

### 2. Define the scenarios

A scenario is an object in a known state that satisfies the invariant. For a DTO:
`E1` is a freshly constructed object (default constructor), and `E2` is an object loaded with
valid values that are **different in every field**. Distinct values are what expose a setter
that writes into the wrong field.

### 3. Partition into equivalence classes

Read `references/partitioning.md` (rules per data type and how to pick boundary values).
Build the table with V/I numbering:

| Entrada | Condición (SRS/contrato) | Clases válidas | Clases inválidas | Valores límite |
|---------|--------------------------|----------------|------------------|----------------|

Every restriction in the spec yields at least one invalid class. Every numeric bound gets the
bound value and its neighbours. For `numero >= 0` that means −1, 0 and 1, plus the type extremes.

### 4. Derive the cases

- Valid cases cover as many valid classes at once as possible.
- **One invalid class per case**, with everything else valid. If a case has two invalid inputs,
  the first rejection hides the second.
- Add one case for each boundary value not covered yet.
- Every expected result must be observable and concrete: the getter values, or `AssertionError`.
  "No falla" is not an expected result.

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-----------|------------------|------------------|--------------------|-----------|

IDs are `CU1…`. Leave `Resultado` empty until the suite runs.

### 5. Write the design into the report section

Create or update `docs/informe/secciones/03-unitarias.md` in Spanish, with these headings
(`#` for the section; the export numbers them):

```
# Testing unitario de AsociadoDTO
## Objetivo y alcance
## Oráculo y supuestos
## Diseño de escenarios
## Clases de equivalencia y valores límite
## Casos de prueba
## Resultados
## Defectos detectados
## Fallas sembradas
## Conclusiones
```

The section is the formal design the assignment asks for, so it has to stand on its own:
someone reading only the report should be able to follow why each case exists.

### 6. Code the tests

Read `references/junit5-unit.md` for the class skeleton, the `-ea` guard, parameterized rows with
IDs, and the assertion patterns.

- `<Clase>Test` in the same package as the class, with `@Tag("unit")`.
- One method per case (`cu3_numeroCeroValido`) with `@DisplayName("CU3 – ...")`. Homogeneous rows
  can share a `@ParameterizedTest` whose first argument is the case ID.
- Observe results only through the public API. Check **all** getters after a setter, not just
  the one that changed.
- No database, no files, no network. If the class can't be tested without them, it belongs to
  `db-integration-testing`.

### 7. Run

```bash
.skills/test-report/scripts/run_tests.sh unit dev    # while iterating
.skills/test-report/scripts/run_tests.sh unit rojo   # evidence: original code
```

Use `rojo` only on the original code, with nothing but the build fixes applied. If the build
does not compile, see the "Known build blocker" note in `.skills/CONVENTIONS.md`.

### 8. Analyze every red

For each failing test:
1. Confirm it fails **for the right reason**: the message matches the rule under test, not a
   mistake in the test (an NPE in setup, a missing `-ea`).
2. Only now read the implementation to locate the defect.
3. Add a row to `docs/informe/secciones/07-defectos.md`: component, description, quoted
   requirement, detecting case IDs, expected, obtained, severity.
4. Fill in the `Resultado` column of the case table.

Do not change production code or the test to turn a red green. Propose the fix to the user, and
only after they approve it apply it as its own commit (`fix(persistencia): ...`). Then run
`verde`.

### 9. Seed faults (after green)

This is the proof that the suite detects faults. On the green code, introduce **one fault at a
time** in the class under test, run `run_tests.sh unit dev`, record which case caught it, and
restore the file with `git checkout -- <file>`. Good faults for a DTO:

- shifting a boundary (`>=` → `>`),
- deleting one validation,
- a setter that assigns the wrong field,
- a getter that returns another field.

Record them in the section's "Fallas sembradas" table:

| Id | Falla sembrada | Ubicación | Detectada por | Resultado |
|----|----------------|-----------|---------------|-----------|

IDs are `FU1…`. A fault nobody catches means a missing case: add the case and rerun.

### 10. Write results and conclusions

Fill in "Resultados": counts in red and green, and a pointer to the appendix and screenshots
that `test-report` generates. "Conclusiones" should cover:

- what the suite proved,
- which defects it found and which requirement each violates,
- which supuestos turned out to matter,
- the limits of the suite (for example, `assert` only works with `-ea`, so these validations
  vanish in production unless that flag is set).

## Never

- Take an expected result from the implementation. A copied bug becomes a green test.
- Put two invalid classes in one case.
- Verify only the getter of the field that changed. A setter that writes another field passes.
- Expect an `AssertionError` without the `-ea` guard: the test reports a defect that isn't there.
- Turn a supuesto into a requirement silently. If the SRS doesn't say it, label it as a supuesto.
- Leave a seeded fault in the code or commit it.

## References

| File | When |
|------|------|
| `references/partitioning.md` | Step 3; the worked example only if the table is unclear |
| `references/junit5-unit.md` | Step 6 |
