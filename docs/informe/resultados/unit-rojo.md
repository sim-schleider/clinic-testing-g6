# Resultados: Pruebas unitarias (rojo)

Fecha: 2026-10-25 13:16
Código: commit 9f263db · src/main sin cambios
Total: 18 · ✅ pass: 10 · ❌ fail: 8 · 💥 error: 0 · ⏭ skipped: 0

| Clase | Test | Id | Resultado | Detalle |
|-------|------|----|-----------|---------|
| AsociadoDTOTest | `testDefaultConstructorLeavesDefaultValues` | CU1 | ✅ pass |  |
| AsociadoDTOTest | `testConstructorAssignsValidData` | CU2 | ✅ pass |  |
| AsociadoDTOTest | `testConstructorRejectsNullNombre` | CU3 | ❌ fail | el constructor debía rechazar nombre = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNullApellido` | CU4 | ❌ fail | el constructor debía rechazar apellido = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNullDni` | CU5 | ❌ fail | el constructor debía rechazar dni = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNullCiudad` | CU6 | ❌ fail | el constructor debía rechazar ciudad = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNullCalle` | CU7 | ❌ fail | el constructor debía rechazar calle = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNullTelefono` | CU8 | ❌ fail | el constructor debía rechazar telefono = null ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testConstructorRejectsNegativeNumero` | CU9 | ❌ fail | el constructor debía rechazar numero = -1 ==> expected: <true> but was: <false> |
| AsociadoDTOTest | `testStringSettersAssignValidText` | CU10 | ✅ pass |  |
| AsociadoDTOTest | `testSetNumeroAcceptsZero` | CU11 | ❌ fail | java.lang.AssertionError |
| AsociadoDTOTest | `testSetNombreRejectsNull` | CU12 | ✅ pass |  |
| AsociadoDTOTest | `testSetApellidoRejectsNull` | CU13 | ✅ pass |  |
| AsociadoDTOTest | `testSetDniRejectsNull` | CU14 | ✅ pass |  |
| AsociadoDTOTest | `testSetCiudadRejectsNull` | CU15 | ✅ pass |  |
| AsociadoDTOTest | `testSetCalleRejectsNull` | CU16 | ✅ pass |  |
| AsociadoDTOTest | `testSetTelefonoRejectsNull` | CU17 | ✅ pass |  |
| AsociadoDTOTest | `testSetNumeroRejectsMinusOne` | CU18 | ✅ pass |  |
