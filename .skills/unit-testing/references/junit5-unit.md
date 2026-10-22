# JUnit 5 patterns for unit tests

Java 17, JUnit Jupiter 5.10. Imports used below:

```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
```

## Class skeleton

```java
package persistencia;

@Tag("unit")
@DisplayName("AsociadoDTO – pruebas unitarias")
class AsociadoDTOTest {

    // Without -ea the production asserts don't run: every "invalid input" case would report
    // a defect that does not exist. Fail fast with a clear message instead.
    @BeforeAll
    static void requiereAssertsHabilitados() {
        boolean habilitados = false;
        assert habilitados = true;
        assertTrue(habilitados, "Ejecutar con -ea (Surefire ya lo hace; en el IDE agregarlo a la configuración)");
    }

    @Nested
    @DisplayName("E1 – DTO recién creado")
    class EscenarioVacio {
        private AsociadoDTO dto;

        @BeforeEach
        void escenario() {
            dto = new AsociadoDTO();
        }

        @Test
        @DisplayName("CU1 – setNombre con texto válido lo asigna")
        void cu1_setNombreValido() {
            dto.setNombre("Laura");
            assertEquals("Laura", dto.getNombre());
        }
    }

    @Nested
    @DisplayName("E2 – DTO con todos los datos válidos")
    class EscenarioCompleto {
        private AsociadoDTO dto;

        @BeforeEach
        void escenario() {
            // A different value in every field, so a setter that writes the wrong field shows up.
            dto = new AsociadoDTO("Laura", "Singleton", "22334455", "Mar del Plata", "Alem", 1450, "2236112233");
        }
    }
}
```

`@Nested` classes show up as groups in IntelliJ and in the screenshots, one per scenario.

## Rejection through `assert`

```java
@Test
@DisplayName("CU7 – setNombre(null) se rechaza")
void cu7_setNombreNuloRechazado() {
    assertThrows(AssertionError.class, () -> dto.setNombre(null));
}
```

When the rejection is expected, also check that the object did not change. A validation that
throws only after assigning still corrupts the object:

```java
assertThrows(AssertionError.class, () -> dto.setNumero(-1));
assertEquals(1450, dto.getNumero(), "el valor previo no debe modificarse");
```

## A setter changes only its own field

```java
@Test
@DisplayName("CU4 – setCalle cambia solo la calle")
void cu4_setCalleSoloCambiaCalle() {
    dto.setCalle("Luro");
    assertAll(
        () -> assertEquals("Luro", dto.getCalle()),
        () -> assertEquals("Laura", dto.getNombre()),
        () -> assertEquals("Singleton", dto.getApellido()),
        () -> assertEquals("22334455", dto.getDni()),
        () -> assertEquals("Mar del Plata", dto.getCiudad()),
        () -> assertEquals(1450, dto.getNumero()),
        () -> assertEquals("2236112233", dto.getTelefono()));
}
```

`assertAll` reports every mismatch at once, not just the first, which makes the red more
informative.

## Parameterized rows with case IDs

Put the case ID first so it ends up in the test name, and from there in the results table:

```java
@ParameterizedTest(name = "{0} – setNumero({1}) es válido")
@CsvSource({
    "CU10, 0",
    "CU11, 1",
    "CU12, 2147483647"
})
void numeroValido(String id, int numero) {
    dto.setNumero(numero);
    assertEquals(numero, dto.getNumero());
}

@ParameterizedTest(name = "{0} – setNumero({1}) se rechaza")
@CsvSource({
    "CU13, -1",
    "CU14, -2147483648"
})
void numeroInvalido(String id, int numero) {
    assertThrows(AssertionError.class, () -> dto.setNumero(numero));
}
```

`null` in a `@CsvSource` needs `nullValues = "null"`. `@NullAndEmptySource` supplies `null` and
`""` for a single-argument String test, but a test can't carry a different ID per row that way.
Prefer `@CsvSource` with explicit IDs.

## Naming and assertions

- `assertEquals(expected, actual, message)`. The expected value goes first; swapping them
  produces misleading failure messages.
- Compare `double` with a delta: `assertEquals(2.0, r, 1e-9)`.
- Never use an empty `catch` in a test: an unexpected exception should fail the test.
- Test classes end in `Test`, or Surefire won't run them.
