package modelo.modeloDominio.ambulancia;

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
 * Oráculo: .skills/concurrency-testing/references/ambulance-oracle.md §1 a §3.
 */
@Tag("concurrency")
class AmbulanciaMaquinaDeEstadosTest {

    private Ambulancia ambulancia;
    private Asociado asociado;
    private Operario operario;

    /** Ambulancia nueva (Disponible, libre, simulación activa), un asociado y un operario. */
    @BeforeEach
    void setUpAmbulancia() {
        ambulancia = new Ambulancia();
        Domicilio domicilio = new Domicilio("Calle", 1, "Ciudad");
        asociado = new Asociado("Ana", "Diaz", "30111222", "2230001111", domicilio);
        operario = new Operario("Juan", "Perez", "20111222", "2230002222", domicilio, ambulancia);
    }

    /** Verifica el estado actual y el invariante de ocupación que le corresponde. */
    private void assertEstado(Class<? extends State> esperado) {
        assertInstanceOf(esperado, ambulancia.getEstadoActual());
        boolean debeEstarOcupada = esperado == AtendiendoEnDomicilioState.class
                || esperado == TrasladandoPacienteClinicaState.class
                || esperado == EnTallerState.class;
        assertEquals(debeEstarOcupada, ambulancia.isOcupado(),
                "ocupación incorrecta en " + esperado.getSimpleName());
    }

    // CC1 – Flujo de atención a domicilio: Disponible → AtendiendoEnDomicilio →
    // RegresandoClinicaSinPaciente → Disponible. Esperado: cada estado con su ocupación.
    @Test
    void testFlujoAtencionDomicilio() {
        assertEstado(DisponibleState.class);
        ambulancia.atenderDomicilio(asociado);
        assertEstado(AtendiendoEnDomicilioState.class);
        ambulancia.regresarSinPaciente();
        assertEstado(RegresandoClinicaSinPacienteState.class);
        ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
        assertEstado(DisponibleState.class);
    }

    // CC2 – Flujo de traslado: Disponible → TrasladandoPacienteClinica → Disponible.
    // Esperado: al retornar a la clínica queda Disponible y libre (Javadoc y UML).
    @Test
    void testFlujoTraslado() {
        ambulancia.trasladarALaClinica(asociado);
        assertEstado(TrasladandoPacienteClinicaState.class);
        ambulancia.regresarSinPaciente();
        assertEstado(DisponibleState.class);
    }

    // CC3 – Flujo de mantenimiento: Disponible → EnTaller → RegresandoDelTaller → Disponible.
    // Esperado: cada estado con su ocupación.
    @Test
    void testFlujoMantenimiento() {
        ambulancia.solicitarMantenimiento(operario);
        assertEstado(EnTallerState.class);
        ambulancia.volviendoDelTaller();
        assertEstado(RegresandoDelTallerState.class);
        ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
        assertEstado(DisponibleState.class);
    }

    // CC4 – Retorno automático mientras atiende un domicilio. Esperado: pasa a
    // RegresandoClinicaSinPaciente y, por el invariante, queda libre (supuesto: si no, los hilos
    // que esperan la ambulancia siguen bloqueados).
    @Test
    void testRetornoAutomaticoKeepsOccupancyInvariant() {
        ambulancia.atenderDomicilio(asociado);
        ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
        assertEstado(RegresandoClinicaSinPacienteState.class);
    }

    // CC5 – Pedido de atención a domicilio mientras la ambulancia regresa del taller (estado que
    // rechaza el pedido). Esperado (supuesto): el estado no cambia y la ambulancia no queda ocupada.
    @Test
    void testRejectedRequestDoesNotOccupy() {
        ambulancia.solicitarMantenimiento(operario);
        ambulancia.volviendoDelTaller();

        ambulancia.atenderDomicilio(asociado);

        assertEstado(RegresandoDelTallerState.class);
    }
}
