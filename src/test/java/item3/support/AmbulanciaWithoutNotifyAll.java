package item3.support;

import modelo.modeloDominio.ambulancia.Ambulancia;

/**
 * Doble de prueba con una falla sembrada (FC1), sin tocar el código de producción: igual a
 * Ambulancia, pero su regresarSinPaciente() no llama a notifyAll(), así que los hilos que esperan
 * la ambulancia no se despiertan. Lo usa AmbulanciaConcurrencyTest (CC13).
 */
public class AmbulanciaWithoutNotifyAll extends Ambulancia {

    @Override
    public synchronized void regresarSinPaciente() {
        getEstadoActual().RetornoClinica();
        setOcupado(false);
    }
}
