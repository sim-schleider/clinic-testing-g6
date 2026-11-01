package persistencia;

import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static persistencia.BaseDeDatosDePrueba.*;

/**
 * Pruebas de integración AsociadoDTO → DataAccessObject → BaseDeDatos → MySQL real (SRS §3.3).
 * Cada escritura se verifica con SQL propio (BaseDeDatosDePrueba), nunca leyendo con el mismo DAO.
 * Los valores límite salen del esquema que crea iniciarBD(): VARCHAR(30) para nombre, apellido y
 * calle, VARCHAR(35) para ciudad, VARCHAR(10) para DNI y VARCHAR(15) para telefono.
 */
@Tag("integration")
class DataAccessObjectIntegrationTest {

    private static final String FILA_1 = "Juan|State|12345678|Buenos Aires|123|Mar del Plata|2235551234\n";

    private DataAccessObject dao;

    /** Cada test parte de la tabla recreada con los 20 datos semilla. */
    @BeforeEach
    void setUpDao() throws Exception {
        reiniciarBaseDeDatos();
        dao = new DataAccessObject();
    }

    private static Asociado asociado(String nombre, String apellido, String dni, String calle,
                                     int numero, String ciudad, String telefono) {
        return new Asociado(nombre, apellido, dni, telefono, new Domicilio(calle, numero, ciudad));
    }

    /** Asociado válido con un valor distinto en cada campo, para detectar columnas cruzadas. */
    private static Asociado asociadoValido() {
        return asociado("Nombre", "Apellido", "40111222", "Calle", 777, "Ciudad", "2230001111");
    }

    // ---------------------------------------------------------------- guardar

    // CI8 – guardar con datos válidos. Esperado: una fila nueva con cada dato en su columna (SRS §3.3).
    @Test
    void testGuardarInsertsEveryColumn() throws SQLException {
        dao.guardar(asociadoValido());

        assertEquals(21, contarFilas());
        assertEquals("Nombre|Apellido|40111222|Calle|777|Ciudad|2230001111\n", filas("DNI = '40111222'"));
    }

    // CI9 – guardar con cada texto en su largo máximo (valor límite válido). Esperado: se guarda completo.
    @Test
    void testGuardarAcceptsMaximumLengths() throws SQLException {
        dao.guardar(asociado("N".repeat(30), "A".repeat(30), "1".repeat(10), "C".repeat(30), 1,
                "M".repeat(35), "2".repeat(15)));

        assertEquals("N".repeat(30) + "|" + "A".repeat(30) + "|" + "1".repeat(10) + "|" + "C".repeat(30)
                + "|1|" + "M".repeat(35) + "|" + "2".repeat(15) + "\n", filas("DNI = '1111111111'"));
    }

    // CI10 a CI15 – guardar con un texto un carácter más largo que su columna (valor límite
    // inválido), una columna por caso. Esperado: SQLException y la tabla sin filas nuevas.

    private void assertGuardarRechazado(Asociado asociado) throws SQLException {
        assertThrows(SQLException.class, () -> dao.guardar(asociado));
        assertEquals(20, contarFilas(), "no debía insertarse ninguna fila");
    }

    // CI10 – nombre de 31 caracteres.
    @Test
    void testGuardarRejectsNombreTooLong() throws SQLException {
        assertGuardarRechazado(asociado("N".repeat(31), "Apellido", "40111222", "Calle", 777, "Ciudad", "2230001111"));
    }

    // CI11 – apellido de 31 caracteres.
    @Test
    void testGuardarRejectsApellidoTooLong() throws SQLException {
        assertGuardarRechazado(asociado("Nombre", "A".repeat(31), "40111222", "Calle", 777, "Ciudad", "2230001111"));
    }

    // CI12 – DNI de 11 caracteres.
    @Test
    void testGuardarRejectsDniTooLong() throws SQLException {
        assertGuardarRechazado(asociado("Nombre", "Apellido", "1".repeat(11), "Calle", 777, "Ciudad", "2230001111"));
    }

    // CI13 – calle de 31 caracteres.
    @Test
    void testGuardarRejectsCalleTooLong() throws SQLException {
        assertGuardarRechazado(asociado("Nombre", "Apellido", "40111222", "C".repeat(31), 777, "Ciudad", "2230001111"));
    }

    // CI14 – ciudad de 36 caracteres.
    @Test
    void testGuardarRejectsCiudadTooLong() throws SQLException {
        assertGuardarRechazado(asociado("Nombre", "Apellido", "40111222", "Calle", 777, "M".repeat(36), "2230001111"));
    }

    // CI15 – telefono de 16 caracteres.
    @Test
    void testGuardarRejectsTelefonoTooLong() throws SQLException {
        assertGuardarRechazado(asociado("Nombre", "Apellido", "40111222", "Calle", 777, "Ciudad", "2".repeat(16)));
    }

    // CI16 – guardar con el DNI de la fila semilla 1. Esperado: violación de unicidad
    // (SRS §3.1, el DNI "actúa como identificador único") y la tabla sin filas nuevas.
    @Test
    void testGuardarRejectsDuplicateDni() throws SQLException {
        Asociado repetido = asociado("Nombre", "Apellido", "12345678", "Calle", 777, "Ciudad", "2230001111");

        assertThrows(SQLIntegrityConstraintViolationException.class, () -> dao.guardar(repetido));
        assertEquals(20, contarFilas());
    }

    // CI17 – guardar(null). Esperado: AssertionError (SRS §3.3, "validará que no sea nula") y sin filas nuevas.
    @Test
    void testGuardarRejectsNull() throws SQLException {
        assertThrows(AssertionError.class, () -> dao.guardar(null));
        assertEquals(20, contarFilas());
    }

    // ---------------------------------------------------------------- cargar, cargarConLimite, cargarBD

    // CI18 – cargar(1), primera fila. Esperado: un DTO igual a la fila 1 (SRS §3.3, carga individual).
    @Test
    void testCargarFirstRow() {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(1));
        assertEquals(FILA_1, comoFila(dto));
    }

    // CI19 – cargar(20), última fila. Esperado: un DTO igual a la fila 20.
    @Test
    void testCargarLastRow() throws SQLException {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(20));
        assertEquals(filas("id = 20"), comoFila(dto));
    }

    // CI20 – cargar(21), id inexistente. Esperado (supuesto): devuelve null, sin excepción.
    @Test
    void testCargarMissingIdReturnsNull() {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(21));
        assertNull(dto);
    }

    // CI21 – cargarConLimite(1), valor límite inferior. Esperado: solo la fila 1.
    @Test
    void testCargarConLimiteOne() throws SQLException {
        ArrayList<AsociadoDTO> dtos = dao.cargarConLimite(1);

        assertEquals(1, dtos.size());
        assertEquals(FILA_1, comoFila(dtos.get(0)));
    }

    // CI22 – cargarConLimite(20), límite igual a la cantidad de filas. Esperado: 20 elementos.
    @Test
    void testCargarConLimiteEqualToRows() throws SQLException {
        assertEquals(20, dao.cargarConLimite(20).size());
    }

    // CI23 – cargarConLimite(21), límite mayor a la cantidad de filas. Esperado: 20 elementos.
    @Test
    void testCargarConLimiteGreaterThanRows() throws SQLException {
        assertEquals(20, dao.cargarConLimite(21).size());
    }

    // CI24 – cargarBD(). Esperado: la tabla completa, cada DTO igual a su fila (SRS §3.3, colección completa).
    @Test
    void testCargarBDReturnsWholeTable() throws SQLException {
        StringBuilder cargado = new StringBuilder();
        for (AsociadoDTO dto : dao.cargarBD()) {
            cargado.append(comoFila(dto));
        }
        assertEquals(tablaCompleta(), cargado.toString());
    }

    // ---------------------------------------------------------------- actualizar

    // CI25 – actualizar el asociado de la fila 7 (a propósito no la 1), identificado por su DNI.
    // Esperado: cambia la fila 7 y ninguna otra (SRS §3.3, actualiza "basándose en criterios
    // únicos como el número de documento (DNI)").
    @Test
    void testActualizarChangesOnlyThatDni() throws SQLException {
        String otrasFilas = filas("id <> 7");
        Asociado cambiado = asociado("Laura", "Singleton", "22334455", "Nueva", 99, "Batan", "2230009999");

        assertDoesNotThrow(() -> dao.actualizar(cambiado));

        assertEquals("Laura|Singleton|22334455|Nueva|99|Batan|2230009999\n", filas("id = 7"));
        assertEquals(otrasFilas, filas("id <> 7"), "no debía cambiar ninguna otra fila");
    }

    // CI26 – actualizar un DNI que no está en la tabla. Esperado (supuesto): sin excepción y
    // ninguna fila cambia.
    @Test
    void testActualizarMissingDniChangesNothing() throws SQLException {
        String antes = tablaCompleta();

        assertDoesNotThrow(() -> dao.actualizar(asociadoValido()));

        assertEquals(antes, tablaCompleta());
    }

    // ---------------------------------------------------------------- eliminar

    // CI27 – eliminar un DNI existente. Esperado: 19 filas y ese DNI ya no está (SRS §3.3).
    @Test
    void testEliminarExistingDni() throws SQLException {
        dao.eliminar("22334455");

        assertEquals(19, contarFilas());
        assertEquals("", filas("DNI = '22334455'"));
    }

    // CI28 – eliminar un DNI inexistente. Esperado (supuesto): sin excepción y la tabla intacta.
    @Test
    void testEliminarMissingDniChangesNothing() throws SQLException {
        String antes = tablaCompleta();
        dao.eliminar("99999999");
        assertEquals(antes, tablaCompleta());
    }

    // CI29 – eliminar(""). Esperado (supuesto): AssertionError y la tabla intacta.
    @Test
    void testEliminarRejectsEmptyDni() throws SQLException {
        assertThrows(AssertionError.class, () -> dao.eliminar(""));
        assertEquals(20, contarFilas());
    }

    // CI30 – eliminar(null). Esperado (supuesto): AssertionError y la tabla intacta.
    @Test
    void testEliminarRejectsNullDni() throws SQLException {
        assertThrows(AssertionError.class, () -> dao.eliminar(null));
        assertEquals(20, contarFilas());
    }

    // ---------------------------------------------------------------- errores de mapeo y de SQL

    // CI31 – La tabla tiene una fila con numero = 0 (insertada con SQL propio) y se llama a
    // cargarBD(). Esperado: 21 DTO, el último con numero 0 (SRS §3.1: numero "mayor o igual a 0").
    @Test
    void testCargarBDMapsNumeroZero() throws SQLException {
        ejecutar("INSERT INTO Asociados (nombre, apellido, DNI, calle, numero, ciudad, telefono) "
                + "VALUES ('Cero', 'Numero', '50000000', 'Calle', 0, 'Ciudad', '2230000000')");

        ArrayList<AsociadoDTO> dtos = assertDoesNotThrow(() -> dao.cargarBD());

        assertEquals(21, dtos.size());
        assertEquals(0, dtos.get(20).getNumero());
    }

    // CI32 – La tabla no existe (borrada con SQL propio) y se llama a cargarBD().
    // Esperado: la SQLException llega a quien llama al DAO.
    @Test
    void testCargarBDWithoutTableThrowsSQLException() throws SQLException {
        ejecutar("DROP TABLE Asociados");
        assertThrows(SQLException.class, () -> dao.cargarBD());
    }
}
