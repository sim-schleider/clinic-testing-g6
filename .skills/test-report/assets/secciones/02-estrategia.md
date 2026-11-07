# Estrategia de testing

## Niveles de prueba

| Nivel | Componentes | Recursos externos | Técnica principal | Consigna |
|-------|-------------|-------------------|-------------------|----------|
| Unitario (atómico) | `AsociadoDTO` | Ninguno | Clases de equivalencia y valores límite | 1 |
| Integración | `AsociadoDTO`, `DataAccessObject`, `BaseDeDatos` | MySQL 8 real | Integración ascendente, estados × valores | 2 |
| Unitario con dobles de prueba | `Ambulancia` / estados concretos | Ninguno (mocks) | Mocks con Mockito | 3 |
| Integración y concurrencia | `Ambulancia`, estados, `RetornoAutomatico`, hilos | Hilos reales | Transición de estados, escenarios concurrentes | 3 |

TODO: justificar por qué cada consigna se aborda en ese nivel.

## Criterio de validación de las pruebas

TODO: el resultado esperado de cada caso surge de la especificación (SRS, enunciado, diagramas UML,
contratos Javadoc e informe de diseño), nunca del comportamiento actual del código. Lo que la
especificación no define se documenta como supuesto en cada sección.

## Ejecución de las suites y fallas sembradas

TODO: describir el procedimiento:
1. Cada suite se ejecuta una vez, sobre el código de su punto de la consigna, y su resultado
   muestra qué casos pasan (verde) y cuáles fallan (rojo).
2. Cada caso que falla se revisa contra el código y su defecto se anota en el registro.
3. Los defectos que la consigna pide corregir se corrigen en un commit propio.
4. Las fallas se siembran desde los tests (dobles de prueba, estados forzados), para demostrar que
   la suite las detecta.

## Entorno y herramientas

TODO: Java 17, Maven, JUnit 5.10, Mockito 5.11, MySQL 8 en Docker. Ver el Apéndice C para reproducir
las ejecuciones.

## Convenciones

| Prefijo | Significado |
|---------|-------------|
| V*n* / I*n* | Clase de equivalencia válida / inválida |
| CU*n* | Caso de prueba unitario |
| CI*n* | Caso de prueba de integración con la base de datos |
| CM*n* | Caso de prueba con mocks |
| CC*n* | Caso de prueba de integración y concurrencia |
| D*n* | Defecto detectado |
| FU*n* / FI*n* / FM*n* / FC*n* | Falla sembrada en cada suite |

Cada test lleva el identificador de su caso en el nombre, lo que permite cruzar las tablas de diseño
con los resultados de ejecución del Apéndice A.
