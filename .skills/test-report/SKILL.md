---
name: test-report
description: Produce the deliverables of the testing assignment. Runs the suites and saves the red and green evidence (results tables and screenshots), keeps the defect registry, writes the general sections of the informe (introducción, estrategia, conclusiones), generates the appendices, checks everything against the "Criterios de Entrega" and exports the informe to PDF. Use this whenever the user mentions the informe or report, "capturas en verde y en rojo", evidence, screenshots of test runs, "apéndice", "resultados", "registro de defectos", "conclusiones", exporting to PDF/Word, running a suite "para la entrega", or asks what is missing before submitting. Not for designing or writing the tests themselves (unit-testing, db-integration-testing, mock-testing, concurrency-testing), although those skills call its scripts to run suites.
---

# Test report: evidence, informe and delivery check

This skill owns everything that turns test work into the deliverable:

| Assignment criterion | Produced by |
|----------------------|-------------|
| Código fuente de las suites | the testing skills; listed in the appendix by `build_appendix.py` |
| Capturas en verde y en rojo | `run_tests.sh` → `capture.py` |
| Diseño formal de escenarios, casos con clases de equivalencia y valores límite | the testing skills' sections, checked by `check_report.py` |
| Conclusiones y resultados obtenidos | sections 03–06 and 08, and appendices A–D |

The layout of `docs/informe/` and the defect registry format are in `.skills/CONVENTIONS.md`. Each
testing skill writes its own section, and this skill writes the rest, then assembles and checks
the whole. The informe is written in Spanish.

## Scripts

All of them run from the project root:

| Script | Does |
|--------|------|
| `scripts/run_tests.sh <suite> <fase> [--no-run] [-- mvn args]` | Runs a suite (unit, integration, mock, concurrency, all). For `rojo`/`verde` it saves `resultados/<suite>-<fase>.{log,md}` and `capturas/<suite>-<fase>.png`; `dev` only prints |
| `scripts/surefire_summary.py` | Surefire XML → results table with case IDs, date and commit (called by `run_tests.sh`) |
| `scripts/capture.py <resumen.md> --out <png>` | Results table → JUnit-style panel → PNG through headless Chromium |
| `scripts/build_appendix.py` | Regenerates appendices A (results), B (screenshots) and D (test code) |
| `scripts/check_report.py` | Delivery checklist; exits 1 if something blocks the delivery |
| `scripts/export_pdf.sh [--docx]` | Sections + carátula → `docs/informe/informe.pdf` (pandoc + Chromium) |

## Procedure

### 1. Set up (once)

1. Create the informe skeleton without overwriting existing files:
   ```bash
   mkdir -p docs/informe/secciones docs/informe/resultados docs/informe/capturas
   cp -n .skills/test-report/assets/secciones/*.md docs/informe/secciones/
   cp -n .skills/test-report/assets/caratula.html docs/informe/caratula.html
   ```
2. Ask the user for the carátula data: materia, group number, members, teachers and delivery
   date. Fill in the `{{…}}` placeholders. Don't invent names.
3. Make sure test names carry the case IDs into the XML. The skills use `@DisplayName("CU3 – …")`
   and parameterized rows named `"{0} – …"`. Surefire writes method names by default. To get
   display names, which is needed so parameterized rows keep their own IDs, add this inside the
   Surefire `<configuration>` in `pom.xml`, then tell the user:
   ```xml
   <statelessTestsetReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter">
       <usePhrasedTestCaseMethodName>true</usePhrasedTestCaseMethodName>
   </statelessTestsetReporter>
   ```
   Method names that start with the ID (`cu3_…`) work either way.
4. Check that the code compiles. The known build blocker is described in `.skills/CONVENTIONS.md`. Its fix is
   a production change, so ask first and commit it separately.

### 2. Capture the evidence

The evidence is only worth something if it is honest:

- **`rojo`** runs on the **original** code. Only the build fixes may be applied, and no seeded
  fault. The summary header records the commit and whether `src/main` had uncommitted changes.
  `check_report.py` rejects evidence taken with uncommitted production changes.
- **`verde`** runs after the approved fixes are **committed**.
- Run each suite separately (`unit`, `integration`, `mock`, `concurrency`), so each screenshot
  shows one assignment item.

```bash
.skills/test-report/scripts/run_tests.sh unit rojo
# ... the user approves fixes, one commit each ...
.skills/test-report/scripts/run_tests.sh unit verde
```

If Maven can't run here (no Java in WSL, and Docker without WSL integration), the user runs
`mvn test -Dgroups=unit` from IntelliJ's Maven panel. Then summarize with
`run_tests.sh unit rojo --no-run`.

The rendered screenshot reproduces the real Surefire results. If the user prefers IDE
screenshots, save them as `docs/informe/capturas/<suite>-<fase>.png` and they take precedence.

A test still red in `verde` must be explained: either an open defect, or one that won't be fixed
and why. Don't hide it.

### 3. Keep the defect registry consistent

`secciones/07-defectos.md` gets one row per defect from every skill. Here, make sure that:
- IDs are unique and consecutive (`D1…`);
- `Detectado por` names cases that exist;
- `Estado` is `Abierto`, `Corregido (<commit>)` or `No se corrige: <motivo>`.

Severity criteria are at the top of the template.

### 4. Write the general sections

Read `references/report-structure.md` for the content of each section and the writing guidelines.

- `01-introduccion.md` and `02-estrategia.md`: objective, system under test, scope, levels,
  oracle, red/green cycle, environment, conventions.
- `08-conclusiones.md`: write it **last**, from the actual numbers in the results and in the
  registry.
- `92-apendice-entorno.md`: environment adjustments that were needed.

Replace every `TODO`. The checker blocks the delivery while any remain.

### 5. Generate the appendices

```bash
python3 -I .skills/test-report/scripts/build_appendix.py
```

The appendices are rebuilt from `resultados/`, `capturas/` and `src/test/java`. Never edit the
generated files by hand: the next run overwrites them, and edited results are not evidence.

### 6. Check against the delivery criteria

```bash
python3 -I .skills/test-report/scripts/check_report.py
```

Fix every ❌, and resolve or justify every ⚠. Common fixes:
- a designed case was never run: write the test, or remove the row and explain why;
- a test has no design row: add the row;
- red evidence shows no failures: the suite doesn't demonstrate detection, so review it with the
  corresponding testing skill.

### 7. Export and review

```bash
.skills/test-report/scripts/export_pdf.sh          # docs/informe/informe.pdf
.skills/test-report/scripts/export_pdf.sh --docx   # also .docx (diagrams not rendered)
```

Open the PDF (Read tool, a few pages at a time) and check:
- the carátula and the index;
- the tables are not cut off;
- the Mermaid diagrams rendered (they need network access for mermaid.js);
- the screenshots are readable.

Only then report the delivery as ready.

## Never

- Edit a results table or a generated appendix by hand, or write results the scripts didn't
  produce.
- Label a run on modified code (seeded faults, uncommitted fixes) as `rojo` original or `verde`.
- Invent carátula data, results, defects or conclusions that the evidence doesn't support.
- Write the conclusions before the evidence exists.
- Overwrite a section that a testing skill owns (03–06), except to fix formatting with the user's
  OK.
- Commit or push anything unless the user asks.

## References

| File | When |
|------|------|
| `references/report-structure.md` | Step 4, and when reviewing any section |
