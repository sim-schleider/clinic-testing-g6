# Equivalence partitioning and boundary values

## 1. What counts as an input

The inputs of a case are:
- the **parameters** of the method,
- the **state of the object** (the scenario),
- any **external data** the method reads.

A setter called on an empty object and on a loaded object is two different classes of the same
input dimension.

## 2. Partition rules by data type

| Spec says | Valid classes | Invalid classes |
|-----------|---------------|-----------------|
| Reference "not null" | non-null value | `null` |
| String "not null", nothing else | non-empty text; `""` and blank text as **supuestos** | `null` |
| String with a length limit `n` | length 1..n | length 0 (if forbidden), length n+1 |
| String with a format (DNI digits, phone) | matches format | each distinct way of breaking it (letters, too short, too long) |
| Integer range `[a, b]` | `a..b` | `< a`, `> b` |
| Integer `>= a` (no upper bound) | `>= a` | `< a`; the upper type limit `Integer.MAX_VALUE` is a valid boundary |
| Enumerated set | each value (one class per value if behaviour differs) | a value outside the set |
| Boolean | `true`, `false` | — |

When the spec forbids only `null`, the empty string is neither clearly valid nor clearly
invalid. Do not decide silently: list it as a supuesto and design a case for it. Its expected
result follows the supuesto stated in the report.

## 3. Output partitions and scenarios

Also partition the **outputs**. For a setter, the outputs are:
- the value read back,
- the other fields staying unchanged,
- the rejection.

Each distinct output deserves at least one case. Treat the scenario (empty object or loaded
object) as one more input dimension.

## 4. Boundary values

For each bound, test the bound itself and its immediate neighbours:

- `x >= 0` → −1 (invalid), 0 (valid, the bound), 1 (valid).
- `x > 0` → 0 (invalid), 1 (valid), 2 (valid).
- Range `[1, 30]` → 0, 1, 2, 29, 30, 31.
- Length `<= 30` → lengths 29, 30, 31 (and 0 if empty is restricted).
- Type extremes when there is no explicit upper bound: `Integer.MAX_VALUE` (valid) and
  `Integer.MIN_VALUE` (invalid when negatives are forbidden).

The boundary is where an off-by-one in the code (`>` instead of `>=`) shows up. A suite without
the exact bound value cannot detect that defect.

## 5. From classes to cases

1. One or more **valid** cases that together cover every valid class, each covering as many as
   possible.
2. **One case per invalid class**, with all other inputs valid.
3. One case per boundary value not already covered.
4. Number cases `CU1…` and list the classes each covers, so coverage can be checked by reading
   the table.

## 6. Worked example: `Domicilio`

Contract (Javadoc of `Domicilio`): `calle` not null, `numero > 0`, `ciudad` not null.

| Entrada | Condición | Clases válidas | Clases inválidas | Valores límite |
|---------|-----------|----------------|------------------|----------------|
| calle | no nula | V1: texto no nulo | I1: `null` | — |
| numero | `> 0` | V2: `numero >= 1` | I2: `numero <= 0` | 0, 1, 2, `Integer.MAX_VALUE` |
| ciudad | no nula | V3: texto no nulo | I3: `null` | — |

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado |
|----|-----------|------------------|------------------|--------------------|
| CU1 | — | `new Domicilio("Alem", 1450, "Mar del Plata")` | V1, V2, V3 | getters devuelven `"Alem"`, `1450`, `"Mar del Plata"` |
| CU2 | — | `new Domicilio(null, 1450, "Mar del Plata")` | I1 | `AssertionError` |
| CU3 | — | `new Domicilio("Alem", 0, "Mar del Plata")` | I2 (límite) | `AssertionError` |
| CU4 | — | `new Domicilio("Alem", 1, "Mar del Plata")` | V2 (límite) | `getNumero() == 1` |
| CU5 | — | `new Domicilio("Alem", 1450, null)` | I3 | `AssertionError` |

Note that `Domicilio` (`numero > 0`) and the SRS rule for `AsociadoDTO.numero` (`>= 0`) are
different. When a DTO is mapped into a `Domicilio`, a value of 0 is valid on one side and
invalid on the other. That is an integration concern; mention it, and leave it to
`db-integration-testing`.
