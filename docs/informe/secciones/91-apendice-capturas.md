<!-- Archivo generado por build_appendix.py: no editar a mano. -->

# Apéndice B: Capturas de ejecución {.unnumbered}

La consigna requiere capturas de cada suite en rojo (detección de los errores sobre el código provisto) y en verde (funcionamiento correcto). Las capturas las toma el equipo desde el IDE, sobre la misma ejecución cuyos resultados figuran en el Apéndice A, y se guardan en `docs/informe/capturas/<suite>-<fase>.png`.

## Capturas requeridas {.unnumbered}

Las siguientes capturas todavía no fueron tomadas. Cada una debe mostrar el panel de resultados de JUnit en IntelliJ (árbol de tests con sus estados y el total de pasados y fallidos) de la ejecución indicada.

| Suite | Fase | Ejecución | Archivo |
|-------|------|-----------|---------|
| Pruebas unitarias (AsociadoDTO) | Rojo | `mvn test -Dgroups=unit` | `capturas/unit-rojo.png` |
| Pruebas unitarias (AsociadoDTO) | Verde | `mvn test -Dgroups=unit` | `capturas/unit-verde.png` |
| Pruebas de integración con MySQL | Rojo | `mvn test -Dgroups=integration` | `capturas/integration-rojo.png` |
| Pruebas de integración con MySQL | Verde | `mvn test -Dgroups=integration` | `capturas/integration-verde.png` |
| Pruebas con mocks (Ambulancia y State) | Rojo | `mvn test -Dgroups=mock` | `capturas/mock-rojo.png` |
| Pruebas con mocks (Ambulancia y State) | Verde | `mvn test -Dgroups=mock` | `capturas/mock-verde.png` |
| Pruebas de integración y concurrencia (máquina de estados) | Rojo | `mvn test -Dgroups=concurrency` | `capturas/concurrency-rojo.png` |
| Pruebas de integración y concurrencia (máquina de estados) | Verde | `mvn test -Dgroups=concurrency` | `capturas/concurrency-verde.png` |
