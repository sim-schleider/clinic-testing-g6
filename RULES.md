# RULES: reglas del trabajo

Reglas del TP final de Calidad de Software (aplicación "Clínica"). Complementan
`.skills/CONVENTIONS.md` y se subordinan a la consigna (`docs/Enunciado_Trabajo_final.md`).
Si hay conflicto con otro documento del repo, rige este archivo.

## 1. Alcance: solo testing

- **No se modifica el código fuente.** Todo lo que está en `src/main/` queda tal como fue
  entregado: no se corrigen defectos, no se renombran clases ni paquetes, no se refactoriza,
  no se ajustan validaciones ni se tocan los `assert`.
- El trabajo consiste únicamente en **diseñar, escribir y ejecutar pruebas** sobre lo
  desarrollado, y **documentar** los resultados.
- Solo se escribe en:
  - `src/test/java/` (suites de pruebas),
  - `docs/informe/` (informe, resultados, capturas),
  - archivos de soporte del entorno de pruebas (`docker-compose.yml`, scripts de
    `.skills/`), sin afectar el comportamiento de la aplicación.
- Un defecto del código **no se corrige**: se **registra** en `docs/informe/secciones/07-defectos.md`
  con estado `No se corrige: fuera de alcance (solo testing)` y se deja el test en rojo.
  Ese rojo es la evidencia que pide la consigna.

## 2. El oráculo es la especificación

- Los resultados esperados salen de la SRS (`docs/SRS_Persistencia_Clinica.md`), la consigna,
  el diagrama UML, el Javadoc y el informe de diseño (`docs/Informe.md`).
- **Nunca** del comportamiento actual del código. Un test que falla porque el código no cumple
  la especificación es un hallazgo, no un test roto.
- Los vacíos de la especificación se escriben como *supuestos* en el informe.

## 3. Lo que exige la consigna

1. **`AsociadoDTO`**: pruebas unitarias estrictamente aisladas (sin MySQL ni otros servicios)
   de asignación, recuperación y validaciones de integridad de los setters.
   Deben detectar las fallas del código provisto.
2. **Integración** de `AsociadoDTO` + `DataAccessObject` + `BaseDeDatos` contra MySQL real:
   Singleton de la conexión, creación automática de tablas y datos semilla, CRUD con consultas
   parametrizadas. Deben atrapar excepciones SQL, errores de mapeo e inconsistencias.
3. **Mocks e integración/concurrencia**: `Ambulancia` con la interfaz `State` mockeada;
   flujo completo de la máquina de estados con `RetornoAutomatico` y otros hilos
   (recursos compartidos, bloques sincronizados). Deben descubrir los problemas de
   sincronización o de estado.

El diseño de casos usa **clases de equivalencia** y **valores límite**.

## 4. Entregables

- Código fuente completo de las suites de pruebas.
- Capturas de ejecución en **verde** (funcionamiento correcto) y en **rojo** (detección de errores).
- Informe con: diseño formal de escenarios, casos de prueba (clases de equivalencia y valores
  límite), resultados y conclusiones. Los resultados van en el apéndice.
- Entregables escritos en español.

## 5. Cómo se prueba la capacidad de detección sin tocar el código

Como no se edita `src/main/`, la suite debe demostrar que atrapa fallas por dos vías:

- **Defectos reales** del código provisto: quedan en rojo y se registran.
- **Dobles de prueba y entornos controlados** (mocks, bases de datos en estados forzados,
  subclases o stubs definidos **dentro de `src/test/`**) para simular fallas de SQL, de mapeo,
  de sincronización o de estado.

No se siembran fallas en el código fuente ni siquiera de forma temporal.

## 6. Convenciones de los tests

- Java 17, Maven, JUnit 5.10, Mockito 5.11, MySQL 8 (`docker-compose.yml`). Surefire corre con
  `-ea`: los inválidos esperan `AssertionError`.
- Clases en `src/test/java`, mismo paquete que la clase bajo prueba, nombre terminado en `Test`,
  con `@Tag("unit" | "integration" | "mock" | "concurrency")`.
- Cada método empieza con su ID (`cu3_...`) y lleva `@DisplayName("CU3 – ...")`.
  IDs: `CU` unitarias, `CI` integración BD, `CM` mocks, `CC` concurrencia, `D` defectos.
- Se adapta el entorno al código, nunca al revés (conexión fija en `BaseDeDatos`:
  `localhost:3306`, `progra_c`/`progra_c`, base `Grupo_7`).
- Ejecución: `.skills/test-report/scripts/run_tests.sh <suite> <fase>`.

## 7. Git

- Commits en inglés, formato Conventional Commits, un commit por cambio lógico
  (`test(...)`, `docs(...)`, `chore(...)`).
- Sin coautoría ni atribución a Claude/Anthropic.
- No se hace commit ni push salvo que se pida.

## 8. Excepción única: bloqueadores de compilación

El código entregado no compilaba (`EnTallerState.java` declaraba `package model...` en lugar de
`modelo...`, y `TrasladandoPacienteClinicasState.java` contenía la clase
`TrasladandoPacienteClinicaState`). Por decisión del equipo se corrigieron ambos en un único
commit acotado (`fix(ambulancia): ...`). **Es la única excepción** a la regla de no tocar el
código fuente. Ambos se registran como defectos D1 y D2 del código provisto.
