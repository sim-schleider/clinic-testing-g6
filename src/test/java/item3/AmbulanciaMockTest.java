package item3;

import modelo.modeloAplicacion.NotificacionSimulacion;
import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Ambulancia con State mockeado (ítem 3)")
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
    @DisplayName("CM1 – atenderDomicilio delega en el estado, ocupa y notifica")
    void cm1_atenderDomicilioDelegatesToState() {
        ambulance.atenderDomicilio(associate);

        verify(state).SolicitudDeAtencionDomicilio();
        verifyNoMoreInteractions(state);
        assertTrue(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM2 – trasladarALaClinica con la ambulancia libre. Esperado: delega una vez
    // SolicitudDeTraslado y nada más, queda ocupada y notifica.
    @Test
    @DisplayName("CM2 – trasladarALaClinica delega en el estado, ocupa y notifica")
    void cm2_trasladarDelegatesToState() {
        ambulance.trasladarALaClinica(associate);

        verify(state).SolicitudDeTraslado();
        verifyNoMoreInteractions(state);
        assertTrue(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM3 – solicitarMantenimiento con la ambulancia libre. Esperado: delega una vez
    // SolicitudMantenimiento y nada más, queda ocupada y notifica.
    @Test
    @DisplayName("CM3 – solicitarMantenimiento delega en el estado, ocupa y notifica")
    void cm3_solicitarMantenimientoDelegatesToState() {
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
    @DisplayName("CM4 – regresarSinPaciente delega en el estado, libera y notifica")
    void cm4_regresarSinPacienteDelegatesAndReleases() {
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
    @DisplayName("CM5 – volviendoDelTaller delega en el estado, libera y notifica")
    void cm5_volviendoDelTallerDelegatesAndReleases() {
        ambulance.setOcupado(true);

        ambulance.volviendoDelTaller();

        verify(state).SolicitudMantenimiento();
        verifyNoMoreInteractions(state);
        assertFalse(ambulance.isOcupado());
        assertObserverNotified();
    }

    // CM6 – retornoAutomatico. Esperado: delega una vez RetornoClinica.
    @Test
    @DisplayName("CM6 – retornoAutomatico delega RetornoClinica en el estado")
    void cm6_retornoAutomaticoDelegatesToState() {
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));

        verify(state).RetornoClinica();
        verifyNoMoreInteractions(state);
    }

    // ---------------------------------------------------------------- Simulación terminada

    // CM7 – atenderDomicilio con la ambulancia libre y la simulación terminada.
    // Esperado: vuelve sin delegar en el estado y sin ocuparse.
    @Test
    @DisplayName("CM7 – Con la simulación terminada, atenderDomicilio no delega")
    void cm7_atenderDomicilioAfterSimulacionDoesNothing() {
        ambulance.setSimulacionActiva(false);

        ambulance.atenderDomicilio(associate);

        verifyNoInteractions(state);
        assertFalse(ambulance.isOcupado());
    }

    // CM8 – trasladarALaClinica con la ambulancia ocupada y la simulación terminada.
    // Esperado: no se queda esperando (vuelve antes de 2 s) y no delega en el estado.
    @Test
    @DisplayName("CM8 – Ocupada y con la simulación terminada, trasladarALaClinica no espera")
    void cm8_trasladarWhenBusyAfterSimulacionDoesNotWait() {
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
    @DisplayName("CM9 – La ambulancia delega en el estado que dejó la transición anterior")
    void cm9_delegatesToStateSetByPreviousTransition() {
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
