# Mockito 5 patterns (JUnit 5)

Mockito 5.11 with `mockito-junit-jupiter`. Mockito 5 uses the inline mock maker by default, so
it can mock concrete classes such as `Ambulancia` and `Asociado`. Imports used below:

```java
import modelo.modeloAplicacion.NotificacionSimulacion;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Observer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

## Group A: Ambulancia with a mocked State

```java
package modelo.modeloDominio.ambulancia;

@Tag("mock")
@ExtendWith(MockitoExtension.class)
@DisplayName("Ambulancia con State simulado")
class AmbulanciaMockTest {

    @Mock State estado;
    @Mock Asociado asociado;
    @Mock Observer observador;
    Ambulancia ambulancia;

    @BeforeEach
    void escenario() {
        ambulancia = new Ambulancia();      // the constructor installs a real DisponibleState...
        ambulancia.setState(estado);        // ...which is replaced by the mock: no concrete state is involved
        ambulancia.addObserver(observador);
    }

    @Test
    @DisplayName("CM1 – atenderDomicilio con la ambulancia libre delega una vez en el estado y la ocupa")
    void cm1_atenderDomicilioDelega() {
        lenient().when(asociado.getNombre()).thenReturn("Ana");

        ambulancia.atenderDomicilio(asociado);

        verify(estado).SolicitudDeAtencionDomicilio();
        verifyNoMoreInteractions(estado);
        assertTrue(ambulancia.isOcupado());
    }

    @Test
    @DisplayName("CM2 – atenderDomicilio notifica a los observadores")
    void cm2_atenderDomicilioNotifica() {
        lenient().when(asociado.getNombre()).thenReturn("Ana");

        ambulancia.atenderDomicilio(asociado);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(observador, atLeastOnce()).update(eq(ambulancia), captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(n -> n instanceof NotificacionSimulacion ns && !ns.getMensaje().isBlank()));
    }

    @Test
    @DisplayName("CM5 – con la simulación terminada no se delega en el estado")
    void cm5_simulacionInactivaNoDelega() {
        ambulancia.setSimulacionActiva(false);
        ambulancia.atenderDomicilio(asociado);
        verifyNoInteractions(estado);
    }

    @Test
    @DisplayName("CM8 – setState(null) se rechaza")
    void cm8_setStateNulo() {
        assertThrows(AssertionError.class, () -> ambulancia.setState(null));
    }
}
```

`lenient()` is there because whether the message uses the name is not part of the contract.
Strict stubs would otherwise fail the test with `UnnecessaryStubbingException` when it doesn't.

### A state that changes the state

This checks that `Ambulancia` delegates to the **current** state at each call:

```java
@Test
@DisplayName("CM9 – tras una transición, la siguiente solicitud va al nuevo estado")
void cm9_delegaEnElEstadoActual() {
    State siguiente = mock(State.class);
    doAnswer(inv -> { ambulancia.setState(siguiente); return null; })
            .when(estado).SolicitudDeAtencionDomicilio();

    ambulancia.atenderDomicilio(asociado);
    ambulancia.regresarSinPaciente();

    verify(siguiente).RetornoClinica();
    verify(estado, never()).RetornoClinica();
}
```

### Order of effects

```java
InOrder orden = inOrder(estado, observador);
orden.verify(estado).SolicitudDeTraslado();
orden.verify(observador).update(eq(ambulancia), any());
```

Use `InOrder` only where the oracle defines an order. Otherwise it over-specifies the test.

## Group B: concrete states with a mocked Ambulancia

```java
@Tag("mock")
@ExtendWith(MockitoExtension.class)
@DisplayName("Estados concretos con Ambulancia simulada")
class EstadosMockTest {

    @Mock Ambulancia ambulancia;

    @Test
    @Timeout(5)
    @DisplayName("CM20 – Disponible + SolicitudDeTraslado pasa a TrasladandoPacienteClinica")
    void cm20_disponibleTraslado() {
        new DisponibleState(ambulancia).SolicitudDeTraslado();
        verify(ambulancia).setState(isA(TrasladandoPacienteClinicaState.class));
    }

    @Test
    @Timeout(5)
    @DisplayName("CM21 – EnTaller rechaza SolicitudDeTraslado: no cambia de estado y notifica")
    void cm21_enTallerRechazaTraslado() {
        new EnTallerState(ambulancia).SolicitudDeTraslado();
        verify(ambulancia, never()).setState(any());
        verify(ambulancia).notifyObservers(isA(NotificacionSimulacion.class));
    }
}
```

`notifyObservers` is overloaded. `isA(NotificacionSimulacion.class)` selects
`notifyObservers(Object)`, and a bare `verify(ambulancia).notifyObservers()` would check the
no-argument version instead.

### All 24 cells as one parameterized test

```java
enum Evento { TRASLADO, DOMICILIO, MANTENIMIENTO, RETORNO }

static void disparar(State s, Evento e) {
    switch (e) {
        case TRASLADO -> s.SolicitudDeTraslado();
        case DOMICILIO -> s.SolicitudDeAtencionDomicilio();
        case MANTENIMIENTO -> s.SolicitudMantenimiento();
        case RETORNO -> s.RetornoClinica();
    }
}

static State crear(String estado, Ambulancia a) {
    return switch (estado) {
        case "Disponible" -> new DisponibleState(a);
        case "AtendiendoEnDomicilio" -> new AtendiendoEnDomicilioState(a);
        case "TrasladandoPacienteClinica" -> new TrasladandoPacienteClinicaState(a);
        case "EnTaller" -> new EnTallerState(a);
        case "RegresandoDelTaller" -> new RegresandoDelTallerState(a);
        case "RegresandoClinicaSinPaciente" -> new RegresandoClinicaSinPacienteState(a);
        default -> throw new IllegalArgumentException(estado);
    };
}

// esperado: the simple class name of the next state, or "-" when the state must not change.
@ParameterizedTest(name = "{0} – {1} + {2} → {3}")
@CsvSource({
    "CM20, Disponible, TRASLADO, TrasladandoPacienteClinicaState",
    "CM21, EnTaller, TRASLADO, -",
    // ... one row per cell of the oracle table
})
@Timeout(5)
void transicion(String id, String estado, Evento evento, String esperado) {
    disparar(crear(estado, ambulancia), evento);
    if (esperado.equals("-")) {
        verify(ambulancia, never()).setState(any());
    } else {
        ArgumentCaptor<State> nuevo = ArgumentCaptor.forClass(State.class);
        verify(ambulancia).setState(nuevo.capture());
        assertEquals(esperado, nuevo.getValue().getClass().getSimpleName());
    }
}
```

## Common Mockito mistakes

| Symptom | Cause |
|---------|-------|
| `UnnecessaryStubbingException` | A stub the test never uses. Remove it, or make it `lenient()` if its use is optional |
| `verify` passes but nothing was tested | The test asserted on its own stub instead of on the collaboration |
| `WantedButNotInvoked` on `notifyObservers()` | Wrong overload; use `notifyObservers(isA(...))` |
| Test hangs | The real code reached `wait()` because the ambulance was busy. That is a concurrency case, not a mock case |
