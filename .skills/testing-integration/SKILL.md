---
name: testing-integration
description: "Diseña, codifica y ejecuta pruebas de integración con el método de la cátedra Taller de Programación I: elige estrategia (ascendente, o descendente con stubs y mocks), enumera los estados de los elementos (objetos, BD, archivos, hilos) y los valores que atraviesan la interacción, combina estados por valores en la tabla Entrada / Condiciones de entrada / Salida esperada / Condiciones de salida y escribe tests JUnit 4 + Mockito 4 contra MySQL real en Docker, persistencia, mocks y concurrencia (synchronized, wait/notify, hilos). Usar cuando se pida 'pruebas de integración', 'testeá el DAO, la base de datos o el CRUD', 'Singleton de conexión', 'mocks', 'stubs', 'Mockito', 'patrón State con mocks', 'concurrencia', 'hilos', 'deadlock' o 'casos desde el diagrama de secuencia'. NO usar para una sola clase aislada desde la SRS (usar testing-black-box) ni para cobertura por caminos (usar testing-white-box)."
---

# Testing de integración

Produce, para una interacción entre componentes: el diagrama de secuencia, la estrategia,
la tabla de estados de los elementos, los valores representativos, la tabla de casos de
integración de la cátedra, los tests JUnit 4 (con MySQL real, mocks o hilos, según
corresponda) y el resultado pass/fail de cada caso.

Aplica los criterios de la cátedra (Taller de Programación I) con JUnit 4 + Mockito 4 sobre
Java 8. Es genérica: los datos de cada proyecto (SRS, contrato, clases) se leen del propio
proyecto.

Las rutas `scripts/…` y `references/…` son relativas a `.claude/skills/testing-integration/`. Los
comandos se ejecutan desde la **raíz del proyecto** (donde está `pom.xml`), por ejemplo
`.claude/skills/testing-integration/scripts/mvn.sh -- clean test`.

## Cuándo usar

- Varias clases que colaboran: DTO → DAO → conexión, modelo → controlador, contexto → estados.
- Recursos externos reales: base de datos (CRUD, esquema, datos semilla, Singleton de
  conexión) o archivos (persistencia).
- Reemplazar dependencias con **stubs o mocks** (Mockito), incluido el patrón State con
  estados simulados.
- **Concurrencia**: recursos compartidos, `synchronized`, `wait`/`notify`, hilos que
  compiten, terminación ordenada.

**NO usar** para una sola clase aislada desde la SRS (usar `testing-black-box`) ni para la
cobertura por caminos de un método (usar `testing-white-box`).

## Entradas

- **Consigna y especificación:** `docs/consigna/`, `docs/**/SRS*` (el oráculo).
- **Diseño:** diagramas de secuencia o de clases (`docs/informe/`, `UML*.png`): de ahí salen
  las interacciones y, para las máquinas de estado, las transiciones esperadas.
- **Código:** las clases que interactúan, para trazar las llamadas reales y encontrar los
  **puntos de inyección** (constructor, setters como `setState`, `setServicio`).
- **Configuración de recursos:** la URL JDBC, el usuario y la clave están en la clase de
  conexión (buscar `jdbc:`); también las rutas de archivos y los intervalos de los hilos.
- **Conocimiento del proyecto:** `docs/testing.md` o `AGENTS.md` si existen (solo la sección que aplique).

## Procedimiento

**OBLIGATORIO, antes del Paso 1:** leer `references/course-criteria.md` completo y
`references/integration-technique.md` §1–§5.

### Paso 1: Delimitar la integración

Listar los componentes, los recursos externos y las interacciones a probar. Por cada
operación, dibujar el **diagrama de secuencia** en Mermaid (`sequenceDiagram`, plantilla en
§5), trazado sobre el código real.

✔ Cada interacción tiene su diagrama. Las unidades que intervienen ya están probadas, o el
riesgo de que no lo estén queda anotado.

### Paso 2: Elegir la estrategia y qué va real y qué va con mock

Por defecto, **ascendente** (de las hojas hacia arriba, sin stubs). **Descendente con
mocks** si la dependencia no existe, no es determinística, es lenta, tiene un estado
difícil de reproducir, o si **la consigna pide mocks**. Armar la tabla
`Componente | Real / Mock | Motivo`.

✔ Ningún mock reemplaza la interacción que se quiere probar.

### Paso 3: Estados de los elementos

Tabla `Elemento | Estados posibles | Cómo se provoca en el test` (objeto presente o
ausente, BD activa, inaccesible o con la tabla ausente, recurso ocupado o libre, hilo en
espera…). Las formas de provocar cada estado están en §2.

✔ Cada elemento tiene al menos su estado normal y uno de falla, y cada estado se puede provocar.

### Paso 4: Valores adecuados

Por cada método de la cadena: sus particiones y, sobre todo, los valores que **atraviesan**
hacia el componente siguiente (*AtraviesaB*, §3). Los que no atraviesan ya se cubrieron en
la prueba unitaria.

### Paso 5: Combinar en casos

Tabla `Id | Entrada | Condiciones de entrada | Salida esperada | Condiciones de salida |
Resultado`, con Id `CI1…`, combinando como en la cátedra (§4): **cada estado de falla con
el valor normal** y **cada valor representativo con todo en estado normal**, más las
combinaciones críticas. Las **condiciones de salida** se verifican por un camino
**independiente** del componente probado (§4).

Guardar todo en `docs/pruebas/integracion/<Integracion>.md`.

✔ Cada estado y cada valor representativo aparecen en al menos un caso. Cada condición de
salida es verificable.

### Paso 6: Codificar en JUnit 4

Cargar **solo** las referencias que aplican:

- Hay BD o archivos → **OBLIGATORIO** leer `references/database-jdbc.md`.
- Hay mocks o stubs → **OBLIGATORIO** leer `references/mockito4-mocks.md`.
- Hay hilos, `synchronized` o `wait`/`notify` → **OBLIGATORIO** leer `references/concurrency.md`.

Una clase `<Componente>IntegracionTest` por interacción, con un método por caso `ciN_…`.
Cada test arranca de un estado conocido (`@Before`) y **todo test con hilos o E/S lleva
timeout**. Si al `pom.xml` le falta Mockito, el driver JDBC o Surefire, agregarlo según
`references/execution-environment.md` §1 y avisarle al usuario.

✔ Compila, y cada fila de la tabla tiene su test o su ejecución especial documentada (BD inaccesible).

### Paso 7: Ejecutar y documentar

**OBLIGATORIO si es la primera ejecución en el proyecto o si falla la build:** leer
`references/execution-environment.md`.

1. Con BD: `scripts/mysql.sh up --usuario <del código> --clave <del código>`.
2. `scripts/mvn.sh [--mysql tpe-mysql] --log docs/pruebas/resultados/<fecha>-integracion.log -- -Dtest='*IntegracionTest' clean test`
3. Los estados que se provocan desde afuera (por ejemplo, la BD inaccesible con
   `scripts/mysql.sh lock`) van en una ejecución aparte, como indica `database-jdbc.md` §6.
4. `python3 -I scripts/surefire_summary.py --clase '*IntegracionTest' --titulo "Integración <X>" --salida docs/pruebas/resultados/<fecha>-integracion.md`
5. `scripts/mysql.sh down` al terminar.

✔ Cada caso tiene su resultado. Los timeouts se tratan como rojos (posible deadlock o un
`wait` sin `notify`), no como fallas del entorno.

### Paso 8: Analizar los rojos e informar

Por cada rojo: confirmar que falla por la razón correcta (un `Communications link failure`
es del entorno, no un defecto) e informar **caso, condiciones de entrada, esperado (con la
cita de la SRS o del diseño), obtenido y componente sospechoso**. No corregir el código
ni el test sin el OK del usuario.

## Salidas

| Archivo | Contenido |
|---------|-----------|
| `docs/pruebas/integracion/<Integracion>.md` | Diagramas de secuencia, estrategia, real frente a mock, estados, valores, tabla de casos |
| `src/test/java/<paquete>/<Componente>IntegracionTest.java` | Tests JUnit 4 (`ciN_…`) |
| `docs/pruebas/resultados/<fecha>-integracion.md` + `.log` | Pass/fail y log de Maven |

## NUNCA

- **Mockear la interacción que se está probando** o la clase bajo prueba: deja de ser integración.
- Usar mocks donde la consigna pide **comunicación real** con la BD.
- **Verificar una escritura con el mismo componente que escribió** (guardar con el DAO y
  leer con el DAO): un error simétrico, como columnas cruzadas, pasa invisible. Se
  verifica con una consulta JDBC propia.
- Dejar la BD sucia entre tests o depender del orden: el `@Before` deja el estado conocido.
- Olvidar que un **Singleton conserva su estado** entre tests y entre clases de la misma JVM
  (por ejemplo, una conexión cerrada por un test rompe los siguientes).
- Escribir un test de hilos **sin timeout**, o sincronizarlo con `Thread.sleep`.
- Hacer un `assert` dentro de otro hilo sin capturar la falla: JUnit no la ve.
- Simular la BD caída deteniendo o pausando el contenedor (los tests se cuelgan): usar `mysql.sh lock`.
- Cambiar la URL o las credenciales del código para que el test conecte: se adapta el entorno, no el código.
- Usar el comportamiento actual del código como resultado esperado.
- Cambiar un test o el código de producción para que un rojo pase a verde sin el OK del usuario.

## Referencias

| Archivo | Cuándo cargarlo |
|---------|-----------------|
| `references/course-criteria.md` | **Siempre**, antes del Paso 1 |
| `references/integration-technique.md` | §1–§5 antes del Paso 1; §6 (lista de problemas OO) en el Paso 5; §7 si hacen falta ejemplos resueltos |
| `references/database-jdbc.md` | Paso 6, **solo si hay BD o archivos** |
| `references/mockito4-mocks.md` | Paso 6, **solo si hay mocks o stubs** |
| `references/concurrency.md` | Paso 6, **solo si hay hilos o sincronización** |
| `references/execution-environment.md` | Paso 6 si falta algo en el `pom`; Paso 7 en la primera ejecución o si falla Maven o MySQL |

Los archivos `course-criteria.md`, `execution-environment.md` y `scripts/` son **comunes**:
vienen de `testing-skill-creator` y no se editan acá.
