# SAF Verification Service - Sistema de Verificación de Áreas Forestales

## Fecha de Actualización
13 de enero de 2026

## Descripción del Proyecto

Sistema de verificación de áreas forestales desarrollado para el Ministerio del Ambiente del Ecuador (MAE). Implementa validaciones geoespaciales EUDR (European Union Deforestation Regulation) para predios agrícolas.

## Estructura del Proyecto

```
SAF_Services/
├── saf-verification-service/          # Proyecto principal
│   └── ANALISIS_CAPACIDAD.md         # Análisis de capacidad del sistema
├── scripts base de datos/            # Scripts de base de datos organizados
│   ├── 01_crear_bases_datos.sql      # Crear BD saf_interconexion y saf_postgis
│   ├── 02_crear_tablas_saf.sql       # Crear tablas del sistema SAF
│   ├── 03_crear_vistas_postgis.sql   # Vistas PostGIS para capas MAE
│   ├── 04_crear_usuario_aplicacion.sql # Usuario y permisos
│   ├── 05_datos_iniciales_capas.sql  # Datos de prueba para capas
│   ├── 06_datos_iniciales_configuracion.sql # Configuración inicial
│   └── README.md                     # Guía de instalación de BD
└── README.md                         # Este archivo
```

## Componentes Principales

### 1. Servicio de Verificación (`saf-verification-service/`)
- **Propósito**: Documentación y análisis del sistema de verificación
- **Contenido**: Análisis de capacidad, arquitectura y especificaciones
- **Estado**: Documentación completa del proyecto

### 2. Scripts de Base de Datos (`scripts base de datos/`)
- **Propósito**: Instalación completa de las bases de datos del sistema
- **Bases de datos**:
  - `saf_interconexion`: PostgreSQL con tablas del sistema SAF
  - `saf_postgis`: PostGIS con capas geográficas del MAE
- **Tablas principales**:
  - `saf_validation_layers`: 9 capas de validación geográfica
  - `saf_validation_thresholds`: Umbrales escalonados por tamaño de predio
  - `saf_request_logs`: Auditoría de solicitudes al servicio
  - `saf_error_logs`: Registro de errores del sistema
  - `saf_predio_logs`: Detalle de validación por predio

## Arquitectura del Sistema

### Tecnologías Utilizadas
- **Base de datos**: PostgreSQL con PostGIS para análisis geoespacial
- **Lenguaje**: Java 11 con JAX-WS para servicios SOAP
- **Servidor**: JBoss EAP 7.4 (despliegue)
- **Validaciones**: 9 capas geográficas del MAE
- **Umbrales**: Sistema escalonado por tamaño de predio (4 niveles)

### Capas de Validación Implementadas
1. **Áreas de Conservación Nacional** - Sin tolerancia
2. **Áreas de Conservación Regional** - Sin tolerancia
3. **Cobertura Boscosa (Bosque/No Bosque)** - Sin tolerancia
4. **Uso del Suelo Agrícola** - Validación de compatibilidad
5. **Uso del Suelo Forestal** - Validación de compatibilidad
6. **Zonas de Amortiguamiento** - Sin tolerancia
7. **Corredores Biológicos** - Sin tolerancia
8. **Fuentes de Agua** - Sin tolerancia
9. **Ríos Principales** - Sin tolerancia
10. **Infraestructura Crítica** - Sin tolerancia

### Umbrales de Validación por Tamaño
- **0-5 ha**: Sin tolerancia de intersección
- **5-50 ha**: Máximo 1% de intersección
- **50-500 ha**: Máximo 5% de intersección
- **500+ ha**: Máximo 10% de intersección

## Instalación

### 1. Base de Datos
```bash
cd "scripts base de datos"
# Ejecutar scripts en orden (01-06)
```

### 2. Servicio de Verificación
- Desplegar EAR en JBoss EAP 7.4
- Configurar datasource para saf_interconexion
- Configurar endpoint SOAP

## Documentación

### 📚 Documentos Principales
- **`Documentos/MANUAL_INSTALACION.md`**: Guía completa de instalación desde Java/Maven hasta BD
- **`Documentos/MANUAL_PROGRAMADOR.md`**: Arquitectura, flujo y funciones del sistema
- **`Documentos/DIAGRAMAS_ARQUITECTURA.md`**: Diagramas UML de clases y componentes

### 🗄️ Base de Datos
- **`scripts base de datos/README.md`**: Guía detallada de instalación de BD
- **`Documentos/DICCIONARIO_DATOS_SAF.md`**: Especificaciones completas de tablas

### 🔧 Desarrollo
- **`saf-verification-service/ANALISIS_CAPACIDAD.md`**: Análisis completo del sistema
- **`saf-verification-service/GUIA_INSTALACION.md`**: Guía técnica de instalación
- **`saf-verification-service/GUIA_PROGRAMADOR.md`**: Detalles de implementación

### 📊 Diagramas
- **`Documentos/DIAGRAMAS_ARQUITECTURA.md`**: Diagramas PlantUML (clases y componentes)
- **`Documentos/DIAGRAMAS_MERMAID.md`**: Diagramas Mermaid integrados
- **`Documentos/diagrama_*.mmd`**: Archivos Mermaid individuales
- **`Documentos/generar_diagramas_mermaid.sh`**: Script para generar PNG/SVG

## Estado del Proyecto

✅ **Completado**:
- Arquitectura del sistema definida
- Scripts de base de datos organizados
- 9 capas de validación implementadas
- Sistema de umbrales escalonados
- Documentación completa

🔄 **En desarrollo**:
- Implementación del servicio SOAP JAX-WS
- Integración con capas del MAE
- Testing automatizado

## Contacto

Proyecto desarrollado para el Ministerio del Ambiente del Ecuador (MAE) - Sistema de verificación EUDR.

---
*Última actualización: 13 de enero de 2026*