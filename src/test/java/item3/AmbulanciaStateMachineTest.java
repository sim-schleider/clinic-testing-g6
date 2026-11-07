package item3;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.AtendiendoEnDomicilioState;
import modelo.modeloDominio.ambulancia.DisponibleState;
import modelo.modeloDominio.ambulancia.EnTallerState;
import modelo.modeloDominio.ambulancia.RegresandoClinicaSinPacienteState;
import modelo.modeloDominio.ambulancia.RegresandoDelTallerState;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.ambulancia.TrasladandoPacienteClinicaState;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de la máquina de estados real acoplada a Ambulancia (sin mocks), en un
 * solo hilo: se recorren los flujos completos del diagrama de estados usando solo los métodos
 * públicos de Ambulancia. Después de cada paso se verifica el estado y el invariante de ocupación
 * (Javadoc de cada estado): la ambulancia está ocupada si y solo si está AtendiendoEnDomicilio,
 * TrasladandoPacienteClinica o EnTaller.
 * Los resultados esperados salen del diagrama de estados (UML), el Javadoc de cada estado y el
 * informe de diseño.
 */
@Tag("concurrency")
@DisplayName("Máquina de estados real de la Ambulancia (ítem 3)")
class AmbulanciaStateMachineTest {

    private Ambulancia ambulance;
    private Asociado associate;
    private Operario operator;

    /** Ambulancia nueva (Disponible, libre, simulación activa), un asociado y un operario. */
    @BeforeEach
    void setUpAmbulance() {
        ambulance = new Ambulancia();
        Domicilio address = new Domicilio("Calle", 1, "Ciudad");
        associate = new Asociado("Lucas", "Paniagua", "30111222", "2230001111", address);
        operator = new Operario("Valentino", "Hernan", "20111222", "2230002222", address, ambulance);
    }

    /** Verifica el estado actual y el invariante de ocupación que le corresponde. */
    private void assertState(Class<? extends State> expected) {
        assertInstanceOf(expected, ambulance.getEstadoActual());
        boolean shouldBeBusy = expected == AtendiendoEnDomicilioState.class
                || expected == TrasladandoPacienteClinicaState.class
                || expected == EnTallerState.class;
        assertEquals(shouldBeBusy, ambulance.isOcupado(),
                "ocupación incorrecta en " + expected.getSimpleName());
    }

    // CC1 – Flujo de atención a domicilio: Disponible → AtendiendoEnDomicilio →
    // RegresandoClinicaSinPaciente → Disponible. Esperado: cada estado con su ocupación.
    @Test
    @DisplayName("CC1 – Flujo de atención a domicilio hasta volver a Disponible")
    void cc1_atencionDomicilioFlow() {
        assertState(DisponibleState.class);
        ambulance.atenderDomicilio(associate);
        assertState(AtendiendoEnDomicilioState.class);
        ambulance.regresarSinPaciente();
        assertState(RegresandoClinicaSinPacienteState.class);
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));
        assertState(DisponibleState.class);
    }

    // CC2 – Flujo de traslado: Disponible → TrasladandoPacienteClinica → Disponible.
    // Esperado: al retornar a la clínica queda Disponible y libre (Javadoc y UML).
    @Test
    @DisplayName("CC2 – Flujo de traslado hasta volver a Disponible")
    void cc2_trasladoFlow() {
        ambulance.trasladarALaClinica(associate);
        assertState(TrasladandoPacienteClinicaState.class);
        ambulance.regresarSinPaciente();
        assertState(DisponibleState.class);
    }

    // CC3 – Flujo de mantenimiento: Disponible → EnTaller → RegresandoDelTaller → Disponible.
    // Esperado: cada estado con su ocupación.
    @Test
    @DisplayName("CC3 – Flujo de mantenimiento hasta volver a Disponible")
    void cc3_mantenimientoFlow() {
        ambulance.solicitarMantenimiento(operator);
        assertState(EnTallerState.class);
        ambulance.volviendoDelTaller();
        assertState(RegresandoDelTallerState.class);
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));
        assertState(DisponibleState.class);
    }

    // CC4 – Retorno automático mientras atiende un domicilio. Esperado: pasa a
    // RegresandoClinicaSinPaciente y, por el invariante, queda libre (supuesto: si no, los hilos
    // que esperan la ambulancia siguen bloqueados).
    @Test
    @DisplayName("CC4 – El retorno automático desde AtendiendoEnDomicilio libera la ambulancia")
    void cc4_retornoAutomaticoKeepsOccupancyInvariant() {
        ambulance.atenderDomicilio(associate);
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));
        assertState(RegresandoClinicaSinPacienteState.class);
    }

    // CC5 – Pedido de atención a domicilio mientras la ambulancia regresa del taller (estado que
    // rechaza el pedido). Esperado (supuesto): el estado no cambia y la ambulancia no queda ocupada.
    @Test
    @DisplayName("CC5 – Un pedido rechazado no deja la ambulancia ocupada")
    void cc5_rejectedRequestDoesNotOccupy() {
        ambulance.solicitarMantenimiento(operator);
        ambulance.volviendoDelTaller();

        ambulance.atenderDomicilio(associate);

        assertState(RegresandoDelTallerState.class);
    }
}
