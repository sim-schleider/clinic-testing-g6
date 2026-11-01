package modelo.modeloDominio.ambulancia;

import modelo.modeloAplicacion.NotificacionSimulacion;
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
 * Pruebas de Ambulancia con la interfaz State reemplazada por un mock: no se usa ningún estado
 * concreto. Se verifica que la ambulancia delegue en el estado actual el método que corresponde,
 * una sola vez, y que maneje la ocupación, la simulación y las notificaciones.
 * Oráculo: .skills/concurrency-testing/references/ambulance-oracle.md §3 (Javadoc e informe de diseño).
 * Los casos con la ambulancia ocupada y la simulación activa bloquean el hilo: van en la suite de
 * concurrencia.
 */
@Tag("mock")
class AmbulanciaMockTest {

    private Ambulancia ambulancia;
    private State estado;
    private Observer observador;
    private Asociado asociado;
    private Operario operario;

    /** Ambulancia libre, con la simulación activa, un estado mock y un observador mock. */
    @BeforeEach
    void setUpAmbulanciaConEstadoMock() {
        ambulancia = new Ambulancia();
        estado = mock(State.class);
        ambulancia.setState(estado);
        observador = mock(Observer.class);
        ambulancia.addObserver(observador);
        Domicilio domicilio = new Domicilio("Calle", 1, "Ciudad");
        asociado = new Asociado("Ana", "Diaz", "30111222", "2230001111", domicilio);
        operario = new Operario("Juan", "Perez", "20111222", "2230002222", domicilio, ambulancia);
    }

    /** El observador recibió al menos una notificación de la ambulancia. */
    private void assertNotificado() {
        verify(observador, atLeastOnce()).update(same(ambulancia), any(NotificacionSimulacion.class));
    }

    // ---------------------------------------------------------------- Pedidos con la ambulancia libre

    // CM1 – atenderDomicilio con la ambulancia libre. Esperado: delega una vez
    // SolicitudDeAtencionDomicilio y nada más, queda ocupada y notifica.
    @Test
    void testAtenderDomicilioDelegatesToState() {
        ambulancia.atenderDomicilio(asociado);

        verify(estado).SolicitudDeAtencionDomicilio();
        verifyNoMoreInteractions(estado);
        assertTrue(ambulancia.isOcupado());
        assertNotificado();
    }

    // CM2 – trasladarALaClinica con la ambulancia libre. Esperado: delega una vez
    // SolicitudDeTraslado y nada más, queda ocupada y notifica.
    @Test
    void testTrasladarDelegatesToState() {
        ambulancia.trasladarALaClinica(asociado);

        verify(estado).SolicitudDeTraslado();
        verifyNoMoreInteractions(estado);
        assertTrue(ambulancia.isOcupado());
        assertNotificado();
    }

    // CM3 – solicitarMantenimiento con la ambulancia libre. Esperado: delega una vez
    // SolicitudMantenimiento y nada más, queda ocupada y notifica.
    @Test
    void testSolicitarMantenimientoDelegatesToState() {
        ambulancia.solicitarMantenimiento(operario);

        verify(estado).SolicitudMantenimiento();
        verifyNoMoreInteractions(estado);
        assertTrue(ambulancia.isOcupado());
        assertNotificado();
    }

    // ---------------------------------------------------------------- Liberaciones

    // CM4 – regresarSinPaciente con la ambulancia ocupada. Esperado: delega una vez
    // RetornoClinica, queda libre y notifica.
    @Test
    void testRegresarSinPacienteDelegatesAndReleases() {
        ambulancia.setOcupado(true);

        ambulancia.regresarSinPaciente();

        verify(estado).RetornoClinica();
        verifyNoMoreInteractions(estado);
        assertFalse(ambulancia.isOcupado());
        assertNotificado();
    }

    // CM5 – volviendoDelTaller con la ambulancia ocupada. Esperado: delega una vez
    // SolicitudMantenimiento (EnTaller → RegresandoDelTaller), queda libre y notifica.
    @Test
    void testVolviendoDelTallerDelegatesAndReleases() {
        ambulancia.setOcupado(true);

        ambulancia.volviendoDelTaller();

        verify(estado).SolicitudMantenimiento();
        verifyNoMoreInteractions(estado);
        assertFalse(ambulancia.isOcupado());
        assertNotificado();
    }

    // CM6 – retornoAutomatico. Esperado: delega una vez RetornoClinica.
    @Test
    void testRetornoAutomaticoDelegatesToState() {
        ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));

        verify(estado).RetornoClinica();
        verifyNoMoreInteractions(estado);
    }

    // ---------------------------------------------------------------- Argumentos nulos

    // CM7 – atenderDomicilio(null). Esperado: AssertionError, sin tocar el estado ni ocuparse.
    @Test
    void testAtenderDomicilioRejectsNull() {
        assertThrows(AssertionError.class, () -> ambulancia.atenderDomicilio(null));
        verifyNoInteractions(estado);
        assertFalse(ambulancia.isOcupado());
    }

    // CM8 – trasladarALaClinica(null). Esperado: AssertionError, sin tocar el estado ni ocuparse.
    @Test
    void testTrasladarRejectsNull() {
        assertThrows(AssertionError.class, () -> ambulancia.trasladarALaClinica(null));
        verifyNoInteractions(estado);
        assertFalse(ambulancia.isOcupado());
    }

    // CM9 – solicitarMantenimiento(null). Esperado: AssertionError, sin tocar el estado ni ocuparse.
    @Test
    void testSolicitarMantenimientoRejectsNull() {
        assertThrows(AssertionError.class, () -> ambulancia.solicitarMantenimiento(null));
        verifyNoInteractions(estado);
        assertFalse(ambulancia.isOcupado());
    }

    // CM10 – setState(null). Esperado: AssertionError y el estado actual sin cambios.
    @Test
    void testSetStateRejectsNull() {
        assertThrows(AssertionError.class, () -> ambulancia.setState(null));
        assertSame(estado, ambulancia.getEstadoActual());
    }

    // ---------------------------------------------------------------- Simulación terminada

    // CM11 – atenderDomicilio con la ambulancia libre y la simulación terminada.
    // Esperado: vuelve sin delegar en el estado y sin ocuparse.
    @Test
    void testAtenderDomicilioAfterSimulacionDoesNothing() {
        ambulancia.setSimulacionActiva(false);

        ambulancia.atenderDomicilio(asociado);

        verifyNoInteractions(estado);
        assertFalse(ambulancia.isOcupado());
    }

    // CM12 – trasladarALaClinica con la ambulancia ocupada y la simulación terminada.
    // Esperado: no se queda esperando (vuelve antes de 2 s) y no delega en el estado.
    @Test
    void testTrasladarWhenBusyAfterSimulacionDoesNotWait() {
        ambulancia.setOcupado(true);
        ambulancia.setSimulacionActiva(false);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> ambulancia.trasladarALaClinica(asociado));

        verifyNoInteractions(estado);
    }
}
