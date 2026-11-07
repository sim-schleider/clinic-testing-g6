package item2.support;

import persistencia.AsociadoDTO;
import persistencia.BaseDeDatos;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utilidades de las pruebas de integración. Todo lo que verifica la base usa una conexión propia
 * del test y SQL propio, nunca el DAO bajo prueba: si el DAO guardara y leyera un campo cruzado,
 * una ida y vuelta con el mismo DAO no lo detectaría.
 */
public final class DatabaseTestSupport {

    private static final String URL = "jdbc:mysql://localhost:3306/Grupo_7?useSSL=false&serverTimezone=UTC";

    private DatabaseTestSupport() {
    }

    /**
     * Descarta el Singleton (por reflexión, sin tocar el código) y crea uno nuevo. Al crearse,
     * BaseDeDatos ejecuta iniciarBD(): cada test empieza con la tabla recreada y los 20 datos semilla.
     */
    public static BaseDeDatos resetDatabase() throws Exception {
        Field instanceField = BaseDeDatos.class.getDeclaredField("instancia");
        instanceField.setAccessible(true);
        BaseDeDatos previous = (BaseDeDatos) instanceField.get(null);
        if (previous != null) {
            previous.cerrarConexion();
        }
        instanceField.set(null, null);
        return BaseDeDatos.getInstancia();
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(URL, "progra_c", "progra_c");
    }

    public static void execute(String sql) throws SQLException {
        try (Connection c = openConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    /** La primera celda del resultado de una consulta (por ejemplo, un COUNT). */
    public static String queryValue(String sql) throws SQLException {
        try (Connection c = openConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    public static int countRows() throws SQLException {
        return Integer.parseInt(queryValue("SELECT COUNT(*) FROM Asociados"));
    }

    /**
     * Las filas que cumplen la condición, ordenadas por id, una por línea con el formato
     * "nombre|apellido|dni|calle|numero|ciudad|telefono". Vacío si no hay ninguna.
     */
    public static String rowsWhere(String where) throws SQLException {
        StringBuilder text = new StringBuilder();
        try (Connection c = openConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Asociados WHERE " + where + " ORDER BY id")) {
            while (rs.next()) {
                text.append(rs.getString("nombre")).append('|').append(rs.getString("apellido")).append('|')
                        .append(rs.getString("DNI")).append('|').append(rs.getString("calle")).append('|')
                        .append(rs.getInt("numero")).append('|').append(rs.getString("ciudad")).append('|')
                        .append(rs.getString("telefono")).append('\n');
            }
        }
        return text.toString();
    }

    /** Toda la tabla: sirve para comprobar que una operación no cambió nada. */
    public static String wholeTable() throws SQLException {
        return rowsWhere("TRUE");
    }

    /** El DTO en el mismo formato que rowsWhere(), para compararlos. */
    public static String asRow(AsociadoDTO dto) {
        return dto.getNombre() + "|" + dto.getApellido() + "|" + dto.getDni() + "|" + dto.getCalle() + "|"
                + dto.getNumero() + "|" + dto.getCiudad() + "|" + dto.getTelefono() + "\n";
    }
}
