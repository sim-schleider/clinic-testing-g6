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
| Suite de 4 casos (CU1–CU4), solo clases válidas y el valor límite 0 | `src/test/java/persistencia/AsociadoDTOTest.java` |
| Sección del informe (oráculo con precondiciones, supuestos S1–S3, clases V1–V14, casos, resultados, conclusiones) | `docs/informe/secciones/03-unitarias.md` |
| Defecto D3 (`setNumero` rechaza 0) | `docs/informe/secciones/07-defectos.md` |
| Ejecución en rojo | `docs/informe/resultados/unit-rojo.md` y `.log`: **desactualizados**, todavía tienen los 18 casos |
| Captura en rojo | Pendiente, la toma el equipo desde el IDE (Apéndice B) |

## Qué falta

1. **Volver a correr la suite en rojo** con `run_tests.sh unit rojo` para regenerar
   `resultados/unit-rojo.{md,log}`. Resultado esperado: 4 casos, 3 pasan y 1 falla (CU4, por D3).
   Los números que figuran hoy en la sección salen de la corrida del 25/10, en la que estos cuatro
   métodos tenían el mismo código. Al regenerar, sacar el comentario `PENDIENTE` de
   `03-unitarias.md`.
2. **Tomar la captura en rojo** de esa corrida.
3. **Decidir si se corrige D3** ("corregir si es necesario el código del ítem 1", al principio del
   punto 2 de la consigna):
   - **Opción A:** no se corrige. D3 queda en rojo como evidencia y el informe explica por qué no
     hay fase verde.
   - **Opción B:** se corrige D3 en su propio commit (`fix(persistencia): accept zero in
     AsociadoDTO.setNumero`), se corre `verde` y se toma la captura en verde. En el branch
     `test/item-2-db-integration` ya existe ese arreglo (`6cda0de`). El otro arreglo de ese branch,
     el del constructor (`aeb579f`), ya no hace falta.
4. **Revisar el apéndice de resultados.** Cuando se genere `90-apendice-resultados.md` (lo arma
   el script de `test-report`), verificar que aparezcan los 4 casos con su ID.

## Qué escribir en el informe

La sección `03-unitarias.md` ya está actualizada con el criterio de precondiciones. Solo hay que:

- si se elige la opción B, agregar una fila "Verde (código corregido)" a la tabla de resultados,
  citar el commit de la corrección y actualizar el estado de D3 en el registro de defectos;
- si se elige la opción A, dejarla como está y asegurarse de que la conclusión general (sección 8)
  explique por qué no hay fase verde.
