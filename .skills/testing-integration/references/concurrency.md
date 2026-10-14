# Pruebas de concurrencia (JUnit 4, Java 8)

Secciones: §1 qué verificar · §2 reglas · §3 utilidades · §4 patrones · §5 intervalos fijos
largos · §6 diagnosticar un test colgado.

## §1. Qué verificar

| Propiedad | Pregunta | Evidencia en el test |
|-----------|----------|----------------------|
| Exclusión mutua | ¿Dos hilos entran a la vez a la sección crítica? | Stub con contador de concurrentes: el máximo es 1 |
| Espera condicionada | ¿Un hilo que no puede avanzar queda en `wait()`? | `getState() == WAITING` |
| Despertar | ¿Se despierta cuando se libera el recurso? | Después de liberar, el hilo termina (`join` + `!isAlive()`) |
| Ausencia de deadlock | ¿Todos terminan? | Timeout + `ThreadMXBean.findDeadlockedThreads() == null` |
| Terminación ordenada | ¿Con la bandera en falso o con `interrupt()`, salen? | `join(ms)` y `!isAlive()` |
| Invariantes después de la carrera | ¿El estado final es válido? | Contadores, estado actual y colecciones verificados en el hilo del test |

## §2. Reglas

1. **Todo test con hilos lleva timeout**: `@Test(timeout = ms)` o la regla `Timeout`.
   Un timeout es un **rojo válido** (deadlock o `wait` sin `notify`).
2. **No sincronizar con `Thread.sleep`**: se usa `esperarHasta(...)` (§3) con un límite.
3. **Las aserciones van en el hilo del test.** Los hilos auxiliares registran lo que pasó
   (atómicos, colas) y sus excepciones se capturan (§3).
4. **Repetir**: los errores de concurrencia son no determinísticos. Un caso que pasa una
   vez no prueba nada; se repite N veces dentro del test.
5. **Limpiar**: en `@After`, desactivar las banderas, interrumpir y hacer `join` de los
   hilos que queden vivos, para que no contaminen el test siguiente.

## §3. Utilidades

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

static void esperarHasta(BooleanSupplier condicion, long timeoutMs, String mensaje) throws InterruptedException
{
    long limite = System.currentTimeMillis() + timeoutMs;
    while (!condicion.getAsBoolean())
    {
        if (System.currentTimeMillis() > limite)
        {
            fail(mensaje);
        }
        Thread.sleep(10);
    }
}

static Thread iniciar(Runnable tarea, String nombre, AtomicReference<Throwable> error)
{
    Thread hilo = new Thread(tarea, nombre);
    hilo.setUncaughtExceptionHandler((t, e) -> error.compareAndSet(null, e));
    hilo.start();
    return hilo;
}

static void assertTermina(Thread hilo, long timeoutMs) throws InterruptedException
{
    hilo.join(timeoutMs);
    assertFalse("El hilo " + hilo.getName() + " no terminó (¿deadlock o wait sin notify?)", hilo.isAlive());
}

static void assertSinDeadlock()
{
    ThreadMXBean mx = ManagementFactory.getThreadMXBean();
    assertNull("Hay hilos en deadlock", mx.findDeadlockedThreads());
}
```

Regla de timeout global, que en una falla muestra dónde quedó trabado el hilo:

```java
@Rule
public Timeout limite = Timeout.builder()
        .withTimeout(10, TimeUnit.SECONDS)
        .withLookingForStuckThread(true)
        .build();
```

## §4. Patrones

### a) Espera y despertar (`wait`/`notify`)

```java
@Test(timeout = 10000)
public void ci5_solicitudEsperaMientrasOcupadoYSeDespiertaAlLiberar() throws Exception
{
    AtomicReference<Throwable> error = new AtomicReference<>();
    recurso.setSimulacionActiva(true);
    recurso.setOcupado(true);                                   // provocar el estado "ocupado"

    Thread solicitante = iniciar(() -> recurso.atenderDomicilio(asociado), "solicitante", error);
    esperarHasta(() -> solicitante.getState() == Thread.State.WAITING, 2000,
                 "con el recurso ocupado, el solicitante debería quedar en wait()");

    recurso.liberarSegunLaApi();                                // la operación que, según el diseño, libera y notifica
    assertTermina(solicitante, 3000);
    assertNull("excepción en el hilo solicitante", error.get());
}
```

`liberarSegunLaApi()` es la operación real del diseño que libera el recurso (por ejemplo,
volver a la clínica o volver del taller). Hay que elegirla leyendo el diagrama de estados y
el código: si ninguna operación hace `notify`/`notifyAll`, el test queda **colgado hasta el
timeout**, y ese es el hallazgo.

### b) Contención (largada simultánea)

```java
int hilos = 8;
CountDownLatch largada = new CountDownLatch(1);
CountDownLatch fin = new CountDownLatch(hilos);
AtomicReference<Throwable> error = new AtomicReference<>();
for (int i = 0; i < hilos; i++)
{
    iniciar(() -> {
        try { largada.await(); recurso.operar(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { fin.countDown(); }
    }, "h" + i, error);
}
largada.countDown();
assertTrue("No terminaron todos los hilos", fin.await(5, TimeUnit.SECONDS));
assertNull(error.get());
// invariantes: stub.maximoConcurrente == 1, estado final válido, contadores coherentes
```

Repetir el bloque (`for (int r = 0; r < 50; r++)`) con el escenario reseteado en cada vuelta.

### c) Terminación ordenada

```java
Thread t = iniciar(tareaPeriodica, "periodica", error);
recurso.setSimulacionActiva(false);          // o t.interrupt()
assertTermina(t, LIMITE);
```

- Con `interrupt()`, verificar que el código **sale limpio** (no relanza ni traga en silencio
  la interrupción y sigue en el bucle).
- Si el hilo está en `wait()`, desactivar la bandera **no alcanza** si nadie hace
  `notifyAll`: es un caso interesante.

### d) Mocks con hilos

`verify(mock, timeout(2000)).metodo()` espera a que otro hilo haga la llamada, sin `sleep`.
Para contar llamadas concurrentes conviene un stub manual con atómicos (`mockito4-mocks.md` §6).

## §5. Intervalos fijos largos y esperas aleatorias en el código

Si el código de producción duerme (un `Thread.sleep(INTERVALO)` con una constante privada
de varios segundos, o esperas aleatorias):

- **No cambiar la constante** ni el código para acelerar los tests sin el OK del usuario.
- Para probar la salida limpia: arrancar el hilo, **interrumpirlo** (el `catch
  (InterruptedException)` del `sleep` tiene que terminar el `run`) y verificar que termina
  enseguida.
- Para probar un ciclo completo: timeout = **intervalo + margen** (por ejemplo, 10 s → 15 s)
  y no más de un ciclo por test.
- Si el intervalo hace impracticable la prueba, se informa como **hallazgo de testabilidad**
  (sugerencia: un intervalo inyectable por constructor).

## §6. Diagnosticar un test colgado

1. El timeout de JUnit (con `withLookingForStuckThread(true)`) muestra la pila del hilo trabado.
2. `assertSinDeadlock()` distingue un **deadlock** (dos monitores cruzados) de un **wait
   sin notify** (un hilo en `WAITING` y nadie que lo despierte).
3. En el informe: qué hilo quedó en qué método y línea, en qué estado (`BLOCKED` o
   `WAITING`) y qué condición de entrada lo provocó.
