package item1;

import persistencia.AsociadoDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de AsociadoDTO. Las reglas de la SRS (String no nulos, numero >= 0) son
 * precondiciones del constructor parametrizado y de los setters: el cliente garantiza que se
 * cumplen, así que solo se prueban las clases válidas. Cada clase se cubre con un único caso; el
 * representante de numero es su valor límite 0.
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

    // ---------------------------------------------------------------- Constructor por defecto

    // CU1 – Escenario E1. Constructor por defecto (sin entradas).
    // Esperado: null en los String y numero = 0 (S3).
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
        AsociadoDTO nuevo = new AsociadoDTO("Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
        assertDatos(nuevo, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }

    // ---------------------------------------------------------------- Setters (E2)

    // CU3 – Los seis setters String con texto válido, distinto al de E2.
    // Clases: V8, V9, V10, V11, V12, V13. Esperado: cada campo toma su valor; numero sin cambios.
    // La documentación solo establece que dni y telefono no pueden ser nulos; no define un formato.
    @Test
    void testStringSettersAssignValidText() {
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
    void testSetNumeroAcceptsZero() {
        dto.setNumero(0);
        assertDatos(dto, "Marina", "Lombardo", "40000000", "Mar del Plata", "Ratiery", 0, "2236767676");
    }
}
