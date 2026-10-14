# Técnica de caja negra: guía operativa

Secciones: §1 partición por tipo · §2 particiones de salida · §3 valores límite ·
§4 escenarios · §5 tabla de decisión · §6 transición de estados · §7 ejemplos resueltos.

## §1. Reglas de partición por tipo de entrada

| Entrada / restricción | Clases válidas | Clases inválidas | Notas |
|-----------------------|----------------|------------------|-------|
| `int` en rango `[a, b]` | V: `a ≤ x ≤ b` | I: `x < a`; I: `x > b` | Dos inválidas, una por lado |
| `int` con cota `x ≥ a` | V: `x ≥ a` | I: `x < a` | Si la SRS no da cota superior, `Integer.MAX_VALUE` es válido (anotarlo como supuesto) |
| `double` en rango | V: dentro | I: debajo; I: encima | `NaN` e `Infinity` como inválidas si el dominio es real acotado |
| `String` "no nulo" | V: no nulo | I: `null` | `""` y `"   "` son **supuestos**: si la SRS no los prohíbe, son válidos, pero conviene probarlos aparte e informar |
| `String` con formato (DNI, mail, fecha) | V: formato correcto | I: una por cada forma de romperlo que la especificación distinga (largo, caracteres, separadores) | No inventar reglas de formato que la SRS no tiene |
| `String` con largo máximo *n* | V: 1..n | I: largo *n+1*; (I o V) `""` | El largo máximo puede venir de la columna de la BD (anotarlo) |
| Constante de un conjunto (`INGLES`, `FRANCES`…) | V: **una clase por valor** si el comportamiento cambia según el valor | I: un valor fuera del conjunto (`-1`, `99`) | Típico del contrato "`id` pertenece a {…}" |
| `boolean` | V: `true`; V: `false` | — | Dos clases solo si el comportamiento difiere |
| Colección | V: vacía, un elemento, varios | I: `null` (si se prohíbe); I: más del máximo | "Vacía" suele ser un escenario |
| Referencia a objeto | V: objeto válido (por estado relevante) | I: `null`; I: objeto en un estado inválido | El estado del objeto pasado puede generar varias clases |
| Identificador único (DNI, código) | V: no registrado | I: ya registrado (duplicado) | Necesita un escenario con datos precargados |

**Partir una clase** cuando la especificación (o el sentido común del dominio) sugiere que
el programa trata distinto a un subconjunto. **No partir** por partir: dos clases con el
mismo comportamiento esperado son una sola.

## §2. Particiones de salida

Además de particionar las entradas, particionar **los resultados distintos** que define la
especificación y asegurar al menos un caso por cada uno. Ejemplo: si el incremento puede ser
"0", "solo por hijos menores", "solo por hijos de 18 a 21", "por cantidad de hijos" y
"combinado", cada uno es una clase de salida.

## §3. Valores límite

Para cada borde, el **límite y sus vecinos inmediatos**:

| Restricción | Valores a probar |
|-------------|------------------|
| `x ≥ 0` (int) | `-1` (I), `0` (V), `1` (V) |
| `a ≤ x ≤ b` | `a-1`, `a`, `a+1`, `b-1`, `b`, `b+1` (mínimo: `a-1, a, b, b+1`) |
| `x > 5` (int) | `5` (no cumple), `6` (cumple) |
| `edad < 18` y `18 ≤ edad ≤ 21` | `17`, `18`, `21`, `22`; la frontera entre las dos clases se prueba de los dos lados |
| `double x > 0` | `0.0` (I), un valor positivo pequeño (`0.01` o `Double.MIN_VALUE`) |
| Largo máximo *n* | largo `n-1`, `n`, `n+1` |
| Colección con máximo *m* | `0`, `1`, `m`, `m+1` |
| `int` sin cota explícita | `Integer.MAX_VALUE`, `Integer.MIN_VALUE` (desbordes) como casos de robustez |

- **"Entre 18 y 21"** es ambiguo (¿incluye el 21?). Es un supuesto: se documenta y se
  prueban 21 y 22.
- Los valores límite **son casos**: cada uno va a la tabla con su Id y sus clases cubiertas.

## §4. El escenario como dimensión

El estado del objeto antes de llamar al método es una entrada más:

| Escenario | Cuándo hace falta |
|-----------|-------------------|
| Objeto recién construido (vacío) | Setters, primera inserción, colecciones vacías |
| Objeto con datos válidos | Modificaciones, duplicados, búsquedas que encuentran |
| Objeto "lleno" o en el límite | Máximos, capacidad |
| Objeto en el estado X (patrón State) | Ver §6 |

En código, cada escenario es un `setupEscenarioN()` llamado desde los tests (estilo de la
cátedra) o una clase de test por escenario (`BancoTestEsc_1`).

## §5. Tabla de decisión (solo si el resultado depende de combinaciones)

1. Condiciones (filas de arriba), acciones o resultados (filas de abajo).
2. Una columna por combinación relevante (2ⁿ como máximo; se colapsan las que no importan con `—`).
3. Cada columna es un caso.

| | R1 | R2 | R3 | R4 |
|---|---|---|---|---|
| Tiene hijos < 18 | S | N | N | — |
| Tiene hijos de 18 a 21 | N | S | N | — |
| Cantidad de hijos > 5 | N | N | N | S |
| **Incremento** | 10% por hijo | 5% por hijo | 0 | + 5% extra |

## §6. Transición de estados (solo para objetos con estados)

1. Tomar los estados y eventos **de la especificación o del diseño documentado** (UML de
   estados o de clases con las flechas `create`), no del código.
2. Armar la tabla **estado actual × evento → estado siguiente / respuesta**. Las celdas sin
   transición documentada son "se ignora" o "inválido", según la especificación; si no lo
   dice, es un **supuesto**.
3. Casos: **cada transición válida** al menos una vez; **cada evento en cada estado**
   (incluidos los que no deben cambiar el estado); y secuencias que recorran el ciclo
   completo (desde el estado inicial hasta volver a él).

| Estado actual \ Evento | E1 | E2 | E3 |
|------------------------|----|----|----|
| A (inicial) | → B | → C | se ignora |
| B | se ignora | → A | → C |

**Ejemplo del TP (Clínica):** el diseño documentado de la `Ambulancia` está en
`docs/informe/03-informe-tpe2026.md` (UML 1, flechas `create`). Por ejemplo,
`DisponibleState` crea `AtendiendoEnDomicilioState`, `TrasladandoPacienteClinicaState` y
`EnTallerState`. Con eso se arma la tabla y se contrasta con `getEstadoActual()`. Cuando la
máquina de estados se prueba **junto con los hilos** o con **mocks**, es trabajo de
`testing-integration`.

## §7. Ejemplos resueltos

### 7.1 Cátedra: `Empleado.getIncremento(double sueldoBase)` (clase 06)

Especificación: por cada hijo menor de 18, +10% del sueldo base; por cada hijo de 18 a 21,
+5%; con más de 5 hijos, +5% extra. Precondición: `sueldoBase > 0`, `hijos != null`.

| Entrada | Válidas | Inválidas (violan la pre) | Límites |
|---------|---------|---------------------------|---------|
| sueldoBase | V1: `> 0` | I1: `≤ 0` | `0.0`, `0.01` |
| edad de cada hijo | V2: `< 18`; V3: `18..21`; V4: `> 21` | — | 17, 18, 21, 22 |
| cantidad de hijos (escenario) | V5: 0; V6: 1..5; V7: `> 5` | I2: `hijos == null` | 5, 6 |

| Id | Escenario | Entrada | Cubre | Esperado |
|----|-----------|---------|-------|----------|
| CN1 | sin hijos | sueldo 1000 | V1, V5 | 0 |
| CN2 | 1 hijo de 10 | 1000 | V1, V2, V6 | 100 |
| CN3 | 1 hijo de 20 | 1000 | V3 | 50 |
| CN4 | 1 hijo de 25 | 1000 | V4 | 0 |
| CN5 | hijos de 17 y 18 | 1000 | límite 17/18 | 150 |
| CN6 | hijos de 21 y 22 | 1000 | límite 21/22 | 50 (supuesto: 21 incluido) |
| CN7 | 6 hijos de 30 | 1000 | V7, límite 6 | 50 |
| CN8 | 5 hijos de 30 | 1000 | límite 5 | 0 |

Contra el código de la diapositiva, CN2, CN3, CN5 y CN6 dan rojo (porcentajes invertidos)
y todos los que esperan algo distinto de 0 también (devuelve `0.0`). Es el ejemplo de la
cátedra de por qué el oráculo tiene que ser la SRS.

### 7.2 TP Clínica: `AsociadoDTO` (SRS §3.1)

`docs/consigna/02-srs-persistencia-clinica.md` §3.1: `nombre`, `apellido`, `dni`,
`ciudad`, `calle` y `telefono` no pueden ser nulos; `numero ≥ 0`.

| Entrada | Válidas | Inválidas | Límites / supuestos |
|---------|---------|-----------|---------------------|
| nombre, apellido, dni, ciudad, calle, telefono (cada uno) | V: no nulo | I: `null` | `""`: supuesto (la SRS solo prohíbe `null`) |
| numero | V: `≥ 0` | I: `< 0` | `-1`, `0`, `1` |
| escenario | E1: DTO vacío (`new AsociadoDTO()`); E2: DTO completo (constructor con parámetros) | — | — |

Casos tipo: un caso válido con **valores distintos en cada campo** que verifique los 7
getters (detecta campos cruzados entre constructor y setters); un caso inválido por campo
(7 en total); los límites de `numero`; y el constructor con parámetros con una entrada
inválida por caso. El mecanismo de rechazo (asertos, según el Informe del TP) define el
resultado esperado: `AssertionError`, con `-ea`.

### 7.3 Cátedra: `Traductor.agregarTraduccion(pal, trad, idDestino)` (clase 05)

- Escenarios: E1, el traductor con 12 palabras por idioma; E2, el traductor vacío.
- Clases: idioma destino (una por valor: INGLES, FRANCES, ITALIANO), palabra ya existente
  o nueva, traducción ya existente o nueva.
- Pruebas con propósito: (1) agrega palabras válidas a cualquier diccionario (E2);
  (2) no permite palabras en español repetidas (E1); (3) no permite traducciones repetidas (E1).
