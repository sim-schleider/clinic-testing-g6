---
name: testing-skill-creator
description: "Crea, actualiza y valida skills de testing de software que siguen los criterios de la cátedra Taller de Programación I (UNMdP): escenarios, casos de prueba, tablas de la cátedra, JUnit 4 + Mockito 4 sobre Java 8, ejecución con Maven/Docker y evidencia en verde y en rojo. Mantiene una fuente común de criterios y scripts que copia en cada skill generada. Usar cuando se pida 'creá una skill de testing', 'nueva skill de caja negra / caja blanca / integración / unitario / persistencia / mocks / concurrencia', 'actualizá los criterios de las skills de testing', 'sincronizá las skills de testing' o 'validá la skill de testing'. NO usar para diseñar, escribir o ejecutar tests (usar testing-black-box, testing-white-box o testing-integration) ni para skills que no son de testing (usar skill-creator)."
---

# Creador de skills de testing (criterios de la cátedra)

Genera skills de técnicas de testing con una estructura común, los criterios de la cátedra
y los mismos scripts de ejecución, y las mantiene sincronizadas.

## Arquitectura

```
.claude/skills/
├── testing-skill-creator/            ← esta skill: FUENTE de lo común
│   ├── assets/common/                 ← se copia tal cual a cada skill hija
│   │   ├── references/course-criteria.md   (vocabulario, reglas, tablas, JUnit 4)
│   │   ├── references/execution-environment.md   (pom, Maven/Docker, MySQL, problemas frecuentes)
│   │   └── scripts/ mvn.sh · mysql.sh · surefire_summary.py
│   ├── assets/template/SKILL.md.tmpl
│   ├── references/ techniques-canon.md · skill-design-guide.md
│   └── scripts/ new_skill.py · sync_common.py · validate_skill.py
└── testing-<technique>/                ← skills hijas (SKILL.md + references propias + copia de lo común)
```

**Regla:** lo común se edita **solo** en `assets/common/` y se propaga con
`sync_common.py`. Una skill hija nunca edita su copia.

## Cuándo usar

- Crear una skill para una técnica o nivel de testing nuevo (unitario, persistencia,
  concurrencia, mocks, sistema…).
- Cambiar un criterio común (formato de tabla, versión de JUnit, script de ejecución) en
  todas las skills de testing a la vez.
- Validar que las skills de testing estén completas y sincronizadas.

**NO usar** para aplicar una técnica a un código (usar `testing-black-box`,
`testing-white-box` o `testing-integration`).

## Procedimiento para crear una skill

### Paso 1: Definir la skill

Acordar con el usuario (proponer los valores por defecto y preguntar solo lo que no esté claro):

| Decisión | Valor por defecto |
|----------|-------------------|
| Técnica y nivel | lo que pidió el usuario |
| Alcance | diseñar + escribir tests + ejecutar + documentar evidencia |
| Stack | JUnit 4.13.2 + Mockito 4.11.0, Java 8, Maven |
| Enfoque | genérica (criterios de la cátedra) que lee los datos del proyecto en `docs/` |
| Nombre | `testing-<technique>` en minúsculas con guiones |

✔ Están definidas la técnica, el alcance y el nombre, y no existe ya una skill que la cubra
(revisar las carpetas `testing-*`).

### Paso 2: Cargar el canon

**OBLIGATORIO:** leer `references/skill-design-guide.md` completo y, de
`references/techniques-canon.md`, **solo la sección de la técnica** (A caja negra, B caja
blanca, C integración, D unitario, E persistencia, F concurrencia). Si la técnica no está
en el canon, agregar primero su sección con el material de la cátedra que dé el usuario.

✔ Está listado el procedimiento de la cátedra para la técnica, con su formato de tabla y sus trampas.

### Paso 3: Generar el esqueleto

```bash
python3 -I .claude/skills/testing-skill-creator/scripts/new_skill.py testing-<technique> \
        --titulo "Testing de <técnica>" --descripcion "<description completa>"
```

Crea `SKILL.md` desde la plantilla, `references/` y la copia de los comunes.

### Paso 4: Escribir el SKILL.md

Completar cada `<!-- … -->` de la plantilla:

- **description:** qué hace + "Usar cuando" con frases textuales + palabras clave +
  "NO usar para … (usar …)", derivando a las skills hermanas.
- **Procedimiento:** los pasos del canon como `### Paso N: verbo + entregable`, cada uno
  con un `✔` verificable. El último paso es siempre *Ejecutar y documentar*, ya incluido en
  la plantilla.
- **NUNCA:** las trampas del canon y las que surjan de la experiencia, nada obvio.
- **Referencias:** una fila por archivo, con el paso en que se carga y cuándo **no** cargarlo.

Que quede en 150–250 líneas: la teoría larga y los ejemplos van a `references/`.

### Paso 5: Escribir las references propias

- `references/<technique>-technique.md`: la teoría operativa (reglas de decisión, casos
  borde, ejemplos de la cátedra resueltos).
- `references/junit4-<technique>.md` u otras: patrones de código JUnit 4 / Mockito 4 que se
  puedan copiar y que compilen en Java 8.
- Cada una tiene que estar citada en SKILL.md con su punto de carga (el validador avisa si no).

### Paso 6: Validar

```bash
python3 -I .claude/skills/testing-skill-creator/scripts/validate_skill.py .claude/skills/testing-<technique>
```

Corregir hasta llegar a **0 errores**. Los avisos (⚠) se revisan y se resuelven o se justifican.

### Paso 7: Probar en un caso real

Aplicar la skill nueva, como mínimo el diseño, sobre una clase real del proyecto. Revisar
que las tablas tengan el formato de la cátedra, que los pasos se entiendan sin contexto
extra y que los puntos de control detecten un análisis incompleto. Corregir la skill con
lo que se aprenda.

### Paso 8: Registrar

Agregar la skill a la lista de skills del `AGENTS.md` del proyecto (si existe), con una
línea sobre cuándo usarla.

## Procedimiento para cambiar un criterio común

1. Editar el archivo en `assets/common/` (nunca la copia de una skill hija).
2. `python3 -I .claude/skills/testing-skill-creator/scripts/sync_common.py`
3. `python3 -I .claude/skills/testing-skill-creator/scripts/validate_skill.py .claude/skills/testing-*`
4. Si el cambio afecta pasos de las skills (por ejemplo, un formato de tabla nuevo), revisar
   el SKILL.md de cada hija.

## NUNCA

- Editar `course-criteria.md`, `execution-environment.md` o `scripts/` dentro de una skill
  hija: la próxima sincronización pisa el cambio.
- Copiar la teoría completa al SKILL.md: el cuerpo es el procedimiento y la teoría va en
  `references/` con su punto de carga.
- Escribir una description sin las frases que diría el usuario ni sin el "NO usar para":
  la skill no se activa, o se activa en lugar de su hermana.
- Poner ejemplos de código que no compilen en Java 8: lambdas sí, `var` y records no;
  Mockito 5 tampoco.
- Dar una skill por terminada sin pasar el validador y sin probarla en un caso real.

## Referencias

| Archivo | Cuándo cargarlo |
|---------|-----------------|
| `references/skill-design-guide.md` | Paso 2, siempre que se cree una skill |
| `references/techniques-canon.md` | Paso 2, **solo la sección de la técnica** |
| `assets/common/references/course-criteria.md` | Para cambiar un criterio común; no hace falta para crear una skill |
| `assets/common/references/execution-environment.md` | Para cambiar versiones o la forma de ejecutar |
| `assets/template/SKILL.md.tmpl` | No se lee a mano: lo usa `new_skill.py` |
