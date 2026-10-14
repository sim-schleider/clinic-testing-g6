# Patrones JUnit 4 para caja negra (Java 8)

Todo compila con JUnit 4.13.2 y Java 8. Hay que adaptar los nombres a la API real de la
clase bajo prueba.

## 1. Esqueleto con escenarios (estilo de la cátedra)

```java
package persistencia;                                   // mismo paquete que la clase bajo prueba

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Caja negra de AsociadoDTO según SRS §3.1. Diseño: docs/pruebas/caja-negra/AsociadoDTO.md
 */
public class AsociadoDTOCajaNegraTest
{
    private AsociadoDTO dto;

    /** Escenario 1: DTO recién creado (vacío). */
    private void setupEscenario1()
    {
        dto = new AsociadoDTO();
    }

    /** Escenario 2: DTO completo, con un valor distinto en cada campo. */
    private void setupEscenario2()
    {
        dto = new AsociadoDTO("Nom", "Ape", "11111111", "Ciu", "Cal", 10, "Tel");
    }

    @Test
    public void cn1_constructorCompletoAsignaCadaCampo()
    {
        setupEscenario2();
        assertEquals("nombre", "Nom", dto.getNombre());
        assertEquals("apellido", "Ape", dto.getApellido());
        assertEquals("dni", "11111111", dto.getDni());
        assertEquals("ciudad", "Ciu", dto.getCiudad());
        assertEquals("calle", "Cal", dto.getCalle());
        assertEquals("numero", 10, dto.getNumero());
        assertEquals("telefono", "Tel", dto.getTelefono());
    }

    @Test
    public void cn2_setNumeroLimiteCero()
    {
        setupEscenario1();
        dto.setNumero(0);
        assertEquals("numero = 0 es válido (límite)", 0, dto.getNumero());
    }

    @Test
    public void cn3_setNumeroNegativoRechazado()
    {
        setupEscenario1();
        assertThrows("numero = -1 viola numero >= 0", AssertionError.class, () -> dto.setNumero(-1));
    }
}
```

- **Valores distintos en cada campo** ("Nom", "Ape", "Ciu", "Cal"…): si un setter o el
  constructor escribe el campo equivocado, el `assertEquals` del otro campo lo detecta.
  Con valores repetidos ("x", "x") el error pasa desapercibido.
- El orden de los parámetros del constructor se toma de la **firma real**, no se supone.

## 2. Rechazos: tres formas

```java
// a) JUnit 4.13: assertThrows (recomendada; permite inspeccionar la excepción)
AssertionError e = assertThrows(AssertionError.class, () -> dto.setNombre(null));

// b) Anotación: corta, pero no dice QUÉ línea lanzó
@Test(expected = AssertionError.class)
public void cn4_setNombreNullRechazado() { setupEscenario1(); dto.setNombre(null); }

// c) Patrón de la cátedra (try / fail / catch): sirve también para verificar el mensaje
@Test
public void cn5_setDniNullRechazado()
{
    setupEscenario1();
    try
    {
        dto.setDni(null);
        fail("Debería rechazar dni null (SRS §3.1)");
    }
    catch (AssertionError e)
    {
        // opcional: verificar e.getMessage()
    }
}
```

**Después de un rechazo, el estado no tiene que cambiar.** Para un caso completo:

```java
setupEscenario2();
assertThrows(AssertionError.class, () -> dto.setNumero(-1));
assertEquals("un rechazo no modifica el valor previo", 10, dto.getNumero());
```

- Si el mecanismo es una excepción propia (`throws DatoInvalidoException`), se usa esa
  clase en lugar de `AssertionError`.
- **No atrapar `Throwable` ni `Exception` genérica**: un NPE haría pasar el test por la razón equivocada.

## 3. Clases válidas homogéneas con `Parameterized`

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class AsociadoDTONumeroValidoCajaNegraTest
{
    @Parameters(name = "{0}: numero={1,number,#}")
    public static Collection<Object[]> casos()
    {
        return Arrays.asList(new Object[][] {
            { "CN6", 0 },                    // límite inferior
            { "CN7", 1 },                    // vecino del límite
            { "CN8", Integer.MAX_VALUE },    // robustez (no hay cota superior: supuesto)
        });
    }

    @Parameter(0) public String id;
    @Parameter(1) public int numero;

    @Test
    public void numeroValidoSeAsigna()
    {
        AsociadoDTO dto = new AsociadoDTO();
        dto.setNumero(numero);
        assertEquals(id, numero, dto.getNumero());
    }
}
```

- El reporte muestra `numeroValidoSeAsigna[CN6: numero=0]` y `surefire_summary.py` toma el
  Id del inicio de los corchetes: **el `name` del `@Parameters` tiene que empezar con el Id**.
- `{1,number,#}` evita que JUnit formatee los enteros con separador de miles
  (`2,147,483,647`).
- Todo `@Test` de una clase `Parameterized` se ejecuta **una vez por fila**: no poner ahí
  tests que no dependan de los parámetros.
- Las clases **inválidas** van en tests separados (§2), no en el parametrizado: cada una
  necesita su propio resultado esperado.

## 4. `double`, colecciones e identidad

```java
assertEquals("incremento con 1 hijo menor", 100.0, emp.getIncremento(1000.0), 1e-6);
assertTrue("debería estar vacía", lista.isEmpty());
assertEquals("tamaño", 3, lista.size());
assertNull("palabra inexistente → null (contrato)", traductor.traducir("zzz", ESPANOL, INGLES));
assertSame("misma instancia", a, b);
```

## 5. Suite por técnica (opcional)

```java
@RunWith(Suite.class)
@Suite.SuiteClasses({ AsociadoDTOCajaNegraTest.class, AsociadoDTONumeroValidoCajaNegraTest.class })
public class CajaNegraSuite { }
```

Con Maven alcanza con `-Dtest='*CajaNegraTest'`. La suite es útil para la captura del IDE
(una sola barra verde o roja).
