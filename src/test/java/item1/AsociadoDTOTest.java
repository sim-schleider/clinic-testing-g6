package item1;

import persistencia.AsociadoDTO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de AsociadoDTO. Las reglas de la SRS (String no nulos, numero >= 0) son
 * validaciones de integridad de los setters: según el informe de diseño, los setters "incorporan
 * aserciones (assert) para validar la integridad básica de los datos —como evitar nombres nulos o
 * números negativos—". Por eso cada setter tiene su clase válida y su clase inválida, y una entrada
 * inválida debe rechazarse con AssertionError sin modificar el DTO. El constructor parametrizado no
 * declara validaciones: para él las reglas son precondiciones y solo se prueban las clases válidas.
 */
@Tag("unit")
@DisplayName("AsociadoDTO – pruebas unitarias (ítem 1)")
class AsociadoDTOTest {

    private AsociadoDTO dto;

    /**
     * Las validaciones de los setters son assert: sin -ea no se ejecutan, los casos inválidos
     * fallarían por la razón equivocada y un rechazo indebido (como D3) quedaría oculto.
     */
    @BeforeAll
    static void requireAssertionsEnabled() {
        assertTrue(AsociadoDTO.class.desiredAssertionStatus(),
                "Ejecutar con -ea (Surefire ya lo hace; en el IDE agregarlo a la configuración de ejecución)");
    }

    /**
     * Escenario E2: DTO cargado con valores válidos y distintos en cada campo, para detectar un
     * setter o getter que use otro campo.
     */
    @BeforeEach
    void setUpLoadedAsociado() {
        dto = new AsociadoDTO("Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
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

    /** Verifica que el DTO conserva los datos del escenario E2. */
    private void assertDatosE2() {
        assertDatos(dto, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 777, "2236767676");
    }

    /**
     * Verifica un rechazo: la llamada lanza AssertionError y el DTO conserva los datos de E2 (una
     * validación que rechaza después de asignar dejaría el objeto corrupto).
     */
    private void assertRechaza(Executable llamada) {
        assertThrows(AssertionError.class, llamada, "el setter debía rechazar el valor");
        assertDatosE2();
    }

    // ---------------------------------------------------------------- Constructor por defecto

    // CU1 – Escenario E1. Constructor por defecto (sin entradas).
    // Esperado: null en los String y numero = 0 (S3).
    @Test
    @DisplayName("CU1 – El constructor por defecto deja los valores por defecto")
    void cu1_defaultConstructorLeavesDefaultValues() {
        AsociadoDTO vacio = new AsociadoDTO();
        assertDatos(vacio, null, null, null, null, null, 0, null);
    }

    // ---------------------------------------------------------------- Constructor parametrizado

    // CU2 – Constructor con todos los datos válidos y numero en su valor límite 0.
    // Clases: V1, V2, V3, V4, V5, V6, V7. Esperado: cada getter devuelve su valor.
    @Test
    @DisplayName("CU2 – El constructor asigna datos válidos, con numero = 0")
    void cu2_constructorAssignsValidData() {
        AsociadoDTO nuevo = new AsociadoDTO("Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
        assertDatos(nuevo, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }

    // ---------------------------------------------------------------- Setters, clases válidas (E2)

    // CU3 – Los seis setters String con texto válido, distinto al de E2.
    // Clases: V8, V9, V10, V11, V12, V13. Esperado: cada campo toma su valor; numero sin cambios.
    // La documentación solo establece que dni y telefono no pueden ser nulos; no define un formato.
    @Test
    @DisplayName("CU3 – Los setters de texto asignan texto válido")
    void cu3_stringSettersAssignValidText() {
        dto.setNombre("Simon");
        dto.setApellido("Schleider");
        dto.setDni("40111222");
        dto.setCiudad("Bahía Blanca");
        dto.setCalle("San Juan");
        dto.setTelefono("2494001122");
        assertDatos(dto, "Simon", "Schleider", "40111222", "Bahía Blanca", "San Juan", 777, "2494001122");
    }

    // CU4 – setNumero(0). Clase V14, valor límite (frontera exacta).
    // Esperado: numero = 0; los demás campos sin cambios.
    @Test
    @DisplayName("CU4 – setNumero(0) acepta el valor límite 0")
    void cu4_setNumeroAcceptsZero() {
        dto.setNumero(0);
        assertDatos(dto, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }

    // CU5 – setNumero(1). Clase V14, valor siguiente al límite.
    // Esperado: numero = 1; los demás campos sin cambios.
    @Test
    @DisplayName("CU5 – setNumero(1) acepta el valor siguiente al límite")
    void cu5_setNumeroAcceptsOne() {
        dto.setNumero(1);
        assertDatos(dto, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 1, "2236767676");
    }

    // CU6 – setNumero(Integer.MAX_VALUE). Clase V14, extremo del tipo (la SRS no fija un máximo).
    // Esperado: numero = Integer.MAX_VALUE; los demás campos sin cambios.
    @Test
    @DisplayName("CU6 – setNumero(Integer.MAX_VALUE) acepta el extremo del tipo")
    void cu6_setNumeroAcceptsMaxValue() {
        dto.setNumero(Integer.MAX_VALUE);
        assertDatos(dto, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", Integer.MAX_VALUE, "2236767676");
    }

    // CU7 – Los seis setters String con "" (texto de longitud 0). Clases V8–V13, valor límite (S1).
    // Esperado: cada campo queda en ""; numero sin cambios.
    @Test
    @DisplayName("CU7 – Los setters de texto aceptan el texto vacío")
    void cu7_stringSettersAcceptEmptyText() {
        dto.setNombre("");
        dto.setApellido("");
        dto.setDni("");
        dto.setCiudad("");
        dto.setCalle("");
        dto.setTelefono("");
        assertDatos(dto, "", "", "", "", "", 777, "");
    }

    // CU8 – Los seis setters String con "   " (solo espacios). Clases V8–V13 (S1).
    // Esperado: cada campo queda en "   "; numero sin cambios.
    @Test
    @DisplayName("CU8 – Los setters de texto aceptan texto con solo espacios")
    void cu8_stringSettersAcceptBlankText() {
        dto.setNombre("   ");
        dto.setApellido("   ");
        dto.setDni("   ");
        dto.setCiudad("   ");
        dto.setCalle("   ");
        dto.setTelefono("   ");
        assertDatos(dto, "   ", "   ", "   ", "   ", "   ", 777, "   ");
    }

    // ---------------------------------------------------------------- Setters, clases inválidas (E2)

    // CU9 – setNombre(null). Clase I1. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU9 – setNombre(null) se rechaza sin modificar el DTO")
    void cu9_setNombreRejectsNull() {
        assertRechaza(() -> dto.setNombre(null));
    }

    // CU10 – setApellido(null). Clase I2. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU10 – setApellido(null) se rechaza sin modificar el DTO")
    void cu10_setApellidoRejectsNull() {
        assertRechaza(() -> dto.setApellido(null));
    }

    // CU11 – setDni(null). Clase I3. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU11 – setDni(null) se rechaza sin modificar el DTO")
    void cu11_setDniRejectsNull() {
        assertRechaza(() -> dto.setDni(null));
    }

    // CU12 – setCiudad(null). Clase I4. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU12 – setCiudad(null) se rechaza sin modificar el DTO")
    void cu12_setCiudadRejectsNull() {
        assertRechaza(() -> dto.setCiudad(null));
    }

    // CU13 – setCalle(null). Clase I5. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU13 – setCalle(null) se rechaza sin modificar el DTO")
    void cu13_setCalleRejectsNull() {
        assertRechaza(() -> dto.setCalle(null));
    }

    // CU14 – setTelefono(null). Clase I6. Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU14 – setTelefono(null) se rechaza sin modificar el DTO")
    void cu14_setTelefonoRejectsNull() {
        assertRechaza(() -> dto.setTelefono(null));
    }

    // CU15 – setNumero(-1). Clase I7, valor límite (el primer valor inválido).
    // Esperado: AssertionError y el DTO sin cambios.
    @Test
    @DisplayName("CU15 – setNumero(-1) se rechaza sin modificar el DTO")
    void cu15_setNumeroRejectsMinusOne() {
        assertRechaza(() -> dto.setNumero(-1));
    }
}
