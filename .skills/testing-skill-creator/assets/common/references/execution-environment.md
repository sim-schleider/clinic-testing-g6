# Entorno de ejecución: Java 8 + Maven + JUnit 4 + Mockito 4

<!-- ARCHIVO COMÚN: la fuente es testing-skill-creator/assets/common/. No editar la copia. -->

Leer este archivo **antes de la primera ejecución** en un proyecto, o cuando falle la
compilación o la ejecución. Si el `pom.xml` ya tiene todo y los tests corren, no hace falta.

## 1. Verificar el `pom.xml`

El proyecto necesita estas dependencias y plugins. Hay que agregar **solo lo que falte**,
sin tocar el resto del `pom` ni bajar versiones que ya estén.

```xml
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>                                  <!-- solo si hay mocks -->
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>4.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>                                  <!-- solo si el código usa MySQL -->
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <trimStackTrace>false</trimStackTrace>
            </configuration>
        </plugin>
        <plugin>                                  <!-- cobertura (caja blanca) -->
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution><goals><goal>prepare-agent</goal></goals></execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Por qué estas versiones:**
- **Mockito 5 no sirve con Java 8**: requiere Java 11. La última compatible es la **4.11.0**.
  Para mockear clases `final` o métodos `static` hace falta `mockito-inline:4.11.0` en
  lugar de `mockito-core`.
- **JUnit 4.13.2** es la API de las diapositivas y ya trae `assertThrows`.
- `mysql-connector-j` tiene que estar con scope **compile** (no `test`) si el código de
  producción usa JDBC. Así el programa también corre fuera de los tests.
- **JaCoCo** escribe el `argLine` del agente. Si el `pom` define su propio `<argLine>` en
  Surefire, tiene que incluir `@{argLine}`; si no, la cobertura sale en 0.

## 2. Ejecutar

Siempre con `scripts/mvn.sh`: usa Maven local si existe y, si no, Docker con JDK 8.

```bash
S=.claude/skills/<esta-skill>/scripts          # ruta relativa a la raíz del proyecto
$S/mvn.sh -- clean test                                              # todos los tests
$S/mvn.sh -- -Dtest=AsociadoDTOCajaNegraTest clean test              # una clase
$S/mvn.sh -- -Dtest='AsociadoDTOCajaNegraTest#cn3*' clean test       # métodos por patrón
$S/mvn.sh --log docs/pruebas/resultados/$(date +%F)-caja-negra.log -- -Dtest='*CajaNegraTest' clean test
python3 -I $S/surefire_summary.py --clase '*CajaNegraTest' --titulo "Caja negra AsociadoDTO" \
        --salida docs/pruebas/resultados/$(date +%F)-caja-negra.md
```

- **Que Maven termine con error porque fallaron tests es normal** cuando se buscan
  defectos. Lo que hay que revisar es el resumen, no el código de salida.
- **Con tests en rojo, Maven corta antes de generar la cobertura de JaCoCo.** Para tener el
  reporte igual: `$S/mvn.sh -- -Dmaven.test.failure.ignore=true clean test`. No se pone en el
  `pom`, para no ocultar los fallos en una ejecución normal.
- **`-Dtest` con un patrón que no coincide con nada** hace fallar la build con "No tests
  were executed". Agregar `-Dsurefire.failIfNoSpecifiedTests=false` si se ejecutan
  patrones amplios.
- **Siempre `clean test`**: Surefire **no borra** los reportes de corridas anteriores, y el
  resumen mezclaría resultados viejos. Si no se puede limpiar, filtrar con `--clase`.
- **`clean` también borra la cobertura de JaCoCo**: hay que resumir resultados y cobertura
  **inmediatamente después** de la corrida que interesa, antes de cualquier otra ejecución (por
  ejemplo, la corrida aparte con la base bloqueada).
- **`mvn test` también compila `src/main`.** Si el código de producción no compila, no se
  ejecuta ningún test: primero hay que resolver eso con el usuario (no se arregla en
  silencio).

## 3. Base de datos MySQL (solo integración)

```bash
$S/mysql.sh up --usuario <user> --clave <pass>        # los del código (buscar la URL JDBC)
$S/mvn.sh --mysql tpe-mysql -- -Dtest='*IntegracionTest' clean test
$S/mysql.sh down                                       # al terminar
```

- `--mysql tpe-mysql` hace que el contenedor de Maven comparta la red del de MySQL, así
  que la URL `jdbc:mysql://localhost:3306/...` del código funciona sin cambios.
- Para correr desde el IDE: `mysql.sh up --publicar 3306 ...`. El puerto 3306 del host
  tiene que estar libre.
- **BD inaccesible**: `mysql.sh lock --usuario <user>`, ejecutar los tests de ese estado y
  después `mysql.sh unlock`. No se detiene ni se pausa el contenedor: los tests comparten
  su red y quedarían colgados.

## 4. Problemas frecuentes

| Síntoma | Causa | Solución |
|---------|-------|----------|
| `Public Key Retrieval is not allowed` | Usuario con `caching_sha2_password` y `useSSL=false` | `mysql.sh up` ya crea el usuario con `mysql_native_password`; si el usuario lo creó otro, recrearlo |
| `Communications link failure` | MySQL no está levantado, o se ejecutó sin `--mysql` | `mysql.sh status`; usar `mvn.sh --mysql tpe-mysql` |
| `Access denied for user` | Usuario o clave distintos a los del código | `mysql.sh up --usuario X --clave Y` con los valores de la clase de conexión |
| El test que espera `AssertionError` falla con "Expected exception" | Asertos desactivados (`-ea`) | Surefire los activa; en el IDE agregar `-ea` a la configuración |
| Un test queda colgado indefinidamente | `wait()` sin `notify`, deadlock o conexión que no responde | `@Test(timeout = …)` o la regla `Timeout` (ver concurrencia) |
| `No tests were executed` | El patrón `-Dtest` no coincide, o la clase no termina en `Test` | Revisar el nombre; `-Dsurefire.failIfNoSpecifiedTests=false` |
| JaCoCo reporta 0% | `argLine` propio pisa el del agente | Usar `<argLine>@{argLine} ...</argLine>` |
| Archivos de `target/` con dueño `root` | Docker corrió sin `-u` | `mvn.sh` ya pasa `-u $(id -u):$(id -g)`; borrar `target/` una vez |
| Mockito: `Cannot mock/spy class … final class` | Clase `final` | `mockito-inline:4.11.0` en lugar de `mockito-core` |
