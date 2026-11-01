# Apéndice C: Entorno de ejecución y reproducción {.unnumbered}

## Versiones {.unnumbered}

| Herramienta | Versión |
|-------------|---------|
| Java | 17 |
| Maven | 3.9 (imagen `maven:3.9-eclipse-temurin-17`) |
| JUnit Jupiter | 5.10.2 |
| Mockito | 5.11.0 |
| MySQL | 8.0 (imagen `mysql:8.0`) |
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
- **Compilación.** El código entregado no compila (defectos D1 y D2). Las ejecuciones del 31/10/2026
  se hicieron con esos dos arreglos aplicados; después se revirtieron por indicación de la cátedra.
  Con el código tal como se entregó, `mvn test` termina en error de compilación antes de ejecutar
  cualquier suite.
