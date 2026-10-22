# JDBC patterns for integration tests

Java 17, JUnit 5.10, `mysql-connector-j` 8.3. Imports used below:

```java
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
```

## 1. Independent verification connection

A separate `Connection`, opened by the test and **not** obtained from `BaseDeDatos`, is the
oracle channel. Use the same coordinates as the code under test (they are hardcoded there), but
select the database explicitly.

```java
final class BdDirecta {
    static final String URL = "jdbc:mysql://localhost:3306/Grupo_7?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    static final String USUARIO = "progra_c";
    static final String CLAVE = "progra_c";

    static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    static int contarFilas() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Asociados")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Reads one row by DNI, column by column, without going through the DAO. */
    static Fila buscarPorDni(String dni) throws SQLException {
        String sql = "SELECT id, nombre, apellido, DNI, calle, numero, ciudad, telefono FROM Asociados WHERE DNI = ?";
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Fila(rs.getInt("id"), rs.getString("nombre"), rs.getString("apellido"),
                        rs.getString("DNI"), rs.getString("calle"), rs.getInt("numero"),
                        rs.getString("ciudad"), rs.getString("telefono"));
            }
        }
    }

    record Fila(int id, String nombre, String apellido, String dni, String calle, int numero,
                String ciudad, String telefono) {}
}
```

`allowPublicKeyRetrieval=true` only goes into the **test's** URL. The production URL stays as it
is; if production cannot connect, fix the environment (§7).

## 2. Known state before each test

`iniciarBD()` is public, and per the SRS it drops and recreates the table with the seed data. Use
it to reset state, and test it on its own in a dedicated case verified through `BdDirecta`:

```java
@BeforeEach
void restaurarSemilla() {
    BaseDeDatos.getInstancia().iniciarBD();
}
```

If `iniciarBD` itself is broken, every test fails at once. Look at the dedicated `iniciarBD` case
first when reading the results.

## 3. Values that expose swapped columns

Use a different, recognizable value per column, and lengths at the boundaries:

```java
// Asociado(nombre, apellido, dni, telefono, Domicilio(calle, numero, ciudad)): mind the order.
Asociado nuevo = new Asociado("NombrePrueba", "ApellidoPrueba", "40111222", "2234000001",
        new Domicilio("CallePrueba", 4321, "CiudadPrueba"));
dao.guardar(nuevo);

BdDirecta.Fila fila = BdDirecta.buscarPorDni("40111222");
assertNotNull(fila, "el registro debe existir");
assertAll(
    () -> assertEquals("NombrePrueba", fila.nombre()),
    () -> assertEquals("ApellidoPrueba", fila.apellido()),
    () -> assertEquals("CallePrueba", fila.calle()),
    () -> assertEquals(4321, fila.numero()),
    () -> assertEquals("CiudadPrueba", fila.ciudad()),
    () -> assertEquals("2234000001", fila.telefono()));
```

Strings of an exact length: `"x".repeat(30)`, `"x".repeat(31)`.

## 4. Exceptions

```java
@Test
@Timeout(value = 10, unit = TimeUnit.SECONDS)
@DisplayName("CI7 – guardar con DNI repetido lanza violación de unicidad y no inserta")
void ci7_guardarDniRepetido() throws SQLException {
    Asociado repetido = new Asociado("Otro", "Nombre", "12345678", "2235000000",
            new Domicilio("Calle", 10, "Mar del Plata"));
    assertThrows(SQLIntegrityConstraintViolationException.class, () -> dao.guardar(repetido));
    assertEquals(20, BdDirecta.contarFilas());
}
```

Useful types:
- `SQLIntegrityConstraintViolationException`: unique or not-null violation.
- `com.mysql.cj.jdbc.exceptions.MysqlDataTruncation`: "Data too long", raised in MySQL 8's
  default strict mode.
- plain `SQLException`: anything else, for example reading a `ResultSet` before `next()`.

An exception of an **unexpected type** (an NPE where the contract promises `SQLException`) is a
finding about error handling. Assert the documented type, and let the red show the difference.

## 5. The Singleton

```java
@Test
@DisplayName("CI1 – getInstancia devuelve siempre la misma instancia")
void ci1_singletonMismaInstancia() {
    assertSame(BaseDeDatos.getInstancia(), BaseDeDatos.getInstancia());
}
```

Behaviour after `cerrarConexion()` is a legitimate case: is the connection still usable, and does
`getInstancia()` recover? Since the Singleton outlives the test, restore it afterwards by clearing
the static field. This is a test-only intervention; document it in the section.

```java
@AfterEach
void reiniciarSingleton() throws Exception {
    var campo = BaseDeDatos.class.getDeclaredField("instancia");
    campo.setAccessible(true);
    BaseDeDatos actual = (BaseDeDatos) campo.get(null);
    if (actual != null && actual.getConexion() != null && !actual.getConexion().isClosed()) {
        actual.cerrarConexion();
    }
    campo.set(null, null);
}
```

Put this `@AfterEach` only in the class that closes connections, so the other classes keep
sharing the instance as production does. Singleton thread-safety (two threads calling
`getInstancia()` at once) is a concurrency case; mention it and leave it to
`concurrency-testing`, or add it here with a `CountDownLatch`.

## 6. "Server down" run

The URL is hardcoded, so "server unreachable" can only be provoked from outside. Keep those cases
in their own class, disabled by default:

```java
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@Tag("integration")
@EnabledIfSystemProperty(named = "bd.caida", matches = "true")
class BaseDeDatosSinServidorIntegrationTest {
    @Test
    @Timeout(30)
    @DisplayName("CI20 – sin servidor, getInstancia informa el error de conexión")
    void ci20_sinServidor() { /* expected per contract: SQLException surfaced / clear failure */ }
}
```

Run them like this:

```bash
docker compose stop mysql
.skills/test-report/scripts/run_tests.sh integration dev -- -Dbd.caida=true -Dtest=BaseDeDatosSinServidorIntegrationTest
docker compose start mysql
```

## 7. Environment problems (not defects)

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Communications link failure` | MySQL not up or not reachable from the Maven container | `docker compose up -d --wait mysql`. The `maven` service uses `network_mode: host`; if Docker Desktop doesn't route it, use `network_mode: "service:mysql"` |
| `Access denied for user 'progra_c'` | Container created with other credentials | `docker compose down -v` and start again (credentials come from `docker-compose.yml`) |
| `Public Key Retrieval is not allowed` | MySQL 8 `caching_sha2_password` with `useSSL=false` | Add `command: --default-authentication-plugin=mysql_native_password` to the `mysql` service, then `docker compose down -v && docker compose up -d mysql` |
| `Unknown database 'Grupo_7'` in `BdDirecta` | `iniciarBD` didn't run | Call `BaseDeDatos.getInstancia()` before using `BdDirecta` |

Change the **environment** (compose file, container), never the URL or credentials in
`BaseDeDatos`.
