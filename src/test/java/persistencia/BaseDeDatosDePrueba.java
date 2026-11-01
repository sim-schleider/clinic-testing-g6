package persistencia;

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
final class BaseDeDatosDePrueba {

    private static final String URL = "jdbc:mysql://localhost:3306/Grupo_7?useSSL=false&serverTimezone=UTC";

    private BaseDeDatosDePrueba() {
    }

    /**
     * Descarta el Singleton (por reflexión, sin tocar el código) y crea uno nuevo. Al crearse,
     * BaseDeDatos ejecuta iniciarBD(): cada test empieza con la tabla recreada y los 20 datos semilla.
     */
    static BaseDeDatos reiniciarBaseDeDatos() throws Exception {
        Field instancia = BaseDeDatos.class.getDeclaredField("instancia");
        instancia.setAccessible(true);
        BaseDeDatos anterior = (BaseDeDatos) instancia.get(null);
        if (anterior != null) {
            anterior.cerrarConexion();
        }
        instancia.set(null, null);
        return BaseDeDatos.getInstancia();
    }

    static Connection conexionPropia() throws SQLException {
        return DriverManager.getConnection(URL, "progra_c", "progra_c");
    }

    static void ejecutar(String sql) throws SQLException {
        try (Connection c = conexionPropia(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    static int contarFilas() throws SQLException {
        try (Connection c = conexionPropia(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Asociados")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Las filas que cumplen la condición, ordenadas por id, una por línea con el formato
     * "nombre|apellido|dni|calle|numero|ciudad|telefono". Vacío si no hay ninguna.
     */
    static String filas(String where) throws SQLException {
        StringBuilder texto = new StringBuilder();
        try (Connection c = conexionPropia(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Asociados WHERE " + where + " ORDER BY id")) {
            while (rs.next()) {
                texto.append(rs.getString("nombre")).append('|').append(rs.getString("apellido")).append('|')
                        .append(rs.getString("DNI")).append('|').append(rs.getString("calle")).append('|')
                        .append(rs.getInt("numero")).append('|').append(rs.getString("ciudad")).append('|')
                        .append(rs.getString("telefono")).append('\n');
            }
        }
        return texto.toString();
    }

    /** Toda la tabla: sirve para comprobar que una operación no cambió nada. */
    static String tablaCompleta() throws SQLException {
        return filas("TRUE");
    }

    /** El DTO en el mismo formato que filas(), para compararlos. */
    static String comoFila(AsociadoDTO dto) {
        return dto.getNombre() + "|" + dto.getApellido() + "|" + dto.getDni() + "|" + dto.getCalle() + "|"
                + dto.getNumero() + "|" + dto.getCiudad() + "|" + dto.getTelefono() + "\n";
    }
}
