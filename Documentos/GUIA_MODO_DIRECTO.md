# Guía de Uso - Servicio de Verificación SAF

## 🎯 Resumen de Cambios (2026-03-11)

El servicio de verificación ahora soporta **DOS MODOS** de operación:

### 1️⃣ MODO EXTERNO (Original)
Consulta predios desde el servicio externo usando cédula/RUC del propietario.

### 2️⃣ MODO DIRECTO (Nuevo)
Recibe la información de los predios directamente en la solicitud, sin consultar el servicio externo.

---

## 📋 Nuevos Componentes

### Clase: `PredioInfo`

Representa la información de un predio que puede enviarse directamente.

**Campos:**

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `predioId` | String | ✅ Sí | Identificador único del predio |
| `geometryWKT` | String | ✅ Sí | Geometría en formato WKT (EPSG:4326) |
| `areaM2` | Double | ❌ No | Área en m² (se calcula si no viene) |
| `predioCodigo` | String | ❌ No | Código catastral |
| `ownerIdentifier` | String | ❌ No | Cédula/RUC del propietario |
| `ownerName` | String | ❌ No | Nombre del propietario |
| `provincia` | String | ❌ No | Nombre de la provincia |
| `canton` | String | ❌ No | Nombre del cantón |
| `parroquia` | String | ❌ No | Nombre de la parroquia |
| `srid` | Integer | ❌ No | SRID (default: 4326) |

---

## 🔧 Request Modificado

### `VerifyPrediosByIdentifierRequest`

**Nuevo campo:**
```java
private List<PredioInfo> prediosData;
```

**Método helper:**
```java
public boolean hasDirectPredioData()
```

---

## 📡 Ejemplos de Uso

### Ejemplo 1: MODO EXTERNO (Comportamiento Original)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:ver="http://saf.com/verification">
   <soapenv:Body>
      <ver:verifyPrediosByIdentifier>
         <request>
            <identifierType>CEDULA</identifierType>
            <identifierValue>1234567890</identifierValue>
            <verificationType>AREAS_CONSERVACION</verificationType>
         </request>
      </ver:verifyPrediosByIdentifier>
   </soapenv:Body>
</soapenv:Envelope>
```

**Flujo:**
1. ✅ Consulta al servicio externo de predios
2. ✅ Obtiene geometrías de los predios del propietario
3. ✅ Valida contra capas forestales

---

### Ejemplo 2: MODO DIRECTO (Nuevo)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:ver="http://saf.com/verification">
   <soapenv:Body>
      <ver:verifyPrediosByIdentifier>
         <request>
            <verificationType>AREAS_CONSERVACION</verificationType>
            
            <prediosData>
               <PredioInfo>
                  <predioId>PREDIO-001</predioId>
                  <geometryWKT>POLYGON((-78.5 -0.5, -78.5 -0.51, -78.49 -0.51, -78.49 -0.5, -78.5 -0.5))</geometryWKT>
                  <areaM2>1255000</areaM2>
                  <provincia>PICHINCHA</provincia>
               </PredioInfo>
               
               <PredioInfo>
                  <predioId>PREDIO-002</predioId>
                  <geometryWKT>POLYGON((-78.6 -0.6, -78.6 -0.61, -78.59 -0.61, -78.59 -0.6, -78.6 -0.6))</geometryWKT>
                  <!-- areaM2 se calculará automáticamente -->
               </PredioInfo>
            </prediosData>
         </request>
      </ver:verifyPrediosByIdentifier>
   </soapenv:Body>
</soapenv:Envelope>
```

**Flujo:**
1. ❌ NO consulta al servicio externo
2. ✅ Usa las geometrías proporcionadas directamente
3. ✅ Calcula área automáticamente si no viene
4. ✅ Valida contra capas forestales

---

## 🎨 Ejemplos de Geometrías WKT

### Punto
```
POINT(-78.5 -0.5)
```

### Polígono Simple
```
POLYGON((-78.5 -0.5, -78.5 -0.51, -78.49 -0.51, -78.49 -0.5, -78.5 -0.5))
```

### Polígono con Hueco
```
POLYGON(
  (-78.5 -0.5, -78.5 -0.51, -78.49 -0.51, -78.49 -0.5, -78.5 -0.5),
  (-78.495 -0.505, -78.495 -0.506, -78.494 -0.506, -78.494 -0.505, -78.495 -0.505)
)
```

### MultiPolígono
```
MULTIPOLYGON(((-78.5 -0.5, -78.5 -0.51, -78.49 -0.51, -78.49 -0.5, -78.5 -0.5)))
```

---

## ⚙️ Lógica de Decisión

```
┌─────────────────────────────┐
│  Request recibido           │
└──────────┬──────────────────┘
           │
           ▼
    ¿prediosData != null
     && !prediosData.isEmpty()?
           │
     ┌─────┴─────┐
     │           │
    Sí          No
     │           │
     ▼           ▼
┌─────────┐  ┌──────────────┐
│  MODO   │  │    MODO      │
│ DIRECTO │  │  EXTERNO     │
└────┬────┘  └──────┬───────┘
     │              │
     │              ▼
     │     ┌────────────────┐
     │     │ Consultar      │
     │     │ Servicio       │
     │     │ Externo        │
     │     └────────┬───────┘
     │              │
     ▼              ▼
┌────────────────────────┐
│ Validar contra capas   │
│ forestales             │
└────────┬───────────────┘
         │
         ▼
┌─────────────────┐
│    Respuesta    │
└─────────────────┘
```

---

## 💡 Casos de Uso

### MODO EXTERNO
✅ Validación de predios ya registrados  
✅ Consulta por propietario (cédula/RUC)  
✅ Auditoría de predios existentes  

### MODO DIRECTO
✅ Pre-validación antes de registrar  
✅ Validación de lotes nuevos  
✅ Integración con sistemas GIS externos  
✅ Validación masiva con geometrías conocidas  
✅ Testing y desarrollo  
✅ Validación "en el aire" sin persistir datos  

---

## 🔍 Validaciones Automáticas

### En `PredioInfo`:
- ✅ `predioId` no puede estar vacío
- ✅ `geometryWKT` no puede estar vacío
- ✅ `geometryWKT` debe ser WKT válido

### Durante Procesamiento:
- ✅ Si `areaM2` es null/0 → Se calcula con PostGIS
- ✅ Si `srid` es null → Se usa 4326 (WGS84)
- ✅ Si WKT es inválido → Se reporta error para ese predio
- ✅ Los predios inválidos se saltan, continúa con los demás

---

## 📊 Cálculo Automático de Área

Cuando `areaM2` no viene o es 0:

```sql
SELECT ST_Area(ST_GeomFromText(?, 4326)::geography) AS area_m2
```

**Características:**
- Usa PostGIS `ST_Area` con `geography`
- Cálculo preciso en metros cuadrados
- Considera la curvatura de la Tierra
- Geometría debe estar en EPSG:4326

---

## 🚀 Beneficios

### Para Desarrolladores:
- ✅ Mayor flexibilidad en integraciones
- ✅ Reducción de dependencias externas
- ✅ Testing más fácil
- ✅ Retrocompatible (modo externo sigue funcionando)

### Para el Sistema:
- ✅ Menor carga en servicio externo
- ✅ Respuestas más rápidas (modo directo)
- ✅ Menos puntos de falla
- ✅ Validación independiente

### Para Usuarios:
- ✅ Validación previa sin registro
- ✅ Feedback inmediato
- ✅ Planificación más efectiva

---

## ⚠️ Consideraciones

1. **Formato WKT:** Debe estar en coordenadas geográficas (lat/lon), no proyectadas
2. **EPSG:4326:** Sistema de coordenadas WGS84 obligatorio
3. **Área:** Si se proporciona, debe estar en metros cuadrados
4. **Rendimiento:** Para validaciones masivas, considere lotes de máximo 50-100 predios
5. **Logs:** Ambos modos se registran igual en las tablas de auditoría

---

## 📝 Logging

Ambos modos generan logs completos:

```
🔵 Modo DIRECTO: Procesando 3 predios con información proporcionada
📍 Predio cargado desde datos directos: PREDIO-001 (125.50 ha)
📍 Área calculada para predio PREDIO-002: 500000.00 m²
✅ Validación completada exitosamente
```

```
🔵 Modo EXTERNO: Consultando servicio de predios para identificador: 1234567890
📡 Servicio externo respondió con 5 predios
✅ Validación completada exitosamente
```

---

## 🔗 Archivos Relacionados

- **Clase nueva:** `PredioInfo.java`
- **Modificado:** `VerifyPrediosByIdentifierRequest.java`
- **Modificado:** `VerificationService.java`
- **Ejemplos:** `ejemplos_uso_servicio.xml`

---

**Fecha de actualización:** 11 de Marzo de 2026  
**Versión:** 2.0  
**Autor:** Equipo SAF
