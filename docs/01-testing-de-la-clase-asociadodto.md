# Punto 1: Testing de la clase `AsociadoDTO`

> Documento de planificación. No implementa nada: dice qué se pide, qué ya está hecho y qué falta.

## Qué pide la consigna

- Una suite de pruebas **unitarias, estrictamente aisladas** (sin MySQL ni ningún otro servicio)
  que verifique la asignación, la recuperación y las validaciones de los setters de `AsociadoDTO`.
- Demostrar que la suite **detecta las fallas** del código provisto.
- Conclusiones en el informe y resultados en el apéndice.

## Criterio: las reglas de la SRS son precondiciones

La cátedra indicó que las reglas de la SRS §3.1 (`String` no nulos, `numero >= 0`) son
**precondiciones** del constructor parametrizado y de los setters: las garantiza el cliente. Ante
una entrada que las viola el contrato no define nada, así que esas entradas quedan fuera del
dominio y **no se prueban**. Por eso:

- la suite pasó de 18 a 4 casos: salieron los 14 de clases inválidas (antes CU3–CU9 y
  CU12–CU18);
- D4 (el constructor no valida) dejó de ser un defecto y se sacó del registro;
- D3 (`setNumero` rechaza 0) sigue siendo un defecto: 0 cumple la precondición y el método lo
  rechaza.

Cambio en el branch `test/item-1-preconditions` (PR #3).

## Clases de equivalencia

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

Ya no hay clases inválidas: las anteriores I1–I14 (cada `String` en `null` y `numero = -1`, para
el constructor y para cada setter) quedan fuera del dominio por precondición.

## Casos de prueba

| Id | Antes | Escenario | Método y entrada | Clases cubiertas | Resultado esperado | Resultado |
|----|-------|-----------|------------------|------------------|--------------------|-----------|
| CU1 | CU1 | E1 | `new AsociadoDTO()` | — | `null` en los `String`, `numero` = 0 (S3) | ✅ |
| CU2 | CU2 | — | Constructor con datos válidos y `numero` = 0 | V1–V7 (límite 0) | Cada getter devuelve su valor | ✅ |
| CU3 | CU10 | E2 | Los seis setters `String` con texto distinto al de E2 | V8–V13 | Cada campo toma su valor; `numero` sin cambios | ✅ |
| CU4 | CU11 | E2 | `setNumero(0)` | V14 (límite 0) | `numero` = 0; resto sin cambios | ❌ D3 |

## Estado actual

| Qué | Dónde |
|-----|-------|
| Suite de 4 casos (CU1–CU4), solo clases válidas y el valor límite 0 | `src/test/java/item1/AsociadoDTOTest.java` |
| Sección del informe (oráculo con precondiciones, supuestos S1–S3, clases V1–V14, casos, resultados, conclusiones) | `docs/informe/secciones/03-unitarias.md` |
| Defecto D3 (`setNumero` rechaza 0) | `docs/informe/secciones/07-defectos.md` |
| Ejecución | `docs/informe/resultados/unit.{md,log}`: **pendiente**, se genera con `run_tests.sh unit` |
| Captura | Pendiente, la toma el equipo desde el IDE (Apéndice B) |

## Qué falta

1. **Correr la suite** con `run_tests.sh unit` sobre el código de master (el `AsociadoDTO`
   entregado) para generar `resultados/unit.{md,log}`. Resultado esperado: 4 casos, 3 pasan y 1
   falla (CU4, por D3). Al generarlo, sacar el comentario `PENDIENTE` de `03-unitarias.md`.
2. **Tomar la captura** de esa corrida: muestra CU1–CU3 en verde y CU4 en rojo.
3. **D3 se corrige en el punto 2** ("corregir si es necesario el código del ítem 1"), en el branch
   `test/item-2-db-integration` (`6cda0de`). No hay una corrida "antes y después": cada suite se
   ejecuta una vez sobre el código de su punto.
4. **Revisar el apéndice de resultados.** Cuando se genere `90-apendice-resultados.md` (lo arma
   el script de `test-report`), verificar que aparezcan los 4 casos con su ID.
