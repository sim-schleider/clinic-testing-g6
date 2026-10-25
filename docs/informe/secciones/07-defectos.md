# Registro de defectos

Defectos detectados por las suites, con el requisito u oráculo que violan y su estado final.

Severidad:
- **Alta**: impide la operación, pierde o corrompe datos, o produce un bloqueo.
- **Media**: produce un resultado incorrecto con un rodeo posible.
- **Baja**: inconsistencia sin efecto funcional inmediato.

| ID | Componente | Descripción | Oráculo violado | Detectado por | Esperado | Obtenido | Severidad | Estado |
|----|------------|-------------|-----------------|---------------|----------|----------|-----------|--------|
| D1 | `EnTallerState` | El archivo declara `package model.modeloDominio.ambulancia` pero está en `modelo/modeloDominio/ambulancia`: el proyecto no compila | Reglas del lenguaje Java (paquete = directorio); UML | Compilación (`mvn compile`) | El proyecto compila | `duplicate class`, `cannot find symbol Ambulancia` | Alta | Corregido (c1ff2a6), única excepción a la regla de no modificar el código |
| D2 | `TrasladandoPacienteClinicaState` | La clase pública `TrasladandoPacienteClinicaState` está en el archivo `TrasladandoPacienteClinicasState.java` | Reglas del lenguaje Java (clase pública = nombre de archivo) | Compilación (`mvn compile`) | El proyecto compila | Error de compilación por nombre de archivo | Alta | Corregido (c1ff2a6), única excepción a la regla de no modificar el código |
| D3 | `AsociadoDTO.setNumero` | La validación es `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0 | SRS §3.1: `numero` debe ser mayor o igual a 0 | CU11 | `setNumero(0)` asigna 0 | `AssertionError` | Media | No se corrige: fuera de alcance (solo testing) |
| D4 | `AsociadoDTO(...)` (constructor parametrizado) | Asigna los siete campos sin validar: acepta `null` en los `String` y `numero` negativo | SRS §3.1 (las reglas son del componente; supuesto S3) | CU3, CU4, CU5, CU6, CU7, CU8, CU9 | `AssertionError` ante cada dato inválido | Se crea el objeto con el dato inválido | Media | No se corrige: fuera de alcance (solo testing) |
