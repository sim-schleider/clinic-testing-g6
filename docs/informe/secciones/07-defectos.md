# Registro de defectos

Defectos detectados por las suites, con el requisito u oráculo que violan y su estado final.

Severidad:
- **Alta**: impide la operación, pierde o corrompe datos, o produce un bloqueo.
- **Media**: produce un resultado incorrecto con un rodeo posible.
- **Baja**: inconsistencia sin efecto funcional inmediato.

| ID | Componente | Descripción | Oráculo violado | Detectado por | Esperado | Obtenido | Severidad | Estado |
|----|------------|-------------|-----------------|---------------|----------|----------|-----------|--------|
| D1 | `EnTallerState` | El archivo declara `package model.modeloDominio.ambulancia` pero está en `modelo/modeloDominio/ambulancia`: el proyecto no compila | Reglas del lenguaje Java (paquete = directorio); UML | Compilación (`mvn compile`) | El proyecto compila | `duplicate class`, `cannot find symbol Ambulancia` | Alta | No se corrige: fuera de alcance (solo testing). Se corrigió en c1ff2a6 y se revirtió |
| D2 | `TrasladandoPacienteClinicaState` | La clase pública `TrasladandoPacienteClinicaState` está en el archivo `TrasladandoPacienteClinicasState.java` | Reglas del lenguaje Java (clase pública = nombre de archivo) | Compilación (`mvn compile`) | El proyecto compila | Error de compilación por nombre de archivo | Alta | No se corrige: fuera de alcance (solo testing). Se corrigió en c1ff2a6 y se revirtió |
| D3 | `AsociadoDTO.setNumero` | La validación es `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0 | SRS §3.1: `numero` debe ser mayor o igual a 0 | CU11 | `setNumero(0)` asigna 0 | `AssertionError` | Media | No se corrige: fuera de alcance (solo testing) |
| D4 | `AsociadoDTO(...)` (constructor parametrizado) | Asigna los siete campos sin validar: acepta `null` en los `String` y `numero` negativo | SRS §3.1 (las reglas son del componente; supuesto S3) | CU3, CU4, CU5, CU6, CU7, CU8, CU9 | `AssertionError` ante cada dato inválido | Se crea el objeto con el dato inválido | Media | No se corrige: fuera de alcance (solo testing) |
| D9 | Estados de la ambulancia (`SolicitudDeAtencionDomicilio` en cuatro estados, `EnTallerState.RetornoClinica`) | El rechazo no avisa a los observadores: la notificación está comentada | Informe de diseño: "notifica a observadores cuando […] no puede cumplir una solicitud" | CM18, CM22, CM26, CM28, CM30 | `notifyObservers(...)` | No se invoca | Baja | No se corrige: fuera de alcance (solo testing) |
| D10 | `TrasladandoPacienteClinicaState.RetornoClinica` | Pasa a `RegresandoDelTallerState` en lugar de `DisponibleState` | Javadoc ("Cambia el estado de la ambulancia a DisponibleState") y UML | CM24, CC2 | `DisponibleState` | `RegresandoDelTallerState` | Media | No se corrige: fuera de alcance (solo testing) |
| D11 | `Ambulancia.retornoAutomatico` | Cambia el estado pero no libera la ambulancia | Invariante de ocupación (Javadoc de cada estado) | CC4 | Libre en RegresandoClinicaSinPaciente | Ocupada | Media | No se corrige: fuera de alcance (solo testing) |
| D12 | `Ambulancia.atenderDomicilio`, `trasladarALaClinica`, `solicitarMantenimiento` | Marcan la ambulancia como ocupada y notifican éxito aunque el estado rechace el pedido | Invariante de ocupación; informe de diseño (notificar el rechazo) | CC5 | Libre en RegresandoDelTaller | Ocupada | Media | No se corrige: fuera de alcance (solo testing) |
| D13 | `RetornoAutomatico.run` | La condición del `while` está invertida (`!isSimulacionActiva()`): con la simulación activa el hilo termina enseguida | Javadoc: "periódicamente intenta retornar la ambulancia a la clínica mientras la simulación esté activa" | CC10 | Disponible en menos de 15 s | Sigue en RegresandoClinicaSinPaciente | Alta | No se corrige: fuera de alcance (solo testing) |
| D14 | `TrasladandoPacienteClinicaState.RetornoClinica` | Hace `Thread.sleep(1000)` dentro de un método `synchronized` de la ambulancia: bloquea a todos los hilos durante 1 s | Informe de diseño (`synchronized` para exclusión mutua, `wait` para esperar); supuesto S18 | CC11 | La consulta responde mientras el retorno está en curso | `isOcupado()` espera a que termine el retorno | Media | No se corrige: fuera de alcance (solo testing) |
