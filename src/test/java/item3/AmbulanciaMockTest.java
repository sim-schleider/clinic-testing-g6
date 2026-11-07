package item3;

import modelo.modeloAplicacion.NotificacionSimulacion;
import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Observer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de Ambulancia con la interfaz State reemplazada por un mock, sin ningún estado concreto:
 * la ambulancia debe delegar en el estado actual el método que corresponde, una sola vez.
 * Los resultados esperados salen del diagrama UML, el Javadoc de Ambulancia y el informe de diseño.
 */
@Tag("mock")
class AmbulanciaMockTest {

    private Ambulancia ambulance;
    private State state;
    private Observer observer;
    private Asociado associate;
    private Operario operator;

    /** Ambulancia libre, con la simulación activa, un estado mock y un observador mock. */
    @BeforeEach
    void setUpAmbulanceWithMockState() {
        ambulance = new Ambulancia();
        state = mock(State.class);
        ambulance.setState(state);
        observer = mock(Observer.class);
        ambulance.addObserver(observer);
        Domicilio address = new Domicilio("Calle", 1, "Ciudad");
        associate = new Asociado("Lucas", "Paniagua", "30111222", "2230001111", address);
        operator = new Operario("Valentino", "Hernan", "20111222", "2230002222", address, ambulance);
    }

    /** El observador recibió una notificación de la ambulancia. */
    private void assertObserverNotified() {
        verify(observer).update(same(ambulance), any(NotificacionSimulacion.class));
    }

    // ---------------------------------------------------------------- Pedidos con la ambulancia libre

    // CM1 – atenderDomicilio con la ambulancia libre. Esperado: delega una vez
    // SolicitudDeAtencionDomicilio y nada más, queda ocupada y notifica.
    @Test
    void testAtenderDomicilioDelegatesToState() {
        ambulance.atenderDomicilio(associate);

        verify(state).SolicitudDeAtencionDomicilio();
        verifyNoMoreInteractions(state);
        assertTrue(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM2 – trasladarALaClinica con la ambulancia libre. Esperado: delega una vez
    // SolicitudDeTraslado y nada más, queda ocupada y notifica.
    @Test
    void testTrasladarDelegatesToState() {
        ambulance.trasladarALaClinica(associate);

        verify(state).SolicitudDeTraslado();
        verifyNoMoreInteractions(state);
        assertTrue(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM3 – solicitarMantenimiento con la ambulancia libre. Esperado: delega una vez
    // SolicitudMantenimiento y nada más, queda ocupada y notifica.
    @Test
    void testSolicitarMantenimientoDelegatesToState() {
        ambulance.solicitarMantenimiento(operator);

        verify(state).SolicitudMantenimiento();
        verifyNoMoreInteractions(state);
        assertTrue(ambulance.isOcupado());
        assertObserverNotified();
    }

    // ---------------------------------------------------------------- Liberaciones

    // CM4 – regresarSinPaciente con la ambulancia ocupada. Esperado: delega una vez
    // RetornoClinica, queda libre y notifica.
    @Test
    void testRegresarSinPacienteDelegatesAndReleases() {
        ambulance.setOcupado(true);

        ambulance.regresarSinPaciente();

        verify(state).RetornoClinica();
        verifyNoMoreInteractions(state);
        assertFalse(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM5 – volviendoDelTaller con la ambulancia ocupada. Esperado: delega una vez
    // SolicitudMantenimiento (EnTaller → RegresandoDelTaller), queda libre y notifica.
    @Test
    void testVolviendoDelTallerDelegatesAndReleases() {
        ambulance.setOcupado(true);

        ambulance.volviendoDelTaller();

        verify(state).SolicitudMantenimiento();
        verifyNoMoreInteractions(state);
        assertFalse(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM6 – retornoAutomatico. Esperado: delega una vez RetornoClinica.
    @Test
    void testRetornoAutomaticoDelegatesToState() {
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));

        verify(state).RetornoClinica();
        verifyNoMoreInteractions(state);
    }

    // ---------------------------------------------------------------- Simulación terminada

    // CM7 – atenderDomicilio con la ambulancia libre y la simulación terminada.
    // Esperado: vuelve sin delegar en el estado y sin ocuparse.
    @Test
    void testAtenderDomicilioAfterSimulacionDoesNothing() {
        ambulance.setSimulacionActiva(false);

        ambulance.atenderDomicilio(associate);

        verifyNoInteractions(state);
        assertFalse(ambulance.isOcupado());
    }

    // CM8 – trasladarALaClinica con la ambulancia ocupada y la simulación terminada.
    // Esperado: no se queda esperando (vuelve antes de 2 s) y no delega en el estado.
    @Test
    void testTrasladarWhenBusyAfterSimulacionDoesNotWait() {
        ambulance.setOcupado(true);
        ambulance.setSimulacionActiva(false);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> ambulance.trasladarALaClinica(associate));

        verifyNoInteractions(state);
    }

    // ---------------------------------------------------------------- Estado mock con comportamiento

    // CM9 – El estado mock simula una transición: al pedir un traslado cambia la ambulancia a otro
    // estado mock. Luego se regresa sin paciente. Esperado: el retorno se delega en el estado nuevo,
    // no en el anterior (la ambulancia siempre delega en su estado actual).
    @Test
    void testDelegatesToStateSetByPreviousTransition() {
        State otherState = mock(State.class);
        doAnswer(invocation -> {
            ambulance.setState(otherState);
            return null;
        }).when(state).SolicitudDeTraslado();

        ambulance.trasladarALaClinica(associate);
        ambulance.regresarSinPaciente();

        verify(otherState).RetornoClinica();
        verify(state, never()).RetornoClinica();
    }
}
