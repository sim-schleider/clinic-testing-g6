# Especificación de Requisitos de Software (SRS) para el Módulo de Persistencia

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos de software para el módulo de persistencia dentro del sistema de gestión: **Clínica**. El módulo es responsable de gestionar el almacenamiento seguro, la recuperación, la actualización y la eliminación de los datos de los asociados a través de un motor de base de datos relacional.

### 1.2 Alcance

El módulo de persistencia opera como la capa de acceso a datos de la aplicación clínica. Se encarga del mapeo entre las entidades de dominio y las estructuras de base de datos, mantiene el ciclo de vida de las conexiones y garantiza la integridad del esquema de la base de datos al inicializarse. El público objetivo de este documento incluye a arquitectos de software, desarrolladores y mantenedores del sistema.

## 2. Descripción General

### 2.1 Perspectiva del Producto

El módulo de persistencia es un subsistema central que interactúa directamente con una base de datos relacional **MySQL** mediante **JDBC**. Sigue patrones arquitectónicos como Objetos de Transferencia de Datos (**DTO**) para encapsular la información y el patrón **Singleton** para la administración de la conexión a la base de datos.

### 2.2 Funciones del Sistema

- Encapsular los atributos de los datos del asociado mediante validaciones de integridad.
- Administrar un ciclo de vida centralizado de conexión a la base de datos con instancia única.
- Proveer y poblar automáticamente el esquema de la base de datos al iniciar el sistema.
- Ejecutar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre los registros de asociados mediante consultas SQL asociadas a parámetros.

## 3. Requisitos Específicos

### 3.1 Requisitos de Transferencia de Datos e Integridad

El sistema proveerá un componente `AsociadoDTO` que actúa como contenedor de datos con las siguientes reglas estructurales:

| Atributo          | Tipo de Dato | Reglas de Validación                              |
|-------------------|--------------|---------------------------------------------------|
| `nombre`          | `String`     | No puede ser nulo.                                |
| `apellido`        | `String`     | No puede ser nulo.                                |
| `dni`             | `String`     | No puede ser nulo; actúa como identificador único. |
| `ciudad` / `calle`| `String`     | No pueden ser nulos.                              |
| `numero`          | `int`        | Debe ser mayor o igual a 0.                       |
| `telefono`        | `String`     | No puede ser nulo.                                |

### 3.2 Requisitos de Conexión y Administración de Base de Datos

El sistema administrará la conectividad a la base de datos a través de un componente dedicado `BaseDeDatos`:

- El componente aplicará el patrón de diseño **Singleton** para restringir la instanciación a un único manejador de conexión activo durante toda la ejecución.
- El sistema inicializará la conexión utilizando parámetros de configuración predefinidos (URL y credenciales de usuario).
- Al inicializarse, el sistema ejecutará automáticamente una rutina de configuración del entorno (`iniciarBD`) para:
  - verificar o crear el esquema de base de datos objetivo,
  - eliminar y recrear la tabla `Asociados`,
  - poblarla con registros iniciales de prueba.
- El sistema proveerá interfaces de ejecución con parámetros para consultas, inserciones, actualizaciones y eliminaciones utilizando declaraciones preparadas (`PreparedStatement`) para prevenir vulnerabilidades de inyección SQL.

### 3.3 Requisitos de Operaciones de Acceso a Datos

El sistema proveerá un componente `DataAccessObject` (DAO) para exponer los servicios de persistencia a la lógica de la aplicación:

- **Operación de Guardado:** El sistema aceptará una entidad de asociado, validará que no sea nula, la convertirá al formato DTO y ejecutará una inserción SQL persistente.
- **Operaciones de Carga:** El sistema proveerá mecanismos para recuperar registros de asociados:
  - de manera individual por su identificador único,
  - en lotes acotados con un límite especificado,
  - como una colección completa desde la base de datos.
- **Operaciones de Actualización y Eliminación:** El sistema modificará registros existentes o eliminará entradas basándose en criterios únicos como el número de documento (DNI).
