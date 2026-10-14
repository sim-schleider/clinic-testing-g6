---
name: testing-black-box
description: "Diseña, codifica y ejecuta pruebas de caja negra según la cátedra Taller de Programación I: particiona cada entrada (parámetros y estado del escenario) en clases de equivalencia válidas e inválidas, deriva valores límite, arma las tablas de la cátedra, escribe tests JUnit 4 (Parameterized), los ejecuta con Maven/Docker y documenta pass/fail contra la SRS y el contrato. Usar cuando se pida 'pruebas de caja negra', 'clases de equivalencia de X', 'valores límite', 'particiones', 'casos de prueba desde la SRS o el contrato', 'testeá los setters o validaciones de X', 'tabla de decisión' o 'transición de estados'. NO usar para cobertura de código, grafo o complejidad ciclomática (usar testing-white-box) ni para probar varias clases juntas, base de datos, mocks o hilos (usar testing-integration)."
---

# Testing de caja negra

Produce, para una clase o un método: la tabla de clases de equivalencia y valores límite, la
tabla de casos, los tests JUnit 4 y el resultado pass/fail de cada caso, contrastado
**contra la especificación** y no contra el código.

Aplica los criterios de la cátedra (Taller de Programación I) con JUnit 4 + Mockito 4 sobre
Java 8. Es genérica: los datos de cada proyecto (SRS, contrato, clases) se leen del propio
proyecto.

Las rutas `scripts/…` y `references/…` son relativas a `.claude/skills/testing-black-box/`. Los
comandos se ejecutan desde la **raíz del proyecto** (donde está `pom.xml`), por ejemplo
`.claude/skills/testing-black-box/scripts/mvn.sh -- clean test`.

## Cuándo usar

- Probar una clase o un método contra su SRS o su contrato: setters con validaciones,
  cálculos con reglas, métodos con precondiciones.
- Consignas que piden "clases de equivalencia y valores límite" o "pruebas unitarias aisladas".
- Objetos con estados (patrón State): técnica de **transición de estados**.
- Reglas que dependen de combinaciones de condiciones: **tabla de decisión**.

**NO usar** para medir o completar la cobertura del código (usar `testing-white-box`) ni
para probar la interacción entre clases, la BD, los mocks o los hilos (usar `testing-integration`).

## Entradas

Antes de diseñar, buscar en el proyecto:

- **Consigna y especificación:** `docs/consigna/`, `docs/**/SRS*`, `docs/**/*requisitos*`, o
  lo que indique el usuario. Es el **oráculo**.
- **Contrato:** el Javadoc de la clase o método bajo prueba (`pre:`, `@param`, `@return`, `@throws`).
- **Código:** **solo la interfaz pública** (firmas, constructores, getters para observar el
  resultado). La implementación **no se lee para diseñar**; se lee recién en el Paso 8,
  para diagnosticar un rojo.
- **Conocimiento del proyecto:** si existe `docs/testing.md` o un `AGENTS.md`, revisar la
  sección que aplique (no hace falta leerlos enteros).

## Procedimiento

**OBLIGATORIO, antes del Paso 1:** leer `references/course-criteria.md` completo
(vocabulario, reglas, formatos de tabla, convenciones JUnit 4, ubicación de los artefactos).

### Paso 1: Fijar la unidad bajo prueba y su oráculo

Listar los métodos a probar y, para cada uno, **citar textualmente** el requisito que lo
gobierna (SRS §x.y o la línea del contrato). Anotar como **supuestos** las ambigüedades:
lo que la especificación no dice (¿`""` es válido si solo se prohíbe `null`?, ¿hay cota
superior?, ¿cómo se rechaza un dato inválido?). Los supuestos se le consultan al usuario o
se documentan en el diseño.

✔ Cada método tiene su requisito citado. Ningún resultado esperado depende de "lo que hace el código".

### Paso 2: Identificar las entradas y los escenarios

Las entradas son los **parámetros + el estado del objeto (escenario) + los datos
externos que lea**. Definir los escenarios (`Escenario 1: DTO recién creado`,
`Escenario 2: DTO con todos los datos válidos`, …): en un estado conocido, cumpliendo el
invariante y descritos en una línea o con un diagrama de objetos.

✔ Cada escenario se puede construir con la API pública y cumple el invariante.

### Paso 3: Particionar en clases de equivalencia

**OBLIGATORIO:** leer `references/black-box-technique.md` §1–§4 (reglas por tipo de dato,
particiones de salida, escenarios como dimensión).

Armar la tabla `Entrada | Condición (SRS/contrato) | Clases válidas | Clases inválidas |
Valores límite`, numerando las clases (V1, V2…, I1, I2…).

✔ Cada restricción de la especificación genera **al menos una clase inválida**. Cada borde
numérico o de tamaño tiene sus valores límite (el límite y sus vecinos).

### Paso 4: Derivar los casos

- Los casos válidos cubren **la mayor cantidad posible de clases válidas a la vez**.
- **Un caso por clase inválida**, con todo lo demás válido.
- Un caso por cada valor límite que no esté cubierto.
- Si el resultado depende de una **combinación de condiciones**, armar la tabla de
  decisión (§5 de la técnica). Si el objeto tiene **estados**, armar la tabla de
  transiciones (§6). **Cargar esas secciones solo si aplican.**

El **resultado esperado** tiene que ser observable y concreto: valor de retorno, estado
leído con los getters, excepción o error con su tipo. "No falla" no es un resultado esperado.

✔ Cada clase de equivalencia está cubierta por al menos un caso. Ningún caso tiene dos clases inválidas.

### Paso 5: Escribir el diseño

Guardar `docs/pruebas/caja-negra/<Clase>.md` con: requisitos citados, supuestos,
escenarios, la tabla de clases y la tabla de casos `Id | Escenario | Entrada | Clases
cubiertas | Resultado esperado | Resultado`, con Id `CN1…` y la columna Resultado vacía.

### Paso 6: Codificar en JUnit 4

**OBLIGATORIO:** leer `references/junit4-black-box.md` completo.

- Una clase `<Clase>CajaNegraTest` en `src/test/java`, en el mismo paquete que la clase bajo prueba.
- Un método por caso: `cnN_descripcion` (el Id permite cruzar la tabla con el reporte).
  Las filas válidas homogéneas pueden ir en un `Parameterized`.
- El resultado se observa **solo por la API pública**.
- Si al `pom.xml` le faltan JUnit, Surefire o JaCoCo, agregarlos según
  `references/execution-environment.md` §1 y avisarle al usuario qué se agregó.

✔ Cada fila de la tabla tiene su test y el proyecto compila (`scripts/mvn.sh -- test-compile`).

### Paso 7: Ejecutar y documentar

**OBLIGATORIO si es la primera ejecución en el proyecto o si falla la build:** leer
`references/execution-environment.md`.

1. `scripts/mvn.sh --log docs/pruebas/resultados/<fecha>-caja-negra.log -- -Dtest='<Clase>CajaNegraTest' clean test`
2. `python3 -I scripts/surefire_summary.py --clase '<Clase>CajaNegraTest' --titulo "Caja negra <Clase>" --salida docs/pruebas/resultados/<fecha>-caja-negra.md`
3. Completar la columna **Resultado** de la tabla de casos con pass/fail.

✔ Cada caso de la tabla tiene su test y su resultado.

### Paso 8: Analizar los rojos e informar

Por cada test en rojo:
1. Confirmar que **falla por la razón correcta**: el mensaje corresponde a la regla
   probada y no a un error del test (un NPE en el setup, el escenario mal armado).
2. Recién ahora se puede **leer la implementación** para ubicar el defecto.
3. Informar: **caso, esperado (con la cita de la SRS), obtenido y causa probable**.

No corregir el código de producción ni el test sin el OK del usuario. Sugerir pasar por
`testing-white-box` si queda código sin ejercitar.

✔ Ningún rojo queda sin explicar. Los supuestos del Paso 1 quedan confirmados o documentados.

## Salidas

| Archivo | Contenido |
|---------|-----------|
| `docs/pruebas/caja-negra/<Clase>.md` | Requisitos citados, supuestos, escenarios, tabla de clases, tabla de casos |
| `src/test/java/<paquete>/<Clase>CajaNegraTest.java` | Tests JUnit 4, uno por caso (`cnN_…`) |
| `docs/pruebas/resultados/<fecha>-caja-negra.md` + `.log` | Resultados pass/fail y log de Maven |

## NUNCA

- **Diseñar leyendo la implementación**: los casos copian lo que el código hace y dejan de
  detectar lo que no hace.
- Poner **dos clases inválidas en el mismo caso**: la primera que se rechaza oculta a la segunda.
- Olvidar el **escenario como entrada**: el mismo método con el objeto vacío y con datos
  son clases distintas.
- Verificar solo el getter del campo que se cambió: un setter que escribe **otro campo**
  pasa igual. Hay que usar **valores distintos en cada campo** y comprobar todos los getters
  relevantes.
- Probar un rechazo por `assert` sin `-ea`: da rojos falsos (Surefire lo activa; el IDE no).
- Convertir un supuesto en requisito sin decirlo: si la SRS no lo dice, va como supuesto.
- Comparar `double` sin delta, o usar `assertEquals(real, esperado)` con los argumentos invertidos.
- Usar el comportamiento actual del código como resultado esperado.
- Cambiar un test o el código de producción para que un rojo pase a verde sin el OK del usuario.

## Referencias

| Archivo | Cuándo cargarlo |
|---------|-----------------|
| `references/course-criteria.md` | **Siempre**, antes del Paso 1 |
| `references/black-box-technique.md` | Paso 3 (§1–§4); Paso 4: §5 solo con tabla de decisión, §6 solo con estados; §7 si hacen falta ejemplos resueltos |
| `references/junit4-black-box.md` | Paso 6 |
| `references/execution-environment.md` | Paso 6 si falta algo en el `pom`; Paso 7 en la primera ejecución o si falla Maven |

**No cargar** `references/execution-environment.md` §3 (MySQL): la caja negra es aislada. Si
la clase necesita una BD, es trabajo de `testing-integration`.

Los archivos `course-criteria.md`, `execution-environment.md` y `scripts/` son **comunes**:
vienen de `testing-skill-creator` y no se editan acá.
