package item2;

import item2.support.DatabaseTestSupport;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;
import persistencia.AsociadoDTO;
import persistencia.DataAccessObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración AsociadoDTO → DataAccessObject → BaseDeDatos → MySQL.
 * Cada escritura se verifica con SQL propio (DatabaseTestSupport), nunca leyendo con el mismo DAO.
 * Supuesto: las cargas devuelven las filas en el orden del id (la SRS no define un orden).
 */
@Tag("integration")
@DisplayName("DataAccessObject con MySQL – integración (ítem 2)")
class DataAccessObjectIntegrationTest {

    private DataAccessObject dao;

    /** Cada test parte de la tabla recreada con los 20 datos semilla. */
    @BeforeEach
    void setUpDao() throws Exception {
        DatabaseTestSupport.resetDatabase();
        dao = new DataAccessObject();
    }

    /** Crea un Asociado; los argumentos van en el orden de las columnas de la tabla. */
    private static Asociado asociado(String nombre, String apellido, String dni, String calle,
                                     int numero, String ciudad, String telefono) {
        return new Asociado(nombre, apellido, dni, telefono, new Domicilio(calle, numero, ciudad));
    }

    /** Asociado válido con un valor distinto en cada campo, para detectar columnas cruzadas. */
    private static Asociado asociadoValido() {
        return asociado("Valentino", "Hernan", "40111222", "Calle", 777, "Ciudad", "2230001111");
    }

    // ---------------------------------------------------------------- guardar

    // CI7 – guardar con datos válidos. Esperado: una fila nueva con cada dato en su columna.
    @Test
    @DisplayName("CI7 – guardar inserta cada dato en su columna")
    void ci7_guardarInsertsEveryColumn() throws SQLException {
        dao.guardar(asociadoValido());

        assertEquals(21, DatabaseTestSupport.countRows());
        assertEquals("Valentino|Hernan|40111222|Calle|777|Ciudad|2230001111\n",
                DatabaseTestSupport.rowsWhere("DNI = '40111222'"));
    }

    // CI8 – guardar con el DNI de la fila semilla 1. Esperado: violación de unicidad
    // (el DNI "actúa como identificador único") y la tabla sin filas nuevas.
    @Test
    @DisplayName("CI8 – guardar con un DNI repetido lanza una violación de unicidad")
    void ci8_guardarRejectsDuplicateDni() throws SQLException {
        Asociado repetido = asociado("Valentino", "Hernan", "12345678", "Calle", 777, "Ciudad", "2230001111");

        assertThrows(SQLIntegrityConstraintViolationException.class, () -> dao.guardar(repetido));
        assertEquals(20, DatabaseTestSupport.countRows());
    }

    // ---------------------------------------------------------------- cargar, cargarConLimite, cargarBD

    // CI9 – cargar(1), primera fila. Esperado: un DTO igual a la fila 1 (carga individual).
    @Test
    @DisplayName("CI9 – cargar(1) devuelve la primera fila")
    void ci9_cargarFirstRow() throws SQLException {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(1));
        assertEquals(DatabaseTestSupport.rowsWhere("id = 1"), DatabaseTestSupport.asRow(dto));
    }

    // CI10 – cargar(20), última fila. Esperado: un DTO igual a la fila 20.
    @Test
    @DisplayName("CI10 – cargar(20) devuelve la última fila")
    void ci10_cargarLastRow() throws SQLException {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(20));
        assertEquals(DatabaseTestSupport.rowsWhere("id = 20"), DatabaseTestSupport.asRow(dto));
    }

    // CI11 – cargar(21), id inexistente. Esperado (supuesto): devuelve null, sin excepción.
    @Test
    @DisplayName("CI11 – cargar(21) con un id inexistente devuelve null")
    void ci11_cargarMissingIdReturnsNull() {
        AsociadoDTO dto = assertDoesNotThrow(() -> dao.cargar(21));
        assertNull(dto);
    }

    // CI12 – cargarConLimite(1), valor límite inferior. Esperado: solo la fila 1.
    @Test
    @DisplayName("CI12 – cargarConLimite(1) devuelve solo la fila 1")
    void ci12_cargarConLimiteOne() throws SQLException {
        ArrayList<AsociadoDTO> dtos = dao.cargarConLimite(1);

        assertEquals(1, dtos.size());
        assertEquals(DatabaseTestSupport.rowsWhere("id = 1"), DatabaseTestSupport.asRow(dtos.get(0)));
    }

    // CI13 – cargarConLimite(20), límite igual a la cantidad de filas. Esperado: 20 elementos.
    @Test
    @DisplayName("CI13 – cargarConLimite(20) devuelve las 20 filas")
    void ci13_cargarConLimiteEqualToRows() throws SQLException {
        assertEquals(20, dao.cargarConLimite(20).size());
    }

    // CI14 – cargarConLimite(21), límite mayor a la cantidad de filas. Esperado: 20 elementos.
    @Test
    @DisplayName("CI14 – cargarConLimite(21) devuelve las 20 filas")
    void ci14_cargarConLimiteGreaterThanRows() throws SQLException {
        assertEquals(20, dao.cargarConLimite(21).size());
    }

    // CI15 – cargarBD(). Esperado: la tabla completa, cada DTO igual a su fila (colección completa).
    @Test
    @DisplayName("CI15 – cargarBD() devuelve la tabla completa")
    void ci15_cargarBDReturnsWholeTable() throws SQLException {
        StringBuilder cargado = new StringBuilder();
        for (AsociadoDTO dto : dao.cargarBD()) {
            cargado.append(DatabaseTestSupport.asRow(dto));
        }
        assertEquals(DatabaseTestSupport.wholeTable(), cargado.toString());
    }

    // ---------------------------------------------------------------- actualizar

    // CI16 – actualizar el asociado de la fila 7 (a propósito no la 1), identificado por su DNI.
    // Esperado: cambia la fila 7 y ninguna otra (actualiza "basándose en criterios
    // únicos como el número de documento (DNI)").
    @Test
    @DisplayName("CI16 – actualizar cambia solo la fila del DNI")
    void ci16_actualizarChangesOnlyThatDni() throws SQLException {
        String otrasFilas = DatabaseTestSupport.rowsWhere("id <> 7");
        Asociado cambiado = asociado("Laura", "Singleton", "22334455", "Nueva", 99, "Batan", "2230009999");

        assertDoesNotThrow(() -> dao.actualizar(cambiado));

        assertEquals("Laura|Singleton|22334455|Nueva|99|Batan|2230009999\n", DatabaseTestSupport.rowsWhere("id = 7"));
        assertEquals(otrasFilas, DatabaseTestSupport.rowsWhere("id <> 7"), "no debía cambiar ninguna otra fila");
    }

    // CI17 – actualizar un DNI que no está en la tabla. Esperado (supuesto): sin excepción y
    // ninguna fila cambia.
    @Test
    @DisplayName("CI17 – actualizar un DNI inexistente no cambia ninguna fila")
    void ci17_actualizarMissingDniChangesNothing() throws SQLException {
        String antes = DatabaseTestSupport.wholeTable();

        assertDoesNotThrow(() -> dao.actualizar(asociadoValido()));

        assertEquals(antes, DatabaseTestSupport.wholeTable());
    }

    // ---------------------------------------------------------------- eliminar

    // CI18 – eliminar un DNI existente. Esperado: 19 filas y ese DNI ya no está.
    @Test
    @DisplayName("CI18 – eliminar un DNI existente borra solo esa fila")
    void ci18_eliminarExistingDni() throws SQLException {
        dao.eliminar("22334455");

        assertEquals(19, DatabaseTestSupport.countRows());
        assertEquals("", DatabaseTestSupport.rowsWhere("DNI = '22334455'"));
    }

    // CI19 – eliminar un DNI inexistente. Esperado (supuesto): sin excepción y la tabla intacta.
    @Test
    @DisplayName("CI19 – eliminar un DNI inexistente deja la tabla intacta")
    void ci19_eliminarMissingDniChangesNothing() throws SQLException {
        String antes = DatabaseTestSupport.wholeTable();
        dao.eliminar("99999999");
        assertEquals(antes, DatabaseTestSupport.wholeTable());
    }

    // CI20 – eliminar con un DNI que es un intento de inyección SQL. Esperado: el texto se usa como
    // valor del parámetro y no como parte de la consulta, así que no coincide con ningún DNI y la
    // tabla queda intacta (consultas parametrizadas "para prevenir vulnerabilidades de inyección SQL").
    @Test
    @DisplayName("CI20 – eliminar con SQL inyectado en el DNI deja la tabla intacta")
    void ci20_eliminarTreatsSqlInjectionAsPlainValue() throws SQLException {
        String antes = DatabaseTestSupport.wholeTable();
        dao.eliminar("' OR '1'='1");
        assertEquals(antes, DatabaseTestSupport.wholeTable());
    }

    // ---------------------------------------------------------------- errores de mapeo y de SQL

    // CI21 – La tabla tiene una fila con numero = 0 (insertada con SQL propio) y se llama a
    // cargarBD(). Esperado: 21 DTO, el último con numero 0 (numero "mayor o igual a 0").
    @Test
    @DisplayName("CI21 – cargarBD() mapea una fila con numero = 0")
    void ci21_cargarBDMapsNumeroZero() throws SQLException {
        DatabaseTestSupport.execute("INSERT INTO Asociados (nombre, apellido, DNI, calle, numero, ciudad, telefono) "
                + "VALUES ('Simon', 'Schleider', '50000000', 'Calle', 0, 'Ciudad', '2230000000')");

        ArrayList<AsociadoDTO> dtos = assertDoesNotThrow(() -> dao.cargarBD());

        assertEquals(21, dtos.size());
        assertEquals(0, dtos.get(20).getNumero());
    }

    // CI22 – La tabla no existe (borrada con SQL propio) y se llama a cargarBD().
    // Esperado: la SQLException llega a quien llama al DAO (Javadoc: "@throws SQLException Si ocurre
    // un error al interactuar con la base de datos").
    @Test
    @DisplayName("CI22 – cargarBD() sin la tabla lanza SQLException")
    void ci22_cargarBDWithoutTableThrowsSQLException() throws SQLException {
        DatabaseTestSupport.execute("DROP TABLE Asociados");
        assertThrows(SQLException.class, () -> dao.cargarBD());
    }
}
