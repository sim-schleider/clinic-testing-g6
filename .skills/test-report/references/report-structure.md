# Informe structure and writing guidelines

## 1. Order of the document

The export concatenates `docs/informe/secciones/*.md` by file name, after the carátula and an
automatic index:

| File | Section | Written by |
|------|---------|------------|
| `caratula.html` | Carátula (university, faculty, course, title, group, members, teachers, date) | test-report |
| `01-introduccion.md` | 1. Introducción | test-report |
| `02-estrategia.md` | 2. Estrategia de testing | test-report |
| `03-unitarias.md` | 3. Testing unitario de AsociadoDTO (consigna 1) | unit-testing |
| `04-integracion-bd.md` | 4. Testing de integración con MySQL (consigna 2) | db-integration-testing |
| `05-mocks.md` | 5. Testing con mocks (consigna 3) | mock-testing |
| `06-concurrencia.md` | 6. Integración y concurrencia (consigna 3) | concurrency-testing |
| `07-defectos.md` | 7. Registro de defectos | every skill (one row per defect) |
| `08-conclusiones.md` | 8. Conclusiones | test-report |
| `90-apendice-resultados.md` | Apéndice A: Resultados de ejecución | generated |
| `91-apendice-capturas.md` | Apéndice B: Capturas | generated |
| `92-apendice-entorno.md` | Apéndice C: Entorno y reproducción | test-report |
| `93-apendice-codigo.md` | Apéndice D: Código fuente de las suites | generated |

Heading levels: each file starts with one `#` heading, its subsections are `##` and `###`.
Section numbers are added on export (`--number-sections`), so don't write them by hand.
Appendices carry `{.unnumbered}`.

## 2. What each general section must say

**Introducción**:
- the purpose of the work, in the assignment's terms: atomic (unit) testing versus integration
  with external resources;
- the system under test, limited to the modules involved;
- the scope, and what is out of scope;
- the reference documents used as oracle.

**Estrategia**:
- the table of levels (component, resource, technique, assignment item), and why each item is
  tested at that level;
- the oracle rule: the specification, not the code, and how supuestos are handled;
- the red/green cycle and fault seeding, explained as the method for "demostrar que la suite
  detecta los fallos";
- the environment and the ID conventions.

**Conclusiones** (written last, from the evidence):
1. A summary table:
   `Suite | Casos | Fallidos en rojo | Fallidos en verde | Defectos | Fallas sembradas detectadas`.
2. Effectiveness: which level found which kind of defect, and why another level would have
   missed it. This is the core argument of a *testing strategy*.
3. Limitations: what a green suite doesn't guarantee, the supuestos taken, and what was found
   by code review instead of tests.
4. Lessons learned: concrete, tied to this project, not generic.

## 3. Writing guidelines

- Use Spanish (rioplatense academic register), impersonal or first person plural ("se diseñaron",
  "diseñamos"), and stay consistent within the document.
- Every claim about the code is backed by a case ID, a defect ID or an appendix reference.
  "La suite detectó el defecto D4 (caso CI9, ver Apéndice A)" beats "la suite funciona bien".
- Quote requirements textually, with their source ("SRS §3.1: *Debe ser mayor o igual a 0*").
- Before each table, one or two sentences on what it shows. Never leave a table without context.
- Mark supuestos explicitly, keeping them separate from the requirements.
- Name code identifiers in `code` format, and keep them exactly as in the code (`setNumero`, not
  "set número").
- Don't paste large chunks of code. Cite the file and line (`DataAccessObject.java:85`) and, at
  most, the 2–5 relevant lines.
- Write figure captions as the image alt text (`![Suite unitaria, en rojo](../capturas/unit-rojo.png)`);
  pandoc turns it into a caption.

## 4. Defect descriptions

A good row lets a reader reproduce the defect without the code open:

| ID | Componente | Descripción | Oráculo violado | Detectado por | Esperado | Obtenido | Severidad | Estado |
|----|------------|-------------|-----------------|---------------|----------|----------|-----------|--------|
| D3 | `AsociadoDTO.setNumero` | Rechaza el número 0, que la SRS admite | SRS §3.1: "Debe ser mayor o igual a 0" | CU10 | `numero = 0` aceptado | `AssertionError` | Media | Corregido (`a1b2c3d`) |

- **Descripción** says what is wrong, not the fix.
- **Obtenido** quotes the failure message or the observed state from the results.
- **Estado** is updated when the fix is committed. "No se corrige" requires a reason, for
  example "fuera del alcance de la consigna" or "decisión de diseño documentada en el informe
  de la aplicación".
