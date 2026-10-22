# Concurrency test patterns (JUnit 5, Java 17)

## 1. Helpers

Put these in a small test-support class (`HilosDePrueba`) in the same test package.

```java
package modelo.modeloDominio.ambulancia;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.fail;

final class HilosDePrueba {
    private final List<Thread> hilos = new ArrayList<>();
    private final List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());

    /** Starts a worker whose exceptions are captured instead of being lost. */
    Thread lanzar(String nombre, Runnable accion) {
        Thread t = new Thread(() -> {
            try {
                accion.run();
            } catch (Throwable e) {
                errores.add(e);
            }
        }, nombre);
        t.setDaemon(true);
        hilos.add(t);
        t.start();
        return t;
    }

    /** Polls a condition until it holds or the deadline passes. Not a sleep-based synchronization:
     *  the test proceeds as soon as the condition is observable. */
    static void esperarHasta(BooleanSupplier condicion, Duration limite, String descripcion) {
        long fin = System.nanoTime() + limite.toNanos();
        while (!condicion.getAsBoolean()) {
            if (System.nanoTime() > fin) {
                fail("No se cumplió a tiempo: " + descripcion + "\n" + volcadoDeHilos());
            }
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    static boolean esperando(Thread t) {
        Thread.State s = t.getState();
        return s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING;
    }

    static boolean bloqueado(Thread t) {
        return t.getState() == Thread.State.BLOCKED;
    }

    void sinErroresEnHilos() {
        if (!errores.isEmpty()) {
            AssertionError e = new AssertionError("Excepción en un hilo de trabajo: " + errores.get(0));
            errores.forEach(e::addSuppressed);
            throw e;
        }
    }

    /** Cleanup for @AfterEach: interrupt and join everything that was started. */
    void terminarTodos(Duration limite) throws InterruptedException {
        for (Thread t : hilos) t.interrupt();
        for (Thread t : hilos) t.join(limite.toMillis());
        hilos.clear();
    }

    static String volcadoDeHilos() {
        var mx = ManagementFactory.getThreadMXBean();
        long[] bloqueados = mx.findDeadlockedThreads();
        StringBuilder sb = new StringBuilder();
        if (bloqueados != null) sb.append("DEADLOCK entre hilos ").append(Arrays.toString(bloqueados)).append('\n');
        for (ThreadInfo info : mx.dumpAllThreads(true, true)) {
            if (info.getThreadName().startsWith("CC")) sb.append(info);
        }
        return sb.toString();
    }
}
```

Name worker threads with the case ID (`"CC4-asociado-2"`), so the dump only shows the relevant
ones.

## 2. Test class skeleton

```java
@Tag("concurrency")
@DisplayName("Ambulancia – concurrencia")
class AmbulanciaConcurrencyTest {
    private Ambulancia ambulancia;
    private HilosDePrueba hilos;

    @BeforeEach
    void escenario() {
        ambulancia = new Ambulancia();
        hilos = new HilosDePrueba();
    }

    @AfterEach
    void limpiar() throws InterruptedException {
        ambulancia.setSimulacionActiva(false);   // per contract, wakes every waiting thread
        hilos.terminarTodos(Duration.ofSeconds(2));
    }

    private static Asociado asociado(String nombre) {
        // Constructor without an ambulance: it does not register itself or start anything.
        return new Asociado(nombre, "Prueba", "40000001", "2230000000",
                new Domicilio("Calle", 100, "Mar del Plata"));
    }
}
```

## 3. Scenario catalogue

### Mutual exclusion and waiting

```java
@Test
@Timeout(5)
@DisplayName("CC4 – con la ambulancia ocupada, un segundo pedido espera hasta que se libera")
void cc4_segundoPedidoEspera() {
    ambulancia.atenderDomicilio(asociado("Ana"));             // occupies (Disponible → AtendiendoEnDomicilio)
    Thread segundo = hilos.lanzar("CC4-asociado-2", () -> ambulancia.trasladarALaClinica(asociado("Beto")));

    HilosDePrueba.esperarHasta(() -> HilosDePrueba.esperando(segundo), Duration.ofSeconds(2),
            "el segundo pedido debe quedar en espera");
    assertInstanceOf(AtendiendoEnDomicilioState.class, ambulancia.getEstadoActual());

    ambulancia.regresarSinPaciente();                         // frees and must wake the waiter
    HilosDePrueba.esperarHasta(() -> !segundo.isAlive(), Duration.ofSeconds(3),
            "el pedido en espera debe completarse al liberarse la ambulancia");
    hilos.sinErroresEnHilos();
}
```

### Orderly termination

Start N waiters on a busy ambulance and call `setSimulacionActiva(false)`. Then check:
- all threads finish within the deadline;
- the state did not change (no delegation after termination).

### Occupancy invariant after each operation

```java
static void invarianteOcupacion(Ambulancia a) {
    State s = a.getEstadoActual();
    boolean debeEstarOcupada = s instanceof AtendiendoEnDomicilioState
            || s instanceof TrasladandoPacienteClinicaState
            || s instanceof EnTallerState;
    assertEquals(debeEstarOcupada, a.isOcupado(),
            "invariante de ocupación violado en estado " + s.getClass().getSimpleName());
}
```

Call it after every step of every flow. It is the cheapest way to catch a stale `ocupado` flag,
which in production leaves threads waiting forever.

### `RetornoAutomatico`

```java
@Test
@Timeout(5)
@DisplayName("CC10 – con la simulación activa, el hilo de retorno automático sigue vivo")
void cc10_retornoAutomaticoSigueVivo() throws InterruptedException {
    Thread retorno = hilos.lanzar("CC10-retorno", new RetornoAutomatico(ambulancia));
    retorno.join(500);
    assertTrue(retorno.isAlive(), "el hilo terminó aunque la simulación está activa");
}

@Test
@Tag("lento")
@Timeout(25)
@DisplayName("CC11 – el retorno automático devuelve la ambulancia dentro del intervalo")
void cc11_retornoAutomaticoProduceRetorno() {
    ambulancia.atenderDomicilio(asociado("Ana"));            // AtendiendoEnDomicilio
    hilos.lanzar("CC11-retorno", new RetornoAutomatico(ambulancia));
    HilosDePrueba.esperarHasta(
            () -> ambulancia.getEstadoActual() instanceof RegresandoClinicaSinPacienteState,
            Duration.ofSeconds(15), "el retorno automático debe ocurrir dentro de ~10 s");
    invarianteOcupacion(ambulancia);
}
```

Interruption: start the thread, `interrupt()` it, and expect it to finish within about 1 s.

### Slow transition holding the lock

Some transitions take time. Measure whether a short synchronized query (`isOcupado()`) is blocked
while another thread runs a transition:

```java
ambulancia.trasladarALaClinica(asociado("Ana"));        // TrasladandoPacienteClinica: its RetornoClinica is the slow one
Thread lento = hilos.lanzar("CC14-transicion", () -> ambulancia.regresarSinPaciente());
HilosDePrueba.esperarHasta(() -> HilosDePrueba.esperando(lento) || !lento.isAlive(), Duration.ofSeconds(2), "transición en curso");
long t0 = System.nanoTime();
ambulancia.isOcupado();
long ms = (System.nanoTime() - t0) / 1_000_000;
assertTrue(ms < 200, "una consulta quedó bloqueada " + ms + " ms por una transición en curso");
```

The expected result is a **supuesto** (holding a monitor while sleeping blocks every other actor).
Justify it in the report with the Informe's own description of `synchronized` and `wait`.

### Concurrent registration and iteration

Iterate (`verificarYFinalizarSimulacion`) while other threads call `agregarAsociado`. Repeat it
(`@RepeatedTest(20)`), and capture any `ConcurrentModificationException` through the worker
helper.

### Small full simulation (smoke)

Run 2–3 real `Asociado` threads (`maxCantSolicitudes = 1`), one `Operario` and one
`RetornoAutomatico`, as `Clinica.iniciarSimulacion` does but without the DB or the view. Then:
- expect every `Asociado` thread to finish within a generous deadline (60–90 s, because
  `TiempoMuerto` sleeps up to 8 s per step);
- check the invariant at the end.

On timeout, the dump in the failure message is the evidence for the report. Tag it `lento`.

## 4. Repetition and detection rate

```java
@RepeatedTest(value = 20, name = "{displayName} [{currentRepetition}/{totalRepetitions}]")
@Timeout(5)
@DisplayName("CC15 – dos pedidos simultáneos nunca entran juntos")
void cc15_exclusionMutuaRepetida() { ... }
```

Surefire reports each repetition as a test, and the results table counts how many failed. That
count is the detection rate for the report.

## 5. Deadlock evidence

`ThreadMXBean.findDeadlockedThreads()` detects monitor deadlocks. A thread stuck in `wait()` with
nobody left to call `notifyAll` is **not** a deadlock in that sense. It is a lost wake-up, and it
shows up as a timeout with the thread in `WAITING`. Name it correctly in the report.

## 6. Fault-seeding catalogue

Seed one fault at a time on the **green** code. Restore with `git checkout -- <file>`.

| Id | Fault | Where | Expected to be caught by |
|----|-------|-------|--------------------------|
| FC1 | Remove `synchronized` from a request method | `Ambulancia.atenderDomicilio` | repeated mutual-exclusion case (`IllegalMonitorStateException` on `wait`, or two entries) |
| FC2 | `while` → `if` around `wait()` | any request method | waiter proceeds while still occupied (repeated case) |
| FC3 | Delete `notifyAll()` | `regresarSinPaciente` | waiter never wakes (timeout) |
| FC4 | Do not reset `ocupado` | `regresarSinPaciente` / `volviendoDelTaller` | occupancy invariant; waiter timeout |
| FC5 | Wrong next state | any state's transition | state-machine cell case |
| FC6 | Invert the loop guard | `RetornoAutomatico.run` | "thread stays alive" case |
| FC7 | Notify before changing state | `Ambulancia` request method | ordering case (if the oracle defines one) |
| FC8 | `notifyAll` → `notify` with several waiters | `setSimulacionActiva(false)` | termination case with N > 1 waiters |

A fault that survives means a missing case. Add it, rerun, and report both facts.
