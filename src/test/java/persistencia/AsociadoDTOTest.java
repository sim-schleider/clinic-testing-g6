package persistencia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de AsociadoDTO. Según la documentación, la clase no declara precondiciones,
 * así que toda entrada inválida debe ser rechazada por el componente con AssertionError.
 * Cada clase de equivalencia se cubre con un único caso; el representante de numero es su
 * valor límite (0 válido, -1 inválido).
 */
@Tag("unit")
class AsociadoDTOTest {

    private AsociadoDTO dto;

    /**
     * Escenario E2: DTO cargado con valores válidos y distintos en cada campo, para detectar un
     * setter o getter que use otro campo.
     */
    @BeforeEach
    void setUpLoadedAsociado() {
        dto = new AsociadoDTO("Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
    }

    /** Verifica los siete getters. */
    private static void assertDatos(AsociadoDTO dto, String nombre, String apellido, String dni,
                                    String ciudad, String calle, int numero, String telefono) {
        assertEquals(nombre, dto.getNombre(), "nombre");
        assertEquals(apellido, dto.getApellido(), "apellido");
        assertEquals(dni, dto.getDni(), "dni");
        assertEquals(ciudad, dto.getCiudad(), "ciudad");
        assertEquals(calle, dto.getCalle(), "calle");
        assertEquals(numero, dto.getNumero(), "numero");
        assertEquals(telefono, dto.getTelefono(), "telefono");
    }

    /** Verifica que el DTO conserva los valores del escenario E2. */
    private static void assertLoadedAsociado(AsociadoDTO dto) {
        assertDatos(dto, "Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
    }

    // ---------------------------------------------------------------- Constructor por defecto

    // CU1 – Escenario E1. Constructor por defecto (sin entradas).
    // Esperado: null en los String y numero = 0 (S6).
    @Test
    void testDefaultConstructorLeavesDefaultValues() {
        AsociadoDTO vacio = new AsociadoDTO();
        assertDatos(vacio, null, null, null, null, null, 0, null);
    }

    // ---------------------------------------------------------------- Constructor parametrizado

    // CU2 – Constructor con todos los datos válidos y numero en su valor límite 0.
    // Clases: V1, V2, V3, V4, V5, V6, V7. Esperado: cada getter devuelve su valor.
    @Test
    void testConstructorAssignsValidData() {
        AsociadoDTO nuevo = new AsociadoDTO("Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
        assertDatos(nuevo, "Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }

    // Una clase inválida por test: si se juntaran, el primer rechazo ocultaría a los demás.
    // fail() lanza AssertionFailedError, subclase de AssertionError: por eso no va dentro del
    // try; el rechazo se registra en una variable y se verifica después.

    // CU3 – Constructor con nombre = null. Clase I1. Esperado: AssertionError (S3).
    @Test
    void testConstructorRejectsNullNombre() {
        boolean rechazado = false;
        try {
            new AsociadoDTO(null, "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar nombre = null");
    }

    // CU4 – Constructor con apellido = null. Clase I2. Esperado: AssertionError (S3).
    @Test
    void testConstructorRejectsNullApellido() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", null, "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar apellido = null");
    }

    // CU5 – Constructor con dni = null. Clase I3. Esperado: AssertionError (S3).
    // La documentación solo establece que dni no puede ser nulo; no define un formato.
    @Test
    void testConstructorRejectsNullDni() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", "Lombardo", null, "Mar del Plata", "Ratiery", 777, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar dni = null");
    }

    // CU6 – Constructor con ciudad = null. Clase I4. Esperado: AssertionError (S3).
    @Test
    void testConstructorRejectsNullCiudad() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", "Lombardo", "40000000", null, "Ratiery", 777, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar ciudad = null");
    }

    // CU7 – Constructor con calle = null. Clase I5. Esperado: AssertionError (S3).
    @Test
    void testConstructorRejectsNullCalle() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", "Lombardo", "40000000", "Mar del Plata", null, 777, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar calle = null");
    }

    // CU8 – Constructor con telefono = null. Clase I6. Esperado: AssertionError (S3).
    // La documentación solo establece que telefono no puede ser nulo; no define un formato.
    @Test
    void testConstructorRejectsNullTelefono() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar telefono = null");
    }

    // CU9 – Constructor con numero = -1. Clase I7, valor límite. Esperado: AssertionError (S3).
    @Test
    void testConstructorRejectsNegativeNumero() {
        boolean rechazado = false;
        try {
            new AsociadoDTO("Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", -1, "2236767676");
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "el constructor debía rechazar numero = -1");
    }

    // ---------------------------------------------------------------- Setters válidos (E2)

    // CU10 – Los seis setters String con texto válido, distinto al de E2.
    // Clases: V8, V9, V10, V11, V12, V13. Esperado: cada campo toma su valor; numero sin cambios.
    // La documentación solo establece que dni y telefono no pueden ser nulos; no define un formato.
    @Test
    void testStringSettersAssignValidText() {
        dto.setNombre("Lucas");
        dto.setApellido("Cauquen");
        dto.setDni("40111222");
        dto.setCiudad("Bahía Blanca");
        dto.setCalle("San Juan");
        dto.setTelefono("2494001122");
        assertDatos(dto, "Lucas", "Cauquen", "40111222", "Bahía Blanca", "San Juan", 777, "2494001122");
    }

    // CU11 – setNumero(0). Clase V14, valor límite (frontera exacta).
    // Esperado: numero = 0; los demás campos sin cambios.
    @Test
    void testSetNumeroAcceptsZero() {
        dto.setNumero(0);
        assertDatos(dto, "Valentino", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }

    // ---------------------------------------------------------------- Setters inválidos (E2)

    // CU12 – setNombre(null). Clase I8. Esperado: AssertionError y E2 sin cambios (S4).
    @Test
    void testSetNombreRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setNombre(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setNombre(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU13 – setApellido(null). Clase I9. Esperado: AssertionError y E2 sin cambios (S4).
    @Test
    void testSetApellidoRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setApellido(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setApellido(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU14 – setDni(null). Clase I10. Esperado: AssertionError y E2 sin cambios (S4).
    // La documentación solo establece que dni no puede ser nulo; no define un formato.
    @Test
    void testSetDniRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setDni(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setDni(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU15 – setCiudad(null). Clase I11. Esperado: AssertionError y E2 sin cambios (S4).
    @Test
    void testSetCiudadRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setCiudad(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setCiudad(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU16 – setCalle(null). Clase I12. Esperado: AssertionError y E2 sin cambios (S4).
    @Test
    void testSetCalleRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setCalle(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setCalle(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU17 – setTelefono(null). Clase I13. Esperado: AssertionError y E2 sin cambios (S4).
    // La documentación solo establece que telefono no puede ser nulo; no define un formato.
    @Test
    void testSetTelefonoRejectsNull() {
        boolean rechazado = false;
        try {
            dto.setTelefono(null);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setTelefono(null) debía rechazarse");
        assertLoadedAsociado(dto);
    }

    // CU18 – setNumero(-1). Clase I14, valor límite (último inválido antes de la frontera).
    // Esperado: AssertionError y E2 sin cambios (S4).
    @Test
    void testSetNumeroRejectsMinusOne() {
        boolean rechazado = false;
        try {
            dto.setNumero(-1);
        } catch (AssertionError e) {
            rechazado = true;
        }
        assertTrue(rechazado, "setNumero(-1) debía rechazarse");
        assertLoadedAsociado(dto);
    }
}
