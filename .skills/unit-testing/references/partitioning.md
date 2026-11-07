# Equivalence partitioning and boundary values

## 1. What counts as an input

The inputs of a case are:
- the **parameters** of the method,
- the **state of the object** (the scenario),
- any **external data** the method reads.

A setter called on an empty object and on a loaded object is two different classes of the same
input dimension.

## 2. Precondition or guaranteed behaviour

Before partitioning, classify each restriction (see "Preconditions are not tested" in
`SKILL.md`):

- **Precondition** (every Javadoc `pre:`, the SRS §3.1 rules in the `AsociadoDTO` constructor):
  the values that violate it are outside the domain. They are not an equivalence class and get
  no case.
- **Guaranteed behaviour** (the contract says what happens, for example `@throws` or a `post:`
  covering the input, or the validations of the `AsociadoDTO` setters, which the design report
  documents): those values form invalid classes, each with its own case.

## 3. Partition rules by data type

The "outside the domain / invalid" column holds values outside the domain when the restriction is
a precondition, and invalid classes when the restriction is guaranteed behaviour.

| Spec says | Valid classes | Outside the domain / invalid |
|-----------|---------------|------------------------------|
| Reference "not null" | non-null value | `null` |
| String "not null", nothing else | non-empty text; `""` and blank text as **supuestos** | `null` |
| String with a length limit `n` | length 1..n | length 0 (if forbidden), length n+1 |
| String with a format (DNI digits, phone) | matches format | each distinct way of breaking it (letters, too short, too long) |
| Integer range `[a, b]` | `a..b` | `< a`, `> b` |
| Integer `>= a` (no upper bound) | `>= a`; the upper type limit `Integer.MAX_VALUE` is a valid boundary | `< a` |
| Enumerated set | each value (one class per value if behaviour differs) | a value outside the set |
| Boolean | `true`, `false` | — |

When the spec forbids only `null`, the empty string is neither clearly valid nor clearly
outside the domain. Do not decide silently: list it as a supuesto and design a case for it. Its
expected result follows the supuesto stated in the report.

## 4. Output partitions and scenarios

Also partition the **outputs**. For a setter, the outputs are:
- the value read back,
- the other fields staying unchanged,
- the rejection, only when the contract guarantees one.

Each distinct output deserves at least one case. Treat the scenario (empty object or loaded
object) as one more input dimension.

## 5. Boundary values

For each bound, take the bound itself and its immediate neighbours, **keeping only those inside
the domain** when the bound comes from a precondition:

- precondition `x >= 0` → 0 (the bound), 1. −1 is outside the domain and is not tested.
- precondition `x > 0` → 1 (the bound), 2. 0 is outside the domain.
- guaranteed behaviour `x >= 0` (for example, "throws if negative") → −1 (invalid), 0, 1.
- Range `[1, 30]` → 1, 2, 29, 30; plus 0 and 31 only if the contract guarantees their rejection.
- Length `<= 30` → lengths 29, 30; plus 31 only if the contract guarantees its rejection.
- Type extremes when there is no explicit upper bound: `Integer.MAX_VALUE` (valid).
  `Integer.MIN_VALUE` only when negatives are guaranteed behaviour, not a precondition.

The boundary is where an off-by-one in the code (`>` instead of `>=`) shows up. A suite without
the exact bound value cannot detect that defect: a check that rejects the valid bound breaks the
contract even when the restriction is a precondition.

## 6. From classes to cases

1. One or more **valid** cases that together cover every valid class, each covering as many as
   possible.
2. **One case per invalid class** (guaranteed behaviour only), with all other inputs valid.
3. One case per boundary value not already covered.
4. Number cases `CU1…` and list the classes each covers, so coverage can be checked by reading
   the table.

## 7. Worked example: `Domicilio`

Contract (Javadoc of `Domicilio`): `calle` not null, `numero > 0`, `ciudad` not null, all three
as preconditions.

| Entrada | Condición | Tipo | Clases válidas | Fuera del dominio | Valores límite |
|---------|-----------|------|----------------|-------------------|----------------|
| calle | no nula | Precondición | V1: texto no nulo | `null` (no se prueba) | — |
| numero | `> 0` | Precondición | V2: `numero >= 1` | `numero <= 0` (no se prueba) | 1, 2, `Integer.MAX_VALUE` |
| ciudad | no nula | Precondición | V3: texto no nulo | `null` (no se prueba) | — |

| Id | Escenario | Método y entrada | Clases cubiertas | Resultado esperado |
|----|-----------|------------------|------------------|--------------------|
| CU1 | — | `new Domicilio("Alem", 1450, "Mar del Plata")` | V1, V2, V3 | getters devuelven `"Alem"`, `1450`, `"Mar del Plata"` |
| CU2 | — | `new Domicilio("Alem", 1, "Mar del Plata")` | V2 (límite) | `getNumero() == 1` |
| CU3 | — | `new Domicilio("Alem", Integer.MAX_VALUE, "Mar del Plata")` | V2 (extremo del tipo) | `getNumero() == Integer.MAX_VALUE` |

If the contract instead said "throws `IllegalArgumentException` if `numero <= 0`", that would be
guaranteed behaviour: `numero <= 0` would become class I1, with cases for 0 (the boundary) and
−1, each expecting the exception.

Note that `Domicilio` (`numero > 0`) and the SRS rule for `AsociadoDTO.numero` (`>= 0`) are
different. When a DTO is mapped into a `Domicilio`, a value of 0 is valid on one side and
invalid on the other. That is an integration concern; mention it, and leave it to
`db-integration-testing`.
