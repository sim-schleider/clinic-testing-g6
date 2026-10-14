# Guía de diseño de skills de testing

Reglas para que una skill de testing **se active cuando corresponde** y **guíe de verdad**
al agente. Están condensadas de los criterios de `skill-judge` y adaptadas a testing.

## 1. Frontmatter

```yaml
---
name: testing-<technique>            # minúsculas, números y guiones; ≤ 64; igual al nombre de la carpeta
description: "<QUÉ hace>. Usar cuando <CUÁNDO: frases textuales que diría el usuario>.
  <PALABRAS CLAVE>. NO usar para <X> (usar <otra-skill>)."
---
```

- **La descripción es lo único que el agente ve antes de activar la skill.** Si no tiene
  las palabras que el usuario va a decir, la skill no se usa nunca.
- Tiene que tener: **qué hace** (técnica, entregables), **cuándo** (frases entre comillas,
  en el registro del usuario: "armá los casos de caja negra", "clases de equivalencia de
  X"), **palabras clave** (valores límite, particiones, grafo, McCabe, mocks, stub…) y
  **cuándo NO usarla**, derivando a la skill hermana.
- Hasta 1024 caracteres. En español, porque el usuario escribe en español.

## 2. Patrón: *Process* (~150–250 líneas en SKILL.md)

Las skills de técnica de testing son procesos con fases y puntos de control:

```
# Título
Una línea: qué produce.
## Cuándo usar / NO usar
## Entradas                  ← qué leer del proyecto (SRS, contrato, código, docs/)
## Procedimiento
### Paso 1 …                 ← verbo + entregable + ✔ punto de control
### Paso N: Ejecutar y documentar
## Salidas                   ← archivos y formato exacto
## NUNCA                     ← errores caros, no obviedades
## Referencias               ← cuándo cargar cada una y cuándo NO
```

El procedimiento va en SKILL.md. La teoría detallada, las tablas largas y los ejemplos van en
`references/` y se cargan **en el paso que los necesita**.

## 3. Lo que agrega la skill (*knowledge delta*)

No explicar lo que el agente ya sabe (qué es JUnit, qué es un `if`). Sí incluir:

- **Lo propio de la cátedra**: vocabulario (escenario, caso de prueba, prueba con
  propósito), formatos de tabla, orden de trabajo y procedimientos exactos (los 5 pasos de
  caja blanca, los 3 pasos de integración).
- **Las trampas que cuestan horas**: asertos sin `-ea`, Mockito 5 con Java 8, el Singleton
  que conserva estado entre tests, `assertEquals` con argumentos invertidos, tests de
  concurrencia sin timeout, un rojo que falla por un error del test.
- **Decisiones con criterio**: cuándo partir una clase de equivalencia, cuándo un mock y
  cuándo la dependencia real, cuántos casos alcanzan.

Prueba: un experto que lee la lista NUNCA tendría que pensar "esto lo aprendí a los golpes",
no "obvio".

## 4. Grado de libertad según el riesgo

| Parte de la skill | Libertad | Por qué |
|-------------------|----------|---------|
| Análisis (qué particiones, qué estados) | Media: criterios + ejemplos | Hace falta criterio, pero con método |
| Formato de tablas y nombres | **Baja**: plantilla exacta | El informe tiene que coincidir con la cátedra |
| Ejecución (Maven, Docker, MySQL) | **Baja**: scripts fijos | Un error acá hace perder mucho tiempo |
| Corrección de código de producción | **Ninguna** sin el OK del usuario | Puede tapar el defecto que la consigna pide detectar |

## 5. Carga progresiva de referencias

- **SKILL.md** con menos de 500 líneas (lo ideal es menos de 250).
- **Cada referencia se carga desde un paso**, con una instrucción explícita:
  `**OBLIGATORIO:** antes de este paso, leé references/x-technique.md completo.`
- **Indicar también lo que NO hay que cargar**: `No cargar references/concurrency.md si
  no hay hilos.`
- Los archivos comunes (`course-criteria.md`, `execution-environment.md`, `scripts/`)
  vienen de la meta-skill. **No se editan en la skill hija.**

## 6. Puntos de control

Cada paso termina con un `✔` verificable: "la tabla tiene al menos una clase inválida por
cada restricción de la SRS", "las 3 fórmulas de V(G) dan lo mismo", "cada estado del
elemento aparece en al menos un caso". Eso evita que el agente avance con un análisis a medias.

## 7. Antes de dar la skill por terminada

1. `python3 -I scripts/validate_skill.py <carpeta>` sin errores.
2. **Probarla en un caso real**: aplicar al menos el diseño sobre una clase del proyecto y
   revisar que las tablas salgan con el formato de la cátedra.
3. Opcional: evaluarla con la skill global `skill-judge`.
