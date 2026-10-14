# Medir la cobertura con JaCoCo

JaCoCo es el motor que usa EclEmma, la herramienta que muestra la cátedra (clase 06).

## 1. Generar el reporte

Requiere el plugin `jacoco-maven-plugin` en el `pom` (`execution-environment.md` §1).

```bash
scripts/mvn.sh -- -Dmaven.test.failure.ignore=true -Dtest='<Clase>*Test' clean test
```

- `-Dmaven.test.failure.ignore=true`: **sin esto, si hay rojos, Maven corta antes de
  generar el reporte.**
- Se generan `target/site/jacoco/index.html` (navegable, colores por línea),
  `jacoco.xml` (lo lee `coverage.py`) y `jacoco.csv`.
- La cobertura es **la de los tests ejecutados en la última corrida**: para medir la combinada
  de caja negra + caja blanca, incluir las dos clases en `-Dtest`. Cualquier ejecución
  posterior con `clean` (por ejemplo, una corrida aparte con un estado especial) **reemplaza**
  el reporte: medir primero y ejecutar lo demás después.

## 2. Resumir

```bash
python3 -I scripts/coverage.py --clase <Clase> [--metodo <metodo>] [--fuentes src/main/java] \
        [--salida docs/pruebas/resultados/<fecha>-cobertura-<Clase>.md]
```

Salida:

```
| Método | Líneas | Ramas | V(G) JaCoCo |
| `media` | 2/2 (100%) | 2/4 (50%) | 3 |

Pendiente de cubrir:
- `media` L7 (ramas 2/4): `if (x < 0 || y < 0) { return -1; }`
```

- **"no ejecutada"**: ningún test pasó por esa línea. Falta un camino.
- **"ramas c/t"**: la línea tiene *t* ramas a nivel de bytecode y se cubrieron *c*. En
  `a || b` hay 4 ramas (V y F de cada condición), así que **el reporte de ramas de JaCoCo
  se aproxima a la cobertura de condición**, no solo a la de decisión.

## 3. Qué contador corresponde a cada criterio de la cátedra

| Criterio de la cátedra | Contador de JaCoCo | Comentario |
|------------------------|--------------------|------------|
| Cobertura de sentencia | LINE / INSTRUCTION | 100% de líneas **no** implica 100% de decisiones |
| Cobertura de decisión | BRANCH (por decisión) | Un `if` sin `else` muestra la rama faltante como parcial |
| Cobertura de condición | BRANCH (por condición, en `&&`/`\|\|`) | Cada condición simple aporta 2 ramas |
| Cobertura de camino | — | JaCoCo no mide caminos: se controla con la tabla de caminos básicos |
| V(G) | COMPLEXITY (covered + missed) | Cuenta cada `assert` como decisión; tiene que coincidir con la manual |

## 4. Casos que no conviene perseguir (se justifican en el diseño)

| Situación | Por qué no llega al 100% | Qué se escribe |
|-----------|--------------------------|----------------|
| `catch` de una excepción imposible de provocar en la unidad | Hace falta un recurso externo (BD, archivo) | "Cubierto en integración" o justificación |
| Código defensivo inalcanzable (`default` de un `switch` exhaustivo) | No hay entrada que lo active | Informarlo como posible código muerto |
| Constructor privado o `<clinit>` | No es lógica bajo prueba | Excluir del análisis |

## 5. Ciclo de completado

```
medir → listar lo pendiente → ¿es realizable?
          ├─ sí → nuevo caso Tn (volver al Paso 6) → codificar → medir de nuevo
          └─ no → justificar en el diseño (tabla "No cubierto")
```

Terminar cuando los métodos bajo prueba tengan **100% de las decisiones cubierto o
justificado**. La V(G) de JaCoCo tiene que coincidir con la manual (contando cada `assert`
como nodo predicado): si no coincide, revisar el grafo.
