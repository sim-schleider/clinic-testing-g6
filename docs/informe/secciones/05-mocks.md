# Testing con mocks: Ambulancia y la interfaz State

## Objetivo y alcance

Aprovechar el diseño desacoplado basado en la interfaz `State` para probar de forma atómica, con
dobles de prueba, las dos mitades del patrón State (primera parte del ítem 3 de la consigna):

- `Ambulancia` **sin ningún estado concreto**: la interfaz `State` se reemplaza por un mock, como si
  los estados todavía no estuvieran implementados.
- **Cada estado concreto sin la ambulancia real**: `Ambulancia` se reemplaza por un mock y se
  verifica a qué estado pide pasar o si rechaza el pedido.

Suites (`@Tag("mock")`, Mockito 5):

- `AmbulanciaMockTest` (CM1–CM12, CM37)
- `EstadosMockTest` (CM13–CM36)

Fuera de alcance: los casos en que la ambulancia está ocupada y la simulación activa, porque
bloquean el hilo. Se prueban en la sección de concurrencia.

## Oráculo y supuestos

**Oráculo.** El diagrama UML de estados, el Javadoc de `Ambulancia` y de cada estado, y el informe
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
el pedido vuelve sin delegar; los argumentos nulos se rechazan con `AssertionError`.

**Supuestos:**

| Id | Supuesto | Justificación |
|----|----------|---------------|
| S14 | Se verifica que llegue una notificación, no el texto de su tipo | El informe de diseño habla de los tipos `INFO`, `ALERTA` y `ERROR`; el código usa `OK`, `NO_AMBULANCIA` y `AMBULANCIA` |
| S15 | `RetornoClinica` en EnTaller se trata como un pedido más: si se rechaza, se notifica | El Javadoc dice "no puede retornar a la clínica", pero el retorno es un evento interno, no una solicitud de un usuario |

## Dobles de prueba usados

| Suite | Doble | Qué reemplaza | Para qué |
|-------|-------|---------------|----------|
| `AmbulanciaMockTest` | `mock(State.class)` | Los seis estados concretos | Verificar a qué método del estado delega la ambulancia y cuántas veces (`verify`, `verifyNoMoreInteractions`) |
| `AmbulanciaMockTest` | `mock(Observer.class)` | La vista | Verificar que la ambulancia notifica |
| `AmbulanciaMockTest` | `doAnswer` sobre el mock de `State` | Un estado que hace una transición | Simular el comportamiento de un estado (CM37) |
| `EstadosMockTest` | `mock(Ambulancia.class)` | La ambulancia real | Verificar `setState(isA(...))` en las transiciones y `notifyObservers(...)` en los rechazos |

Los mocks se usan como **espías de interacción**: lo que se verifica no es un valor devuelto, sino
qué métodos llamó el componente bajo prueba sobre su colaborador.

## Clases de equivalencia

Para `AmbulanciaMockTest`:

| Variable | Clases |
|----------|--------|
| Argumento (`Asociado`, `Operario`, `State`) | no nulo / nulo |
| Ocupación | libre / ocupada |
| Simulación | activa / terminada |
| Comportamiento del estado mock | no hace nada / cambia de estado |

Para `EstadosMockTest`, la técnica es la **prueba de transición de estados**: cada una de las 24
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
| CM7 | Libre, activa | `atenderDomicilio(null)` | `AssertionError`, sin tocar el estado | ✅ |
| CM8 | Libre, activa | `trasladarALaClinica(null)` | `AssertionError`, sin tocar el estado | ✅ |
| CM9 | Libre, activa | `solicitarMantenimiento(null)` | `AssertionError`, sin tocar el estado | ✅ |
| CM10 | — | `setState(null)` | `AssertionError`, estado sin cambios | ✅ |
| CM11 | Libre, terminada | `atenderDomicilio(a)` | Vuelve sin delegar y sin ocuparse | ✅ |
| CM12 | Ocupada, terminada | `trasladarALaClinica(a)` | No espera (vuelve antes de 2 s) y no delega | ✅ |
| CM37 | El estado mock cambia a otro estado al pedir un traslado | `trasladarALaClinica(a)` y `regresarSinPaciente()` | El retorno se delega en el estado nuevo, no en el anterior | ✅ |

`EstadosMockTest` (ambulancia mock), una fila por celda de la tabla de transiciones:

| Id | Estado | Evento | Esperado | Resultado |
|----|--------|--------|----------|-----------|
| CM13 | Disponible | Traslado | Trasladando | ✅ |
| CM14 | Disponible | Atención domicilio | AtendiendoEnDomicilio | ✅ |
| CM15 | Disponible | Mantenimiento | EnTaller | ✅ |
| CM16 | Disponible | Retorno clínica | Sin acción | ✅ |
| CM17 | AtendiendoEnDomicilio | Traslado | Rechaza | ✅ |
| CM18 | AtendiendoEnDomicilio | Atención domicilio | Rechaza | ❌ D9 |
| CM19 | AtendiendoEnDomicilio | Mantenimiento | Rechaza | ✅ |
| CM20 | AtendiendoEnDomicilio | Retorno clínica | RegresandoClinicaSinPaciente | ✅ |
| CM21 | Trasladando | Traslado | Rechaza | ✅ |
| CM22 | Trasladando | Atención domicilio | Rechaza | ❌ D9 |
| CM23 | Trasladando | Mantenimiento | Rechaza | ✅ |
| CM24 | Trasladando | Retorno clínica | Disponible | ❌ D10 |
| CM25 | EnTaller | Traslado | Rechaza | ✅ |
| CM26 | EnTaller | Atención domicilio | Rechaza | ❌ D9 |
| CM27 | EnTaller | Mantenimiento | RegresandoDelTaller | ✅ |
| CM28 | EnTaller | Retorno clínica | Rechaza (S15) | ❌ D9 |
| CM29 | RegresandoDelTaller | Traslado | Rechaza | ✅ |
| CM30 | RegresandoDelTaller | Atención domicilio | Rechaza | ❌ D9 |
| CM31 | RegresandoDelTaller | Mantenimiento | Rechaza | ✅ |
| CM32 | RegresandoDelTaller | Retorno clínica | Disponible | ✅ |
| CM33 | RegresandoClinicaSinPaciente | Traslado | Trasladando | ✅ |
| CM34 | RegresandoClinicaSinPaciente | Atención domicilio | AtendiendoEnDomicilio | ✅ |
| CM35 | RegresandoClinicaSinPaciente | Mantenimiento | Rechaza | ✅ |
| CM36 | RegresandoClinicaSinPaciente | Retorno clínica | Disponible | ✅ |

## Resultados

Ejecución del 31/10/2026, sobre el código provisto con solo las correcciones de compilación D1 y D2
aplicadas. Esas correcciones se revirtieron después por indicación de la cátedra: con el código tal
como se entregó la suite no compila (ver los defectos D1 y D2 en el registro de defectos).

| Suite | Total | Pasan | Fallan |
|-------|-------|-------|--------|
| `AmbulanciaMockTest` | 13 | 13 | 0 |
| `EstadosMockTest` | 24 | 18 | 6 |
| Total | 37 | 31 | 6 |

`AmbulanciaMockTest` queda en verde: la ambulancia delega correctamente en su estado. Los seis rojos
están todos en los estados concretos.

## Defectos detectados

| Id | Ubicación | Descripción | Detectado por |
|----|-----------|-------------|---------------|
| D9 | `SolicitudDeAtencionDomicilio` de AtendiendoEnDomicilio, Trasladando, EnTaller y RegresandoDelTaller; `EnTallerState.RetornoClinica` | El rechazo no avisa a los observadores: la notificación está comentada en el código | CM18, CM22, CM26, CM28, CM30 |
| D10 | `TrasladandoPacienteClinicaState.RetornoClinica` | Pasa a `RegresandoDelTallerState` en lugar de `DisponibleState`, contra su propio Javadoc y el UML | CM24 |

## Conclusiones

- Aislar cada mitad del patrón permitió **ubicar exactamente** dónde está cada falla: la ambulancia
  delega bien (13 de 13 en verde) y los errores están en estados concretos puntuales, sin hilos ni
  esperas. Con la máquina de estados real, D10 aparece como un estado final incorrecto después de
  un flujo, pero no se ve en cuál de las transiciones está el error.
- Los mocks permitieron probar la ambulancia antes de tener los estados (consigna: *"ante la
  ausencia temporal de las implementaciones concretas"*) y simular un estado que cambia de estado
  (CM37).
- Limitaciones:
  - Un test con mocks no puede ver problemas de sincronización: eso queda para la sección de
    concurrencia.
  - Los rechazos verificados prueban que el estado llama a `notifyObservers`, no que el observador
    reciba el mensaje. Los estados no pueden llamar a `setChanged()` (es `protected` en
    `java.util.Observable`). Cuando el rechazo llega por `retornoAutomatico`, que no llama a
    `setChanged()`, la notificación se pierde.
  - CM7–CM10 dependen de ejecutar con `-ea`.
