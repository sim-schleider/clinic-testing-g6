package persistencia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static persistencia.BaseDeDatosDePrueba.*;

/**
 * Pruebas de integración de BaseDeDatos contra MySQL real (SRS §3.2): Singleton de la conexión,
 * creación automática del esquema y de la tabla, y datos semilla. Requiere el contenedor MySQL de
 * docker-compose.yml levantado.
 */
@Tag("integration")
class BaseDeDatosIntegrationTest {

    private BaseDeDatos bd;

    /** Cada test parte de un Singleton recién creado: tabla recreada y 20 datos semilla. */
    @BeforeEach
    void setUpBaseDeDatos() throws Exception {
        bd = reiniciarBaseDeDatos();
    }

    // CI1 – Dos llamadas a getInstancia(). Esperado: la misma instancia (SRS §3.2, Singleton).
    @Test
    void testGetInstanciaReturnsSameInstance() {
        assertSame(bd, BaseDeDatos.getInstancia());
        assertSame(BaseDeDatos.getInstancia(), BaseDeDatos.getInstancia());
    }

    // CI2 – Inicialización. Esperado: existe el esquema Grupo_7 (SRS §3.2, "verificar o crear el esquema").
    @Test
    void testIniciarBDCreatesSchema() throws SQLException {
        try (Connection c = conexionPropia(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'Grupo_7'")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "el esquema Grupo_7 debía existir");
        }
    }

    // CI3 – Inicialización. Esperado: la tabla Asociados existe con una columna por cada
    // atributo del asociado (SRS §3.1 y §3.2) más el id.
    @Test
    void testIniciarBDCreatesTableWithColumns() throws SQLException {
        StringBuilder columnas = new StringBuilder();
        try (Connection c = conexionPropia(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT column_name FROM information_schema.columns "
                     + "WHERE table_schema = 'Grupo_7' AND table_name = 'Asociados' ORDER BY ordinal_position")) {
            while (rs.next()) {
                columnas.append(rs.getString(1).toLowerCase()).append(' ');
            }
        }
        assertEquals("id nombre apellido dni calle numero ciudad telefono ", columnas.toString());
    }

    // CI4 – Datos semilla. Esperado: 20 filas, todas con los campos no nulos, numero >= 0 y
    // DNI distintos (SRS §3.1: las reglas del asociado; SRS §3.2: "poblarla con registros iniciales").
    @Test
    void testIniciarBDLoadsValidSeedData() throws SQLException {
        assertEquals(20, contarFilas(), "cantidad de datos semilla");
        assertEquals("", filas("nombre IS NULL OR apellido IS NULL OR DNI IS NULL OR calle IS NULL "
                + "OR ciudad IS NULL OR telefono IS NULL OR numero < 0"), "filas semilla que violan la SRS");
        try (Connection c = conexionPropia(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT DNI) FROM Asociados")) {
            rs.next();
            assertEquals(20, rs.getInt(1), "los 20 DNI debían ser distintos");
        }
    }

    // CI5 – iniciarBD() con una fila extra en la tabla. Esperado: la tabla se recrea y vuelve a
    // tener exactamente los 20 datos semilla (SRS §3.2, "eliminar y recrear la tabla").
    @Test
    void testIniciarBDRecreatesTable() throws SQLException {
        ejecutar("INSERT INTO Asociados (nombre, apellido, DNI, calle, numero, ciudad, telefono) "
                + "VALUES ('Extra', 'Extra', '99999999', 'Calle', 1, 'Ciudad', '1')");
        assertEquals(21, contarFilas(), "preparación: la fila extra debía insertarse");

        bd.iniciarBD();

        assertEquals(20, contarFilas());
        assertEquals("", filas("DNI = '99999999'"), "la fila extra debía desaparecer");
    }

    // CI6 – cerrarConexion() y luego getInstancia(). Esperado (supuesto): el Singleton sigue
    // entregando un manejador de conexión activo (SRS §3.2, "un único manejador de conexión activo").
    @Test
    void testGetInstanciaAfterCerrarConexionGivesActiveConnection() throws SQLException {
        bd.cerrarConexion();
        assertTrue(bd.getConexion().isClosed(), "preparación: la conexión debía quedar cerrada");

        Connection conexion = BaseDeDatos.getInstancia().getConexion();

        assertFalse(conexion.isClosed(), "getInstancia() devolvió un manejador con la conexión cerrada");
    }

    // CI7 – Insertar con SQL propio un asociado sin DNI. Esperado (supuesto): la tabla lo rechaza,
    // porque el DNI "no puede ser nulo; actúa como identificador único" (SRS §3.1).
    @Test
    void testSchemaRejectsNullDni() {
        assertThrows(SQLException.class, () -> ejecutar(
                "INSERT INTO Asociados (nombre, apellido, calle, numero, ciudad, telefono) "
                        + "VALUES ('Sin', 'Dni', 'Calle', 1, 'Ciudad', '1')"),
                "la tabla aceptó un asociado con DNI nulo");
    }
}
