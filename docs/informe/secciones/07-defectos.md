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
| D3 | `AsociadoDTO.setNumero` | La validación es `numero > 0` en lugar de `numero >= 0`: rechaza el valor límite 0 | SRS §3.1: `numero` debe ser mayor o igual a 0 | CU11, CI31 | `setNumero(0)` asigna 0 | `AssertionError` | Media | No se corrige: fuera de alcance (solo testing) |
| D4 | `AsociadoDTO(...)` (constructor parametrizado) | Asigna los siete campos sin validar: acepta `null` en los `String` y `numero` negativo | SRS §3.1 (las reglas son del componente; supuesto S3) | CU3, CU4, CU5, CU6, CU7, CU8, CU9 | `AssertionError` ante cada dato inválido | Se crea el objeto con el dato inválido | Media | No se corrige: fuera de alcance (solo testing) |
| D5 | `BaseDeDatos.getInstancia` | Después de `cerrarConexion()` sigue entregando el mismo manejador, con la conexión cerrada | SRS §3.2: "un único manejador de conexión activo" (supuesto S11) | CI6 | Conexión abierta | `isClosed()` = `true` | Media | No se corrige: fuera de alcance (solo testing) |
| D6 | `BaseDeDatos.iniciarBD` (esquema) | Ninguna columna es `NOT NULL`: la tabla acepta un asociado sin DNI | SRS §3.1: el DNI "no puede ser nulo; actúa como identificador único" (supuesto S12) | CI7 | `SQLException` | La fila se inserta | Media | No se corrige: fuera de alcance (solo testing) |
| D7 | `DataAccessObject.cargar` | Lee el `ResultSet` sin llamar a `next()` | SRS §3.3: recuperar registros "de manera individual por su identificador único" | CI18, CI19, CI20 | El DTO de la fila pedida (o `null` si no existe) | `SQLException: Before start of result set` | Alta | No se corrige: fuera de alcance (solo testing) |
| D8 | `BaseDeDatos.ejecutarActualizacion` | El `UPDATE` fija `WHERE id = 1` (`setInt(8, 1)`): siempre modifica la fila 1 | SRS §3.3: actualiza "basándose en criterios únicos como el número de documento (DNI)" | CI25, CI26 | Cambia solo la fila del DNI | `Duplicate entry '22334455'` (CI25); la fila 1 sobrescrita (CI26) | Alta | No se corrige: fuera de alcance (solo testing) |
