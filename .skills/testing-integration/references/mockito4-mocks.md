# Mocks y stubs con Mockito 4 (JUnit 4, Java 8)

Secciones: §1 configuración · §2 programar respuestas · §3 verificar interacciones ·
§4 patrón State con mocks · §5 Observer · §6 stub manual · §7 qué no mockear.

## §1. Configuración

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@RunWith(MockitoJUnitRunner.StrictStubs.class)   // falla si se programa algo que no se usa
public class ContextoIntegracionTest
{
    @Mock
    private State estado;                          // o: State estado = mock(State.class);
}
```

- **Mockito 4.11.0** (`mockito-core`). La versión 5 requiere Java 11.
- Mockear una clase `final` o un método `static` requiere `mockito-inline:4.11.0`.
- La clase de la cátedra usa `mock(...)` sin runner, y también vale.

## §2. Programar respuestas (stub)

```java
when(servicioA.sumar(2, 3)).thenReturn(5);                      // ejemplo de la cátedra
when(repo.buscar(anyString())).thenReturn(null);
when(repo.buscar("caido")).thenThrow(new IllegalStateException("BD caída"));
doThrow(new RuntimeException("falla")).when(estado).SolicitudMantenimiento();   // métodos void
doAnswer(inv -> { contexto.setState(otroEstado); return null; }).when(estado).RetornoClinica();
```

- Los métodos `void` de un mock **no hacen nada** por defecto, y los que devuelven valores
  devuelven `null`, `0`, `false` o colecciones vacías.
- Si en una llamada se usa un *matcher* (`any()`, `eq()`), **todos** los argumentos tienen
  que ser matchers: `verify(x).m(eq("a"), anyInt())`.

## §3. Verificar interacciones (mock)

```java
verify(estado).SolicitudDeTraslado();                    // exactamente 1 vez
verify(estado, times(2)).RetornoClinica();
verify(estado, never()).SolicitudMantenimiento();
verify(estado, atLeastOnce()).SolicitudDeAtencionDomicilio();
verify(estado, timeout(2000)).RetornoClinica();          // espera hasta 2 s (otro hilo)
verifyNoMoreInteractions(estado);

InOrder orden = inOrder(estadoA, estadoB);
orden.verify(estadoA).SolicitudDeTraslado();
orden.verify(estadoB).RetornoClinica();

ArgumentCaptor<NotificacionSimulacion> captor = ArgumentCaptor.forClass(NotificacionSimulacion.class);
```

## §4. Patrón State con mocks ("ante la ausencia de las implementaciones concretas")

Objetivo: probar **el contexto** (la clase que tiene `estadoActual`) sin depender de los
estados reales. Se verifica que **delegue en el método correcto del estado** y que maneje
**sus propias responsabilidades** (banderas, sincronización, notificaciones).

```java
@Test(timeout = 5000)
public void ci2_trasladoDelegaEnElEstadoActual()
{
    Ambulancia ambulancia = new Ambulancia();     // el constructor crea su estado inicial real…
    State estado = mock(State.class);
    ambulancia.setState(estado);                  // …por eso el mock se inyecta DESPUÉS
    ambulancia.setSimulacionActiva(true);

    Asociado asociado = mock(Asociado.class);     // colaborador accesorio (solo aporta el nombre)
    when(asociado.getNombre()).thenReturn("Ana");

    ambulancia.trasladarALaClinica(asociado);

    verify(estado).SolicitudDeTraslado();
    assertSame("con un mock, nadie debería cambiar el estado", estado, ambulancia.getEstadoActual());
}
```

- **Con el mock, el estado no cambia solo**: los estados reales hacen
  `contexto.setState(new X(...))`, pero el mock no. Eso es justamente lo que aísla al contexto.
  Para simular una transición: `doAnswer(inv -> { ctx.setState(otroMock); return null; })`.
- **Leer antes el método del contexto**: si espera (`wait()`) mientras una bandera esté
  activa (por ejemplo, "ocupado"), prepararla para que no se bloquee, o el test se cuelga.
  Siempre con `timeout`.
- Un caso por operación del contexto **y** por cada combinación relevante de banderas.

## §5. Observer: verificar notificaciones

```java
Observer observador = mock(Observer.class);
ambulancia.addObserver(observador);

ambulancia.atenderDomicilio(asociado);

ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
verify(observador, atLeastOnce()).update(eq(ambulancia), captor.capture());
NotificacionSimulacion ultima = (NotificacionSimulacion) captor.getValue();
assertEquals("tipo de la última notificación", "OK", ultima.getTipo());
```

`Observable.notifyObservers` **solo notifica si antes se llamó a `setChanged()`**. Si la
notificación no llega, puede ser un defecto del código: se informa como hallazgo.

## §6. Stub manual (sin Mockito)

Útil con hilos (contadores seguros) o cuando la consigna pide explícitamente un "doble de prueba":

```java
class StateRegistrador implements State
{
    final AtomicInteger traslados = new AtomicInteger();
    final AtomicInteger concurrentes = new AtomicInteger();
    final AtomicInteger maximoConcurrente = new AtomicInteger();

    @Override public void SolicitudDeTraslado()
    {
        int ahora = concurrentes.incrementAndGet();
        maximoConcurrente.accumulateAndGet(ahora, Math::max);   // detecta dos hilos adentro a la vez
        traslados.incrementAndGet();
        concurrentes.decrementAndGet();
    }
    @Override public void RetornoClinica() { }
    @Override public void SolicitudMantenimiento() { }
    @Override public void SolicitudDeAtencionDomicilio() { }
}
```

Al final se verifica `assertEquals(1, stub.maximoConcurrente.get())`: la exclusión mutua se cumplió.

## §7. Qué no mockear

- **La clase bajo prueba** ni **la interacción que se está probando**.
- Los **objetos de valor** (DTO, `String`, colecciones): se usan instancias reales.
- La **BD** cuando la consigna pide comunicación real con el motor.
- Los tipos que no son del proyecto, cuando se pueden usar reales (`ArrayList`, `File` en un `TemporaryFolder`).
