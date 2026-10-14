---
name: testing-white-box
description: "Diseña, codifica y ejecuta pruebas de caja blanca con el procedimiento de 5 pasos de la cátedra Taller de Programación I: grafo de flujo numerado, complejidad ciclomática de McCabe V(G) por las 3 fórmulas, caminos básicos independientes, tabla de casos T1..Tn, tests JUnit 4, ejecución con Maven/Docker y cobertura con JaCoCo para completar los casos faltantes. Cubre los criterios de sentencia, decisión, condición (simple, múltiple, mínima múltiple) y camino. Usar cuando se pida 'caja blanca', 'cobertura de código', 'grafo de control o de flujo', 'complejidad ciclomática', 'McCabe', 'caminos básicos', 'cobertura de decisión o de condición', 'qué líneas no se ejecutan' o 'completá casos hasta el 100%'. NO usar para derivar casos desde la SRS sin mirar el código (usar testing-black-box) ni para interacción entre clases, BD, mocks o hilos (usar testing-integration)."
---

# Testing de caja blanca

Produce, para uno o más métodos: el código numerado, el grafo de flujo, V(G) por las 3
fórmulas, los caminos básicos, la tabla de casos T1…Tn, los tests JUnit 4 y la cobertura
medida con JaCoCo, con los casos que falten agregados o justificados.

Aplica los criterios de la cátedra (Taller de Programación I) con JUnit 4 + Mockito 4 sobre
Java 8. Es genérica: los datos de cada proyecto (SRS, contrato, clases) se leen del propio
proyecto.

Las rutas `scripts/…` y `references/…` son relativas a `.claude/skills/testing-white-box/`. Los
comandos se ejecutan desde la **raíz del proyecto** (donde está `pom.xml`), por ejemplo
`.claude/skills/testing-white-box/scripts/mvn.sh -- clean test`.

## Cuándo usar

- Asegurar que se ejecuten todas las sentencias, ambas salidas de cada decisión y todos los
  caminos independientes de un método.
- Calcular la complejidad ciclomática de McCabe y derivar el conjunto básico de caminos.
- Completar los casos de una suite de caja negra que deja código sin ejercitar (orden de la
  cátedra: primero caja negra, después medir y completar con caja blanca).
- Responder "¿qué líneas o ramas no cubren mis tests?".

**NO usar** para diseñar desde la SRS sin mirar el código (usar `testing-black-box`) ni
para probar la interacción entre clases, la BD, los mocks o los hilos (usar `testing-integration`).

## Entradas

- **Código completo** de los métodos bajo prueba: es la base del diseño.
- **Especificación** (SRS en `docs/consigna/`, `docs/**/SRS*`) y **contrato** (Javadoc):
  **el resultado esperado sigue saliendo de acá**, no del código. La caja blanca elige las
  *entradas* mirando el código; el *oráculo* sigue siendo la especificación.
- **Tests existentes** (por ejemplo `*CajaNegraTest`): se mide primero su cobertura para no
  duplicar casos.
- **Conocimiento del proyecto:** `docs/testing.md` o `AGENTS.md` si existen (solo la sección que aplique).

## Procedimiento

**OBLIGATORIO, antes del Paso 1:** leer `references/course-criteria.md` completo, y
`references/white-box-technique.md` §1–§3 (grafo por construcción de Java, V(G), caminos básicos).

### Paso 1: Numerar el código

Copiar el método al diseño y marcar, como en las diapositivas: **cada condición simple**
(en `a || b` y en `a && b`, una por condición) y **cada bloque de sentencias
consecutivas**, con números únicos `(1)…(n)`.

✔ Cada `if`, `while`, `for`, `case`, `catch`, `?:`, `&&`, `||` y `assert` tiene su
número. Ninguna sentencia quedó sin bloque.

### Paso 2: Obtener el grafo de flujo

Dibujarlo en Mermaid (`flowchart TD`) con las aristas de decisión rotuladas `V`/`F`, y
listar los nodos y los arcos. Plantilla en `references/white-box-technique.md` §7.

✔ Cada nodo predicado tiene exactamente 2 salidas (un `switch` tiene una por `case`).
Hay un único nodo de entrada y uno de salida.

### Paso 3: Calcular V(G) por las 3 fórmulas

`arcos − nodos + 2` · `regiones` (las cerradas + la exterior) · `nodos predicado + 1`.

✔ **Las 3 dan lo mismo.** Si no coinciden, el grafo está mal: volver al Paso 2.

### Paso 4: Definir los caminos básicos

V(G) caminos independientes con el método de la *línea base* (§3): primero el camino más
simple realizable, después invertir **una decisión por vez**, de modo que cada camino nuevo
agregue al menos un arco sin recorrer. Los bucles se atraviesan como máximo una vez.

✔ Hay V(G) caminos y entre todos recorren todos los arcos. Los no realizables están
marcados y reemplazados por otro camino básico, o justificados.

### Paso 5: Decidir los criterios adicionales

- **Por defecto:** cobertura de decisión mediante los caminos básicos.
- **Condiciones compuestas** (`&&`, `||`): agregar la **mínima cobertura de condición
  múltiple** (§4). **Cargar §4 solo si hay condiciones compuestas.**
- **Bucles:** casos con 0, 1 y varias iteraciones (§5). **Cargar §5 solo si hay bucles.**

### Paso 6: Diseñar los casos

Tabla `Id | Objetivo de la prueba | Datos de entrada | Procedimiento | Salida esperada |
Resultado`, con Id `T1…`. Objetivo: "Verificar camino 2: 1, 2-3, 4-5, 10, 1, 11". Salida
esperada: **según la especificación**. Guardar todo en `docs/pruebas/caja-blanca/<Clase>.md`.

✔ Cada camino básico y cada combinación de condición elegida tiene su caso con datos concretos.

### Paso 7: Codificar en JUnit 4

- `<Clase>CajaBlancaTest` en el mismo paquete, con un método por caso `tN_caminoX_…`.
- Para forzar un camino se arma el **escenario** necesario (estado del objeto,
  colecciones), siempre por la API pública.
- Si el método escribe en `System.out` o lee de `System.in`, se redirigen y se restauran en
  `@After` (§8).
- Si al `pom.xml` le falta JaCoCo, agregarlo según `references/execution-environment.md` §1 y
  avisarle al usuario.

✔ Compila y cada fila de la tabla tiene su test.

### Paso 8: Ejecutar, medir y completar

**OBLIGATORIO:** leer `references/jacoco-coverage.md`. Y `references/execution-environment.md`
si es la primera ejecución o falla la build.

1. `scripts/mvn.sh --log docs/pruebas/resultados/<fecha>-caja-blanca.log -- -Dmaven.test.failure.ignore=true -Dtest='<Clase>*Test' clean test`
   (incluir los tests de caja negra para medir la cobertura combinada).
2. `python3 -I scripts/coverage.py --clase <Clase> --salida docs/pruebas/resultados/<fecha>-cobertura-<Clase>.md`
3. `python3 -I scripts/surefire_summary.py --clase '<Clase>CajaBlancaTest' --titulo "Caja blanca <Clase>" --salida docs/pruebas/resultados/<fecha>-caja-blanca.md`
4. Comparar la **V(G) de JaCoCo** con la manual: tienen que coincidir. JaCoCo cuenta cada
   `assert` como una decisión, así que en el grafo cada `assert` es un nodo predicado.
5. Por cada línea o rama pendiente: agregar un caso (volver al Paso 6) o **justificar** por
   qué no se cubre (camino no realizable, código defensivo inalcanzable, `catch` que solo
   se provoca con un recurso externo).

✔ Los métodos bajo prueba tienen el 100% de las decisiones cubierto o cada rama faltante
justificada en el diseño. La columna Resultado está completa.

### Paso 9: Analizar los rojos e informar

Por cada rojo: confirmar que falla por la razón correcta e informar **caso, camino,
esperado (con la cita de la SRS), obtenido y línea probable del defecto**. No corregir el
código ni el test sin el OK del usuario.

## Salidas

| Archivo | Contenido |
|---------|-----------|
| `docs/pruebas/caja-blanca/<Clase>.md` | Código numerado, grafo Mermaid, V(G) por las 3 fórmulas, caminos, criterios, tabla T1…Tn, justificación de lo no cubierto |
| `src/test/java/<paquete>/<Clase>CajaBlancaTest.java` | Tests JUnit 4, uno por caso (`tN_…`) |
| `docs/pruebas/resultados/<fecha>-caja-blanca.md` + `.log` | Pass/fail y log de Maven |
| `docs/pruebas/resultados/<fecha>-cobertura-<Clase>.md` | Cobertura por método y líneas o ramas pendientes |

## NUNCA

- **Sacar la salida esperada del código**: la caja blanca elige entradas; el oráculo es la especificación.
- Tratar `a || b` o `a && b` como **un solo nodo**: se pierden caminos y V(G) sale baja.
- Calcular V(G) con **una sola fórmula**: las 3 tienen que coincidir.
- Dar por cubierto un método con el **100% de líneas**: un `if` sin `else` tiene todas las
  líneas ejecutadas y aun así le falta la rama falsa.
- Olvidar la rama falsa de un `assert`: cuenta como decisión (para JaCoCo también) y
  necesita un caso que lo dispare (con `-ea`).
- Forzar un camino no realizable tocando el código de producción o con reflexión: se documenta.
- Dejar `System.out` o `System.in` redirigidos sin restaurarlos en `@After`.
- Cambiar un test o el código de producción para que un rojo pase a verde sin el OK del usuario.

## Referencias

| Archivo | Cuándo cargarlo |
|---------|-----------------|
| `references/course-criteria.md` | **Siempre**, antes del Paso 1 |
| `references/white-box-technique.md` | §1–§3 antes del Paso 1; §4 solo con condiciones compuestas; §5 solo con bucles; §6 si hacen falta ejemplos resueltos; §7 en el Paso 2; §8 en el Paso 7 |
| `references/jacoco-coverage.md` | Paso 8 |
| `references/execution-environment.md` | Paso 7 si falta JaCoCo en el `pom`; Paso 8 en la primera ejecución o si falla Maven |
| `scripts/coverage.py` | Paso 8 (resume `target/site/jacoco/jacoco.xml`) |

**No cargar** `references/execution-environment.md` §3 (MySQL) salvo que el método bajo prueba
necesite la BD (en ese caso, conviene `testing-integration`).

Los archivos `course-criteria.md`, `execution-environment.md`, `mvn.sh`, `mysql.sh` y
`surefire_summary.py` son **comunes**: vienen de `testing-skill-creator` y no se editan acá.
`scripts/coverage.py` es propio de esta skill.
