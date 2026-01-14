# Scripts de Base de Datos - Sistema SAF Interconexión

## Fecha de Actualización
13 de enero de 2026

## Descripción
Esta carpeta contiene todos los scripts necesarios para crear y configurar las bases de datos del Sistema SAF de Verificación de Áreas Forestales.

## ⚠️ Importante sobre Datos Geográficos
Los datos geográficos (capas del MAE) **NO** se incluyen en estos scripts porque serán proporcionados directamente por el servidor del Ministerio del Ambiente del Ecuador (MAE). Los scripts solo configuran la estructura de base de datos para recibir estos datos.

## Estructura de Scripts

### 01_crear_bases_datos.sql
- **Propósito**: Crear las bases de datos principales del sistema
- **Bases creadas**:
  - `saf_interconexion`: Base de datos principal para el servicio de verificación
  - `saf_postgis`: Base de datos para capas geográficas con PostGIS
- **Requisitos**: Ejecutar como superusuario de PostgreSQL
- **Extensiones**: Habilita PostGIS, PostGIS Topology y PostGIS Raster

### 02_crear_tablas_saf.sql
- **Propósito**: Crear todas las tablas del sistema SAF según el diccionario de datos
- **Tablas creadas**:
  - `saf_validation_layers`: Configuración de capas de validación
  - `saf_validation_thresholds`: Umbrales escalonados por tamaño de predio
  - `saf_request_logs`: Auditoría de solicitudes al servicio
  - `saf_error_logs`: Registro de errores y excepciones
  - `saf_predio_logs`: Detalle de validación por predio
  - `config_parameters`: Parámetros de configuración del sistema
- **Base de datos**: saf_interconexion

### 03_crear_vistas_postgis.sql
- **Propósito**: Crear vistas en la base de datos PostGIS para acceso optimizado
- **Base de datos**: saf_postgis
- **Vistas**: Vistas del MAE (Ministerio del Ambiente del Ecuador)
- **Nota**: Las vistas se crean sobre las tablas que serán pobladas por el MAE

### 04_crear_usuario_aplicacion.sql
- **Propósito**: Crear usuario de aplicación y configurar permisos
- **Usuario creado**: saf_app
- **Permisos**: Acceso de lectura/escritura a saf_interconexion, solo lectura a saf_postgis

### 05_datos_iniciales_configuracion.sql
- **Propósito**: Insertar configuración inicial del sistema SAF
- **Base de datos**: saf_interconexion
- **Contenido**:
  - 9 capas de validación activas (configuradas para las capas del MAE)
  - Umbrales escalonados por tamaño de predio (4 niveles)
  - Configuración de mensajes EUDR

## Orden de Ejecución

1. **Como superusuario PostgreSQL**:
   ```bash
   psql -U postgres -f 01_crear_bases_datos.sql
   ```

2. **En saf_interconexion**:
   ```bash
   psql -U postgres -d saf_interconexion -f 02_crear_tablas_saf.sql
   psql -U postgres -d saf_interconexion -f 05_datos_iniciales_configuracion.sql
   ```

3. **En saf_postgis**:
   ```bash
   psql -U postgres -d saf_postgis -f 03_crear_vistas_postgis.sql
   # NOTA: Los datos geográficos serán cargados por el MAE
   ```

4. **Configurar usuario de aplicación**:
   ```bash
   psql -U postgres -f 04_crear_usuario_aplicacion.sql
   ```

## 📋 Proceso de Carga de Datos Geográficos

Los datos geográficos del MAE deben ser cargados por el Ministerio del Ambiente del Ecuador siguiendo estos pasos:

1. **Coordinar con el MAE** para acceso a las capas oficiales
2. **Cargar las capas** en las tablas correspondientes de `saf_postgis`
3. **Verificar las vistas** creadas en el paso 3
4. **Configurar las capas** en `saf_validation_layers` según las tablas cargadas

### Tablas Esperadas en saf_postgis:
- `areas_conservacion` - Áreas de conservación nacional
- `areas_conservacion_regional` - Áreas de conservación regional
- `bosque_no_bosque` - Cobertura boscosa
- `uso_suelo_agricola` - Uso del suelo agrícola
- `uso_suelo_forestal` - Uso del suelo forestal
- `zonas_amortiguamiento` - Zonas de amortiguamiento
- `corredores_biologicos` - Corredores biológicos
- `fuentes_agua` - Fuentes de agua
- `rios_principales` - Ríos principales
- `infraestructura_critica` - Infraestructura crítica

## Verificación de Instalación

Después de ejecutar todos los scripts, verificar:

```sql
-- Conectar a saf_interconexion
\c saf_interconexion

-- Verificar tablas creadas
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'saf_%';

-- Verificar capas activas
SELECT COUNT(*) as capas_activas FROM saf_validation_layers WHERE active = true;

-- Verificar umbrales configurados
SELECT COUNT(*) as umbrales_configurados FROM saf_validation_thresholds;

-- Conectar a saf_postgis
\c saf_postgis

-- Verificar PostGIS
SELECT PostGIS_Version();

-- Verificar capas geográficas
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
```

## Notas Importantes

- Los scripts usan `IF NOT EXISTS` y `ON CONFLICT DO NOTHING` para evitar errores en re-ejecuciones
- Todas las claves foráneas tienen restricciones de integridad referencial
- Los índices están optimizados para las consultas del servicio
- Los datos geográficos son proporcionados por el MAE (Ministerio del Ambiente del Ecuador)
- Las contraseñas deben cambiarse en producción

## Soporte

Para soporte técnico, consultar la documentación en:
- `DICCIONARIO_DATOS_SAF.md`: Especificaciones completas de tablas
- `CONFIGURACION.md`: Guía de configuración del sistema
- `VALIDACION_IMPLEMENTACION.md`: Detalles de implementación