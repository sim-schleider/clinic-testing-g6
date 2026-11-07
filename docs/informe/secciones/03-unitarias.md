# Testing unitario de AsociadoDTO

## Objetivo y alcance

Verificar de forma estrictamente aislada la asignación, la recuperación y las validaciones de
integridad de `persistencia.AsociadoDTO` (ítem 1 de la consigna), y demostrar que la suite detecta
las fallas del código provisto sin levantar servicios externos.

Alcance: los dos constructores, los siete setters y los siete getters. La suite no usa base de
datos, archivos ni red. La unicidad del `dni` queda fuera: es responsabilidad de la persistencia y
se prueba en la integración con la base de datos.

Suite: `src/test/java/item1/AsociadoDTOTest.java` (`@Tag("unit")`).

## Criterio de validación y supuestos

**Criterio de validación.** SRS §3.1, "Requisitos de Transferencia de Datos e Integridad":

| Atributo | Tipo | Regla de validación |
|----------|------|---------------------|
| `nombre`, `apellido`, `dni`, `ciudad`, `calle`, `telefono` | `String` | No puede ser nulo |
| `numero` | `int` | Debe ser mayor o igual a 0 |

**Los setters validan las reglas de la SRS.** La consigna pide comprobar "las validaciones internas
de integridad implementadas en los setters", y el informe de diseño de la aplicación dice que los
setters "incorporan aserciones (`assert`) para validar la integridad básica de los datos —como
evitar nombres nulos o números negativos—". Rechazar una entrada que viola la SRS es, entonces, un
comportamiento del setter que se puede verificar: cada setter tiene una clase válida y una
inválida, y el resultado esperado de una entrada inválida es un `AssertionError`, con el DTO sin
modificar. Que el objeto no cambie es parte del resultado esperado: una validación que rechazara
después de asignar dejaría el DTO con un dato que viola la SRS.

**El constructor parametrizado no declara validaciones.** Ni el Javadoc ni el informe de diseño
dicen que el constructor valide sus argumentos (el informe habla de "constructores flexibles").
Para él, las reglas de la SRS son precondiciones: el cliente garantiza que nunca recibe un `String`
nulo ni un `numero` negativo, y ante una entrada que las viola el contrato no define ningún
comportamiento. Por eso el constructor solo tiene clases válidas.

**Supuestos** (lo que la especificación no dice):

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S1 | `""` y `"   "` pertenecen a la clase válida de cada `String` | La SRS solo prohíbe `null`; según el informe de diseño, los campos vacíos los filtra la vista (`VentanaPrincipal`). Se prueban como valores límite de la clase válida (CU7 y CU8) |
| S2 | La unicidad del `dni` no es responsabilidad del DTO | Se verifica en la integración con la base de datos |
| S3 | El constructor por defecto deja los valores por defecto de Java (`null` y 0) | Su postcondición es "una instancia a inicializar" |
| S15 | La validación de un setter se manifiesta como `AssertionError` y deja el DTO sin cambios | El informe de diseño describe las validaciones como `assert`; la consigna pide ejecutar la suite con ellas activas (`-ea`) |

Observación: el Javadoc de la clase menciona un atributo `id`, pero la clase no ofrece getter ni
setter para él.

Observación: la SRS no define el formato del `dni` ni del `telefono`; solo exige que no sean nulos.
El componente acepta cualquier texto no nulo (por ejemplo, `"abc"` como DNI), lo que cumple la
especificación pero permite datos sin sentido. Como no hay requisito que lo respalde, no se
definieron clases por formato; se recomienda que la especificación los defina.

Observación: el constructor parametrizado acepta datos que los setters rechazan (por ejemplo, un
`nombre` nulo). No se registra como defecto, porque ningún documento le asigna esa validación, pero
es una inconsistencia del diseño: la integridad del DTO depende de cómo se lo construye.

## Diseño de escenarios

| Id | Escenario | Estado |
|----|-----------|--------|
| E1 | DTO recién creado con el constructor por defecto | Valores por defecto de Java (S3) |
| E2 | DTO cargado con valores válidos y **distintos en cada campo**: `"Marina"`, `"Lombardo"`, `"40000000"`, `"Mar del Plata"`, `"Ratiery"`, `777`, `"2236767676"` | Cumple todas las reglas de la SRS |

E2 se arma antes de cada prueba (`@BeforeEach`). Que sus valores sean distintos permite detectar
un setter o getter que lea o escriba otro campo: después de cada operación se verifican los
**siete** getters, no solo el del campo que cambió. En los casos inválidos, la verificación de los
siete getters es la que muestra que el DTO quedó igual que en E2.

## Clases de equivalencia y valores límite

El constructor parametrizado y los setters son métodos distintos, así que cada uno tiene sus
propias clases, aunque la regla de la SRS sea la misma.

| Método | Entrada | Condición (SRS §3.1) | Tipo | Clases válidas | Clases inválidas | Valores límite |
|--------|---------|----------------------|------|----------------|------------------|----------------|
| Constructor | `nombre` | No nulo | Precondición | V1 no nulo | — (precondición) | — |
| Constructor | `apellido` | No nulo | Precondición | V2 no nulo | — (precondición) | — |
| Constructor | `dni` | No nulo | Precondición | V3 no nulo | — (precondición) | — |
| Constructor | `ciudad` | No nula | Precondición | V4 no nula | — (precondición) | — |
| Constructor | `calle` | No nula | Precondición | V5 no nula | — (precondición) | — |
| Constructor | `telefono` | No nulo | Precondición | V6 no nulo | — (precondición) | — |
| Constructor | `numero` | `numero ≥ 0` | Precondición | V7 `numero ≥ 0` | — (precondición) | **0** |
| `setNombre` | `nombre` | No nulo | Validación del setter | V8 no nulo | I1 `null` | `""`, `"   "` |
| `setApellido` | `apellido` | No nulo | Validación del setter | V9 no nulo | I2 `null` | `""`, `"   "` |
| `setDni` | `dni` | No nulo | Validación del setter | V10 no nulo | I3 `null` | `""`, `"   "` |
| `setCiudad` | `ciudad` | No nula | Validación del setter | V11 no nula | I4 `null` | `""`, `"   "` |
| `setCalle` | `calle` | No nula | Validación del setter | V12 no nula | I5 `null` | `""`, `"   "` |
| `setTelefono` | `telefono` | No nulo | Validación del setter | V13 no nulo | I6 `null` | `""`, `"   "` |
| `setNumero` | `numero` | `numero ≥ 0` | Validación del setter | V14 `numero ≥ 0` | I7 `numero < 0` | **−1**, **0**, 1, `Integer.MAX_VALUE` |

Valores límite:

- **`numero` en el setter:** la frontera entre I7 y V14 está entre −1 y 0. Se prueban los dos lados
  de la frontera (−1, el primer valor inválido, y 0, el primer válido), el valor siguiente al límite
  (1) y el extremo del tipo (`Integer.MAX_VALUE`), porque la SRS no fija un máximo. Un error de
  frontera (`>` en lugar de `>=`, o `>= 1`) solo se manifiesta en 0 o en −1.
- **`numero` en el constructor:** como la regla es precondición, solo se prueba el límite del
  dominio válido, 0. El −1 queda fuera del dominio.
- **Entradas `String`:** la SRS no fija largos, así que el único límite es la longitud 0. Según S1,
  `""` y el texto con solo espacios están en la clase válida, y son los valores más cercanos a la
  frontera con `null`: un setter que validara de más (por ejemplo, `!isEmpty()` o `!isBlank()`)
  los rechazaría.

## Casos de prueba

Cada clase se cubre al menos una vez, un mismo caso válido cubre varias clases a la vez, y cada caso
inválido tiene **una sola** entrada inválida (si tuviera dos, el primer rechazo ocultaría al
segundo). Cada caso es un método de prueba cuyo nombre empieza con el Id del caso (por ejemplo,
`cu4_setNumeroAcceptsZero`) y cuyo `@DisplayName` repite el Id, así que el Id aparece en la
ejecución y en las capturas.

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-----------|------------------|------------------|--------------------|-----------|
| CU1 | E1 | `new AsociadoDTO()` | — | Getters: `null` en los `String`, `numero` = 0 (S3) | ✅ |
| CU2 | — | Constructor con datos válidos y `numero` = 0 | V1–V7 (límite 0) | Cada getter devuelve su valor | ✅ |
| CU3 | E2 | Los seis setters `String` con texto distinto al de E2 | V8–V13 | Cada campo toma su valor; `numero` sin cambios | ✅ |
| CU4 | E2 | `setNumero(0)` | V14 (límite 0) | `numero` = 0; resto sin cambios | ❌ D3 |
| CU5 | E2 | `setNumero(1)` | V14 (siguiente al límite) | `numero` = 1; resto sin cambios | ✅ |
| CU6 | E2 | `setNumero(Integer.MAX_VALUE)` | V14 (extremo del tipo) | `numero` = `Integer.MAX_VALUE`; resto sin cambios | ✅ |
| CU7 | E2 | Los seis setters `String` con `""` | V8–V13 (longitud 0, S1) | Cada campo queda en `""`; `numero` sin cambios | ✅ |
| CU8 | E2 | Los seis setters `String` con `"   "` | V8–V13 (solo espacios, S1) | Cada campo queda en `"   "`; `numero` sin cambios | ✅ |
| CU9 | E2 | `setNombre(null)` | I1 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU10 | E2 | `setApellido(null)` | I2 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU11 | E2 | `setDni(null)` | I3 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU12 | E2 | `setCiudad(null)` | I4 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU13 | E2 | `setCalle(null)` | I5 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU14 | E2 | `setTelefono(null)` | I6 | `AssertionError`; el DTO igual que en E2 | ✅ |
| CU15 | E2 | `setNumero(-1)` | I7 (límite −1) | `AssertionError`; el DTO igual que en E2 | ✅ |

Los getters no reciben entradas: se verifican en todos los casos a través de `assertDatos`.

Antes de los casos, la suite verifica que las aserciones estén habilitadas (`-ea`). Si no lo
están, todos los casos fallan con un mensaje que lo indica: sin `-ea` los casos inválidos fallarían
por una razón equivocada y CU4 pasaría aunque D3 siguiera en el código.

## Resultados

La suite se ejecutó una vez, sobre `AsociadoDTO` tal como se entregó, con solo las correcciones de
compilación D1 y D2 aplicadas en una copia temporal (Apéndice C), mediante `run_tests.sh unit`:

<!-- PENDIENTE: regenerar resultados/unit.{md,log} con run_tests.sh unit sobre el código
entregado (con 6cda0de revertido en la copia temporal, ver Apéndice C). -->

| Total | Pasan | Fallan |
|-------|-------|--------|
| 15 | 14 | 1 |

Pasan 14 casos: el constructor por defecto, el constructor parametrizado, los setters de texto
(con texto común, vacío y con solo espacios) y los valores 1 y `Integer.MAX_VALUE` de `setNumero`
funcionan como dice la SRS. Los siete setters rechazan su entrada inválida (`null` o −1) y dejan el
DTO sin cambios. Falla CU4 (defecto D3), y se verificó contra la implementación para confirmar que
la causa es el defecto y no un error del test. La tabla completa está en el apéndice de resultados
(`resultados/unit.md`). La captura de esta ejecución es requerida y la toma el equipo desde el IDE
(ver Apéndice B).

D3 se corrige en el punto 2, que pide "corregir si es necesario el código del ítem 1" (ver la
sección de integración y el registro de defectos). Con la corrección aplicada, los 15 casos pasan.

## Defectos detectados

| Id | Ubicación | Descripción | Requisito violado | Detectado por |
|----|-----------|-------------|-------------------|---------------|
| D3 | `AsociadoDTO.setNumero` | Valida `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0, que la SRS admite | SRS §3.1: `numero` ≥ 0 | CU4 |

El detalle (esperado, obtenido, severidad y estado) está en el registro de defectos.

## Fallas sembradas

No se sembraron fallas: la regla del trabajo prohíbe modificar el código de producción, incluso de
forma temporal. La capacidad de detección de la suite se demuestra con el defecto real D3. El
diseño cubre además los errores típicos de una validación: un setter que no validara (CU9 a CU15),
que validara de más (CU7 y CU8), que corriera la frontera de `numero` hacia cualquiera de los dos
lados (CU4 y CU15), o que asignara antes de validar (la verificación del DTO sin cambios en CU9 a
CU15).

## Conclusiones

- La suite cubre las 14 clases válidas y las 7 clases inválidas del componente con 15 casos, sin
  depender de servicios externos. Cada setter se prueba en los dos lados de su regla: acepta los
  valores que la SRS admite y rechaza los que prohíbe sin modificar el DTO.
- Detectó un defecto real del código provisto. D3 es un error de frontera (`>` en lugar de `>=`)
  que solo se manifiesta en el valor límite 0: un representante cualquiera de la clase válida (por
  ejemplo `500`) no lo habría encontrado, y tampoco el límite inválido −1, que el código rechaza
  bien (CU15). Hacen falta los dos lados de la frontera para ubicar el error.
- Distinguir los setters del constructor es determinante. Los setters implementan las validaciones
  que pide la consigna y se prueban con sus clases inválidas; el constructor no declara ninguna, de
  modo que aceptar un dato nulo no es un defecto suyo. Esa diferencia queda como observación de
  diseño. El supuesto S1 también condiciona el diseño: si la cátedra considera nulos de hecho `""`
  o `"   "`, CU7 y CU8 pasarían a ser casos inválidos.
- Limitación: las validaciones dependen de `assert`, que solo se ejecuta con la opción `-ea` de la
  JVM. Sin ella el componente acepta cualquier dato y D3 queda oculto. Maven ejecuta la suite con
  `-ea`, y la suite falla con un mensaje claro si se la corre sin esa opción (por ejemplo, desde el
  IDE sin configurarla).
