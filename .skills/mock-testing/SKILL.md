---
name: mock-testing
description: Design, write and run atomic tests with Mockito 5 test doubles (JUnit 5) and write the design, results and defects into the report section. For this project, Ambulancia is tested with a mocked State interface (no concrete states needed), and each concrete State is tested with a mocked Ambulancia to verify its transitions in isolation. Use this whenever the user mentions mocks, "dobles de prueba", stubs, Mockito, "testing con mocks", verify/ArgumentCaptor, isolating Ambulancia from its states, testing the State pattern without the concrete classes, or the "Testing con Mocks" part of the assignment. Not for running the real state machine or threads (concurrency-testing), the database (db-integration-testing), plain DTO validation (unit-testing) or assembling the report (test-report).
---

# Mock testing: Ambulancia and the State pattern with Mockito

This skill produces:
- the real-versus-mock design,
- the case table,
- JUnit 5 + Mockito 5 tests,
- red/green results, the defects found and the seeded faults,
- the report section `docs/informe/secciones/05-mocks.md`, written in Spanish.

The project conventions are in `.skills/CONVENTIONS.md`.

## What the assignment asks, and why mocks fit

"Aprovechar el diseño desacoplado basado en la interfaz State para construir pruebas atómicas
utilizando dobles de prueba (mocks), simulando comportamientos de estados ante la ausencia
temporal de las implementaciones concretas."

`Ambulancia` only talks to its state through the `State` interface, and `setState` is the
injection point. With a mocked `State`, a test can check what `Ambulancia` itself is responsible
for:
- that it delegates the right event to the current state, exactly once;
- that it manages the `ocupado` flag;
- that it notifies its observers;
- that it respects its preconditions.

The behaviour of the concrete states can't leak into the result. The reverse also works. A
concrete state with a mocked `Ambulancia` lets you verify **its** transition (which state it
installs through `setState`) without the rest of the machine. Together, the two directions
pinpoint which side of the collaboration a defect is on. The real machine with real threads
belongs to `concurrency-testing`.

## Inputs

- **Oracle**: `.skills/concurrency-testing/references/ambulance-oracle.md`. Read §1 (the
  transition table), §2 (the occupancy invariant) and §3 (the `Ambulancia` method contract). That
  file is derived from the UML, the Javadoc and `docs/Informe.md`, not from the code.
- **Code**: `modelo/modeloDominio/ambulancia/*`: `Ambulancia`, `State`, the six states and
  `RetornoAutomatico`. Read them for signatures and injection points only. The oracle file gives
  the expected results.

## Procedure

### 1. Decide what is real and what is a double

| Componente | Real / Mock | Motivo |
|------------|-------------|--------|
| Ambulancia (bajo prueba) | Real | Es la unidad bajo prueba; nunca se mockea |
| State | Mock | Pedido por la consigna; aísla a Ambulancia de los estados concretos |
| Asociado, Operario | Mock | Solo aportan `getNombre()`; los reales arrancan hilos y se registran en la ambulancia |
| Observer | Mock | Permite capturar las notificaciones (`ArgumentCaptor`) |

For the second group of tests (concrete states), invert the table: the state is real and
`Ambulancia` is a mock.

### 2. Design the cases

**Group A, Ambulancia with a mocked State.** One case per row of the method contract (§3 of the
oracle), with the ambulance free and the simulation active:
- which State method was called, and that it was called **once** (`verify` +
  `verifyNoMoreInteractions`);
- `ocupado` afterwards;
- that a `NotificacionSimulacion` reached the observer.

Add these cases too:
- the simulation is inactive (no delegation);
- `null` arguments (`AssertionError`);
- a mocked state that installs another mock through `setState`, to check that `Ambulancia`
  delegates to the **current** state and doesn't keep the old one;
- the order of effects (`InOrder`) where the oracle defines one.

**Group B, each concrete State with a mocked Ambulancia.** One case per cell of the 6×4
transition table:
- `verify(amb).setState(isA(Expected.class))` for a transition;
- `verify(amb, never()).setState(any())` plus a notification check for a rejection.

These cases are atomic: if a transition is wrong, only that cell is red.

Cases that need a thread to block (the ambulance is busy) are concurrency cases. List them and
leave them to `concurrency-testing`.

Use the case table with IDs `CM1…`:

| Id | Escenario | Método y entrada | Dobles y stubs | Resultado esperado (verificación) | Resultado |
|----|-----------|------------------|----------------|-----------------------------------|-----------|

Write the design into `docs/informe/secciones/05-mocks.md` in Spanish:

```
# Testing con mocks: Ambulancia y la interfaz State
## Objetivo y alcance
## Oráculo y supuestos
## Diseño: componentes reales y dobles de prueba
## Casos de prueba
### Ambulancia con State simulado
### Estados concretos con Ambulancia simulada
## Resultados
## Defectos detectados
## Fallas sembradas
## Conclusiones
```

### 3. Code the tests

Read `references/mockito5-patterns.md`.

- Use `AmbulanciaMockTest` and `EstadosMockTest` in package `modelo.modeloDominio.ambulancia`,
  with `@Tag("mock")` and `@ExtendWith(MockitoExtension.class)`.
- One method per case (`cm3_atenderDomicilioDelegaEnEstado`) with
  `@DisplayName("CM3 – ...")`. Group B can be a `@ParameterizedTest` over the 24 cells, with
  the case ID as the first argument.
- Some states sleep inside a transition, so put `@Timeout` on Group B.

### 4. Run

```bash
.skills/test-report/scripts/run_tests.sh mock dev
.skills/test-report/scripts/run_tests.sh mock rojo
```

### 5. Analyze every red

1. Confirm the red is about the behaviour under test. A Mockito
   `UnnecessaryStubbingException` or `WrongTypeOfReturnValue` is a test bug.
2. Then read the implementation to explain it.
3. Register the defect in `07-defectos.md`, citing the oracle row (UML, Javadoc or Informe).

A red in Group B (the state) next to a green in Group A (`Ambulancia`) locates the defect in the
state, and vice versa. Say so in the analysis, because it's the argument for why mocks were
worth using.

Propose fixes; apply them only with the user's OK, one commit each. Then run `verde`.

### 6. Seed faults (after green)

One at a time, run `mock dev`, record, then restore with `git checkout`. Good faults here:

- `Ambulancia` delegates the wrong event (`SolicitudDeTraslado` inside `atenderDomicilio`);
- it forgets `setOcupado(true)`;
- it calls the state twice;
- it drops `setChanged()` before notifying;
- a state installs the wrong next state;
- a rejection that changes state.

Record them in "Fallas sembradas" with IDs `FM1…`.

### 7. Results and conclusions

"Resultados" gives the counts for red and green and a pointer to the appendix. "Conclusiones"
covers:
- what isolation with mocks made visible, such as which defects are in `Ambulancia` and which
  are in the states;
- what mocks **cannot** show: synchronization, waiting and real threads. Hand over to the
  concurrency section here.

## Never

- Mock `Ambulancia` in the tests of `Ambulancia`, or the state in the tests of that state. The
  unit under test is always real.
- Stub something and then assert on the stub. It proves nothing about the code.
- Take the expected transition from the code. Take it from the oracle table.
- Use `Thread.sleep` to wait for something. Single-threaded mock tests don't need it, and the
  blocking cases belong to `concurrency-testing`.
- Leave a seeded fault in the code or commit it.

## References

| File | When |
|------|------|
| `.skills/concurrency-testing/references/ambulance-oracle.md` | Step 2 (§1–§3) |
| `references/mockito5-patterns.md` | Step 3 |
