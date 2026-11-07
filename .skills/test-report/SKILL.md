---
name: test-report
description: Produce the deliverables of the testing assignment. Runs each suite once and saves the evidence (results tables of passing and failing tests), lists the screenshots the team must take, keeps the defect registry, writes the general sections of the informe (introducción, estrategia, conclusiones), generates the appendices, checks everything against the "Criterios de Entrega" and exports the informe to PDF. Use this whenever the user mentions the informe or report, "capturas en verde y en rojo", evidence, screenshots of test runs, "apéndice", "resultados", "registro de defectos", "conclusiones", exporting to PDF/Word, running a suite "para la entrega", or asks what is missing before submitting. Not for designing or writing the tests themselves (unit-testing, db-integration-testing, mock-testing, concurrency-testing), although those skills call its scripts to run suites.
---

# Test report: evidence, informe and delivery check

This skill owns everything that turns test work into the deliverable:

| Assignment criterion | Produced by |
|----------------------|-------------|
| Código fuente de las suites | the testing skills; listed in the appendix by `build_appendix.py` |
| Capturas en verde y en rojo | taken by the team from the IDE; `build_appendix.py` lists the ones still required |
| Diseño formal de escenarios, casos con clases de equivalencia y valores límite | the testing skills' sections, checked by `check_report.py` |
| Conclusiones y resultados obtenidos | sections 03–06 and 08, and appendices A–D |

The layout of `docs/informe/` and the defect registry format are in `.skills/CONVENTIONS.md`. Each
testing skill writes its own section, and this skill writes the rest, then assembles and checks
the whole. The informe is written in Spanish.

## Scripts

All of them run from the project root:

| Script | Does |
|--------|------|
| `scripts/run_tests.sh <suite> [--dev] [--no-run] [-- mvn args]` | Runs a suite (unit, integration, mock, concurrency, all) and saves `resultados/<suite>.{log,md}`; `--dev` only prints |
| `scripts/surefire_summary.py` | Surefire XML → results table with case IDs, date and commit (called by `run_tests.sh`) |
| `scripts/build_appendix.py` | Regenerates appendices A (results), B (screenshots taken by the team, and the ones still required) and D (test code) |
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

- Each suite runs **once**, on the code of its item's branch as committed. Only the build fixes
  may be applied (in a temporary copy), and no seeded fault. The summary header records the
  commit and whether `src/main` had uncommitted changes; `check_report.py` rejects evidence
  taken with uncommitted production changes.
- There are no "before/after" phases: the run shows which tests pass and which fail, and every
  failing test points to a defect in the registry.
- Run each suite separately (`unit`, `integration`, `mock`, `concurrency`), so each result
  table and each screenshot covers one assignment item.

```bash
.skills/test-report/scripts/run_tests.sh unit
```

If Maven can't run here (no Java in WSL, and Docker without WSL integration), the user runs
`mvn test -Dgroups=unit` from IntelliJ's Maven panel. Then summarize with
`run_tests.sh unit --no-run`.

**Screenshots are never produced here.** Don't render, generate or fake them (no HTML panels,
no headless browser, no images built from the results table): only a real screenshot of the
IDE counts as evidence, and taking it is the team's job. For each run:

1. Tell the user which screenshot is now required: the JUnit results panel in IntelliJ for that
   suite, saved as `docs/informe/capturas/<suite>.png`.
2. Leave the request written in the informe: `build_appendix.py` turns every missing screenshot
   into a row of the "Capturas requeridas" table in Apéndice B, and shows the ones that exist.
3. In the testing sections, refer to the screenshot by pointing to Apéndice B, never as if it
   already existed.

Every failing test must be explained: an open defect, or one that won't be fixed and why.
Don't hide it.

### 3. Keep the defect registry consistent

`secciones/07-defectos.md` gets one row per defect from every skill. Here, make sure that:
- IDs are unique and consecutive (`D1…`);
- `Detectado por` names cases that exist;
- `Estado` is `Abierto`, `Corregido (<commit>)` or `No se corrige: <motivo>`.

Severity criteria are at the top of the template.

### 4. Write the general sections

Read `references/report-structure.md` for the content of each section and the writing guidelines.

- `01-introduccion.md` and `02-estrategia.md`: objective, system under test, scope, levels,
  oracle, how the suites are run, environment, conventions.
- `08-conclusiones.md`: write it **last**, from the actual numbers in the results and in the
  registry.
- `92-apendice-entorno.md`: environment adjustments that were needed.

Replace every `TODO`. The checker blocks the delivery while any remain.

### 4b. Write the informe into the shared Google Doc

The group's informe lives in a Google Doc that the team edits together, and the Google Docs MCP
connector (`mcp__claude_ai_Google_Docs__read_doc`, `mcp__claude_ai_Google_Docs__update_doc`) is
available to write into it:

https://docs.google.com/document/d/13xLZ_v2hmLqQIIREEyYeLQkdsPMo5rQ6rHuUes3aQwM/edit?usp=sharing
(document ID `13xLZ_v2hmLqQIIREEyYeLQkdsPMo5rQ6rHuUes3aQwM`)

- Load the `anthropic-skills:google-workspace` skill before the first write.
- **One tab per item of the assignment** (`docs/Enunciado_Trabajo_final.md` §2):

  | Tab | Content (source sections) |
  |-----|---------------------------|
  | `1) Testing de AsociadoDTO` | `03-unitarias.md` + its rows of `07-defectos.md` |
  | `2) Testing de Integración` | `04-integracion-bd.md` + its rows of `07-defectos.md` |
  | `3) Testing con Mocks y Concurrencia` | `05-mocks.md`, `06-concurrencia.md` + their rows of `07-defectos.md` |

- **Never write in the main tab** (`tab=t.0`). The team merges the item tabs into it by hand.
- The local `docs/informe/` files stay the source of truth: write or update the section there
  first, then copy it to its tab. Read the tab before writing, and replace only that item's
  content; never delete text someone else added.
- If a tab is missing, create it (`addDocumentTab` request). If the connector rejects that,
  ask the user to create it by hand with the name above.
- If reading the doc returns `Permission denied`, the connected Google account has no access:
  ask the user to share the doc with that account (as editor) and stop; don't fall back to
  creating another document.
- Screenshots are pasted into the doc by the team. Leave a placeholder line
  `[Captura requerida: <suite>.png]` where each one goes and tell the user.

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
- a suite has no failing tests: it doesn't demonstrate detection, so review it with the
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
- the team's screenshots are in place and readable, and Apéndice B lists no pending one.

Only then report the delivery as ready.

## Never

- Edit a results table or a generated appendix by hand, or write results the scripts didn't
  produce.
- Generate, render or edit a screenshot, or present one as taken when it isn't. Screenshots come
  only from the team.
- Present a run on modified code (seeded faults, uncommitted fixes) as the evidence of the repository's code.
- Invent carátula data, results, defects or conclusions that the evidence doesn't support.
- Write the conclusions before the evidence exists.
- Overwrite a section that a testing skill owns (03–06), except to fix formatting with the user's
  OK.
- Commit or push anything unless the user asks.

## References

| File | When |
|------|------|
| `references/report-structure.md` | Step 4, and when reviewing any section |
