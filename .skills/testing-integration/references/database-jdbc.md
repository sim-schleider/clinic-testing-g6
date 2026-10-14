# Integración con base de datos y archivos

Secciones: §1 preparar · §2 estado conocido · §3 Singleton · §4 CRUD con verificación
independiente · §5 mapeo, consultas con parámetros y excepciones · §6 BD inaccesible ·
§7 archivos (clase 07).

## §1. Preparar

1. En el código, buscar la URL, el usuario y la clave (`grep -rn "jdbc:" src/main/java`) y
   los nombres de **esquema y tablas** (en los `CREATE` y los `SELECT`).
2. `scripts/mysql.sh up --usuario <usuario> --clave <clave>` (crea el usuario con
   privilegios globales, porque el código suele hacer `CREATE DATABASE`).
3. Ejecutar siempre con `scripts/mvn.sh --mysql tpe-mysql -- …`.

En la clase de test, las constantes de conexión **se duplican** con un comentario
`// deben coincidir con <ClaseConexion>`. No se cambian las del código.

## §2. Estado conocido en cada test

```java
private static final String URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC"; // = BaseDeDatos
private static final String USUARIO = "progra_c";                                                  // = BaseDeDatos
private static final String CLAVE = "progra_c";                                                    // = BaseDeDatos

private Connection jdbcPropio() throws SQLException
{
    return DriverManager.getConnection(URL, USUARIO, CLAVE);
}

private void sql(String sentencia) throws SQLException
{
    try (Connection c = jdbcPropio(); Statement s = c.createStatement())
    {
        s.execute(sentencia);
    }
}

@Before
public void estadoConocido() throws SQLException
{
    // Opción A: si la inicialización del código NO es lo que se prueba, reusarla.
    // Opción B (independiente): reconstruir con SQL propio.
    sql("DROP DATABASE IF EXISTS <esquema>");
    // … y llamar a la rutina de inicialización del código si el caso la necesita.
}
```

- Si lo que se prueba es **la inicialización** (crear el esquema y las semillas), el
  `@Before` **borra** el esquema con SQL propio y el test llama a la rutina del código.
  Después se verifica con SQL propio.
- **Cantidad de semillas:** el valor esperado sale de la especificación. Si la SRS no lo
  dice, se cuentan los `INSERT` de la rutina **y se anota como supuesto**.

## §3. El Singleton de conexión

```java
@Test
public void ci1_singletonDevuelveSiempreLaMismaInstancia()
{
    BaseDeDatos a = BaseDeDatos.getInstancia();
    BaseDeDatos b = BaseDeDatos.getInstancia();
    assertSame("getInstancia debe devolver siempre la misma instancia", a, b);
    assertNotNull("la conexión no debe ser null", a.getConexion());
}
```

**Trampa: el Singleton vive mientras viva la JVM.** Todos los tests de la misma ejecución
comparten la instancia y su conexión:

- Un test que llama a `cerrarConexion()` rompe los siguientes. Hay que verificar si
  `getInstancia()` la reabre (eso también es un caso) o aislarlo.
- Para aislar: `scripts/mvn.sh -- -DreuseForks=false -Dtest=… test`, que corre cada clase de
  test en una JVM nueva. Si hace falta reiniciar dentro de una clase, se puede resetear el
  campo `static` por reflexión **en el test** (es el último recurso; verificar el nombre del
  campo en el código y avisarle al usuario):

```java
@After
public void reiniciarSingleton() throws Exception
{
    java.lang.reflect.Field campo = BaseDeDatos.class.getDeclaredField("instancia"); // nombre real del campo
    campo.setAccessible(true);
    campo.set(null, null);
}
```

## §4. CRUD con verificación independiente

**Regla:** lo que escribe el componente bajo prueba **se verifica con JDBC propio**, no
con el mismo componente. Si el `INSERT` y el `SELECT` del DAO tienen las columnas cruzadas
de la misma manera, guardar y leer con el DAO "anda" igual.

```java
private Map<String, Object> filaPorDni(String dni) throws SQLException
{
    try (Connection c = jdbcPropio();
         PreparedStatement ps = c.prepareStatement("SELECT * FROM <esquema>.<tabla> WHERE dni = ?"))
    {
        ps.setString(1, dni);
        try (ResultSet rs = ps.executeQuery())
        {
            if (!rs.next())
            {
                return null;
            }
            Map<String, Object> fila = new HashMap<>();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++)
            {
                fila.put(md.getColumnLabel(i).toLowerCase(), rs.getObject(i));
            }
            return fila;
        }
    }
}

@Test(timeout = 10000)
public void ci4_guardarPersisteCadaCampoEnSuColumna() throws Exception
{
    // valores DISTINTOS por campo para detectar columnas cruzadas
    dao.guardar(asociadoCon("Nom", "Ape", "30111222", "Ciu", "Cal", 77, "Tel"));
    Map<String, Object> fila = filaPorDni("30111222");
    assertNotNull("debería existir la fila con DNI 30111222", fila);
    assertEquals("columna nombre", "Nom", fila.get("nombre"));
    assertEquals("columna calle", "Cal", fila.get("calle"));
    assertEquals("columna ciudad", "Ciu", fila.get("ciudad"));
    assertEquals("columna numero", 77, ((Number) fila.get("numero")).intValue());
    // … el resto de las columnas
}
```

Ciclo completo: **insertar → verificar con JDBC → cargar con el DAO y comparar → actualizar →
verificar → eliminar → verificar que no está**. Cada paso es un caso (o una prueba con propósito).

**Cargas con límite:** probar el límite 0, 1, N exacto y mayor que el total (valores
límite), y el comportamiento con la tabla vacía.

## §5. Mapeo, consultas con parámetros y excepciones

```java
@Test(timeout = 10000)
public void ci7_valorConComillasSeGuardaLiteral() throws Exception
{
    String malicioso = "x' OR '1'='1";
    dao.guardar(asociadoCon(malicioso, "Ape", "30999888", "Ciu", "Cal", 1, "Tel"));
    assertEquals("con PreparedStatement el valor se guarda tal cual",
                 malicioso, filaPorDni("30999888").get("nombre"));
}

@Test(timeout = 10000)
public void ci8_dniDuplicadoRechazado() throws Exception
{
    dao.guardar(asociadoCon("A", "B", "30555444", "C", "D", 1, "T"));
    try
    {
        dao.guardar(asociadoCon("Otro", "B", "30555444", "C", "D", 1, "T"));
        fail("SRS: el DNI es identificador único; el segundo guardado debería fallar");
    }
    catch (SQLException e)
    {
        // opcional: assertTrue(e instanceof SQLIntegrityConstraintViolationException);
    }
}
```

- **Si el esquema no tiene la restricción** que exige la SRS (por ejemplo, `UNIQUE` en el
  DNI), el caso queda en rojo: es un hallazgo de integridad de datos (§6 de la técnica).
- **Si el código traga la `SQLException`** (`printStackTrace()` y sigue), el efecto
  observable es otro (un `null`, un NPE más adelante). Hay que documentarlo como hallazgo y
  no "adaptar" el resultado esperado.
- Guardar `null` cuando la SRS dice "validará que no sea nula": el esperado es el rechazo
  con el mecanismo del contrato (`AssertionError` con `-ea`, o la excepción documentada).
- **Para provocar un error de SQL del servidor**, la consulta de `executeQuery` tiene que empezar
  con `SELECT` (por ejemplo, `SELECT * FORM Tabla`). Si no, Connector/J la rechaza **del lado del
  cliente** con otro mensaje ("cannot issue statements that do not produce result sets"), y un
  test que verifique `SQLSyntaxErrorException` falla por la razón equivocada.

## §6. BD inaccesible (ejecución aparte)

El test no puede bloquear la BD por sí mismo (corre dentro del contenedor de Maven). Se
marca con una **suposición** y se ejecuta con el usuario bloqueado:

```java
@Test(timeout = 15000)
public void ci9_bdInaccesibleInformaError()
{
    Assume.assumeTrue("Solo con -Dbd.estado=inaccesible (mysql.sh lock)",
                      "inaccesible".equals(System.getProperty("bd.estado")));
    // ejercitar la operación y verificar el resultado esperado según la SRS (SQLException, mensaje…)
}
```

```bash
scripts/mysql.sh lock --usuario <usuario>
scripts/mvn.sh --mysql tpe-mysql -- -Dbd.estado=inaccesible -DreuseForks=false -Dtest='<Clase>IntegracionTest#ci9*' clean test
scripts/mysql.sh unlock --usuario <usuario>
python3 -I scripts/surefire_summary.py --clase '<Clase>IntegracionTest' --titulo "Integración: BD inaccesible" \
        --salida docs/pruebas/resultados/<fecha>-integracion-bd-inaccesible.md
```

En las ejecuciones normales el test sale como **skipped** (⏭ en el resumen). En el diseño
se aclara que se ejecuta aparte.

## §7. Archivos (clase 07)

```java
@Rule
public TemporaryFolder carpeta = new TemporaryFolder();      // si la ruta es configurable

@Before
public void sinArchivo()
{
    File archivo = new File("sinonimos.xml");                // si la ruta está fija en el código
    if (archivo.exists())
    {
        archivo.delete();
    }
}
```

Casos de la cátedra: escribir crea el archivo; ida y vuelta con el objeto vacío; ida y
vuelta con datos (requiere `equals`/`hashCode`, si no se compara campo por campo); leer un
archivo inexistente → la excepción **específica** (patrón *try / fail / catch*); y el
archivo con formato inválido → la excepción que defina el contrato.
