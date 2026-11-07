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

**Las reglas de la SRS son precondiciones.** Según el criterio de la cátedra, las reglas de §3.1
son precondiciones del constructor parametrizado y de los setters: es el cliente quien garantiza
que nunca se invoquen con un `String` nulo ni con un `numero` negativo. Ante una entrada que viola
una precondición el contrato no define ningún comportamiento, de modo que no existe un resultado
esperado contra el cual comparar y **las clases inválidas no se prueban**. Las aserciones (`assert`)
que el informe de diseño describe en los setters son una verificación defensiva de esas
precondiciones, no una funcionalidad del componente.

Lo que sí se prueba es que, con entradas que cumplen las precondiciones, cada método cumple su
postcondición: el valor se asigna y se recupera sin alterar los demás campos. Esto incluye los
valores límite del dominio válido.

Que las precondiciones solo figuren en la SRS y no en el Javadoc (que no declara ningún `pre:`) es
una debilidad de la documentación.

**Supuestos** (lo que la especificación no dice):

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S1 | `""` y `"   "` pertenecen a la clase válida de cada `String` | La SRS solo prohíbe `null`; según el informe de diseño, los campos vacíos los filtra la vista (`VentanaPrincipal`). Al tener el mismo comportamiento esperado que cualquier texto, no forman una clase aparte |
| S2 | La unicidad del `dni` no es responsabilidad del DTO | Se verifica en la integración con la base de datos |
| S3 | El constructor por defecto deja los valores por defecto de Java (`null` y 0) | Su postcondición es "una instancia a inicializar" |

Observación: el Javadoc de la clase menciona un atributo `id`, pero la clase no ofrece getter ni
setter para él.

Observación: la SRS no define el formato del `dni` ni del `telefono`; solo exige que no sean nulos.
El componente acepta cualquier texto no nulo (por ejemplo, `"abc"` como DNI), lo que cumple la
especificación pero permite datos sin sentido. Como no hay requisito que lo respalde, no se
definieron clases por formato; se recomienda que la especificación los defina.

## Diseño de escenarios

| Id | Escenario | Estado |
|----|-----------|--------|
| E1 | DTO recién creado con el constructor por defecto | Valores por defecto de Java (S3) |
| E2 | DTO cargado con valores válidos y **distintos en cada campo**: `"Valentino"`, `"Lombardo"`, `"40000000"`, `"Mar del Plata"`, `"Ratiery"`, `777`, `"2236767676"` | Cumple todas las reglas de la SRS |

E2 se arma antes de cada prueba (`@BeforeEach`). Que sus valores sean distintos permite detectar
un setter o getter que lea o escriba otro campo: después de cada operación se verifican los
**siete** getters, no solo el del campo que cambió.

## Clases de equivalencia y valores límite

El constructor parametrizado y los setters son métodos distintos, así que cada uno tiene sus
propias clases, aunque la regla de la SRS sea la misma. Los valores que violan la regla quedan
fuera del dominio por precondición y no forman clases a probar.

| Método | Entrada | Precondición (SRS §3.1) | Clase válida | Valor límite |
|--------|---------|-------------------------|--------------|--------------|
| Constructor | `nombre` | No nulo | V1 no nulo | — |
| Constructor | `apellido` | No nulo | V2 no nulo | — |
| Constructor | `dni` | No nulo | V3 no nulo | — |
| Constructor | `ciudad` | No nula | V4 no nula | — |
| Constructor | `calle` | No nula | V5 no nula | — |
| Constructor | `telefono` | No nulo | V6 no nulo | — |
| Constructor | `numero` | `numero ≥ 0` | V7 `numero ≥ 0` | **0** |
| `setNombre` | `nombre` | No nulo | V8 no nulo | — |
| `setApellido` | `apellido` | No nulo | V9 no nulo | — |
| `setDni` | `dni` | No nulo | V10 no nulo | — |
| `setCiudad` | `ciudad` | No nula | V11 no nula | — |
| `setCalle` | `calle` | No nula | V12 no nula | — |
| `setTelefono` | `telefono` | No nulo | V13 no nulo | — |
| `setNumero` | `numero` | `numero ≥ 0` | V14 `numero ≥ 0` | **0** |

En las entradas `String` el dominio válido es cualquier texto no nulo y cada clase tiene un único
representante. En `numero` el dominio válido empieza en `0`: se toma como representante ese valor
límite, el menor válido. Así cada clase se cubre una sola vez y, a la vez, con su valor límite. El
valor −1, que en un diseño sin precondiciones sería el límite de la clase inválida, queda fuera del
dominio.

## Casos de prueba

Cada clase se cubre con un único caso, y un mismo caso cubre varias clases válidas a la vez. Cada
caso es un método de prueba (por ejemplo, `testSetNumeroAcceptsZero` para CU4) cuyo comentario
indica el Id del caso y las clases que cubre.

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-----------|------------------|------------------|--------------------|-----------|
| CU1 | E1 | `new AsociadoDTO()` | — | Getters: `null` en los `String`, `numero` = 0 (S3) | ✅ |
| CU2 | — | Constructor con datos válidos y `numero` = 0 | V1–V7 (límite 0) | Cada getter devuelve su valor | ✅ |
| CU3 | E2 | Los seis setters `String` con texto distinto al de E2 | V8–V13 | Cada campo toma su valor; `numero` sin cambios | ✅ |
| CU4 | E2 | `setNumero(0)` | V14 (límite 0) | `numero` = 0; resto sin cambios | ❌ D3 |

Los getters no reciben entradas: se verifican en todos los casos a través de `assertDatos`.

## Resultados

La suite se ejecutó una vez, sobre `AsociadoDTO` tal como se entregó, con solo las correcciones de
compilación D1 y D2 aplicadas en una copia temporal (Apéndice C), mediante `run_tests.sh unit`:

<!-- PENDIENTE: regenerar resultados/unit.{md,log} con run_tests.sh unit sobre el código de master. -->

| Total | Pasan | Fallan |
|-------|-------|--------|
| 4 | 3 | 1 |

Pasan CU1, CU2 y CU3: el constructor por defecto, el constructor parametrizado y los setters de
texto funcionan como dice la SRS. Falla CU4 (defecto D3), y se verificó contra la implementación
para confirmar que la causa es el defecto y no un error del test. La tabla completa está en el
apéndice de resultados (`resultados/unit.md`). La captura de esta ejecución es requerida y la toma
el equipo desde el IDE (ver Apéndice B).

D3 se corrige en el punto 2, que pide "corregir si es necesario el código del ítem 1" (ver la
sección de integración y el registro de defectos).

## Defectos detectados

| Id | Ubicación | Descripción | Requisito violado | Detectado por |
|----|-----------|-------------|-------------------|---------------|
| D3 | `AsociadoDTO.setNumero` | Valida `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0, que cumple la precondición | SRS §3.1: `numero` ≥ 0 | CU4 |

El detalle (esperado, obtenido, severidad y estado) está en el registro de defectos.

## Fallas sembradas

No se sembraron fallas: la regla del trabajo prohíbe modificar el código de producción, incluso de
forma temporal. La capacidad de detección de la suite se demuestra con el defecto real D3.

## Conclusiones

- La suite cubre las 14 clases de equivalencia válidas del componente con 4 casos, cada clase una
  sola vez, sin depender de servicios externos. Las entradas que violan las precondiciones de la
  SRS no se prueban, porque el contrato no define qué debe ocurrir con ellas.
- Detectó un defecto real del código provisto. D3 es un error de frontera (`>` en lugar de `>=`)
  que solo se manifiesta en el valor límite 0: un representante cualquiera de la clase válida (por
  ejemplo `500`) no lo habría encontrado, lo que justifica elegir el valor límite como
  representante. Desde el punto de vista del contrato, el método rechaza una entrada que cumple
  su precondición.
- Interpretar las reglas de la SRS como precondiciones es determinante: con ese criterio, que el
  constructor parametrizado no valide sus argumentos no es un defecto. El supuesto S1 también
  condiciona el diseño: si la cátedra considera nulos de hecho `""` o `"   "`, quedarían fuera del
  dominio válido.
- Limitación: la verificación defensiva de las precondiciones depende de `assert`, que solo se
  ejecuta con la opción `-ea` de la JVM. Sin ella el componente acepta cualquier dato, aunque eso
  no contradice el contrato. D3 se manifiesta como `AssertionError`, así que con la suite corriendo
  sin `-ea` (por ejemplo, desde el IDE sin configurarlo) CU4 pasa y el defecto queda oculto; Maven
  la ejecuta con `-ea`.
