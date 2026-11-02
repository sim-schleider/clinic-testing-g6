# Estrategia de testing

## Niveles de prueba

| Nivel | Componentes | Recursos externos | Técnica principal | Consigna |
|-------|-------------|-------------------|-------------------|----------|
| Unitario (atómico) | `AsociadoDTO` | Ninguno | Clases de equivalencia y valores límite | 1 |
| Integración | `AsociadoDTO`, `DataAccessObject`, `BaseDeDatos` | MySQL 8 real | Integración ascendente, estados × valores | 2 |
| Unitario con dobles de prueba | `Ambulancia` / estados concretos | Ninguno (mocks) | Mocks con Mockito | 3 |
| Integración y concurrencia | `Ambulancia`, estados, `RetornoAutomatico`, hilos | Hilos reales | Transición de estados, escenarios concurrentes | 3 |

TODO: justificar por qué cada consigna se aborda en ese nivel.

## Oráculo de las pruebas

TODO: el resultado esperado de cada caso surge de la especificación (SRS, enunciado, diagramas UML,
contratos Javadoc e informe de diseño), nunca del comportamiento actual del código. Lo que la
especificación no define se documenta como supuesto en cada sección.

## Ciclo rojo / verde y fallas sembradas

Cada suite se corre al menos dos veces, y cada corrida muestra algo distinto:

1. **Rojo: la suite encuentra los errores.** Primero se corre sobre el código tal como se entregó.
   Cada caso que falla se revisa contra el código para confirmar que la causa es un defecto real y
   no un error del test. Los defectos confirmados se anotan en el registro de defectos.
2. **Corrección, solo donde la consigna lo pide.** El punto 2 de la consigna pide "corregir si es
   necesario el código del ítem 1", así que se corrigen los defectos de `AsociadoDTO` (D3 y D4),
   cada uno en su propio commit. El resto del código no se toca: sus defectos quedan registrados
   y sus casos siguen en rojo, porque eso es lo que demuestra que la suite los detecta.
3. **Verde: la corrección funciona y no rompe nada.** Se vuelve a correr la suite. Los casos de los
   defectos corregidos pasan a verde, y los que ya pasaban tienen que seguir pasando.
4. **Fallas introducidas desde los tests.** Como el resto del código no se modifica, para mostrar
   que la suite detecta otros tipos de error se fuerzan situaciones desde el propio test: la base
   de datos en estados especiales (tabla borrada, conexión cerrada) y dobles de prueba (mocks y
   estados falsos) que se comportan mal a propósito.

El código entregado no compila (D1 y D2) y esos defectos tampoco se corrigen. Para poder correr las
pruebas, se aplica el arreglo en una copia temporal del proyecto que no se sube al repositorio
(Apéndice C).

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
