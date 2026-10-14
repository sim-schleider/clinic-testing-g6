# Criterios de la cátedra (común a todas las skills de testing)

<!-- ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. No editar la copia
     dentro de una skill; editar la fuente y correr scripts/sync_common.py. -->

Taller de Programación I (UNMdP, FI). Estas reglas valen para cualquier técnica.

## 1. Vocabulario (usarlo tal cual en tablas e informes)

| Término | Significado operativo |
|---------|-----------------------|
| **Contrato** | Pre, post y excepciones de un método (Javadoc `pre:`, `@param`, `@return`, `@throws`). |
| **SRS** | Requisitos del sistema. Junto con el contrato es el **oráculo**: define el resultado esperado. |
| **Invariante** | Condición del estado del objeto que siempre se cumple (invariante de clase) o que se mantiene dentro de un bucle (invariante de ciclo). |
| **Escenario** | Objeto de la clase bajo prueba en un **estado conocido** que cumple el invariante. En código: `setupEscenarioN()` o una clase de test por escenario. |
| **Caso de prueba** | **Escenario + método + valores de entrada → resultado esperado.** |
| **Prueba con propósito** | Varios casos con un objetivo común ("probar que no se aceptan DNI repetidos"). |
| **Clase de prueba** | Una por cada clase bajo prueba, asociada a ella (`XxxTest`). |
| **Suite** | Conjunto de clases de prueba que se ejecutan juntas. |

## 2. Reglas generales

1. **El oráculo es la especificación, no el código.** El resultado esperado sale de la
   SRS o del contrato. Si el código hace otra cosa, el test queda **en rojo** y eso es un
   hallazgo, no un test mal escrito.
2. **Nunca ajustar un test para que pase.** Tampoco tocar código de producción sin que el
   usuario lo pida. Un rojo se documenta (esperado, obtenido, requisito que viola) y se le
   informa al usuario.
3. **Los casos "normales" respetan la precondición.** Violar la precondición (por ejemplo,
   pasar un `null` donde el contrato lo prohíbe) es un caso aparte. Su resultado esperado
   es el mecanismo que defina el código: `AssertionError` si valida con `assert`, la
   excepción documentada si lanza una.
4. **Las reglas de la SRS que lanzan excepciones se prueban siempre**, con la excepción como
   resultado esperado.
5. **Cada test arranca de un estado conocido**: el `@Before` arma el escenario o resetea
   el recurso (archivo, tabla). Ningún test depende de otro ni del orden de ejecución.
6. **Un rojo tiene que fallar por la razón correcta.** Antes de reportarlo, se comprueba
   que el mensaje de falla corresponde a la regla probada y no a un error del propio test
   (un `NullPointerException` en el setup, una conexión rechazada).
7. **Orden de trabajo de la cátedra:** revisar el código, construir casos de caja negra,
   medir la cobertura y completar con caja blanca lo que falte.

## 3. Formatos de tabla (los que pide la cátedra en el informe)

**Unitario (clase 05):**

| Clase | Método | Esc | Valores de entrada | Resultado |
|-------|--------|-----|--------------------|-----------|

**Prueba con propósito:** se escribe `Objetivo:` y `Escenario:` y debajo:

| Valores de entrada | Resultado |
|--------------------|-----------|

**Caja negra: clases de equivalencia:**

| Entrada | Condición (SRS/contrato) | Clases válidas | Clases inválidas | Valores límite |
|---------|--------------------------|----------------|------------------|----------------|

y la tabla de casos, que indica qué clases cubre cada uno:

| Id | Escenario | Entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-----------|---------|------------------|--------------------|-----------|

**Caja blanca (clase 06):**

| Id | Objetivo de la prueba | Datos de entrada | Procedimiento | Salida esperada | Resultado |
|----|-----------------------|------------------|---------------|-----------------|-----------|

**Integración (clase 08):**

| Id | Entrada | Condiciones de entrada | Salida esperada | Condiciones de salida | Resultado |
|----|---------|------------------------|-----------------|-----------------------|-----------|

La columna **Resultado** queda vacía al diseñar. Después de ejecutar se completa con
**pass** o **fail**, usando `scripts/surefire_summary.py`. Los Id son únicos y estables:
`CN1…` (caja negra), `T1…` (caja blanca), `CI1…` (integración). El nombre del método JUnit
incluye el Id (`cn3_numeroNegativoRechazado`) para cruzar la tabla con el reporte.

## 4. Convenciones JUnit 4 (las de las diapositivas)

- `import static org.junit.Assert.*;`, `@Test`, `@Before`, `@After`, `@BeforeClass`,
  `@AfterClass`, `@RunWith(Suite.class)` + `@SuiteClasses({...})`.
- **`assertEquals(mensaje, esperado, real)`**: en ese orden. Los ejemplos de la cátedra a
  veces lo invierten; no copiar ese detalle.
- `double`: siempre con delta, `assertEquals(msg, 2.0, r, 1e-6)`.
- `assertSame` / `assertNotSame` para identidad (Singleton).
- **Excepción esperada:** `@Test(expected = X.class)`, o `assertThrows(X.class, () -> …)`
  (existe desde **JUnit 4.13**). El patrón de la cátedra es *try → llamada →
  `fail("debería lanzar…")` → catch de la excepción esperada*, y es útil cuando además se
  quiere verificar el mensaje.
- **Excepción no esperada:** dentro del `catch`, `fail("No debería lanzar excepción: " + e.getMessage())`.
  **Nunca** un `catch` vacío.
- **Los `assert` de Java solo se evalúan con `-ea`.** Surefire lo activa por defecto; en el
  IDE hay que agregarlo. Sin `-ea`, un test que espera `AssertionError` falla siempre.
- **Comparar objetos con `assertEquals`** requiere `equals`/`hashCode` en la clase. Si no
  los tiene, se compara campo por campo.
- **Nombres:** las clases de test terminan en `Test` (Maven solo ejecuta `*Test`, `Test*`,
  `*Tests` y `*TestCase`) y van en `src/test/java`, en el **mismo paquete** que la clase
  bajo prueba.

## 5. Dónde quedan los artefactos

| Artefacto | Ubicación |
|-----------|-----------|
| Diseño (tablas, grafo, V(G), estados) | `docs/pruebas/<tecnica>/<Clase>.md` |
| Tests | `src/test/java/<paquete>/<Clase><Tecnica>Test.java` (`AsociadoDTOCajaNegraTest`) |
| Resultados de cada ejecución | `docs/pruebas/resultados/<AAAA-MM-DD>-<tecnica>.md` (salida de `surefire_summary.py`) |
| Log crudo de Maven | `docs/pruebas/resultados/<AAAA-MM-DD>-<tecnica>.log` |

Si el proyecto ya tiene otra convención (otra carpeta de docs, otros nombres de test), se
respeta la del proyecto.

## 6. Evidencia verde y rojo

La cátedra pide **capturas en verde** (comportamiento correcto) y **en rojo** (errores
detectados).

- **Rojo:** se ejecuta la suite contra el código tal como está. Los tests que fallan por
  defectos reales son la evidencia.
- **Verde:** se ejecuta después de que el usuario corrige el código, o se muestran los
  tests de los requisitos que el código ya cumple.
- Cada corrida queda en `docs/pruebas/resultados/` con fecha.
- **Capturas:** `python3 -I scripts/results_html.py <resumen>.md … --salida-dir <tmp>` convierte
  cada resumen en una página con el formato del panel de JUnit (barra verde o roja, ✓/✗ por
  test). Después se le saca un PNG de página completa (por ejemplo, con Playwright) y se guarda
  en `docs/pruebas/capturas/`. También se puede capturar el panel del IDE con una suite JUnit
  por consigna (`@RunWith(Suite.class)`).
