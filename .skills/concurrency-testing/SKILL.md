---
name: concurrency-testing
description: Design, write and run integration and concurrency tests (JUnit 5) of the real ambulance state machine. Covers the State transition table, the occupancy invariant, synchronized methods, wait/notifyAll, threads that compete for the ambulance (Asociado, Operario), the RetornoAutomatico background thread, timeouts and deadlock detection, then writes the design, results and defects into the report section. Use this whenever the user mentions concurrency, threads, "hilos", "concurrencia", synchronized, wait/notify, deadlock, race conditions, "recursos compartidos", "bloques sincronizados", RetornoAutomatico, the full state-machine flow, "máquina de estados", or wants to prove the suite catches synchronization or state faults introduced in the system. Not for Ambulancia with a mocked State (mock-testing), the database (db-integration-testing), DTO validation (unit-testing) or assembling the report (test-report).
---

# Concurrency testing: the real state machine and its threads

This skill produces:
- the state-transition design,
- the concurrency scenarios,
- the case table,
- JUnit 5 tests with real threads,
- red/green results, the defects found and the seeded faults,
- the report section `docs/informe/secciones/06-concurrencia.md`, written in Spanish.

The project conventions are in `.skills/CONVENTIONS.md`.

## What the assignment asks

"Evaluar el flujo completo de la máquina de estados acoplada a la clase Ambulancia, validando la
correcta gestión de recursos compartidos, bloques sincronizados y la interacción con hilos
independientes de ejecución", and then "demostrar […] que la suite descubre los problemas de
sincronización o fallos de estado introducidos en el sistema".

That gives three parts:
1. The **state machine** with real states (no mocks): every transition, and whole flows.
2. **Synchronization**: mutual exclusion on the ambulance, threads that wait and are woken,
   orderly termination, and `RetornoAutomatico` running alongside the other threads.
3. **Seeded faults**: break synchronization or state on purpose and show the suite turns red.

## Why concurrency tests need special care

Concurrency defects are nondeterministic. A naive test either passes by luck or hangs the build.
These rules come from that:

- **Never synchronize a test with `Thread.sleep`.** Wait for an observable condition (the thread
  state is `WAITING`, the ambulance state changed) with a deadline. Use a helper that polls a
  condition, or a `CountDownLatch`.
- **Every test has a timeout.** A missing `notifyAll` or a lost wake-up shows up as a hang, and
  the timeout turns that hang into a red with a thread dump instead of a stuck build.
- **A failed assertion inside another thread is invisible to JUnit.** Capture exceptions from
  worker threads and assert on them in the test thread.
- **Repeat what is racy.** A race that shows up 1 time in 20 is still a defect. Use
  `@RepeatedTest`, and report the detection rate.
- **Clean up threads.** End the simulation (`setSimulacionActiva(false)` wakes everyone),
  interrupt, and join with a deadline in `@AfterEach`. A leftover thread can make the next test
  fail.

## Inputs

- **Oracle**: `references/ambulance-oracle.md`:
  - §1 the transition table (24 cells),
  - §2 the occupancy invariant,
  - §3 the `Ambulancia` method contract,
  - §4 `RetornoAutomatico`.

  It is derived from the UML, the Javadoc and `docs/Informe.pdf`, not from the code.
- **Code**: `modelo/modeloDominio/ambulancia/*`, `Asociado`, `Operario`, `TiempoMuerto` (random
  0–8 s sleeps), and `Clinica.iniciarSimulacion` (how the threads are started in production).
  Read them to find the shared state and the synchronized regions. Expected results come from the
  oracle.

## Procedure

### 1. Map the shared state and the threads

Build the table `Recurso compartido | Quién lo lee/escribe (hilo) | Protección`. Cover at least:
- `estadoActual`, `ocupado` and `isSimulacionActiva` of `Ambulancia`;
- the `asociados` list;
- the ambulance's monitor (`wait`/`notifyAll`).

Note any access that isn't protected. Each one is a candidate case, or a code-review finding if a
test can't expose it reliably (for example, missing visibility on a getter).

### 2. Design the state-machine cases

These use real states and a real `Ambulancia`, driven through the public `Ambulancia` methods,
single-threaded:

- **Each cell** of the oracle's transition table: put the ambulance in the source state through
  a real sequence of operations, fire the event, and check the resulting state class and the
  occupancy invariant.
- **Complete flows** (paths through the diagram), such as:
  - Disponible → domicilio → regreso → Disponible;
  - Disponible → traslado → retorno → Disponible;
  - Disponible → taller → vuelta del taller → retorno → Disponible.

  Check the invariant after every step.

If reaching a source state requires a blocked call (the ambulance is busy), use `setOcupado`
**only** to build the scenario, and say so in the case.

### 3. Design the concurrency cases

Use `references/concurrency-patterns.md` for the scenario catalogue. At minimum:

| Escenario | Qué demuestra |
|-----------|---------------|
| Dos hilos piden la ambulancia a la vez | Exclusión mutua: entra uno, el otro queda `WAITING` |
| El hilo en espera se despierta al liberar (`regresarSinPaciente`, `volviendoDelTaller`) | Sin wake-ups perdidos |
| `setSimulacionActiva(false)` con hilos esperando | Terminación ordenada: todos salen sin delegar |
| `RetornoAutomatico` con la simulación activa | El hilo sigue vivo y produce el retorno dentro del intervalo |
| `RetornoAutomatico` al terminar la simulación o al interrumpirlo | El hilo termina |
| Ocupación coherente tras el retorno automático | Invariante §2 (si no, deadlock de los que esperan) |
| Una transición lenta no bloquea las consultas | Detecta `sleep` dentro de una región `synchronized` (**supuesto**: buena práctica, justificarlo) |
| Simulación completa pequeña (2–3 asociados, 1 operario, retorno automático) | Termina sin deadlock en un tiempo acotado |

Use the case table with IDs `CC1…`:

| Id | Escenario e hilos | Acciones | Condición de sincronización | Resultado esperado | Timeout | Resultado |
|----|-------------------|----------|-----------------------------|--------------------|---------|-----------|

Write steps 1–3 into `docs/informe/secciones/06-concurrencia.md` in Spanish:

```
# Testing de integración y concurrencia: máquina de estados de la Ambulancia
## Objetivo y alcance
## Oráculo y supuestos
## Recursos compartidos e hilos
## Tabla de transiciones de estado
## Casos de prueba
### Máquina de estados
### Concurrencia
## Resultados
## Defectos detectados
## Fallas sembradas
## Conclusiones
```

Include the oracle's Mermaid `stateDiagram-v2` in "Tabla de transiciones de estado". It is the
formal design of the scenarios.

### 4. Code the tests

Read `references/concurrency-patterns.md`. It has the helpers: condition waiting, worker
threads that capture their exceptions, thread dumps, deadlock detection and cleanup.

- Use `AmbulanciaStateMachineTest` (single-threaded flows) and `AmbulanciaConcurrencyTest` /
  `RetornoAutomaticoConcurrencyTest` (threads), all with `@Tag("concurrency")`, in package
  `modelo.modeloDominio.ambulancia`.
- One method per case (`cc4_segundoPedidoEsperaHastaLiberar`) with `@DisplayName("CC4 – ...")`.
- Put `@Timeout` on every test, chosen from the real intervals. `RetornoAutomatico` waits 10 s,
  so its tests need about 15–25 s, and the others about 5 s.
- Tests that take longer than about 10 s also get `@Tag("lento")`, so the user can skip them while
  iterating with `-DexcludedGroups=lento`.

### 5. Run

```bash
.skills/test-report/scripts/run_tests.sh concurrency dev
.skills/test-report/scripts/run_tests.sh concurrency rojo
```

Run racy tests repeatedly before trusting a green: `@RepeatedTest(20)`, or several runs.

### 6. Analyze every red

1. Classify the red:
   - **timeout**: a hang. Read the thread dump in the failure message to see who waits on what.
   - **assertion**: wrong state or wrong invariant.
   - **exception from a worker thread**: for example a `ConcurrentModificationException`.
2. Confirm the red is not the test's own race (a missing wait condition, missing cleanup).
3. Read the implementation and explain the mechanism. Examples: a lock held while sleeping, a
   flag not reset, a wrong transition, an inverted loop guard.
4. Register the defect in `07-defectos.md` citing the oracle row.

Propose fixes; apply them only with the user's OK, one commit each. Then run `verde`, repeated.

### 7. Seed faults (after green)

This is the part the assignment explicitly asks to demonstrate. On the green code, seed **one
fault at a time**, run `concurrency dev` (repeat it for racy faults), record the result, then
restore with `git checkout`. The catalogue is in `references/concurrency-patterns.md` §6. It
includes:
- removing `synchronized`;
- replacing `while` with `if` around `wait()`;
- deleting a `notifyAll()`;
- not resetting `ocupado`;
- a wrong transition;
- an inverted guard in `RetornoAutomatico`.

Record them in "Fallas sembradas" with IDs `FC1…`, adding a detection rate column for racy faults
(for example "detectada 18/20").

### 8. Results and conclusions

"Resultados" gives the counts for red and green, the repetitions, and a pointer to the appendix.
"Conclusiones" covers:
- which synchronization and state defects were found;
- how each was detected (hang, invariant, transition);
- the detection rates for seeded faults;
- the limits of testing concurrency. A green run does not prove the absence of races. Mention
  what was found by code review instead of tests, such as unsynchronized getters and non-atomic
  check-then-act.

## Never

- Use `Thread.sleep` as the synchronization between the test and the threads.
- Write a thread test without a timeout.
- Assert inside a worker thread without propagating the failure to the test thread.
- Leave threads running after a test.
- Declare a racy fault "not detected" after one run.
- Mock the states here. The real machine is the point of this suite; mocks live in `mock-testing`.
- Leave a seeded fault in the code or commit it.

## References

| File | When |
|------|------|
| `references/ambulance-oracle.md` | Steps 2–3 (the oracle for every expected result) |
| `references/concurrency-patterns.md` | Steps 3–4; §6 in step 7 |
