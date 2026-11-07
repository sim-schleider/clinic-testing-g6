<!-- Archivo generado por build_appendix.py: no editar a mano. -->

# Apéndice B: Capturas de ejecución {.unnumbered}

La consigna requiere capturas que muestren las pruebas en verde (funcionamiento correcto) y en rojo (detección de los errores). Cada captura muestra una ejecución de una suite, con los tests que pasan y los que fallan. Las toma el equipo desde el IDE, sobre la misma ejecución cuyos resultados figuran en el Apéndice A, y se guardan en `docs/informe/capturas/<suite>.png`.

## Capturas requeridas {.unnumbered}

Las siguientes capturas todavía no fueron tomadas. Cada una debe mostrar el panel de resultados de JUnit en IntelliJ (árbol de tests con sus estados y el total de pasados y fallidos) de la ejecución indicada.

| Suite | Ejecución | Archivo |
|-------|-----------|---------|
| Pruebas unitarias (AsociadoDTO) | `mvn test -Dgroups=unit` | `capturas/unit.png` |
| Pruebas de integración con MySQL | `mvn test -Dgroups=integration` | `capturas/integration.png` |
| Pruebas con mocks (Ambulancia y State) | `mvn test -Dgroups=mock` | `capturas/mock.png` |
| Pruebas de integración y concurrencia (máquina de estados) | `mvn test -Dgroups=concurrency` | `capturas/concurrency.png` |
