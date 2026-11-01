package modelo.modeloDominio.ambulancia;

import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de concurrencia de Ambulancia como recurso compartido: exclusión mutua de los métodos
 * synchronized, hilos que esperan (wait) y se despiertan (notifyAll), fin ordenado de la simulación
 * y el hilo RetornoAutomatico. Oráculo: .skills/concurrency-testing/references/ambulance-oracle.md
 * §3 y §4 (Javadoc e informe de diseño).
 * Reglas para que los resultados no dependan de la suerte: nunca se espera un tiempo fijo, sino una
 * condición observable con un plazo máximo; los hilos del test son daemon, así un hilo que queda
 * bloqueado por un defecto no cuelga la ejecución; cada test usa una ambulancia nueva.
 * Salvo la simulación completa (CC12), se usan hilos propios del test y no Asociado ni Operario,
 * que duermen tiempos al azar de hasta 8 s.
 */
@Tag("concurrency")
class AmbulanciaConcurrenciaTest {

    private Ambulancia ambulancia;
    private Asociado ana;
    private Asociado beto;
    private Operario operario;

    @BeforeEach
    void setUpAmbulancia() {
        ambulancia = new Ambulancia();
        Domicilio domicilio = new Domicilio("Calle", 1, "Ciudad");
        ana = new Asociado("Ana", "Diaz", "30111222", "2230001111", domicilio);
        beto = new Asociado("Beto", "Gomez", "30333444", "2230003333", domicilio);
        operario = new Operario("Juan", "Perez", "20111222", "2230002222", domicilio, ambulancia);
    }

    private static Thread iniciarHilo(Runnable tarea) {
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    /** Espera a que se cumpla la condición; si no se cumple dentro del plazo, el test falla. */
    private static void esperarHasta(BooleanSupplier condicion, long plazoMs, String descripcion)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + plazoMs;
        while (!condicion.getAsBoolean()) {
            if (System.currentTimeMillis() > limite) {
                fail("no ocurrió en " + plazoMs + " ms: " + descripcion);
            }
            Thread.sleep(10);
        }
    }

    private static boolean esperando(Thread hilo) {
        return hilo.getState() == Thread.State.WAITING;
    }

    /**
     * Escenario de CC7 y CC13: el hilo del test ocupa la ambulancia, otro hilo pide un traslado y
     * queda esperando, y el hilo del test la libera con regresarSinPaciente().
     * Devuelve true si el pedido en espera se despertó y terminó dentro de 2 s.
     */
    private boolean pedidoEnEsperaSeDespierta(Ambulancia ambulancia) throws InterruptedException {
        ambulancia.atenderDomicilio(ana);
        Thread pedido = iniciarHilo(() -> ambulancia.trasladarALaClinica(beto));
        esperarHasta(() -> esperando(pedido), 2000, "el pedido de traslado queda esperando");

        ambulancia.regresarSinPaciente();

        pedido.join(2000);
        return !pedido.isAlive();
    }

    // CC6 – Exclusión mutua: con la ambulancia ocupada, otro hilo pide un traslado.
    // Esperado: ese hilo queda esperando (WAITING) y el estado no cambia.
    @Test
    void testBusyAmbulanciaMakesOtherThreadWait() throws InterruptedException {
        ambulancia.atenderDomicilio(ana);

        Thread pedido = iniciarHilo(() -> ambulancia.trasladarALaClinica(beto));

        esperarHasta(() -> esperando(pedido), 2000, "el pedido de traslado queda esperando");
        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulancia.getEstadoActual());
    }

    // CC7 – El pedido en espera se despierta cuando la ambulancia se libera con regresarSinPaciente().
    // Esperado: el hilo termina y la ambulancia queda TrasladandoPacienteClinica.
    @Test
    void testWaitingRequestWakesUpOnRegresarSinPaciente() throws InterruptedException {
        assertTrue(pedidoEnEsperaSeDespierta(ambulancia), "el pedido en espera no se despertó");
        assertInstanceOf(TrasladandoPacienteClinicaState.class, ambulancia.getEstadoActual());
    }

    // CC8 – El operario ocupa la ambulancia (taller) y un asociado pide atención a domicilio.
    // Esperado: el asociado espera y se despierta cuando la ambulancia vuelve del taller.
    @Test
    void testWaitingRequestWakesUpOnVolviendoDelTaller() throws InterruptedException {
        ambulancia.solicitarMantenimiento(operario);
        Thread pedido = iniciarHilo(() -> ambulancia.atenderDomicilio(ana));
        esperarHasta(() -> esperando(pedido), 2000, "el pedido de atención queda esperando");

        ambulancia.volviendoDelTaller();

        pedido.join(2000);
        assertFalse(pedido.isAlive(), "el pedido en espera no se despertó");
    }

    // CC9 – Dos hilos esperan la ambulancia ocupada y la simulación termina.
    // Esperado: los dos terminan sin delegar en el estado (sigue AtendiendoEnDomicilio).
    @Test
    void testEndingSimulacionReleasesAllWaitingThreads() throws InterruptedException {
        ambulancia.atenderDomicilio(ana);
        Thread traslado = iniciarHilo(() -> ambulancia.trasladarALaClinica(beto));
        Thread mantenimiento = iniciarHilo(() -> ambulancia.solicitarMantenimiento(operario));
        esperarHasta(() -> esperando(traslado) && esperando(mantenimiento), 2000, "los dos pedidos esperan");

        ambulancia.setSimulacionActiva(false);

        traslado.join(2000);
        mantenimiento.join(2000);
        assertFalse(traslado.isAlive() || mantenimiento.isAlive(), "quedaron hilos esperando");
        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulancia.getEstadoActual());
    }

    // CC10 – RetornoAutomatico con la simulación activa y la ambulancia RegresandoClinicaSinPaciente.
    // Esperado: dentro de un intervalo (10 s, más margen) la devuelve a Disponible (Javadoc:
    // "periódicamente intenta retornar la ambulancia a la clínica mientras la simulación esté activa").
    @Test
    void testRetornoAutomaticoReturnsAmbulancia() throws InterruptedException {
        ambulancia.atenderDomicilio(ana);
        ambulancia.regresarSinPaciente();

        iniciarHilo(new RetornoAutomatico(ambulancia));

        esperarHasta(() -> ambulancia.getEstadoActual() instanceof DisponibleState, 12000,
                "el retorno automático devuelve la ambulancia a Disponible");
        ambulancia.setSimulacionActiva(false);
    }

    // CC11 – Mientras un hilo hace el retorno a la clínica desde TrasladandoPacienteClinica, otro
    // consulta isOcupado(). Esperado (supuesto, buena práctica de concurrencia): la consulta no
    // queda bloqueada por una espera dentro del bloque synchronized, o sea, responde mientras el
    // retorno todavía está en curso (su hilo sigue en TIMED_WAITING). No se mide tiempo: con la
    // máquina cargada una medición en milisegundos puede dar un falso verde.
    @Test
    void testSlowTransitionDoesNotBlockQueries() throws InterruptedException {
        ambulancia.trasladarALaClinica(ana);
        Thread retorno = iniciarHilo(ambulancia::regresarSinPaciente);
        esperarHasta(() -> retorno.getState() == Thread.State.TIMED_WAITING, 2000, "el retorno está en curso");

        ambulancia.isOcupado();

        assertEquals(Thread.State.TIMED_WAITING, retorno.getState(),
                "isOcupado() tuvo que esperar a que terminara el retorno a la clínica");
    }

    // CC12 – Simulación completa chica: 2 asociados con 1 solicitud cada uno, 1 operario y el
    // retorno automático, con los hilos reales de la aplicación. Esperado: la simulación termina
    // sola, sin bloqueos, en menos de 2 minutos, y cada asociado completó su solicitud.
    @Test
    void testSmallSimulacionFinishes() throws InterruptedException {
        Domicilio domicilio = new Domicilio("Calle", 1, "Ciudad");
        Asociado primero = new Asociado("Ana", "Diaz", "30111222", "2230001111", domicilio, 1, ambulancia);
        Asociado segundo = new Asociado("Beto", "Gomez", "30333444", "2230003333", domicilio, 1, ambulancia);
        Thread[] hilos = {iniciarHilo(primero), iniciarHilo(segundo), iniciarHilo(operario),
                iniciarHilo(new RetornoAutomatico(ambulancia))};

        long limite = System.currentTimeMillis() + 120_000;
        for (Thread hilo : hilos) {
            hilo.join(Math.max(1, limite - System.currentTimeMillis()));
            assertFalse(hilo.isAlive(), "un hilo no terminó: posible bloqueo");
        }
        assertFalse(ambulancia.isSimulacionActiva());
        assertEquals(1, primero.getCantSolicitudesAtendidas());
        assertEquals(1, segundo.getCantSolicitudesAtendidas());
    }

    // CC13 – Falla sembrada FC1 (sin tocar el código): una ambulancia de prueba cuyo
    // regresarSinPaciente() se olvida de llamar a notifyAll(). Esperado: el escenario de CC7
    // detecta la falla, porque el pedido en espera ya no se despierta.
    @Test
    void testSeededMissingNotifyAllIsDetected() throws InterruptedException {
        assertFalse(pedidoEnEsperaSeDespierta(new AmbulanciaSinNotifyAll()),
                "el escenario de CC7 no detectó la falta de notifyAll()");
    }

    /** Doble de prueba con una falla sembrada: reemplaza regresarSinPaciente() sin el notifyAll(). */
    static class AmbulanciaSinNotifyAll extends Ambulancia {
        public synchronized void regresarSinPaciente() {
            getEstadoActual().RetornoClinica();
            setOcupado(false);
        }
    }
}
