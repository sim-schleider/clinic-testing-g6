# Testing con mocks: Ambulancia y la interfaz State

## Objetivo y alcance

Aprovechar el diseño desacoplado basado en la interfaz `State` para probar de forma atómica, con
dobles de prueba, las dos mitades del patrón State (primera parte del ítem 3 de la consigna):

- `Ambulancia` **sin ningún estado concreto**: la interfaz `State` se reemplaza por un mock, como si
  los estados todavía no estuvieran implementados.
- **Cada estado concreto sin la ambulancia real**: `Ambulancia` se reemplaza por un mock y se
  verifica a qué estado pide pasar o si rechaza el pedido.

Suites (`@Tag("mock")`, Mockito 5):

- `AmbulanciaMockTest` (CM1–CM9)
- `StatesMockTest` (CM10–CM33)

Fuera de alcance: los casos en que la ambulancia está ocupada y la simulación activa, porque
bloquean el hilo. Se prueban en la sección de concurrencia.

## Criterio de validación y supuestos

**Criterio de validación.** El diagrama UML de estados, el Javadoc de `Ambulancia` y de cada estado, y el informe
de diseño de la aplicación (`docs/Informe.md`), que dice que la ambulancia *"notifica a observadores
cuando la ambulancia se encuentra en un estado en el que no puede cumplir una solicitud"*.

Tabla de transiciones (fila: estado actual; columna: evento; "Rechaza": no cambia de estado y
notifica):

| Estado actual | Traslado | Atención domicilio | Mantenimiento | Retorno clínica |
|---------------|----------|--------------------|---------------|-----------------|
| Disponible | Trasladando | AtendiendoEnDomicilio | EnTaller | Sin acción |
| AtendiendoEnDomicilio | Rechaza | Rechaza | Rechaza | RegresandoClinicaSinPaciente |
| TrasladandoPacienteClinica | Rechaza | Rechaza | Rechaza | Disponible |
| EnTaller | Rechaza | Rechaza | RegresandoDelTaller | Rechaza |
| RegresandoDelTaller | Rechaza | Rechaza | Rechaza | Disponible |
| RegresandoClinicaSinPaciente | Trasladando | AtendiendoEnDomicilio | Rechaza | Disponible |

Contrato de `Ambulancia`: si está libre y la simulación activa, cada pedido se ocupa, delega **una
sola vez** en el estado actual el método que corresponde y notifica; las liberaciones
(`regresarSinPaciente`, `volviendoDelTaller`) delegan, liberan y notifican; si la simulación terminó,
el pedido vuelve sin delegar.

**Precondiciones.** Ni el Javadoc ni el informe de diseño dicen qué pasa si un pedido recibe un
argumento nulo, así que ese comportamiento no está especificado y no se prueba: el mismo criterio
que en los ítems 1 y 2, donde las entradas fuera del dominio no tienen un resultado esperado.

**Supuestos:**

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S8 | Se verifica que llegue una notificación, no el texto de su tipo | El informe de diseño habla de los tipos `INFO`, `ALERTA` y `ERROR`; el código usa `OK`, `NO_AMBULANCIA` y `AMBULANCIA` |
| S9 | `RetornoClinica` en EnTaller se trata como un pedido más: si se rechaza, se notifica | El Javadoc dice "no puede retornar a la clínica", pero el retorno es un evento interno, no una solicitud de un usuario |

## Dobles de prueba usados

| Suite | Doble | Qué reemplaza | Para qué |
|-------|-------|---------------|----------|
| `AmbulanciaMockTest` | `mock(State.class)` | Los seis estados concretos | Verificar a qué método del estado delega la ambulancia y cuántas veces (`verify`, `verifyNoMoreInteractions`) |
| `AmbulanciaMockTest` | `mock(Observer.class)` | La vista | Verificar que la ambulancia notifica |
| `AmbulanciaMockTest` | `doAnswer` sobre el mock de `State` | Un estado que hace una transición | Simular el comportamiento de un estado (CM9) |
| `StatesMockTest` | `mock(Ambulancia.class)` | La ambulancia real | Verificar `setState(isA(...))` en las transiciones y `notifyObservers(...)` en los rechazos |

Los mocks se usan como **espías de interacción**: lo que se verifica no es un valor devuelto, sino
qué métodos llamó el componente bajo prueba sobre su colaborador.

## Clases de equivalencia

Para `AmbulanciaMockTest`:

| Variable | Clases |
|----------|--------|
| Ocupación | libre / ocupada |
| Simulación | activa / terminada |
| Comportamiento del estado mock | no hace nada / cambia de estado |

Para `StatesMockTest`, la técnica es la **prueba de transición de estados**: cada una de las 24
celdas de la tabla es una clase y se cubre con un único caso.

## Casos de prueba

`AmbulanciaMockTest` (estado mock):

| Id | Situación | Acción | Resultado esperado | Resultado |
|----|-----------|--------|--------------------|-----------|
| CM1 | Libre, activa | `atenderDomicilio(a)` | Delega `SolicitudDeAtencionDomicilio` una vez, queda ocupada, notifica | ✅ |
| CM2 | Libre, activa | `trasladarALaClinica(a)` | Delega `SolicitudDeTraslado` una vez, queda ocupada, notifica | ✅ |
| CM3 | Libre, activa | `solicitarMantenimiento(o)` | Delega `SolicitudMantenimiento` una vez, queda ocupada, notifica | ✅ |
| CM4 | Ocupada | `regresarSinPaciente()` | Delega `RetornoClinica` una vez, queda libre, notifica | ✅ |
| CM5 | Ocupada | `volviendoDelTaller()` | Delega `SolicitudMantenimiento` una vez, queda libre, notifica | ✅ |
| CM6 | Libre | `retornoAutomatico(r)` | Delega `RetornoClinica` una vez | ✅ |
| CM7 | Libre, terminada | `atenderDomicilio(a)` | Vuelve sin delegar y sin ocuparse | ✅ |
| CM8 | Ocupada, terminada | `trasladarALaClinica(a)` | No espera (vuelve antes de 2 s) y no delega | ✅ |
| CM9 | El estado mock cambia a otro estado al pedir un traslado | `trasladarALaClinica(a)` y `regresarSinPaciente()` | El retorno se delega en el estado nuevo, no en el anterior | ✅ |

`StatesMockTest` (ambulancia mock), una fila por celda de la tabla de transiciones:

| Id | Estado | Evento | Esperado | Resultado |
|----|--------|--------|----------|-----------|
| CM10 | Disponible | Traslado | Trasladando | ✅ |
| CM11 | Disponible | Atención domicilio | AtendiendoEnDomicilio | ✅ |
| CM12 | Disponible | Mantenimiento | EnTaller | ✅ |
| CM13 | Disponible | Retorno clínica | Sin acción | ✅ |
| CM14 | AtendiendoEnDomicilio | Traslado | Rechaza | ✅ |
| CM15 | AtendiendoEnDomicilio | Atención domicilio | Rechaza | ❌ D7 |
| CM16 | AtendiendoEnDomicilio | Mantenimiento | Rechaza | ✅ |
| CM17 | AtendiendoEnDomicilio | Retorno clínica | RegresandoClinicaSinPaciente | ✅ |
| CM18 | Trasladando | Traslado | Rechaza | ✅ |
| CM19 | Trasladando | Atención domicilio | Rechaza | ❌ D7 |
| CM20 | Trasladando | Mantenimiento | Rechaza | ✅ |
| CM21 | Trasladando | Retorno clínica | Disponible | ❌ D8 |
| CM22 | EnTaller | Traslado | Rechaza | ✅ |
| CM23 | EnTaller | Atención domicilio | Rechaza | ❌ D7 |
| CM24 | EnTaller | Mantenimiento | RegresandoDelTaller | ✅ |
| CM25 | EnTaller | Retorno clínica | Rechaza (S9) | ❌ D7 |
| CM26 | RegresandoDelTaller | Traslado | Rechaza | ✅ |
| CM27 | RegresandoDelTaller | Atención domicilio | Rechaza | ❌ D7 |
| CM28 | RegresandoDelTaller | Mantenimiento | Rechaza | ✅ |
| CM29 | RegresandoDelTaller | Retorno clínica | Disponible | ✅ |
| CM30 | RegresandoClinicaSinPaciente | Traslado | Trasladando | ✅ |
| CM31 | RegresandoClinicaSinPaciente | Atención domicilio | AtendiendoEnDomicilio | ✅ |
| CM32 | RegresandoClinicaSinPaciente | Mantenimiento | Rechaza | ✅ |
| CM33 | RegresandoClinicaSinPaciente | Retorno clínica | Disponible | ✅ |

## Resultados

La suite se ejecutó una vez, sobre el código provisto con solo las correcciones de compilación D1 y
D2 aplicadas en una copia temporal (Apéndice C), mediante `run_tests.sh mock`:

<!-- PENDIENTE: regenerar resultados/mock.{md,log} con run_tests.sh mock después de commitear los
cambios del branch. Los valores de esta tabla salen de la corrida del 06/11. -->

| Suite | Total | Pasan | Fallan |
|-------|-------|-------|--------|
| `AmbulanciaMockTest` | 9 | 9 | 0 |
| `StatesMockTest` | 24 | 18 | 6 |
| Total | 33 | 27 | 6 |

Pasan los 9 casos de `AmbulanciaMockTest`: la ambulancia delega correctamente en su estado. Los 6
casos que fallan están todos en los estados concretos (`StatesMockTest`). La tabla completa está en
el Apéndice A (`resultados/mock.md`).

## Defectos detectados

| Id | Ubicación | Descripción | Detectado por |
|----|-----------|-------------|---------------|
| D7 | `SolicitudDeAtencionDomicilio` de AtendiendoEnDomicilio, Trasladando, EnTaller y RegresandoDelTaller; `EnTallerState.RetornoClinica` | El rechazo no avisa a los observadores: la notificación está comentada en el código | CM15, CM19, CM23, CM25, CM27 |
| D8 | `TrasladandoPacienteClinicaState.RetornoClinica` | Pasa a `RegresandoDelTallerState` en lugar de `DisponibleState`, contra su propio Javadoc y el UML | CM21 |

## Conclusiones

- Aislar cada mitad del patrón permitió **ubicar exactamente** dónde está cada falla: la ambulancia
  delega bien (pasan los 9 casos) y los errores están en estados concretos puntuales, sin hilos ni
  esperas. Con la máquina de estados real, D8 aparece como un estado final incorrecto después de
  un flujo, pero no se ve en cuál de las transiciones está el error.
- Los mocks permitieron probar la ambulancia antes de tener los estados (consigna: *"ante la
  ausencia temporal de las implementaciones concretas"*) y simular un estado que cambia de estado
  (CM9).
- Limitaciones:
  - Un test con mocks no puede ver problemas de sincronización: eso queda para la sección de
    concurrencia.
  - Los rechazos verificados prueban que el estado llama a `notifyObservers`, no que el observador
    reciba el mensaje. Los estados no pueden llamar a `setChanged()` (es `protected` en
    `java.util.Observable`). Cuando el rechazo llega por `retornoAutomatico`, que no llama a
    `setChanged()`, la notificación se pierde.
  - Los casos dependen de que los estados se puedan construir con una ambulancia mock: si un estado
    llamara a métodos finales de `Ambulancia`, Mockito no podría interceptarlos.
