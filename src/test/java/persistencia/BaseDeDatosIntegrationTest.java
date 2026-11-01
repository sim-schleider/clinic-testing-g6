package persistencia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static persistencia.BaseDeDatosDePrueba.*;

/**
 * Pruebas de integración de BaseDeDatos contra MySQL real (SRS §3.2): Singleton de la conexión,
 * creación automática del esquema y de la tabla, y datos semilla. Requiere MySQL levantado con el
 * usuario de docker-compose.yml.
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
    }

    // CI2 – Se borra el esquema y se crea el Singleton. Esperado: iniciarBD() vuelve a crear el
    // esquema Grupo_7 (SRS §3.2, "verificar o crear el esquema de base de datos objetivo").
    @Test
    void testIniciarBDCreatesSchema() throws Exception {
        ejecutar("DROP DATABASE IF EXISTS Grupo_7");
        reiniciarBaseDeDatos();

        assertEquals("1", valor("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'Grupo_7'"));
    }

    // CI3 – Inicialización. Esperado: la tabla Asociados tiene una columna por cada atributo del
    // asociado (SRS §3.1) más el id. Se comparan en orden alfabético: el orden no está especificado.
    @Test
    void testIniciarBDCreatesTableWithColumns() throws SQLException {
        String columnas = valor("SELECT GROUP_CONCAT(LOWER(column_name) ORDER BY column_name SEPARATOR ' ') "
                + "FROM information_schema.columns WHERE table_schema = 'Grupo_7' AND table_name = 'Asociados'");

        assertEquals("apellido calle ciudad dni id nombre numero telefono", columnas);
    }

    // CI4 – Datos semilla (SRS §3.2, "poblarla con registros iniciales de prueba"). Esperado: 20 filas
    // (la cantidad la fija el script de iniciarBD, no la SRS), todas cumpliendo las reglas de
    // SRS §3.1: campos no nulos, numero >= 0 y DNI distintos.
    @Test
    void testIniciarBDLoadsValidSeedData() throws SQLException {
        assertEquals(20, contarFilas(), "cantidad de datos semilla");
        assertEquals("", filas("nombre IS NULL OR apellido IS NULL OR DNI IS NULL OR calle IS NULL "
                + "OR ciudad IS NULL OR telefono IS NULL OR numero < 0"), "filas semilla que violan la SRS");
        assertEquals("20", valor("SELECT COUNT(DISTINCT DNI) FROM Asociados"), "los DNI debían ser distintos");
    }

    // CI5 – iniciarBD() con una fila extra en la tabla. Esperado: la tabla se recrea y vuelve a
    // tener solo los datos semilla (SRS §3.2, "eliminar y recrear la tabla Asociados").
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
