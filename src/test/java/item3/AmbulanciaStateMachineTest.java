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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de la máquina de estados real acoplada a Ambulancia (sin mocks), en un
 * solo hilo: se recorren los flujos completos del diagrama de estados usando solo los métodos
 * públicos de Ambulancia. Después de cada paso se verifica el estado y el invariante de ocupación
 * (Javadoc de cada estado): la ambulancia está ocupada si y solo si está AtendiendoEnDomicilio,
 * TrasladandoPacienteClinica o EnTaller.
 * Oráculo: diagrama de estados (UML), Javadoc de cada estado e informe de diseño.
 */
@Tag("concurrency")
class AmbulanciaStateMachineTest {

    private Ambulancia ambulance;
    private Asociado associate;
    private Operario operator;

    /** Ambulancia nueva (Disponible, libre, simulación activa), un asociado y un operario. */
    @BeforeEach
    void setUpAmbulance() {
        ambulance = new Ambulancia();
        Domicilio address = new Domicilio("Calle", 1, "Ciudad");
        associate = new Asociado("Ana", "Diaz", "30111222", "2230001111", address);
        operator = new Operario("Juan", "Perez", "20111222", "2230002222", address, ambulance);
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
    void testAtencionDomicilioFlow() {
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
    void testTrasladoFlow() {
        ambulance.trasladarALaClinica(associate);
        assertState(TrasladandoPacienteClinicaState.class);
        ambulance.regresarSinPaciente();
        assertState(DisponibleState.class);
    }

    // CC3 – Flujo de mantenimiento: Disponible → EnTaller → RegresandoDelTaller → Disponible.
    // Esperado: cada estado con su ocupación.
    @Test
    void testMantenimientoFlow() {
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
    void testRetornoAutomaticoKeepsOccupancyInvariant() {
        ambulance.atenderDomicilio(associate);
        ambulance.retornoAutomatico(new RetornoAutomatico(ambulance));
        assertState(RegresandoClinicaSinPacienteState.class);
    }

    // CC5 – Pedido de atención a domicilio mientras la ambulancia regresa del taller (estado que
    // rechaza el pedido). Esperado (supuesto): el estado no cambia y la ambulancia no queda ocupada.
    @Test
    void testRejectedRequestDoesNotOccupy() {
        ambulance.solicitarMantenimiento(operator);
        ambulance.volviendoDelTaller();

        ambulance.atenderDomicilio(associate);

        assertState(RegresandoDelTallerState.class);
    }
}
