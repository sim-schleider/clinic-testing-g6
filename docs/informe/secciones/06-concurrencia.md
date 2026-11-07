# Testing de integración y concurrencia: máquina de estados de la Ambulancia

## Objetivo y alcance

Evaluar el flujo completo de la máquina de estados real acoplada a `Ambulancia`, y la gestión del
recurso compartido entre hilos independientes: bloques `synchronized`, `wait`/`notifyAll` y el hilo
`RetornoAutomatico` (segunda parte del ítem 3 de la consigna). Además, demostrar que la suite
descubre problemas de sincronización y fallos de estado.

Suites (`@Tag("concurrency")`, sin mocks):

- `AmbulanciaStateMachineTest` (CC1–CC5): flujos completos en un solo hilo, con los estados
  reales.
- `AmbulanciaConcurrencyTest` (CC6–CC13): hilos que compiten por la ambulancia.

## Criterio de validación y supuestos

**Criterio de validación.** La tabla de transiciones de la sección de mocks (UML y Javadoc), el contrato de
`Ambulancia` (Javadoc), el Javadoc de `RetornoAutomatico` (*"periódicamente intenta retornar la
ambulancia a la clínica mientras la simulación esté activa"*), y el informe de diseño, que dice que
la ambulancia usa `synchronized` *"para que no los usen varios hilos a la vez"* y `wait()` *"para
poner a dormir algunos hilos hasta que se cumpla una condición específica"*.

**Invariante de ocupación.** El Javadoc del constructor de cada estado dice si la ambulancia queda
ocupada. De ahí sale la regla que se verifica después de cada operación:

> `isOcupado()` es verdadero si y solo si el estado es AtendiendoEnDomicilio,
> TrasladandoPacienteClinica o EnTaller.

Si el indicador queda desactualizado, los hilos que esperan la ambulancia se bloquean de más o
entran dos a la vez.

**Supuestos:**

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S10 | Después del retorno automático también se cumple el invariante | Si no, los hilos que esperan siguen bloqueados aunque la ambulancia esté regresando |
| S11 | Un pedido que el estado rechaza no deja la ambulancia ocupada | Sale del invariante. Choca con el contrato literal ("se ocupa, delega"), que no prevé el rechazo |
| S12 | Ningún hilo duerme con el lock de la ambulancia tomado | Buena práctica de concurrencia: una espera dentro de un bloque `synchronized` frena a todos los demás hilos |
| S13 | Una simulación chica (2 asociados con 1 solicitud) termina en menos de 2 minutos | El plazo sale de las esperas al azar de `TiempoMuerto` (hasta 8 s cada una), no de la especificación |
| S14 | Con la simulación terminada, `RetornoAutomatico` termina dentro de un intervalo (10 s) | El Javadoc dice que retorna "mientras la simulación esté activa", pero no cuándo termina el hilo |

## Recursos compartidos e hilos

| Recurso | Quién lo usa | Protección |
|---------|--------------|------------|
| `estadoActual` | Asociados, operario, `RetornoAutomatico` | `setState` es `synchronized`; `getEstadoActual` no, y el campo no es `volatile` |
| `ocupado` | Todos | Getter y setter `synchronized` |
| `isSimulacionActiva` | Todos y `Clinica.finalizarSimulacion` | Getter y setter `synchronized` |
| Lista `asociados` | `Clinica` (agrega), operario (recorre) | `agregarAsociado` no es `synchronized` |
| Monitor de la ambulancia | `wait()` en los pedidos; `notifyAll()` en las liberaciones y al terminar la simulación | Los `wait()` están dentro de un `while` |

## Flujos de la máquina de estados

Los casos CC1 a CC5 usan solo los métodos públicos de `Ambulancia`, en un solo hilo:

```mermaid
stateDiagram-v2
    [*] --> Disponible
    Disponible --> AtendiendoEnDomicilio: atenderDomicilio
    AtendiendoEnDomicilio --> RegresandoClinicaSinPaciente: regresarSinPaciente
    RegresandoClinicaSinPaciente --> Disponible: retornoAutomatico
    Disponible --> TrasladandoPacienteClinica: trasladarALaClinica
    TrasladandoPacienteClinica --> Disponible: regresarSinPaciente
    Disponible --> EnTaller: solicitarMantenimiento
    EnTaller --> RegresandoDelTaller: volviendoDelTaller
    RegresandoDelTaller --> Disponible: retornoAutomatico
```

## Técnicas para que los resultados no dependan de la suerte

- **Nunca se espera un tiempo fijo.** Se espera una condición observable (un hilo en `WAITING`, un
  cambio de estado) con un plazo máximo. Si el plazo vence, el test falla con un mensaje claro.
- **Plazos holgados** (5 s para despertar un hilo, 15 s para el retorno automático), para que una
  máquina cargada no produzca falsos rojos.
- **Hilos daemon y una ambulancia nueva por test.** Un hilo que queda bloqueado por un defecto no
  cuelga la ejecución ni afecta al test siguiente.
- **Hilos propios del test** en lugar de `Asociado` y `Operario`, que duermen tiempos al azar. Solo
  CC12 usa los hilos reales de la aplicación.
- **Sin mediciones en milisegundos.** CC11 empezó midiendo la demora de `isOcupado()` con un umbral
  de 200 ms y, con la máquina cargada, dio un falso verde. Ahora verifica que la consulta responda
  mientras el otro hilo sigue durmiendo.

## Casos de prueba

| Id | Escenario e hilos | Acciones | Resultado esperado | Resultado |
|----|-------------------|----------|--------------------|-----------|
| CC1 | Un hilo | Domicilio → regreso → retorno automático | AtendiendoEnDomicilio (ocupada), RegresandoClinicaSinPaciente (libre), Disponible (libre) | ✅ |
| CC2 | Un hilo | Traslado → regreso | Trasladando (ocupada), Disponible (libre) | ❌ D8 |
| CC3 | Un hilo | Mantenimiento → vuelta del taller → retorno automático | EnTaller (ocupada), RegresandoDelTaller (libre), Disponible (libre) | ✅ |
| CC4 | Un hilo | Domicilio → retorno automático | RegresandoClinicaSinPaciente y libre (S10) | ❌ D9 |
| CC5 | Un hilo | Mantenimiento → vuelta del taller → pedido de domicilio | Sigue RegresandoDelTaller y libre (S11) | ❌ D10 |
| CC6 | El test ocupa la ambulancia; otro hilo pide un traslado | Esperar a que el hilo quede en `WAITING`; después `regresarSinPaciente()` | Mientras espera, el estado no cambia; al liberarse, el pedido se despierta y la ambulancia queda Trasladando | ✅ |
| CC7 | El operario ocupa la ambulancia; un asociado espera | `volviendoDelTaller()` | El pedido se despierta | ✅ |
| CC8 | Dos hilos esperan la ambulancia ocupada | `setSimulacionActiva(false)` | Los dos terminan sin delegar en el estado | ✅ |
| CC9 | `RetornoAutomatico` con la simulación activa | Esperar hasta 15 s | La ambulancia vuelve a Disponible | ❌ D11 |
| CC10 | `RetornoAutomatico` con la simulación terminada | Esperar hasta 15 s | El hilo termina sin devolver la ambulancia a Disponible (S14) | ✅ |
| CC11 | Un hilo hace el retorno desde Trasladando; el test consulta `isOcupado()` | Consultar mientras el retorno está en curso | Responde sin esperar a que termine el retorno (S12) | ❌ D12 |
| CC12 | 2 `Asociado`, 1 `Operario` y `RetornoAutomatico` reales | Esperar a que terminen | Todos los hilos terminan en menos de 2 minutos (S13) | ✅ |
| CC13 | Falla sembrada FC1: ambulancia de prueba sin `notifyAll()` | El escenario de CC6 | El escenario detecta la falla: el pedido no se despierta | ✅ |

## Resultados

La suite se ejecutó una vez, sobre el código provisto con solo las correcciones de compilación D1 y
D2 aplicadas en una copia temporal (Apéndice C), mediante `run_tests.sh concurrency`:

<!-- PENDIENTE: regenerar resultados/concurrency.{md,log} con run_tests.sh concurrency después de
commitear los cambios del branch. Los valores de esta tabla salen de la corrida del 06/11. -->

| Suite | Total | Pasan | Fallan |
|-------|-------|-------|--------|
| `AmbulanciaStateMachineTest` | 5 | 2 | 3 |
| `AmbulanciaConcurrencyTest` | 8 | 6 | 2 |
| Total | 13 | 8 | 5 |

Pasan los casos de exclusión mutua, de despertar de los hilos en espera, de fin de simulación, del
retorno automático con la simulación terminada y de la simulación completa. Fallan 5 casos, uno por
cada defecto D8 a D12. La tabla completa está en el Apéndice A (`resultados/concurrency.md`).

Los casos concurrentes son deterministas: un hilo solo entra en `WAITING` dentro de `wait()`, y solo
lo despierta un `notifyAll()` del código. Por eso no hace falta repetirlos para estimar una tasa de
detección.

## Defectos detectados

| Id | Ubicación | Descripción | Detectado por |
|----|-----------|-------------|---------------|
| D8 | `TrasladandoPacienteClinicaState.RetornoClinica` | Pasa a RegresandoDelTaller en lugar de Disponible | CM21, CC2 |
| D9 | `Ambulancia.retornoAutomatico` | Cambia el estado pero no libera la ambulancia | CC4 |
| D10 | `atenderDomicilio`, `trasladarALaClinica`, `solicitarMantenimiento` | Ocupan la ambulancia y notifican éxito aunque el estado rechace el pedido | CC5 |
| D11 | `RetornoAutomatico.run` | La condición del `while` está invertida: con la simulación activa el hilo termina enseguida y nunca devuelve la ambulancia | CC9 |
| D12 | `TrasladandoPacienteClinicaState.RetornoClinica` | `Thread.sleep(1000)` dentro de un método `synchronized`: bloquea la ambulancia a todos los hilos durante 1 s | CC11 |

## Fallas sembradas

Como el código de producción no se modifica, la falla se introduce con un doble de prueba definido
en la suite. `support/AmbulanciaWithoutNotifyAll` es una subclase de `Ambulancia` cuyo `regresarSinPaciente()` no
llama a `notifyAll()`. CC13 corre sobre ella el mismo escenario de CC6 y comprueba que el pedido en
espera ya no se despierta: el escenario detecta la falla sembrada.

| Id | Falla sembrada | Detectada por | Resultado |
|----|----------------|---------------|-----------|
| FC1 | Falta `notifyAll()` al liberar la ambulancia | El escenario de CC6 (en CC13) | Detectada |

## Observaciones de revisión de código

- `getEstadoActual()` no es `synchronized` y el campo no es `volatile`: otro hilo puede ver un estado
  viejo. No se pudo reproducir de forma confiable con un test.
- En `Clinica.iniciarSimulacion` cada asociado se agrega dos veces a la lista de la ambulancia (en
  el constructor de `Asociado` y otra vez en `Clinica`).
- `TrasladandoPacienteClinicaState.RetornoClinica` atrapa `Exception` sin hacer nada: si el hilo es
  interrumpido, la interrupción se pierde.
- Por D11, `RetornoAutomatico` nunca llega a ejecutar su ciclo en CC12: la simulación completa
  termina igual, pero sin el retorno automático.

## Conclusiones

- La máquina de estados real confirmó D8, que los mocks ya habían ubicado, y encontró dos
  defectos que los mocks no podían ver porque surgen de combinar la ambulancia con los estados
  reales: el indicador de ocupación queda desactualizado después del retorno automático (D9) y
  después de un pedido rechazado (D10).
- Solo los hilos reales mostraron D11 y D12. D11 es el más grave del ítem: el hilo de retorno
  automático, que según el informe de diseño es "vital para la simulación", termina apenas arranca. Con la simulación terminada, en cambio, no devuelve la ambulancia (CC10).
- La exclusión mutua, los despertares y el fin de la simulación (CC6 a CC8) funcionan bien, y la falla sembrada FC1 muestra
  que esos escenarios detectarían un `notifyAll()` faltante.
- Limitaciones:
  - Que estos casos den verde no prueba la ausencia de condiciones de carrera. Por ejemplo, el
    problema de visibilidad de `getEstadoActual()` quedó como observación.
  - CC12 depende de esperas al azar y tarda entre 20 y 60 s.
