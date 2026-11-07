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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Pruebas de cada estado concreto con la Ambulancia reemplazada por un mock: una por cada celda de
 * la tabla de transiciones (6 estados × 4 eventos). Los resultados esperados salen del diagrama
 * UML, el Javadoc de cada estado y el informe de diseño ("notifica a observadores cuando […] no
 * puede cumplir una solicitud").
 */
@Tag("mock")
@DisplayName("Estados concretos con Ambulancia mockeada (ítem 3)")
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

    // CM10 – Disponible + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    @DisplayName("CM10 – Disponible + SolicitudDeTraslado → TrasladandoPacienteClinica")
    void cm10_disponibleTraslado() {
        new DisponibleState(ambulance).SolicitudDeTraslado();
        assertChangesTo(TrasladandoPacienteClinicaState.class);
    }

    // CM11 – Disponible + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    @DisplayName("CM11 – Disponible + SolicitudDeAtencionDomicilio → AtendiendoEnDomicilio")
    void cm11_disponibleAtencionDomicilio() {
        new DisponibleState(ambulance).SolicitudDeAtencionDomicilio();
        assertChangesTo(AtendiendoEnDomicilioState.class);
    }

    // CM12 – Disponible + SolicitudMantenimiento. Esperado: EnTaller.
    @Test
    @DisplayName("CM12 – Disponible + SolicitudMantenimiento → EnTaller")
    void cm12_disponibleMantenimiento() {
        new DisponibleState(ambulance).SolicitudMantenimiento();
        assertChangesTo(EnTallerState.class);
    }

    // CM13 – Disponible + RetornoClinica. Esperado: sin cambio de estado ("ya se encuentra en la clínica").
    @Test
    @DisplayName("CM13 – Disponible + RetornoClinica → sin cambio")
    void cm13_disponibleRetornoClinica() {
        new DisponibleState(ambulance).RetornoClinica();
        verifyNoInteractions(ambulance);
    }

    // ---------------------------------------------------------------- AtendiendoEnDomicilio

    // CM14 – AtendiendoEnDomicilio + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    @DisplayName("CM14 – AtendiendoEnDomicilio + SolicitudDeTraslado → rechaza")
    void cm14_atendiendoTraslado() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM15 – AtendiendoEnDomicilio + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    @DisplayName("CM15 – AtendiendoEnDomicilio + SolicitudDeAtencionDomicilio → rechaza")
    void cm15_atendiendoAtencionDomicilio() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM16 – AtendiendoEnDomicilio + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    @DisplayName("CM16 – AtendiendoEnDomicilio + SolicitudMantenimiento → rechaza")
    void cm16_atendiendoMantenimiento() {
        new AtendiendoEnDomicilioState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM17 – AtendiendoEnDomicilio + RetornoClinica. Esperado: RegresandoClinicaSinPaciente.
    @Test
    @DisplayName("CM17 – AtendiendoEnDomicilio + RetornoClinica → RegresandoClinicaSinPaciente")
    void cm17_atendiendoRetornoClinica() {
        new AtendiendoEnDomicilioState(ambulance).RetornoClinica();
        assertChangesTo(RegresandoClinicaSinPacienteState.class);
    }

    // ---------------------------------------------------------------- TrasladandoPacienteClinica

    // CM18 – TrasladandoPacienteClinica + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    @DisplayName("CM18 – TrasladandoPacienteClinica + SolicitudDeTraslado → rechaza")
    void cm18_trasladandoTraslado() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM19 – TrasladandoPacienteClinica + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    @DisplayName("CM19 – TrasladandoPacienteClinica + SolicitudDeAtencionDomicilio → rechaza")
    void cm19_trasladandoAtencionDomicilio() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM20 – TrasladandoPacienteClinica + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    @DisplayName("CM20 – TrasladandoPacienteClinica + SolicitudMantenimiento → rechaza")
    void cm20_trasladandoMantenimiento() {
        new TrasladandoPacienteClinicaState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM21 – TrasladandoPacienteClinica + RetornoClinica. Esperado: Disponible (Javadoc:
    // "Cambia el estado de la ambulancia a DisponibleState").
    @Test
    @DisplayName("CM21 – TrasladandoPacienteClinica + RetornoClinica → Disponible")
    void cm21_trasladandoRetornoClinica() {
        new TrasladandoPacienteClinicaState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }

    // ---------------------------------------------------------------- EnTaller

    // CM22 – EnTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    @DisplayName("CM22 – EnTaller + SolicitudDeTraslado → rechaza")
    void cm22_enTallerTraslado() {
        new EnTallerState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM23 – EnTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    @DisplayName("CM23 – EnTaller + SolicitudDeAtencionDomicilio → rechaza")
    void cm23_enTallerAtencionDomicilio() {
        new EnTallerState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM24 – EnTaller + SolicitudMantenimiento. Esperado: RegresandoDelTaller.
    @Test
    @DisplayName("CM24 – EnTaller + SolicitudMantenimiento → RegresandoDelTaller")
    void cm24_enTallerMantenimiento() {
        new EnTallerState(ambulance).SolicitudMantenimiento();
        assertChangesTo(RegresandoDelTallerState.class);
    }

    // CM25 – EnTaller + RetornoClinica. Esperado (supuesto: el retorno se trata como un pedido más):
    // rechaza ("no puede retornar a la clínica").
    @Test
    @DisplayName("CM25 – EnTaller + RetornoClinica → rechaza")
    void cm25_enTallerRetornoClinica() {
        new EnTallerState(ambulance).RetornoClinica();
        assertRejects();
    }

    // ---------------------------------------------------------------- RegresandoDelTaller

    // CM26 – RegresandoDelTaller + SolicitudDeTraslado. Esperado: rechaza.
    @Test
    @DisplayName("CM26 – RegresandoDelTaller + SolicitudDeTraslado → rechaza")
    void cm26_regresandoDelTallerTraslado() {
        new RegresandoDelTallerState(ambulance).SolicitudDeTraslado();
        assertRejects();
    }

    // CM27 – RegresandoDelTaller + SolicitudDeAtencionDomicilio. Esperado: rechaza.
    @Test
    @DisplayName("CM27 – RegresandoDelTaller + SolicitudDeAtencionDomicilio → rechaza")
    void cm27_regresandoDelTallerAtencionDomicilio() {
        new RegresandoDelTallerState(ambulance).SolicitudDeAtencionDomicilio();
        assertRejects();
    }

    // CM28 – RegresandoDelTaller + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    @DisplayName("CM28 – RegresandoDelTaller + SolicitudMantenimiento → rechaza")
    void cm28_regresandoDelTallerMantenimiento() {
        new RegresandoDelTallerState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM29 – RegresandoDelTaller + RetornoClinica. Esperado: Disponible.
    @Test
    @DisplayName("CM29 – RegresandoDelTaller + RetornoClinica → Disponible")
    void cm29_regresandoDelTallerRetornoClinica() {
        new RegresandoDelTallerState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }

    // ---------------------------------------------------------------- RegresandoClinicaSinPaciente

    // CM30 – RegresandoClinicaSinPaciente + SolicitudDeTraslado. Esperado: TrasladandoPacienteClinica.
    @Test
    @DisplayName("CM30 – RegresandoClinicaSinPaciente + SolicitudDeTraslado → TrasladandoPacienteClinica")
    void cm30_regresandoSinPacienteTraslado() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudDeTraslado();
        assertChangesTo(TrasladandoPacienteClinicaState.class);
    }

    // CM31 – RegresandoClinicaSinPaciente + SolicitudDeAtencionDomicilio. Esperado: AtendiendoEnDomicilio.
    @Test
    @DisplayName("CM31 – RegresandoClinicaSinPaciente + SolicitudDeAtencionDomicilio → AtendiendoEnDomicilio")
    void cm31_regresandoSinPacienteAtencionDomicilio() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudDeAtencionDomicilio();
        assertChangesTo(AtendiendoEnDomicilioState.class);
    }

    // CM32 – RegresandoClinicaSinPaciente + SolicitudMantenimiento. Esperado: rechaza.
    @Test
    @DisplayName("CM32 – RegresandoClinicaSinPaciente + SolicitudMantenimiento → rechaza")
    void cm32_regresandoSinPacienteMantenimiento() {
        new RegresandoClinicaSinPacienteState(ambulance).SolicitudMantenimiento();
        assertRejects();
    }

    // CM33 – RegresandoClinicaSinPaciente + RetornoClinica. Esperado: Disponible.
    @Test
    @DisplayName("CM33 – RegresandoClinicaSinPaciente + RetornoClinica → Disponible")
    void cm33_regresandoSinPacienteRetornoClinica() {
        new RegresandoClinicaSinPacienteState(ambulance).RetornoClinica();
        assertChangesTo(DisponibleState.class);
    }
}
