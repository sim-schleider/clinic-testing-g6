# Canon de técnicas de la cátedra (semilla de cada skill de testing)

Contenido mínimo que tiene que tener la skill de cada técnica. Salió de las clases 05
(unitarias/JUnit), 06 (caja blanca), 07 (persistencia) y 08 (integración) de Taller de
Programación I. Lo marcado **[std]** es teoría estándar que las diapositivas dan por sabida.

**Cargar solo la sección de la técnica que se está creando.**

---

## A. Caja negra [std, pedida por la consigna]

- **Objetivo:** derivar casos **solo desde la especificación** (SRS + contrato) y detectar
  requisitos mal implementados o no implementados.
- **Entradas:** la SRS, el contrato (Javadoc) y la firma pública. **No se lee la
  implementación para diseñar.**
- **Procedimiento:**
  1. Listar las entradas: parámetros **y el estado del escenario**.
  2. Particionar cada entrada en clases válidas e inválidas (rango → 1V+2I, cota → 1V+1I,
     conjunto → 1V por valor + 1I, no nulo → 1V+1I, colección → 0/1/n).
  3. Valores límite de cada borde: límite y vecinos (`a-1, a, a+1`).
  4. Casos: los válidos cubren **varias clases a la vez**; **un caso por clase inválida**,
     con el resto válido.
  5. Tablas: clases de equivalencia + casos con "clases cubiertas".
  6. JUnit 4: `@RunWith(Parameterized.class)` para las filas válidas, un `@Test` por clase inválida.
  7. Ejecutar y documentar pass/fail. Un rojo es una discrepancia con la SRS.
- **Extensiones:** tabla de decisión (combinaciones de condiciones) y transición de
  estados (patrón State: estado × evento → siguiente).
- **Trampas:** combinar dos inválidas en un caso (la primera oculta a la segunda); usar el
  código como oráculo; olvidar el escenario como entrada; no separar `""` de `null` cuando
  la SRS solo habla de `null`.
- **Ejemplos de la cátedra:** `Traductor.agregarTraduccion` (escenario vacío frente a
  escenario con 12 palabras), `Empleado.getIncremento` (la SRS contradice al código).

## B. Caja blanca (clase 06)

- **Objetivo:** ejecutar todas las sentencias, ambas salidas de cada decisión, todos los
  caminos independientes y todos los bucles. **Necesita el código.**
- **Procedimiento de 5 pasos:**
  1. **Grafo de flujo:** numerar cada condición (en `a || b` cada condición simple es un
     nodo) y agrupar las secuencias consecutivas en un nodo. Estructuras: secuencia,
     decisión, hacer, mientras.
  2. **V(G) por las 3 fórmulas**, que tienen que coincidir: `arcos − nodos + 2`;
     `regiones` (las cerradas + la exterior); `nodos predicado + 1`.
  3. **Caminos básicos independientes** (V(G) caminos; cada uno agrega al menos un arco
     nuevo; los bucles se atraviesan como máximo una vez).
  4. **Casos:** tabla `Id | Objetivo | Datos de entrada | Procedimiento | Salida esperada | Resultado`.
  5. **Ejecutar y medir la cobertura** (EclEmma/JaCoCo) y completar los casos que falten.
- **Criterios** (de menor a mayor rigor): sentencia < decisión < condición (simple <
  mínima múltiple < múltiple) < camino < flujo de datos. 100% de camino ⇒ 100% de decisión
  ⇒ 100% de sentencia. Condición múltiple: 2ⁿ casos; mínima múltiple: entre n+1 y 2ⁿ.
- **McCabe:** V(G) es la cota superior de casos para la cobertura de decisión. 1–10
  sencillo, 11–20 moderado, 21–50 alto, más de 50 inestable.
- **Trampas:** no partir las condiciones compuestas; caminos no realizables (se documentan,
  no se fuerzan); la condición simple puede no recorrer nunca el lado falso de un `OR`; los
  `return`/`break`/`throw` dentro de un bucle generan arcos extra.
- **Ejemplos de la cátedra:** media de x e y (V(G)=3), `while(a==10)` con 2 `if` anidados
  (V(G)=4, casos T1–T4).

## C. Integración (clase 08)

- **Objetivo:** probar la interacción entre unidades ya probadas y con los recursos
  externos (BD, archivos, red, hilos).
- **Estrategias:** big bang (no recomendada), descendente en profundidad o en anchura (con
  stubs/mocks) y ascendente (sin stubs).
- **Procedimiento de 3 pasos:**
  1. Por cada interacción (diagrama de secuencia), los **estados posibles** de cada
     elemento: objeto (existe / no existe / versión inadecuada), recurso compartido
     (existe / no existe / no accesible), dispositivo (listo / con problemas / desconectado).
  2. Por cada método de la cadena, los **valores adecuados**: particiones y, sobre todo,
     los que **atraviesan** hacia la clase siguiente (*AtraviesaB*).
  3. **Combinar** estados × valores → tabla `Entrada | Condiciones de entrada | Salida esperada | Condiciones de salida`.
- **Problemas OO que busca:** configuración y versiones, funciones faltantes, uso
  incorrecto de archivos o BD, integridad de datos, método equivocado por polimorfismo,
  parámetros erróneos, conflictos de concurrencia.
- **Mocks** (clase 08, Mockito): se usan cuando la dependencia no es determinística, tiene
  un estado difícil de reproducir, es lenta o todavía no existe. `mock`, `when…thenReturn`,
  inyección por setter.
- **Trampas:** un Singleton conserva el estado entre tests; BD sin reiniciar entre tests;
  tests de hilos sin timeout; mockear la clase bajo prueba; usar mocks donde la consigna
  pide la integración real.
- **Ejemplo de la cátedra:** identificación de usuario (IUClave → TClave → BD → Dialogos).

## D. Unitario / JUnit (clase 05)

- **Clase de prueba** por clase; **escenarios** (`setupEscenarioN`, estado conocido que
  cumple el invariante); **caso** = escenario + método + entradas → resultado; **pruebas
  con propósito**.
- JUnit 3 (`extends TestCase`, `testXxx`, `setUp`/`tearDown`) frente a JUnit 4
  (anotaciones, `Assert` estático, `@RunWith(Suite.class)`).
- Asertos de Java para pre, post e invariante; excepciones para las reglas de la SRS.
- **Ejemplos:** `TraductorTest`, `BancoTestEsc_1`.

## E. Persistencia (clase 07)

- `@Before` deja el recurso en un **estado conocido** (borra el archivo, recrea la tabla).
- Casos: escribir crea el recurso; ida y vuelta vacía; ida y vuelta con datos (requiere
  `equals`/`hashCode`); recurso inexistente → excepción específica (*try / fail / catch*).
- Excepciones **específicas** (`FileNotFoundException`, `SQLException`) y no `Exception`.

## F. Concurrencia [std]

- Verificar exclusión mutua, que `wait()` se despierte, que no haya deadlock, la
  terminación ordenada (bandera o `interrupt`) y los invariantes después de la carrera.
- Herramientas: `@Test(timeout)` / regla `Timeout`, `join(ms)` + `isAlive()`,
  `CountDownLatch` para la largada simultánea, captura de excepciones de otros hilos,
  repetición, `ThreadMXBean.findDeadlockedThreads()`, `verify(mock, timeout(ms))`.
