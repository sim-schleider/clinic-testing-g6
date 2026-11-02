# Apéndice C: Entorno de ejecución y reproducción {.unnumbered}

## Versiones {.unnumbered}

| Herramienta | Versión |
|-------------|---------|
| Java | 17 |
| Maven | 3.9 (imagen `maven:3.9-eclipse-temurin-17`) |
| JUnit Jupiter | 5.10.2 |
| Mockito | 5.11.0 |
| MySQL | 8.0.40 (imagen `mysql:8.0.40`) |
| MySQL Connector/J | 8.3.0 |

## Cómo reproducir las ejecuciones {.unnumbered}

```bash
docker compose up -d mysql                                                  # base de datos
docker compose --profile test run --rm maven mvn test -Dgroups=unit         # suite unitaria
docker compose --profile test run --rm maven mvn test -Dgroups=integration  # integración con MySQL
docker compose --profile test run --rm maven mvn test -Dgroups=mock         # mocks
docker compose --profile test run --rm maven mvn test -Dgroups=concurrency  # concurrencia
```

Las pruebas se ejecutan con `-ea` (configurado en Surefire), porque las validaciones del código usan
`assert`. Al ejecutarlas desde el IDE, hay que agregar `-ea` a la configuración de ejecución.

## Ajustes del entorno {.unnumbered}

Se adaptó el entorno al código, nunca al revés:

- **Autenticación de MySQL.** `BaseDeDatos` se conecta con `useSSL=false` y sin
  `allowPublicKeyRetrieval`. Con el plugin de autenticación por defecto de MySQL 8
  (`caching_sha2_password`), el driver rechaza la conexión con *"Public Key Retrieval is not
  allowed"*. `BaseDeDatos` atrapa esa excepción y solo la imprime, así que todos los tests fallaban
  con un `NullPointerException`. Se resolvió creando el usuario `progra_c` con
  `mysql_native_password`, con `command: --default-authentication-plugin=mysql_native_password` en
  el servicio `mysql` de `docker-compose.yml`.
- **Versión fija de MySQL.** Con la imagen genérica `mysql:8.0`, Docker descargó el 02/11/2026 la
  versión 8.0.46. En esa versión la opción anterior deja de aplicarse, el usuario vuelve a crearse
  con `caching_sha2_password` y todos los tests de integración fallan por el mismo problema de
  conexión. Por eso `docker-compose.yml` fija la versión en `mysql:8.0.40`, la misma que se usó
  el 31/10. Si el contenedor ya existía con otra versión, hay que recrearlo:
  `docker compose up -d --force-recreate -V mysql`.
- **Compilación.** El código entregado no compila (defectos D1 y D2), y esos dos defectos no se
  corrigen en el repositorio. Con el código tal como está, `mvn test` termina en error de compilación
  antes de ejecutar cualquier suite. Para poder correr las pruebas se usa una copia temporal del
  proyecto, fuera del repositorio, con los dos arreglos del commit `c1ff2a6` aplicados:

  ```bash
  git worktree add --detach /tmp/clinica-ejecucion HEAD   # copia temporal del proyecto
  cd /tmp/clinica-ejecucion
  git cherry-pick -n c1ff2a6                               # arregla D1 y D2 solo en la copia
  docker run --rm --network host -v "$PWD:/app" -w /app \
    -v clinic-testing-g6_maven-cache:/root/.m2 maven:3.9-eclipse-temurin-17 \
    mvn test -Dgroups=unit                                 # o integration, mock, concurrency
  ```

  La copia no se commitea. Lo único que se corrigió en el repositorio es `AsociadoDTO` (D3 y D4),
  porque la consigna lo pide.
