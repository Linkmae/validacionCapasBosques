# 📘 Manual de Usuario - Sistema SAF Verification Service

**Versión:** 1.0.0  
**Fecha:** 20 de enero de 2026  
**Sistema:** SAF Verification Service - Servicio de Verificación de Intersección con Capas de Bosques  
**Ministerio del Ambiente, Agua y Transición Ecológica (MAATE)**

---

## 📑 Tabla de Contenidos

1. [Introducción](#introducción)
2. [¿Qué es el SAF Verification Service?](#qué-es-el-saf-verification-service)
3. [Usuarios del Sistema](#usuarios-del-sistema)
4. [Acceso al Servicio](#acceso-al-servicio)
5. [Casos de Uso](#casos-de-uso)
6. [Guía de Uso](#guía-de-uso)
7. [Interpretación de Resultados](#interpretación-de-resultados)
8. [Mensajes y Códigos de Respuesta](#mensajes-y-códigos-de-respuesta)
9. [Preguntas Frecuentes](#preguntas-frecuentes)
10. [Soporte y Contacto](#soporte-y-contacto)

---

## 🎯 Introducción

Este manual proporciona instrucciones detalladas para usuarios finales que consumen el **SAF Verification Service**, un servicio web SOAP que permite verificar si un predio intersecta con capas geográficas de áreas de conservación y bosques protegidos del Ecuador.

### Objetivo del Manual

Guiar a los usuarios en:
- Comprender el propósito del servicio
- Realizar consultas correctamente
- Interpretar las respuestas del sistema
- Resolver problemas comunes

---

## 🔍 ¿Qué es el SAF Verification Service?

El **SAF Verification Service** es un servicio web SOAP que permite verificar la intersección de predios (terrenos) con capas geográficas de áreas protegidas y bosques del Ecuador.

### Funcionalidad Principal

Dado un identificador de predio (cédula del propietario, código catastral o número de escritura), el servicio:

1. **Consulta** los datos del predio en el Sistema de Administración Forestal (MAE)
2. **Valida** la intersección con capas geográficas de:
   - Bosques Protectores
   - Vegetación Protectora
   - Patrimonio Forestal del Estado
   - Áreas Naturales Protegidas
   - Socio Bosque
   - Otras capas de conservación
3. **Calcula** el área y porcentaje de intersección
4. **Determina** si el predio cumple con los umbrales permitidos
5. **Retorna** un resultado detallado con información georreferenciada

### Beneficios

- ✅ **Automatización** de verificaciones que antes eran manuales
- ✅ **Rapidez** en la obtención de resultados (segundos)
- ✅ **Precisión** mediante cálculos geoespaciales PostGIS
- ✅ **Trazabilidad** completa de todas las consultas
- ✅ **Integración** con sistemas externos mediante SOAP

---

## 👥 Usuarios del Sistema

### Perfil 1: Sistemas Integradores

**Descripción:** Aplicaciones externas que consumen el servicio SOAP  
**Ejemplos:**
- Sistema de Licencias Ambientales
- Sistema de Permisos de Aprovechamiento Forestal
- Sistema de Registro de Predios
- Portales web ciudadanos

**Acceso:** Mediante credenciales de servicio y endpoint SOAP

### Perfil 2: Desarrolladores

**Descripción:** Técnicos que implementan la integración  
**Responsabilidades:**
- Implementar clientes SOAP
- Manejar errores y excepciones
- Interpretar respuestas XML

**Requisitos:**
- Conocimientos de SOAP/WSDL
- Lenguajes: Java, .NET, PHP, Python, etc.

### Perfil 3: Analistas de Negocio

**Descripción:** Personal que supervisa resultados  
**Responsabilidades:**
- Validar lógica de negocio
- Revisar umbrales de validación
- Generar reportes estadísticos

**Acceso:** Consulta de logs y reportes de auditoría

---

## 🔐 Acceso al Servicio

### Información del Endpoint

#### Ambiente de Producción
```
Endpoint: http://servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService
WSDL: http://servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService?wsdl
```

#### Ambiente de Pruebas (QA)
```
Endpoint: http://qa-servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService
WSDL: http://qa-servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService?wsdl
```

#### Ambiente de Desarrollo
```
Endpoint: http://dev-servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService
WSDL: http://dev-servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService?wsdl
```

### Requisitos Técnicos

- **Protocolo:** SOAP 1.1 / 1.2
- **Formato:** XML
- **Autenticación:** No requiere (por ahora)
- **Timeout recomendado:** 30 segundos
- **Rate limit:** 100 requests/minuto

---

## 💼 Casos de Uso

### Caso de Uso 1: Verificación por Cédula del Propietario

**Escenario:** Un ciudadano solicita un permiso de aprovechamiento forestal  
**Proceso:**
1. Sistema de Permisos consulta predios por cédula
2. SAF retorna todos los predios del propietario
3. Sistema valida cada predio contra capas de bosques
4. Se determina si procede o no el permiso

**Ejemplo:**
```
Entrada: Cédula 1750702068
Salida: 3 predios encontrados, 1 intersecta con Bosques Protectores
```

---

### Caso de Uso 2: Validación por Código de Predio

**Escenario:** Verificar un predio específico antes de otorgar licencia  
**Proceso:**
1. Sistema envía código catastral del predio
2. SAF consulta geometría del predio
3. Calcula intersecciones con todas las capas
4. Retorna porcentajes de afectación

**Ejemplo:**
```
Entrada: Código PRD_001234
Salida: Intersecta 16.7% con Vegetación Protectora (umbral 5%), NO APROBADO
```

---

### Caso de Uso 3: Consulta por Número de Escritura

**Escenario:** Validar predio en proceso de transferencia  
**Proceso:**
1. Notaría envía número de escritura
2. SAF identifica el predio asociado
3. Valida contra áreas protegidas
4. Genera reporte de afectaciones

**Ejemplo:**
```
Entrada: Escritura ESC-2024-001234
Salida: No intersecta con ninguna capa protegida, APROBADO
```

---

## 📖 Guía de Uso

### Estructura de la Solicitud SOAP

#### Operación: `verifyPrediosByIdentifier`

**Parámetros de Entrada:**

| Parámetro | Tipo | Requerido | Descripción | Valores Permitidos |
|-----------|------|-----------|-------------|-------------------|
| `identifierType` | String | Sí | Tipo de identificador | CEDULA, CODIGO_PREDIO, ESCRITURA |
| `identifierValue` | String | Sí | Valor del identificador | Cédula, código o número |
| `verificationType` | String | No | Tipo de validación | AREAS_CONSERVACION, BOSQUES, TODOS |
| `layerNames` | List<String> | No | Capas específicas a verificar | Nombres de capas |

**Ejemplo XML de Solicitud:**

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:ver="http://saf.com/verification">
   <soapenv:Header/>
   <soapenv:Body>
      <ver:verifyPrediosByIdentifier>
         <request>
            <identifierType>CEDULA</identifierType>
            <identifierValue>1750702068</identifierValue>
            <verificationType>AREAS_CONSERVACION</verificationType>
         </request>
      </ver:verifyPrediosByIdentifier>
   </soapenv:Body>
</soapenv:Envelope>
```

---

### Estructura de la Respuesta SOAP

**Elementos Principales:**

```xml
<response>
    <identifierEcho>1750702068</identifierEcho>
    <requestStatus>
        <code>SUCCESS</code>
        <errorType></errorType>
        <message>Verificación completada exitosamente</message>
    </requestStatus>
    <summary>
        <totalPredios>3</totalPredios>
        <prediosWithIntersection>1</prediosWithIntersection>
        <prediosWithoutIntersection>2</prediosWithoutIntersection>
        <totalLayersChecked>12</totalLayersChecked>
        <layersNotLoaded>0</layersNotLoaded>
    </summary>
    <predioVerifications>
        <predioVerification>
            <!-- Detalles del predio 1 -->
        </predioVerification>
        <predioVerification>
            <!-- Detalles del predio 2 -->
        </predioVerification>
    </predioVerifications>
</response>
```

---

### Ejemplo Completo de Respuesta

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:verifyPrediosByIdentifierResponse xmlns:ns2="http://saf.com/verification">
         <return>
            <identifierEcho>1750702068</identifierEcho>
            <requestStatus>
               <code>SUCCESS</code>
               <message>Verificación completada exitosamente</message>
            </requestStatus>
            <summary>
               <totalPredios>1</totalPredios>
               <prediosWithIntersection>1</prediosWithIntersection>
               <prediosWithoutIntersection>0</prediosWithoutIntersection>
               <totalLayersChecked>4</totalLayersChecked>
               <layersNotLoaded>0</layersNotLoaded>
            </summary>
            <predioVerifications>
               <predioVerification>
                  <predioId>PRD_001234</predioId>
                  <predioCodigo>CAT-2024-001234</predioCodigo>
                  <predioOwnerCedula>1750702068</predioOwnerCedula>
                  <predioOwnerName>JUAN PEREZ GOMEZ</predioOwnerName>
                  <predioAreaM2>15000.50</predioAreaM2>
                  <predioSRID>32717</predioSRID>
                  <predioGeometryGeoJSON>{"type":"Polygon","coordinates":[...]}</predioGeometryGeoJSON>
                  
                  <layersResults>
                     <layerResult>
                        <layerId>bosques_protectores</layerId>
                        <layerName>Bosques Protectores</layerName>
                        <wmsLayerName>mae_bosques:bosques_protectores</wmsLayerName>
                        <intersects>true</intersects>
                        <intersectionAreaM2>2500.75</intersectionAreaM2>
                        <percentage>16.67</percentage>
                        <intersectionGeoJSON>{"type":"Polygon","coordinates":[...]}</intersectionGeoJSON>
                        <validationPassed>false</validationPassed>
                        <validationMessage>Predio intersecta 16.67% con Bosques Protectores (máximo permitido: 5%)</validationMessage>
                        <maxAllowedPercentage>5.0</maxAllowedPercentage>
                        <layerNotLoaded>false</layerNotLoaded>
                     </layerResult>
                     
                     <layerResult>
                        <layerId>vegetacion_protectora</layerId>
                        <layerName>Vegetación Protectora</layerName>
                        <wmsLayerName>mae_bosques:vegetacion_protectora</wmsLayerName>
                        <intersects>false</intersects>
                        <intersectionAreaM2>0.0</intersectionAreaM2>
                        <percentage>0.0</percentage>
                        <validationPassed>true</validationPassed>
                        <validationMessage>Predio no intersecta con Vegetación Protectora</validationMessage>
                        <layerNotLoaded>false</layerNotLoaded>
                     </layerResult>
                  </layersResults>
               </predioVerification>
            </predioVerifications>
         </return>
      </ns2:verifyPrediosByIdentifierResponse>
   </soap:Body>
</soap:Envelope>
```

---

## 📊 Interpretación de Resultados

### Estados de la Solicitud

| Código | Descripción | Acción |
|--------|-------------|--------|
| `SUCCESS` | Verificación exitosa | Procesar resultados |
| `ERROR` | Error en el procesamiento | Revisar mensaje de error |
| `PARTIAL` | Éxito parcial (algunas capas no disponibles) | Revisar capas no cargadas |
| `NO_PREDIOS_FOUND` | No se encontraron predios | Verificar identificador |

### Interpretación de Intersecciones

#### ✅ Validación Aprobada
```xml
<validationPassed>true</validationPassed>
<validationMessage>Predio no intersecta con Bosques Protectores</validationMessage>
```
**Significado:** El predio NO intersecta o intersecta dentro del umbral permitido.  
**Acción:** Puede proceder con el trámite.

#### ❌ Validación Rechazada
```xml
<validationPassed>false</validationPassed>
<validationMessage>Predio intersecta 16.67% con Bosques Protectores (máximo permitido: 5%)</validationMessage>
<maxAllowedPercentage>5.0</maxAllowedPercentage>
```
**Significado:** El predio excede el umbral permitido de intersección.  
**Acción:** Requiere análisis adicional o permiso especial.

### Campos Importantes

#### 1. `intersects` (boolean)
- `true`: Existe intersección geométrica
- `false`: No hay intersección

#### 2. `intersectionAreaM2` (decimal)
- Área de intersección en metros cuadrados
- Ejemplo: 2500.75 m²

#### 3. `percentage` (decimal)
- Porcentaje del predio que intersecta
- Fórmula: `(área_intersección / área_predio) × 100`
- Ejemplo: 16.67%

#### 4. `validationPassed` (boolean)
- `true`: Cumple con el umbral
- `false`: Excede el umbral

#### 5. `layerNotLoaded` (boolean)
- `true`: La capa no estaba disponible en el sistema
- `false`: La capa se consultó correctamente

---

## 🚨 Mensajes y Códigos de Respuesta

### Mensajes de Éxito

| Mensaje | Significado |
|---------|-------------|
| "Verificación completada exitosamente" | Todos los predios fueron validados correctamente |
| "Predio no intersecta con [capa]" | No hay intersección con esa capa específica |
| "Predio cumple con el umbral permitido" | Intersección dentro del límite aceptable |

### Mensajes de Advertencia

| Mensaje | Significado | Acción |
|---------|-------------|--------|
| "Capa [nombre] no disponible en el sistema" | La capa no pudo ser consultada | Contactar soporte |
| "No se encontraron predios para el identificador [valor]" | El identificador no existe en el sistema MAE | Verificar dato |
| "Geometría del predio es nula o inválida" | Predio sin coordenadas | Actualizar datos en MAE |

### Mensajes de Error

| Código Error | Mensaje | Causa | Solución |
|--------------|---------|-------|----------|
| `VALIDATION_ERROR` | "Identificador inválido" | Formato incorrecto | Verificar formato de cédula/código |
| `SERVICE_ERROR` | "Servicio de predios no disponible" | MAE fuera de línea | Reintentar más tarde |
| `CONNECTION_ERROR` | "Error conectando a base de datos" | Fallo de BD | Contactar soporte técnico |
| `SPATIAL_ERROR` | "Error calculando intersección geométrica" | Geometría corrupta | Reportar al equipo técnico |
| `INTERNAL_ERROR` | "Error interno del servidor" | Excepción no controlada | Reportar con ID de solicitud |

---

## ❓ Preguntas Frecuentes

### 1. ¿Cuánto tiempo tarda una consulta?

**Respuesta:** El tiempo promedio es de 2-5 segundos, dependiendo de:
- Cantidad de predios del propietario
- Número de capas a validar
- Complejidad de las geometrías

### 2. ¿Puedo consultar varios predios a la vez?

**Respuesta:** Sí, si usa identificador tipo CEDULA, automáticamente se retornan todos los predios del propietario. Para códigos específicos, debe hacer una consulta por predio.

### 3. ¿Qué significa "capa no disponible"?

**Respuesta:** Indica que la capa geográfica no está cargada en el sistema PostGIS o tiene un error. El resultado de esa capa será marcado como `layerNotLoaded=true`.

### 4. ¿Los porcentajes son exactos?

**Respuesta:** Sí, se calculan mediante operaciones geométricas precisas de PostGIS con geometrías en proyección UTM zona 17S (EPSG:32717).

### 5. ¿Puedo obtener la geometría de la intersección?

**Respuesta:** Sí, el campo `intersectionGeoJSON` contiene la geometría de la intersección en formato GeoJSON (coordenadas WGS84).

### 6. ¿Qué pasa si cambio los umbrales de validación?

**Respuesta:** Los umbrales se cargan desde la base de datos y se actualizan automáticamente cada 5 minutos. No requiere redespliegue del servicio.

### 7. ¿Hay límite de consultas?

**Respuesta:** Actualmente hay un rate limit de 100 requests por minuto por IP. Para volúmenes mayores, contactar al equipo técnico.

### 8. ¿Se guardan las consultas?

**Respuesta:** Sí, todas las consultas se registran en la base de datos `saf_interconexion` para auditoría y trazabilidad.

### 9. ¿Puedo filtrar capas específicas?

**Respuesta:** Sí, use el parámetro `layerNames` con los nombres de las capas que desea validar:
```xml
<layerNames>
   <layerName>Bosques Protectores</layerName>
   <layerName>Vegetación Protectora</layerName>
</layerNames>
```

### 10. ¿Qué hago si recibo un error 500?

**Respuesta:** Anote el `identifierEcho` (ID de solicitud) del error y contacte a soporte técnico. El ID permite rastrear el problema en los logs.

---

## 🛠️ Ejemplos de Integración

### Ejemplo en Java (JAX-WS)

```java
import com.saf.verification.VerificationService;
import com.saf.verification.VerificationServiceService;
import com.saf.verification.VerifyPrediosByIdentifierRequest;
import com.saf.verification.VerifyPrediosByIdentifierResponse;

public class SAFClient {
    public static void main(String[] args) {
        try {
            // 1. Crear servicio
            VerificationServiceService service = new VerificationServiceService();
            VerificationService port = service.getVerificationServicePort();
            
            // 2. Crear request
            VerifyPrediosByIdentifierRequest request = new VerifyPrediosByIdentifierRequest();
            request.setIdentifierType("CEDULA");
            request.setIdentifierValue("1750702068");
            request.setVerificationType("AREAS_CONSERVACION");
            
            // 3. Llamar servicio
            VerifyPrediosByIdentifierResponse response = port.verifyPrediosByIdentifier(request);
            
            // 4. Procesar respuesta
            System.out.println("Estado: " + response.getRequestStatus().getCode());
            System.out.println("Mensaje: " + response.getRequestStatus().getMessage());
            System.out.println("Total predios: " + response.getSummary().getTotalPredios());
            
            // 5. Iterar resultados
            for (PredioVerification predio : response.getPredioVerifications()) {
                System.out.println("\nPredio: " + predio.getPredioCodigo());
                for (LayerResult layer : predio.getLayersResults()) {
                    System.out.println("  Capa: " + layer.getLayerName());
                    System.out.println("  Intersecta: " + layer.isIntersects());
                    System.out.println("  Porcentaje: " + layer.getPercentage() + "%");
                    System.out.println("  Aprobado: " + layer.isValidationPassed());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Ejemplo en Python (zeep)

```python
from zeep import Client

# 1. Crear cliente SOAP
wsdl_url = "http://servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService?wsdl"
client = Client(wsdl=wsdl_url)

# 2. Crear request
request = {
    'identifierType': 'CEDULA',
    'identifierValue': '1750702068',
    'verificationType': 'AREAS_CONSERVACION'
}

# 3. Llamar servicio
try:
    response = client.service.verifyPrediosByIdentifier(request)
    
    # 4. Procesar respuesta
    print(f"Estado: {response['requestStatus']['code']}")
    print(f"Mensaje: {response['requestStatus']['message']}")
    print(f"Total predios: {response['summary']['totalPredios']}")
    
    # 5. Iterar resultados
    for predio in response['predioVerifications']:
        print(f"\nPredio: {predio['predioCodigo']}")
        print(f"Propietario: {predio['predioOwnerName']}")
        
        for layer in predio['layersResults']:
            print(f"  - {layer['layerName']}: {layer['percentage']:.2f}% " +
                  f"({'APROBADO' if layer['validationPassed'] else 'RECHAZADO'})")
            
except Exception as e:
    print(f"Error: {str(e)}")
```

### Ejemplo en PHP (SoapClient)

```php
<?php
// 1. Configurar cliente SOAP
$wsdl = "http://servicios.ambiente.gob.ec/saf-verification-service/VerificationService/VerificationService?wsdl";
$client = new SoapClient($wsdl, [
    'trace' => 1,
    'exceptions' => true,
    'connection_timeout' => 30
]);

// 2. Crear request
$request = [
    'identifierType' => 'CEDULA',
    'identifierValue' => '1750702068',
    'verificationType' => 'AREAS_CONSERVACION'
];

try {
    // 3. Llamar servicio
    $response = $client->verifyPrediosByIdentifier(['request' => $request]);
    
    // 4. Procesar respuesta
    $result = $response->return;
    
    echo "Estado: " . $result->requestStatus->code . "\n";
    echo "Mensaje: " . $result->requestStatus->message . "\n";
    echo "Total predios: " . $result->summary->totalPredios . "\n";
    
    // 5. Iterar resultados
    foreach ($result->predioVerifications as $predio) {
        echo "\nPredio: " . $predio->predioCodigo . "\n";
        
        foreach ($predio->layersResults as $layer) {
            $status = $layer->validationPassed ? 'APROBADO' : 'RECHAZADO';
            echo sprintf("  - %s: %.2f%% (%s)\n",
                $layer->layerName,
                $layer->percentage,
                $status
            );
        }
    }
    
} catch (SoapFault $e) {
    echo "Error SOAP: " . $e->getMessage() . "\n";
}
?>
```

---

## 📞 Soporte y Contacto

### Mesa de Ayuda Técnica

**Horario de Atención:** Lunes a Viernes, 08:00 - 17:00  
**Email:** soporte.saf@ambiente.gob.ec  
**Teléfono:** 1800-AMBIENTE (1800-262-436)

### Reportar Problemas

Al reportar un problema, incluya:
1. **ID de solicitud** (`identifierEcho` de la respuesta)
2. **Timestamp** de la consulta
3. **Mensaje de error** completo
4. **Datos de entrada** utilizados
5. **Ambiente** (producción, QA, desarrollo)

### Solicitar Soporte

**Para solicitudes de integración:**
- Email: integraciones@ambiente.gob.ec
- Asunto: "Integración SAF Verification Service - [Nombre Sistema]"

**Para reportar bugs:**
- Email: bugs.saf@ambiente.gob.ec
- Prioridad: ALTA / MEDIA / BAJA

**Para consultas de negocio:**
- Email: saf@ambiente.gob.ec

---

## 📝 Glosario

| Término | Definición |
|---------|------------|
| **Predio** | Terreno o propiedad rural registrada catastralmente |
| **Intersección** | Sobreposición geométrica entre el predio y una capa geográfica |
| **Capa Geográfica** | Conjunto de datos espaciales que representan áreas protegidas o bosques |
| **PostGIS** | Extensión de PostgreSQL para manejo de datos geoespaciales |
| **WKT** | Well-Known Text, formato de texto para geometrías |
| **GeoJSON** | Formato JSON para representar geometrías geográficas |
| **SOAP** | Simple Object Access Protocol, protocolo de servicios web |
| **WSDL** | Web Services Description Language, define la interfaz del servicio |
| **Umbral** | Porcentaje máximo permitido de intersección |
| **SRID** | Spatial Reference System Identifier, código del sistema de coordenadas |
| **UTM 17S** | Sistema de coordenadas Universal Transversa de Mercator zona 17 Sur |

---

## 📄 Apéndices

### Apéndice A: Tipos de Capas Disponibles

| Capa | Descripción | Umbral Típico |
|------|-------------|---------------|
| Bosques Protectores | Bosques declarados bajo régimen de protección | 5% |
| Vegetación Protectora | Áreas con vegetación crítica para conservación | 5% |
| Patrimonio Forestal del Estado | Bosques propiedad del Estado | 0% |
| Áreas Naturales Protegidas | Reservas, parques nacionales, etc. | 0% |
| Socio Bosque | Predios bajo programa de conservación | 10% |

### Apéndice B: Códigos SRID Utilizados

| SRID | Sistema | Uso |
|------|---------|-----|
| 4326 | WGS84 | Coordenadas geográficas (lat/lon) |
| 32717 | UTM 17S | Cálculos de área en Ecuador |

### Apéndice C: Formato de Identificadores

| Tipo | Formato | Ejemplo |
|------|---------|---------|
| CEDULA | 10 dígitos | 1750702068 |
| CODIGO_PREDIO | Alfanumérico | PRD_001234 o CAT-2024-001234 |
| ESCRITURA | Alfanumérico | ESC-2024-001234 |

---

## 📜 Control de Versiones

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0.0 | 2026-01-20 | linkmae | Creación inicial del manual |

---

**© 2026 Ministerio del Ambiente, Agua y Transición Ecológica (MAATE)**  
**Todos los derechos reservados**
