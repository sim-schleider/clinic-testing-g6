# Testing de integración con MySQL

## Objetivo y alcance

Verificar la comunicación real entre `AsociadoDTO`, `DataAccessObject`, `BaseDeDatos` y el motor
MySQL (ítem 2 de la consigna): el patrón Singleton de la conexión, la creación automática del
esquema, de la tabla y de los datos semilla, y las operaciones CRUD con consultas parametrizadas.
Además, demostrar que la suite atrapa excepciones de SQL, errores de mapeo e inconsistencias del
código provisto.

Fuera de alcance: la vista y el controlador, que solo invocan al DAO.

Suites (`@Tag("integration")`):

- `BaseDeDatosIntegrationTest`: Singleton, esquema, tabla y datos semilla (CI1–CI7).
- `DataAccessObjectIntegrationTest`: CRUD, valores límite, errores de SQL y de mapeo (CI8–CI32).
- `BaseDeDatosDePrueba`: utilidades de la suite (conexión propia y reinicio del Singleton).

## Oráculo y supuestos

**Oráculo.** SRS §3.2 y §3.3:

- *"El componente aplicará el patrón de diseño Singleton para restringir la instanciación a un
  único manejador de conexión activo durante toda la ejecución."*
- *"Al inicializarse, el sistema ejecutará automáticamente una rutina de configuración del entorno
  (iniciarBD) para: verificar o crear el esquema de base de datos objetivo, eliminar y recrear la
  tabla Asociados, poblarla con registros iniciales de prueba."*
- *"El sistema aceptará una entidad de asociado, validará que no sea nula, la convertirá al formato
  DTO y ejecutará una inserción SQL persistente."*
- *"El sistema proveerá mecanismos para recuperar registros de asociados: de manera individual por
  su identificador único, en lotes acotados con un límite especificado, como una colección completa
  desde la base de datos."*
- *"El sistema modificará registros existentes o eliminará entradas basándose en criterios únicos
  como el número de documento (DNI)."*

Se suman las reglas de SRS §3.1 (ningún campo nulo, `numero` ≥ 0, el DNI es identificador único) y
los contratos Javadoc de cada método (`pre`, `post` y `@throws SQLException`).

**Supuestos** (lo que la especificación no dice):

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S7 | `cargar` con un id inexistente devuelve `null` | El Javadoc solo describe el caso existente |
| S8 | `actualizar` y `eliminar` con un DNI inexistente no lanzan excepción y no cambian ninguna fila | Comportamiento habitual de un `UPDATE`/`DELETE` sin coincidencias |
| S9 | `eliminar("")` y `eliminar(null)` se rechazan con `AssertionError` | Mismo criterio que el ítem 1: un DNI vacío o nulo es inválido (SRS §3.1) |
| S10 | Un texto más largo que su columna produce `SQLException` | MySQL 8 trabaja en modo estricto por defecto |
| S11 | Después de `cerrarConexion()`, `getInstancia()` entrega una conexión usable | SRS §3.2: "un único manejador de conexión **activo**" |
| S12 | La tabla debe rechazar un asociado sin DNI | SRS §3.1: el DNI "no puede ser nulo; actúa como identificador único" |
| S13 | Las cargas devuelven las filas en el orden del id | La SRS no define un orden; InnoDB devuelve por clave primaria |

## Estrategia de integración

| Componente | Real / Mock | Motivo |
|------------|-------------|--------|
| MySQL 8 | Real | La consigna exige comunicación real con el motor |
| `BaseDeDatos` | Real | Componente bajo prueba |
| `DataAccessObject` | Real | Componente bajo prueba |
| `AsociadoDTO` | Real | Atraviesa la integración (mapeo) |
| `Asociado`, `Domicilio` | Real | Entrada del DAO |

- **Integración ascendente.** Primero `BaseDeDatos` sola contra MySQL y después el DAO encima: si
  falla un CRUD, ya se sabe que la conexión y la tabla están bien. Un mock de JDBC escondería
  justamente los errores de SQL y de mapeo que la consigna pide encontrar.
- **Verificación independiente.** Toda escritura se comprueba con una conexión JDBC propia del test
  y SQL propio, nunca leyendo con el mismo DAO. Si `guardar` pusiera la calle en la columna de la
  ciudad y `cargar` la leyera al revés, una ida y vuelta con el DAO pasaría igual.
- **Estado conocido en cada test.** El `@BeforeEach` descarta el Singleton por reflexión (sin tocar
  el código) y crea uno nuevo; al crearse, `BaseDeDatos` ejecuta `iniciarBD()`, que recrea la tabla
  con los 20 datos semilla.
- **Datos distintos en cada campo** (`"Nombre"`, `"Apellido"`, `"40111222"`, `"Calle"`, `777`,
  `"Ciudad"`, `"2230001111"`), para que un cruce de columnas se note.

Secuencia de `guardar` y de `cargar`, trazada sobre el código:

```mermaid
sequenceDiagram
    participant T as Test
    participant D as DataAccessObject
    participant B as BaseDeDatos
    participant M as MySQL
    T->>D: guardar(asociado)
    D->>D: CrearDTO(asociado) → AsociadoDTO
    D->>B: ejecutarInsert(INSERT ... VALUES (?,...,?), dto)
    B->>M: PreparedStatement.executeUpdate()
    T->>M: SELECT propio por DNI (verificación)
    T->>D: cargar(id)
    D->>B: ejecutarConsulta(SELECT ... WHERE id = ?, id)
    B->>M: executeQuery() → ResultSet
    D->>D: lee el ResultSet sin next() (D7)
```

## Estados del entorno y valores representativos

| Elemento | Estados | Cómo se provoca |
|----------|---------|-----------------|
| Esquema `Grupo_7` | existe / no existe | Normal / `DROP DATABASE` propio (CI2) |
| Tabla `Asociados` | con la semilla / con una fila extra / con una fila con `numero = 0` / inexistente | `@BeforeEach` / `INSERT` o `DROP TABLE` propios |
| Singleton | recién creado / conexión cerrada | `@BeforeEach` / `cerrarConexion()` |
| Fila buscada | existe / no existe | id o DNI de la semilla / uno ausente |
| DNI | nuevo / repetido / vacío / nulo | `"40111222"` / `"12345678"` / `""` / `null` |

La SRS no fija largos, pero el esquema que crea `iniciarBD()` sí; de ahí salen los valores límite:

| Columna | Tipo | Límite válido | Límite inválido |
|---------|------|---------------|-----------------|
| `nombre`, `apellido`, `calle` | `VARCHAR(30)` | 30 | 31 |
| `ciudad` | `VARCHAR(35)` | 35 | 36 |
| `DNI` | `VARCHAR(10) UNIQUE` | 10 | 11 |
| `telefono` | `VARCHAR(15)` | 15 | 16 |

Parámetros del DAO: `id` ∈ {1 (primera fila), 20 (última), 21 (inexistente)}; `limite` ∈ {1, 20
(igual a la cantidad de filas), 21 (mayor)}. Los valores 0 de `id` y `limite` no se prueban porque
el Javadoc los declara como precondición (`id > 0`, `limite > 0`).

## Casos de prueba

| Id | Operación y entrada | Resultado esperado | Rojo | Verde |
|----|---------------------|--------------------|-----------|-------|
| CI1 | `getInstancia()` dos veces | La misma instancia | ✅ | ✅ |
| CI2 | Se borra el esquema y se crea el Singleton | `iniciarBD()` recrea `Grupo_7` | ✅ | ✅ |
| CI3 | Inicialización | La tabla tiene id y los siete atributos de SRS §3.1 | ✅ | ✅ |
| CI4 | Inicialización | 20 filas semilla, sin nulos, `numero` ≥ 0, DNI distintos | ✅ | ✅ |
| CI5 | `iniciarBD()` con una fila extra | Se recrea la tabla: vuelve a 20 filas | ✅ | ✅ |
| CI6 | `cerrarConexion()` y luego `getInstancia()` | Conexión activa (S11) | ❌ D5 | ❌ D5 |
| CI7 | `INSERT` propio sin DNI | `SQLException` (S12) | ❌ D6 | ❌ D6 |
| CI8 | `guardar` con datos válidos | Fila nueva con cada dato en su columna | ✅ | ✅ |
| CI9 | `guardar` con cada texto en su largo máximo | Se guarda completo | ✅ | ✅ |
| CI10–CI15 | `guardar` con nombre 31, apellido 31, DNI 11, calle 31, ciudad 36 o telefono 16 caracteres | `SQLException` y 20 filas (S10) | ✅ | ✅ |
| CI16 | `guardar` con el DNI de la fila 1 | `SQLIntegrityConstraintViolationException` | ✅ | ✅ |
| CI17 | `guardar(null)` | `AssertionError` | ✅ | ✅ |
| CI18 | `cargar(1)` | DTO igual a la fila 1 | ❌ D7 | ❌ D7 |
| CI19 | `cargar(20)` | DTO igual a la fila 20 | ❌ D7 | ❌ D7 |
| CI20 | `cargar(21)` | `null` (S7) | ❌ D7 | ❌ D7 |
| CI21 | `cargarConLimite(1)` | Solo la fila 1 | ✅ | ✅ |
| CI22 | `cargarConLimite(20)` | 20 elementos | ✅ | ✅ |
| CI23 | `cargarConLimite(21)` | 20 elementos | ✅ | ✅ |
| CI24 | `cargarBD()` | La tabla completa, cada DTO igual a su fila | ✅ | ✅ |
| CI25 | `actualizar` el asociado de la fila 7 (DNI `22334455`) | Cambia la fila 7 y ninguna otra | ❌ D8 | ❌ D8 |
| CI26 | `actualizar` un DNI inexistente | Ninguna fila cambia (S8) | ❌ D8 | ❌ D8 |
| CI27 | `eliminar` un DNI existente | 19 filas; el DNI ya no está | ✅ | ✅ |
| CI28 | `eliminar` un DNI inexistente | La tabla intacta (S8) | ✅ | ✅ |
| CI29 | `eliminar("")` | `AssertionError` (S9) | ✅ | ✅ |
| CI30 | `eliminar(null)` | `AssertionError` (S9) | ✅ | ✅ |
| CI31 | `cargarBD()` con una fila con `numero = 0` | 21 DTO, el último con `numero` 0 (SRS §3.1) | ❌ D3 | ✅ |
| CI32 | `cargarBD()` sin la tabla | `SQLException` hacia el llamador (Javadoc `@throws`) | ✅ | ✅ |

## Resultados

La suite se corrió contra un MySQL 8.0.40 real, antes y después de corregir `AsociadoDTO`:

| Fase | Fecha | Código | Total | Pasan | Fallan |
|------|-------|--------|-------|-------|--------|
| Rojo | 31/10/2026 y 02/11/2026 | Como se entregó | 32 | 24 | 8 |
| Verde | 02/11/2026 | `AsociadoDTO` corregido (D3 y D4) | 32 | 25 | 7 |

**En rojo** fallaron 8 casos, y cada uno se revisó contra el código para confirmar que la causa es
un defecto y no un error del test. La corrida del 31/10 se repitió el 02/11 y dio exactamente lo
mismo, así que el resultado es reproducible.

**En verde** pasó CI31, el caso que fallaba por el defecto D3 de `AsociadoDTO`: una fila con
`numero = 0` ahora se lee bien desde la base. Los otros 7 siguen fallando a propósito. Son los
defectos D5 a D8, que están en `BaseDeDatos` y `DataAccessObject`, y la consigna no pide corregir
esas clases: solo pide demostrar que la suite los detecta. Que sigan en rojo es justamente esa
demostración. Ningún caso que pasaba en rojo dejó de pasar.

Como el código entregado no compila (D1 y D2), las dos corridas se hicieron sobre una copia del
proyecto con esos dos arreglos aplicados. La del 31/10 usó el commit `c1ff2a6`; las del 02/11 usaron
una copia temporal que no se sube al repositorio (ver Apéndice C). Las tablas completas están en el
Apéndice A (`resultados/integration-rojo.md` y `resultados/integration-verde.md`).

## Defectos detectados

| Id | Ubicación | Descripción | Detectado por | Estado |
|----|-----------|-------------|---------------|--------|
| D3 | `AsociadoDTO.setNumero` | Rechaza 0: una fila válida con `numero = 0` rompe `cargarBD()` al mapearla | CU11, CI31 | Corregido (`6cda0de`) |
| D5 | `BaseDeDatos.getInstancia` | Después de `cerrarConexion()` sigue entregando el mismo manejador, con la conexión cerrada | CI6 | No se corrige |
| D6 | `BaseDeDatos.iniciarBD` (esquema) | Ninguna columna es `NOT NULL`: la tabla acepta un asociado sin DNI | CI7 | No se corrige |
| D7 | `DataAccessObject.cargar` | Lee el `ResultSet` sin llamar a `next()`: lanza `SQLException` incluso para un id existente | CI18, CI19, CI20 | No se corrige |
| D8 | `BaseDeDatos.ejecutarActualizacion` | Fija `WHERE id = 1` (`setInt(8, 1)`): siempre modifica la fila 1, no la del DNI | CI25, CI26 | No se corrige |

D8 se manifiesta de dos maneras. En CI25 el `UPDATE` intenta poner en la fila 1 el DNI de la fila 7
y MySQL lo rechaza por el `UNIQUE` (`Duplicate entry '22334455'`). En CI26, con un DNI nuevo, la
fila 1 se sobrescribe sin error: es una pérdida de datos silenciosa.

## Observaciones de revisión de código

Puntos que ningún test detecta:

- `CrearDTO(int id, …)` recibe el `id` y lo ignora; `AsociadoDTO` ni siquiera tiene ese campo.
- `ejecutarConsulta` nunca cierra el `PreparedStatement` (pérdida de recursos).
- `abrirConexion` e `iniciarBD` atrapan la `SQLException` y solo la imprimen. Con el servidor caído,
  `getInstancia()` devuelve un objeto con la conexión en `null` y el error aparece después como
  `NullPointerException`. Se observó al preparar el entorno (ver Apéndice C).
- El Javadoc de `eliminar` dice "pre: id debe ser mayor que 0", pero el método recibe un DNI.
- La fila semilla 4 tiene la ciudad `' Mar del Plata'`, con un espacio adelante.
- `Domicilio` exige `numero > 0` y la SRS admite `numero ≥ 0`: un asociado con número 0 no puede
  ni crearse para llegar a `guardar`.
- `getInstancia()` no es `synchronized`: el Singleton no es seguro si lo piden varios hilos a la vez.

## Fallas sembradas

Sin modificar el código, la capacidad de detección se demuestra con la base en estados forzados
desde el test: una fila con `numero = 0` (CI31), la tabla borrada (CI32), el esquema borrado (CI2)
y la conexión cerrada (CI6).

## Conclusiones

- La suite cubre las cuatro operaciones CRUD, el Singleton y la inicialización con 32 casos, y
  detectó cuatro defectos nuevos (D5–D8) que la prueba unitaria no podía ver: todos dependen del SQL
  real, del esquema o del ciclo de vida de la conexión.
- La verificación con SQL propio fue determinante en D8: con un DNI nuevo, `actualizar` no lanza
  error y una lectura con el mismo DAO no habría mostrado que se pisó la fila 1.
- CI31 muestra cómo un defecto unitario (D3) reaparece como error de mapeo en la integración: una
  fila que la SRS considera válida hace fallar la carga completa de la tabla. Al corregir
  `AsociadoDTO`, CI31 pasó a verde sin tocar nada de la persistencia: el problema estaba en el DTO,
  y la integración lo mostraba de otra forma.
- Los valores límite del esquema (CI9–CI15) pasaron: el motor protege los largos máximos. En cambio,
  el esquema no aplica las reglas de nulidad de la SRS (D6).
- Limitaciones: la suite depende de que MySQL esté levantado con el usuario de `docker-compose.yml`,
  del modo estricto (S10) y de `-ea`.
