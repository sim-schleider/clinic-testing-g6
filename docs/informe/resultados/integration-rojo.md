# Resultados: Pruebas de integración con MySQL (rojo)

Fecha: 2026-11-02 18:07
Código: commit 6170639 · src/main CON cambios sin commitear
Condiciones: copia temporal del código con los arreglos de compilación D1 y D2 (como en `c1ff2a6`), que no se suben al repositorio, y MySQL 8.0.40.
Total: 32 · ✅ pass: 24 · ❌ fail: 8 · 💥 error: 0 · ⏭ skipped: 0

| Clase | Test | Id | Resultado | Detalle |
|-------|------|----|-----------|---------|
| BaseDeDatosIntegrationTest | `testGetInstanciaReturnsSameInstance` | CI1 | ✅ pass |  |
| BaseDeDatosIntegrationTest | `testIniciarBDCreatesSchema` | CI2 | ✅ pass |  |
| BaseDeDatosIntegrationTest | `testIniciarBDCreatesTableWithColumns` | CI3 | ✅ pass |  |
| BaseDeDatosIntegrationTest | `testIniciarBDLoadsValidSeedData` | CI4 | ✅ pass |  |
| BaseDeDatosIntegrationTest | `testIniciarBDRecreatesTable` | CI5 | ✅ pass |  |
| BaseDeDatosIntegrationTest | `testGetInstanciaAfterCerrarConexionGivesActiveConnection` | CI6 | ❌ fail | getInstancia() devolvió un manejador con la conexión cerrada ==> expected: <false> but was: <true> |
| BaseDeDatosIntegrationTest | `testSchemaRejectsNullDni` | CI7 | ❌ fail | la tabla aceptó un asociado con DNI nulo ==> Expected java.sql.SQLException to be thrown, but nothing was thrown. |
| DataAccessObjectIntegrationTest | `testGuardarInsertsEveryColumn` | CI8 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarAcceptsMaximumLengths` | CI9 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsNombreTooLong` | CI10 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsApellidoTooLong` | CI11 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsDniTooLong` | CI12 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsCalleTooLong` | CI13 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsCiudadTooLong` | CI14 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsTelefonoTooLong` | CI15 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsDuplicateDni` | CI16 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testGuardarRejectsNull` | CI17 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testCargarFirstRow` | CI18 | ❌ fail | Unexpected exception thrown: java.sql.SQLException: Before start of result set |
| DataAccessObjectIntegrationTest | `testCargarLastRow` | CI19 | ❌ fail | Unexpected exception thrown: java.sql.SQLException: Before start of result set |
| DataAccessObjectIntegrationTest | `testCargarMissingIdReturnsNull` | CI20 | ❌ fail | Unexpected exception thrown: java.sql.SQLException: Illegal operation on empty result set. |
| DataAccessObjectIntegrationTest | `testCargarConLimiteOne` | CI21 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testCargarConLimiteEqualToRows` | CI22 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testCargarConLimiteGreaterThanRows` | CI23 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testCargarBDReturnsWholeTable` | CI24 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testActualizarChangesOnlyThatDni` | CI25 | ❌ fail | Unexpected exception thrown: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '22334455' for key 'Asociados.DNI' |
| DataAccessObjectIntegrationTest | `testActualizarMissingDniChangesNothing` | CI26 | ❌ fail | expected: <Juan\|State\|12345678\|Buenos Aires\|123\|Mar del Plata\|2235551234 Maria\|Template\|87654321\|Libertad\|742\|Mar del Plata\|2235555678 Carlos\|Observer\|11223344\|… |
| DataAccessObjectIntegrationTest | `testEliminarExistingDni` | CI27 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testEliminarMissingDniChangesNothing` | CI28 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testEliminarRejectsEmptyDni` | CI29 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testEliminarRejectsNullDni` | CI30 | ✅ pass |  |
| DataAccessObjectIntegrationTest | `testCargarBDMapsNumeroZero` | CI31 | ❌ fail | Unexpected exception thrown: java.lang.AssertionError |
| DataAccessObjectIntegrationTest | `testCargarBDWithoutTableThrowsSQLException` | CI32 | ✅ pass |  |
