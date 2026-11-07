package item3;

import item3.support.AmbulanciaWithoutNotifyAll;
import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.AtendiendoEnDomicilioState;
import modelo.modeloDominio.ambulancia.DisponibleState;
import modelo.modeloDominio.ambulancia.RegresandoClinicaSinPacienteState;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import modelo.modeloDominio.ambulancia.TrasladandoPacienteClinicaState;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de concurrencia de Ambulancia como recurso compartido (synchronized, wait/notifyAll,
 * RetornoAutomatico). Los resultados esperados salen del Javadoc de Ambulancia y de
 * RetornoAutomatico, y del informe de diseño.
 * Nunca se espera un tiempo fijo: se espera una condición con un plazo máximo. Los hilos del test
 * son daemon, así un hilo bloqueado por un defecto no cuelga la ejecución.
 */
@Tag("concurrency")
@DisplayName("Concurrencia de la Ambulancia (ítem 3)")
class AmbulanciaConcurrencyTest {

    private Ambulancia ambulance;
    private Domicilio address;
    private Asociado lucas;
    private Asociado simon;
    private Operario operator;

    @BeforeEach
    void setUpAmbulance() {
        ambulance = new Ambulancia();
        address = new Domicilio("Calle", 1, "Ciudad");
        lucas = new Asociado("Lucas", "Paniagua", "30111222", "2230001111", address);
        simon = new Asociado("Simon", "Schleider", "30333444", "2230003333", address);
        operator = new Operario("Valentino", "Hernan", "20111222", "2230002222", address, ambulance);
    }

    private static Thread startThread(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Espera a que se cumpla la condición; si no se cumple dentro del plazo, el test falla. */
    private static void waitUntil(BooleanSupplier condition, long timeoutMs, String description)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("no ocurrió en " + timeoutMs + " ms: " + description);
            }
            Thread.sleep(10);
        }
    }

    private static boolean isWaiting(Thread thread) {
        return thread.getState() == Thread.State.WAITING;
    }

    /**
     * Escenario de CC6 y CC13: el hilo del test ocupa la ambulancia, otro hilo pide un traslado y
     * queda esperando sin cambiar el estado (exclusión mutua), y el hilo del test la libera con
     * regresarSinPaciente(). Devuelve true si el pedido en espera se despertó y terminó dentro de 5 s.
     */
    private boolean waitingRequestWakesUp(Ambulancia ambulance) throws InterruptedException {
        ambulance.atenderDomicilio(lucas);
        Thread request = startThread(() -> ambulance.trasladarALaClinica(simon));
        waitUntil(() -> isWaiting(request), 2000, "el pedido de traslado queda esperando");
        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulance.getEstadoActual(),
                "el pedido en espera cambió el estado de la ambulancia ocupada");

        ambulance.regresarSinPaciente();

        request.join(5000);
        return !request.isAlive();
    }

    // CC6 – Exclusión mutua: con la ambulancia ocupada, otro hilo pide un traslado. Esperado: ese hilo
    // queda esperando (WAITING) sin cambiar el estado, se despierta cuando la ambulancia se libera con
    // regresarSinPaciente(), y la ambulancia queda TrasladandoPacienteClinica.
    @Test
    @DisplayName("CC6 – Un pedido en espera se despierta con regresarSinPaciente")
    void cc6_waitingRequestWakesUpOnRegresarSinPaciente() throws InterruptedException {
        assertTrue(waitingRequestWakesUp(ambulance), "el pedido en espera no se despertó");
        assertInstanceOf(TrasladandoPacienteClinicaState.class, ambulance.getEstadoActual());
    }

    // CC7 – El operario ocupa la ambulancia (taller) y un asociado pide atención a domicilio.
    // Esperado: el asociado espera y se despierta cuando la ambulancia vuelve del taller.
    @Test
    @DisplayName("CC7 – Un pedido en espera se despierta con volviendoDelTaller")
    void cc7_waitingRequestWakesUpOnVolviendoDelTaller() throws InterruptedException {
        ambulance.solicitarMantenimiento(operator);
        Thread request = startThread(() -> ambulance.atenderDomicilio(lucas));
        waitUntil(() -> isWaiting(request), 2000, "el pedido de atención queda esperando");

        ambulance.volviendoDelTaller();

        request.join(5000);
        assertFalse(request.isAlive(), "el pedido en espera no se despertó");
    }

    // CC8 – Dos hilos esperan la ambulancia ocupada y la simulación termina.
    // Esperado: los dos terminan sin delegar en el estado (sigue AtendiendoEnDomicilio).
    @Test
    @DisplayName("CC8 – Terminar la simulación libera a todos los hilos en espera")
    void cc8_endingSimulacionReleasesAllWaitingThreads() throws InterruptedException {
        ambulance.atenderDomicilio(lucas);
        Thread transfer = startThread(() -> ambulance.trasladarALaClinica(simon));
        Thread maintenance = startThread(() -> ambulance.solicitarMantenimiento(operator));
        waitUntil(() -> isWaiting(transfer) && isWaiting(maintenance), 2000, "los dos pedidos esperan");

        ambulance.setSimulacionActiva(false);

        transfer.join(5000);
        maintenance.join(5000);
        assertFalse(transfer.isAlive() || maintenance.isAlive(), "quedaron hilos esperando");
        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulance.getEstadoActual());
    }

    // CC9 – RetornoAutomatico con la simulación activa y la ambulancia RegresandoClinicaSinPaciente.
    // Esperado: dentro de un intervalo (10 s, más 5 s de margen) la devuelve a Disponible (Javadoc:
    // "periódicamente intenta retornar la ambulancia a la clínica mientras la simulación esté activa").
    @Test
    @DisplayName("CC9 – RetornoAutomatico devuelve la ambulancia a Disponible")
    void cc9_retornoAutomaticoReturnsAmbulance() throws InterruptedException {
        ambulance.atenderDomicilio(lucas);
        ambulance.regresarSinPaciente();

        startThread(new RetornoAutomatico(ambulance));

        waitUntil(() -> ambulance.getEstadoActual() instanceof DisponibleState, 15000,
                "el retorno automático devuelve la ambulancia a Disponible");
        ambulance.setSimulacionActiva(false);
    }

    // CC10 – RetornoAutomatico con la simulación ya terminada y la ambulancia
    // RegresandoClinicaSinPaciente. Esperado: no la devuelve a Disponible y el hilo termina dentro de
    // un intervalo (10 s, más 5 s de margen) (Javadoc: retorna "mientras la simulación esté activa").
    @Test
    @DisplayName("CC10 – Con la simulación terminada, RetornoAutomatico termina sin retornar")
    void cc10_retornoAutomaticoAfterSimulacionDoesNothing() throws InterruptedException {
        ambulance.atenderDomicilio(lucas);
        ambulance.regresarSinPaciente();
        ambulance.setSimulacionActiva(false);

        Thread retornoAutomatico = startThread(new RetornoAutomatico(ambulance));

        retornoAutomatico.join(15000);
        assertFalse(retornoAutomatico.isAlive(), "el retorno automático siguió corriendo");
        assertInstanceOf(RegresandoClinicaSinPacienteState.class, ambulance.getEstadoActual());
    }

    // CC11 – Mientras un hilo hace el retorno a la clínica desde TrasladandoPacienteClinica, otro
    // consulta isOcupado(). Esperado (supuesto, buena práctica de concurrencia): ningún hilo duerme
    // con el lock de la ambulancia tomado, así que la consulta responde mientras el retorno todavía
    // está en curso (su hilo sigue en TIMED_WAITING).
    @Test
    @DisplayName("CC11 – Una transición lenta no bloquea las consultas de otros hilos")
    void cc11_slowTransitionDoesNotBlockQueries() throws InterruptedException {
        ambulance.trasladarALaClinica(lucas);
        Thread returnThread = startThread(ambulance::regresarSinPaciente);
        waitUntil(() -> returnThread.getState() == Thread.State.TIMED_WAITING, 2000, "el retorno está en curso");

        ambulance.isOcupado();

        assertEquals(Thread.State.TIMED_WAITING, returnThread.getState(),
                "isOcupado() tuvo que esperar a que terminara el retorno a la clínica");
    }

    // CC12 – Simulación completa chica con los hilos reales de la aplicación: 2 asociados con 1
    // solicitud cada uno, 1 operario y RetornoAutomatico. Esperado: termina sola, sin bloqueos.
    // El plazo de 2 minutos sale de las esperas al azar de TiempoMuerto (hasta 8 s cada una).
    @Test
    @DisplayName("CC12 – Una simulación chica con los hilos reales termina sin bloqueos")
    void cc12_smallSimulacionFinishes() throws InterruptedException {
        Thread[] threads = {
                startThread(new Asociado("Lucas", "Paniagua", "30111222", "2230001111", address, 1, ambulance)),
                startThread(new Asociado("Simon", "Schleider", "30333444", "2230003333", address, 1, ambulance)),
                startThread(operator),
                startThread(new RetornoAutomatico(ambulance))};

        for (Thread thread : threads) {
            thread.join(120_000);
            assertFalse(thread.isAlive(), "un hilo no terminó: posible bloqueo");
        }
        assertFalse(ambulance.isSimulacionActiva());
    }

    // CC13 – Falla sembrada FC1 (sin tocar el código): una ambulancia de prueba cuyo
    // regresarSinPaciente() se olvida de llamar a notifyAll(). Esperado: el escenario de CC6
    // detecta la falla, porque el pedido en espera ya no se despierta.
    @Test
    @DisplayName("CC13 – El escenario de CC6 detecta la falla sembrada FC1 (falta notifyAll)")
    void cc13_seededMissingNotifyAllIsDetected() throws InterruptedException {
        assertFalse(waitingRequestWakesUp(new AmbulanciaWithoutNotifyAll()),
                "el escenario de CC6 no detectó la falta de notifyAll()");
    }
}
