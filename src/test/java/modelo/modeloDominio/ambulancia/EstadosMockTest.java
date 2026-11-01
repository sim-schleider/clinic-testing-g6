package modelo.modeloDominio.ambulancia;

import modelo.modeloAplicacion.NotificacionSimulacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Pruebas de cada estado concreto con la Ambulancia reemplazada por un mock: una por cada celda de
 * la tabla de transiciones (6 estados × 4 eventos). Oráculo: ambulance-oracle.md §1 (UML, Javadoc
 * e informe de diseño: "notifica a observadores cuando […] no puede cumplir una solicitud").
 */
@Tag("mock")
class EstadosMockTest {

    private Ambulancia ambulancia;

    @BeforeEach
    void setUpAmbulanciaMock() {
        ambulancia = mock(Ambulancia.class);
    }

    /** Transición: el estado le pide a la ambulancia pasar al estado esperado. */
    private void assertCambiaA(Class<? extends State> esperado) {
        verify(ambulancia).setState(isA(esperado));
    }

    /** Rechazo: el estado no cambia y se avisa a los observadores. */
    private void assertRechaza() {
        verify(ambulancia, never()).setState(any());
        verify(ambulancia).notifyObservers(any(NotificacionSimulacion.class));
    }

    // ---------------------------------------------------------------- Disponible

    // CM13 – Disponible + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    void testDisponibleTraslado() {
        new DisponibleState(ambulancia).SolicitudDeTraslado();
        assertCambiaA(TrasladandoPacienteClinicaState.class);
    }

    // CM14 – Disponible + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    void testDisponibleAtencionDomicilio() {
        new DisponibleState(ambulancia).SolicitudDeAtencionDomicilio();
        assertCambiaA(AtendiendoEnDomicilioState.class);
    }

    // CM15 – Disponible + SolicitudMantenimiento. Esperado: EnTaller.
    @Test
    void testDisponibleMantenimiento() {
        new DisponibleState(ambulancia).SolicitudMantenimiento();
        assertCambiaA(EnTallerState.class);
    }

    // CM16 – Disponible + RetornoClinica. Esperado: sin cambio de estado ("ya se encuentra en la clínica").
    @Test
    void testDisponibleRetornoClinica() {
        new DisponibleState(ambulancia).RetornoClinica();
        verifyNoInteractions(ambulancia);
    }

    // ---------------------------------------------------------------- AtendiendoEnDomicilio

    // CM17 – AtendiendoEnDomicilio + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testAtendiendoTraslado() {
        new AtendiendoEnDomicilioState(ambulancia).SolicitudDeTraslado();
        assertRechaza();
    }

    // CM18 – AtendiendoEnDomicilio + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testAtendiendoAtencionDomicilio() {
        new AtendiendoEnDomicilioState(ambulancia).SolicitudDeAtencionDomicilio();
        assertRechaza();
    }

    // CM19 – AtendiendoEnDomicilio + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testAtendiendoMantenimiento() {
        new AtendiendoEnDomicilioState(ambulancia).SolicitudMantenimiento();
        assertRechaza();
    }

    // CM20 – AtendiendoEnDomicilio + RetornoClinica. Esperado: RegresandoClinicaSinPaciente.
    @Test
    void testAtendiendoRetornoClinica() {
        new AtendiendoEnDomicilioState(ambulancia).RetornoClinica();
        assertCambiaA(RegresandoClinicaSinPacienteState.class);
    }

    // ---------------------------------------------------------------- TrasladandoPacienteClinica

    // CM21 – TrasladandoPacienteClinica + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testTrasladandoTraslado() {
        new TrasladandoPacienteClinicaState(ambulancia).SolicitudDeTraslado();
        assertRechaza();
    }

    // CM22 – TrasladandoPacienteClinica + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testTrasladandoAtencionDomicilio() {
        new TrasladandoPacienteClinicaState(ambulancia).SolicitudDeAtencionDomicilio();
        assertRechaza();
    }

    // CM23 – TrasladandoPacienteClinica + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testTrasladandoMantenimiento() {
        new TrasladandoPacienteClinicaState(ambulancia).SolicitudMantenimiento();
        assertRechaza();
    }

    // CM24 – TrasladandoPacienteClinica + RetornoClinica. Esperado: Disponible (Javadoc:
    // "Cambia el estado de la ambulancia a DisponibleState").
    @Test
    void testTrasladandoRetornoClinica() {
        new TrasladandoPacienteClinicaState(ambulancia).RetornoClinica();
        assertCambiaA(DisponibleState.class);
    }

    // ---------------------------------------------------------------- EnTaller

    // CM25 – EnTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testEnTallerTraslado() {
        new EnTallerState(ambulancia).SolicitudDeTraslado();
        assertRechaza();
    }

    // CM26 – EnTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testEnTallerAtencionDomicilio() {
        new EnTallerState(ambulancia).SolicitudDeAtencionDomicilio();
        assertRechaza();
    }

    // CM27 – EnTaller + SolicitudMantenimiento. Esperado: RegresandoDelTaller.
    @Test
    void testEnTallerMantenimiento() {
        new EnTallerState(ambulancia).SolicitudMantenimiento();
        assertCambiaA(RegresandoDelTallerState.class);
    }

    // CM28 – EnTaller + RetornoClinica. Esperado (supuesto: el retorno se trata como un pedido más):
    // rechaza ("no puede retornar a la clínica").
    @Test
    void testEnTallerRetornoClinica() {
        new EnTallerState(ambulancia).RetornoClinica();
        assertRechaza();
    }

    // ---------------------------------------------------------------- RegresandoDelTaller

    // CM29 – RegresandoDelTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerTraslado() {
        new RegresandoDelTallerState(ambulancia).SolicitudDeTraslado();
        assertRechaza();
    }

    // CM30 – RegresandoDelTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerAtencionDomicilio() {
        new RegresandoDelTallerState(ambulancia).SolicitudDeAtencionDomicilio();
        assertRechaza();
    }

    // CM31 – RegresandoDelTaller + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testRegresandoDelTallerMantenimiento() {
        new RegresandoDelTallerState(ambulancia).SolicitudMantenimiento();
        assertRechaza();
    }

    // CM32 – RegresandoDelTaller + RetornoClinica. Esperado: Disponible.
    @Test
    void testRegresandoDelTallerRetornoClinica() {
        new RegresandoDelTallerState(ambulancia).RetornoClinica();
        assertCambiaA(DisponibleState.class);
    }

    // ---------------------------------------------------------------- RegresandoClinicaSinPaciente

    // CM33 – RegresandoClinicaSinPaciente + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    void testRegresandoSinPacienteTraslado() {
        new RegresandoClinicaSinPacienteState(ambulancia).SolicitudDeTraslado();
        assertCambiaA(TrasladandoPacienteClinicaState.class);
    }

    // CM34 – RegresandoClinicaSinPaciente + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    void testRegresandoSinPacienteAtencionDomicilio() {
        new RegresandoClinicaSinPacienteState(ambulancia).SolicitudDeAtencionDomicilio();
        assertCambiaA(AtendiendoEnDomicilioState.class);
    }

    // CM35 – RegresandoClinicaSinPaciente + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    void testRegresandoSinPacienteMantenimiento() {
        new RegresandoClinicaSinPacienteState(ambulancia).SolicitudMantenimiento();
        assertRechaza();
    }

    // CM36 – RegresandoClinicaSinPaciente + RetornoClinica. Esperado: Disponible.
    @Test
    void testRegresandoSinPacienteRetornoClinica() {
        new RegresandoClinicaSinPacienteState(ambulancia).RetornoClinica();
        assertCambiaA(DisponibleState.class);
    }
}
