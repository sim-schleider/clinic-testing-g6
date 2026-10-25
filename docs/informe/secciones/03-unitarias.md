# Testing unitario de AsociadoDTO

## Objetivo y alcance

Verificar de forma estrictamente aislada la asignación, la recuperación y las validaciones de
integridad de `persistencia.AsociadoDTO` (ítem 1 de la consigna), y demostrar que la suite detecta
las fallas del código provisto sin levantar servicios externos.

Alcance: los dos constructores, los siete setters y los siete getters. La suite no usa base de
datos, archivos ni red. La unicidad del `dni` queda fuera: es responsabilidad de la persistencia y
se prueba en la integración con la base de datos.

Suite: `src/test/java/persistencia/AsociadoDTOTest.java` (`@Tag("unit")`).

## Oráculo y supuestos

**Oráculo.** SRS §3.1, "Requisitos de Transferencia de Datos e Integridad":

| Atributo | Tipo | Regla de validación |
|----------|------|---------------------|
| `nombre`, `apellido`, `dni`, `ciudad`, `calle`, `telefono` | `String` | No puede ser nulo |
| `numero` | `int` | Debe ser mayor o igual a 0 |

El informe de diseño (`docs/Informe.md`) agrega que los setters "incorporan aserciones (`assert`)
para validar la integridad básica de los datos —como evitar nombres nulos o números negativos—".

**Ausencia de precondiciones.** El Javadoc de `AsociadoDTO` no declara ninguna precondición: los
únicos contratos son las postcondiciones de los constructores. Por lo tanto ningún método traslada
al cliente la responsabilidad sobre sus entradas, y el componente debe comportarse correctamente
ante **cualquier** valor, rechazando los que violan la SRS. Por eso se prueban todas las clases
inválidas. Que el contrato solo exista en la SRS y no en el código es una debilidad de la
documentación.

**Supuestos** (lo que la especificación no dice):

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S1 | El rechazo de un dato inválido se manifiesta como `AssertionError` (ejecución con `-ea`) | El informe de diseño indica validación con `assert` |
| S2 | `""` y `"   "` pertenecen a la clase válida de cada `String` | La SRS solo prohíbe `null`; según el informe de diseño, los campos vacíos los filtra la vista (`VentanaPrincipal`). Al tener el mismo comportamiento esperado que cualquier texto, no forman una clase aparte |
| S3 | El constructor parametrizado también debe aplicar las reglas | La SRS las asigna al componente y el constructor no tiene precondiciones: no puede crear un objeto que viole la SRS |
| S4 | Un set rechazado no modifica el objeto | Garantía mínima para no dejar un objeto corrupto |
| S5 | La unicidad del `dni` no es responsabilidad del DTO | Se verifica en la integración con la base de datos |
| S6 | El constructor por defecto deja los valores por defecto de Java (`null` y 0) | Su postcondición es "una instancia a inicializar" |

Observación: el Javadoc de la clase menciona un atributo `id`, pero la clase no ofrece getter ni
setter para él.

Observación: la SRS no define el formato del `dni` ni del `telefono`; solo exige que no sean nulos.
El componente acepta cualquier texto no nulo (por ejemplo, `"abc"` como DNI), lo que cumple la
especificación pero permite datos sin sentido. Como no hay requisito que lo respalde, no se
definieron clases inválidas por formato; se recomienda que la especificación los defina.

## Diseño de escenarios

| Id | Escenario | Estado |
|----|-----------|--------|
| E1 | DTO recién creado con el constructor por defecto | Valores por defecto de Java (S6) |
| E2 | DTO cargado con valores válidos y **distintos en cada campo**: `"Valentino"`, `"Lombardo"`, `"40000000"`, `"Mar del Plata"`, `"Ratiery"`, `777`, `"2236767676"` | Cumple todas las reglas de la SRS |

E2 se arma antes de cada prueba (`@BeforeEach`). Que sus valores sean distintos permite detectar
un setter o getter que lea o escriba otro campo: después de cada operación se verifican los
**siete** getters, no solo el del campo que cambió.

## Clases de equivalencia y valores límite

El constructor parametrizado y los setters son métodos distintos, así que cada uno tiene sus
propias clases, aunque la regla de la SRS sea la misma.

| Método | Entrada | Condición (SRS §3.1) | Clase válida | Clase inválida | Valores límite |
|--------|---------|----------------------|--------------|----------------|----------------|
| Constructor | `nombre` | No puede ser nulo | V1 no nulo | I1 `null` | — |
| Constructor | `apellido` | No puede ser nulo | V2 no nulo | I2 `null` | — |
| Constructor | `dni` | No puede ser nulo | V3 no nulo | I3 `null` | — |
| Constructor | `ciudad` | No puede ser nula | V4 no nula | I4 `null` | — |
| Constructor | `calle` | No puede ser nula | V5 no nula | I5 `null` | — |
| Constructor | `telefono` | No puede ser nulo | V6 no nulo | I6 `null` | — |
| Constructor | `numero` | Debe ser ≥ 0 | V7 `numero ≥ 0` | I7 `numero < 0` | **0** (V7), **−1** (I7) |
| `setNombre` | `nombre` | No puede ser nulo | V8 no nulo | I8 `null` | — |
| `setApellido` | `apellido` | No puede ser nulo | V9 no nulo | I9 `null` | — |
| `setDni` | `dni` | No puede ser nulo | V10 no nulo | I10 `null` | — |
| `setCiudad` | `ciudad` | No puede ser nula | V11 no nula | I11 `null` | — |
| `setCalle` | `calle` | No puede ser nula | V12 no nula | I12 `null` | — |
| `setTelefono` | `telefono` | No puede ser nulo | V13 no nulo | I13 `null` | — |
| `setNumero` | `numero` | Debe ser ≥ 0 | V14 `numero ≥ 0` | I14 `numero < 0` | **0** (V14), **−1** (I14) |

En las entradas `String` la frontera es `null` frente a no nulo, y cada clase tiene un único
representante. En `numero` la frontera es `0`: se toman como representantes los dos valores que la
rodean, 0 (el menor válido) y −1 (el mayor inválido). Así cada clase se cubre una sola vez y, a la
vez, con su valor límite.

## Casos de prueba

Cada clase se cubre con un único caso. Un caso válido cubre varias clases válidas a la vez; cada
caso inválido contiene una sola clase inválida, con el resto de los datos válidos, porque el
primer rechazo ocultaría a los demás. Cada caso es un método de prueba (por ejemplo,
`testSetNumeroAcceptsZero` para CU11) cuyo comentario indica el Id del caso y las clases que cubre.

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-----------|------------------|------------------|--------------------|-----------|
| CU1 | E1 | `new AsociadoDTO()` | — | Getters: `null` en los `String`, `numero` = 0 (S6) | ✅ |
| CU2 | — | Constructor con datos válidos y `numero` = 0 | V1–V7 (límite 0) | Cada getter devuelve su valor | ✅ |
| CU3 | — | Constructor con `nombre` = `null` | I1 | `AssertionError` (S3) | ❌ D4 |
| CU4 | — | Constructor con `apellido` = `null` | I2 | `AssertionError` (S3) | ❌ D4 |
| CU5 | — | Constructor con `dni` = `null` | I3 | `AssertionError` (S3) | ❌ D4 |
| CU6 | — | Constructor con `ciudad` = `null` | I4 | `AssertionError` (S3) | ❌ D4 |
| CU7 | — | Constructor con `calle` = `null` | I5 | `AssertionError` (S3) | ❌ D4 |
| CU8 | — | Constructor con `telefono` = `null` | I6 | `AssertionError` (S3) | ❌ D4 |
| CU9 | — | Constructor con `numero` = −1 | I7 (límite −1) | `AssertionError` (S3) | ❌ D4 |
| CU10 | E2 | Los seis setters `String` con texto distinto al de E2 | V8–V13 | Cada campo toma su valor; `numero` sin cambios | ✅ |
| CU11 | E2 | `setNumero(0)` | V14 (límite 0) | `numero` = 0; resto sin cambios | ❌ D3 |
| CU12 | E2 | `setNombre(null)` | I8 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU13 | E2 | `setApellido(null)` | I9 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU14 | E2 | `setDni(null)` | I10 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU15 | E2 | `setCiudad(null)` | I11 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU16 | E2 | `setCalle(null)` | I12 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU17 | E2 | `setTelefono(null)` | I13 | `AssertionError`; E2 sin cambios (S4) | ✅ |
| CU18 | E2 | `setNumero(-1)` | I14 (límite −1) | `AssertionError`; E2 sin cambios (S4) | ✅ |

Los getters no reciben entradas: se verifican en todos los casos a través de `assertDatos`.

## Resultados

Ejecución sobre el código provisto, con solo las correcciones de compilación D1 y D2 aplicadas
(commit `c1ff2a6`), mediante `run_tests.sh unit rojo`:

| Fase | Total | Pasan | Fallan |
|------|-------|-------|--------|
| Rojo (código provisto) | 18 | 10 | 8 |

Los 8 fallos corresponden a CU3–CU9 (defecto D4) y a CU11 (defecto D3). Cada fallo se verificó
contra la implementación para confirmar que la causa es el defecto y no un error del test. La
tabla completa de resultados está en el apéndice de resultados (`resultados/unit-rojo.md`) y la
captura en `capturas/unit-rojo.png`.

No hay fase verde: por la regla del trabajo, el código de producción no se modifica (salvo D1 y
D2, que impedían compilar), así que los defectos quedan registrados y sus casos permanecen en rojo
como evidencia de detección.

## Defectos detectados

| Id | Ubicación | Descripción | Requisito violado | Detectado por |
|----|-----------|-------------|-------------------|---------------|
| D3 | `AsociadoDTO.setNumero` | Valida `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0 | SRS §3.1: `numero` ≥ 0 | CU11 |
| D4 | Constructor parametrizado | Asigna los siete campos sin validar: acepta `null` y `numero` negativo | SRS §3.1 (S3) | CU3–CU9 |

El detalle (esperado, obtenido, severidad y estado) está en el registro de defectos.

## Fallas sembradas

No se sembraron fallas: la regla del trabajo prohíbe modificar el código de producción, incluso de
forma temporal. La capacidad de detección de la suite se demuestra con los defectos reales D3 y D4.

## Conclusiones

- La suite cubre las 28 clases de equivalencia del componente (14 válidas y 14 inválidas) con 18
  casos, cada clase una sola vez, sin depender de servicios externos.
- Detectó dos defectos reales del código provisto. D3 es un error de frontera (`>` en lugar de
  `>=`) que solo se manifiesta en el valor límite 0: un representante cualquiera de la clase
  válida (por ejemplo `500`) no lo habría encontrado, lo que justifica elegir el valor límite como
  representante. D4 permite construir un `AsociadoDTO` que viola la SRS, eludiendo las
  validaciones de los setters.
- El supuesto S3 es determinante: D4 existe porque se asumió que las reglas de la SRS son del
  componente y no solo de sus setters. El supuesto S2 también condiciona el diseño: si la cátedra
  considera inválidos `""` o `"   "`, cada `String` tendría una clase inválida más.
- Limitación: las validaciones dependen de `assert`, que solo se ejecuta con la opción `-ea` de la
  JVM. Sin ella, ninguna validación actúa y el componente acepta cualquier dato. Si la suite se
  ejecuta sin `-ea` (por ejemplo, desde el IDE sin configurarlo), los casos inválidos fallan sin
  que exista un defecto; Maven la ejecuta con `-ea`.
