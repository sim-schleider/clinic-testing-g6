package item3;

import modelo.modeloAplicacion.NotificacionSimulacion;
import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.AtendiendoEnDomicilioState;
import modelo.modeloDominio.ambulancia.DisponibleState;
import modelo.modeloDominio.ambulancia.EnTallerState;
import modelo.modeloDominio.ambulancia.RegresandoClinicaSinPacienteState;
import modelo.modeloDominio.ambulancia.RegresandoDelTallerState;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.ambulancia.TrasladandoPacienteClinicaState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Pruebas de cada estado concreto con la Ambulancia reemplazada por un mock: una por cada celda de
 * la tabla de transiciones (6 estados × 4 eventos). Oráculo: diagrama UML, Javadoc de cada estado e
 * informe de diseño ("notifica a observadores cuando […] no puede cumplir una solicitud").
 */
@Tag("mock")
class StatesMockTest {

    private Ambulancia ambulance;

    @BeforeEach
    void setUpMockAmbulance() {
        ambulance = mock(Ambulancia.class);
    }

    /** Transición: el estado le pide a la ambulancia pasar al estado esperado. */
    private void assertChangesTo(Class<? extends State> expected) {
        verify(ambulance).setState(isA(expected));
    }

    /** Rechazo: el estado no cambia y se avisa a los observadores. */
    private void assertRejects() {
        verify(ambulance, never()).setState(any());
        verify(ambulance).notifyObservers(any(NotificacionSimulacion.class));
    }

    // ---------------------------------------------------------------- Disponible

    // CM13 – Disponible + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    void testDisponibleTraslado() {
        new DisponibleState(ambulance).SolicitudDeTraslado();
        assertChangesTo(TrasladandoPacienteClinicaState.class);
    }

    // CM14 – Disponible + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    void testDisponibleAtencionDomicilio() {
        new DisponibleState(ambulance).SolicitudDeAtencionDomicilio();
        assertChangesTo(AtendiendoEnDomicilioState.class);
    }

    // CM15 – Disponible + SolicitudMantenimiento. Esperado: EnTaller.
    @Test
    void testDisponibleMantenimiento() {
        new DisponibleState(ambulance).SolicitudMantenimiento();
        assertChangesTo(EnTallerState.class);
    }

    // CM16 – Disponible + RetornoClinica. Esperado: sin cambio de estado ("ya se encuentra en la clínica").
    @Test
    void testDisponibleRetornoClinica() {
        new DisponibleState(ambulance).RetornoClinica();
        verifyNoInteractions(ambulance);
    }

    // ---------------------------------------------------------------- AtendiendoEnDomicilio

    // CM17 – AtendiendoEnDomicilio + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testAtendiendoTraslado() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM18 – AtendiendoEnDomicilio + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testAtendiendoAtencionDomicilio() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM19 – AtendiendoEnDomicilio + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testAtendiendoMantenimiento() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM20 – AtendiendoEnDomicilio + RetornoClinica. Esperado: RegresandoClinicaSinPaciente.
    @Test
    void testAtendiendoRetornoClinica() {
        new AtendiendoEnDomicilioState(ambulance).RetornoClinica();
        assertChangesTo(RegresandoClinicaSinPacienteState.class);
    }

    // ---------------------------------------------------------------- TrasladandoPacienteClinica

    // CM21 – TrasladandoPacienteClinica + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testTrasladandoTraslado() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM22 – TrasladandoPacienteClinica + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testTrasladandoAtencionDomicilio() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM23 – TrasladandoPacienteClinica + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testTrasladandoMantenimiento() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM24 – TrasladandoPacienteClinica + RetornoClinica. Esperado: Disponible (Javadoc:
    // "Cambia el estado de la ambulancia a DisponibleState").
    @Test
    void testTrasladandoRetornoClinica() {
        new TrasladandoPacienteClinicaState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }

    // ---------------------------------------------------------------- EnTaller

    // CM25 – EnTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testEnTallerTraslado() {
        new EnTallerState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM26 – EnTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testEnTallerAtencionDomicilio() {
        new EnTallerState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM27 – EnTaller + SolicitudMantenimiento. Esperado: RegresandoDelTaller.
    @Test
    void testEnTallerMantenimiento() {
        new EnTallerState(ambulance).SolicitudMantenimiento();
        assertChangesTo(RegresandoDelTallerState.class);
    }

    // CM28 – EnTaller + RetornoClinica. Esperado (supuesto: el retorno se trata como un pedido más):
    // rechaza ("no puede retornar a la clínica").
    @Test
    void testEnTallerRetornoClinica() {
        new EnTallerState(ambulance).RetornoClinica();
        assertRejects();
    }

    // ---------------------------------------------------------------- RegresandoDelTaller

    // CM29 – RegresandoDelTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerTraslado() {
        new RegresandoDelTallerState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM30 – RegresandoDelTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerAtencionDomicilio() {
        new RegresandoDelTallerState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM31 – RegresandoDelTaller + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerMantenimiento() {
        new RegresandoDelTallerState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM32 – RegresandoDelTaller + RetornoClinica. Esperado: Disponible.
    @Test
    void testRegresandoDelTallerRetornoClinica() {
        new RegresandoDelTallerState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }

    // ---------------------------------------------------------------- RegresandoClinicaSinPaciente

    // CM33 – RegresandoClinicaSinPaciente + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    void testRegresandoSinPacienteTraslado() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudDeTraslado();
        assertChangesTo(TrasladandoPacienteClinicaState.class);
    }

    // CM34 – RegresandoClinicaSinPaciente + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    void testRegresandoSinPacienteAtencionDomicilio() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudDeAtencionDomicilio();
        assertChangesTo(AtendiendoEnDomicilioState.class);
    }

    // CM35 – RegresandoClinicaSinPaciente + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testRegresandoSinPacienteMantenimiento() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM36 – RegresandoClinicaSinPaciente + RetornoClinica. Esperado: Disponible.
    @Test
    void testRegresandoSinPacienteRetornoClinica() {
        new RegresandoClinicaSinPacienteState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }
}
