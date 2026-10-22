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

TODO: registrar acá cualquier ajuste del entorno que haya hecho falta (no del código).
