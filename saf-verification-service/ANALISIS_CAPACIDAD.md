# 📊 Análisis de Capacidad - SAF Verification Service

## 🎯 Propósito

Este documento presenta un análisis de capacidad del servicio de verificación SAF con proyecciones para los próximos 5 años (2026-2030), considerando los tres ambientes principales: Desarrollo, QA y Producción.

## 📈 Metodología

### Factores Considerados
- **Crecimiento del MAE**: Aumento esperado del 15% anual en usuarios y predios
- **Optimización Continua**: Mejoras en rendimiento y eficiencia
- **Tecnologías**: Evolución de hardware y software
- **Escalabilidad**: Arquitectura preparada para crecimiento

### Escenarios de Carga
- **Base 2026**: 1,000 requests/día (estado actual)
- **Crecimiento**: 15% anual compuesto
- **Pico**: 3x promedio diario en horas pico

---

## 🏗️ Arquitectura por Ambiente

### Ambiente Desarrollo
**Propósito**: Desarrollo, pruebas unitarias, debugging
```text
┌─────────────────────────────────────────────────────────────┐
│  💻 DESARROLLO (Local/VM)                                  │
├─────────────────────────────────────────────────────────────┤
│  • CPU: 2-4 cores                                          │
│  • RAM: 4-8 GB                                             │
│  • Disco: 50-100 GB                                        │
│  • Red: 10-50 Mbps                                         │
│  • JBoss: Standalone mode                                  │
│  • Base de Datos: PostgreSQL local (solo logs)             │
└─────────────────────────────────────────────────────────────┘
```

### Ambiente QA
**Propósito**: Pruebas de integración, carga, aceptación
```text
┌─────────────────────────────────────────────────────────────┐
│  🧪 QA (Servidor Dedicado)                                 │
├─────────────────────────────────────────────────────────────┤
│  • CPU: 4-8 cores                                          │
│  • RAM: 8-16 GB                                            │
│  • Disco: 200-500 GB                                       │
│  • Red: 50-100 Mbps                                        │
│  • JBoss: Standalone mode (1-2 nodos)                      │
│  • Base de Datos: PostgreSQL local (solo logs)             │
│  • Load Balancer: Opcional (Nginx)                         │
└─────────────────────────────────────────────────────────────┘
```

### Ambiente Producción
**Propósito**: Servicio en producción para usuarios finales
```text
┌─────────────────────────────────────────────────────────────┐
│  🏭 PRODUCCIÓN (Servidor Empresarial)                      │
├─────────────────────────────────────────────────────────────┤
│  • CPU: 8-16 cores                                         │
│  • RAM: 16-32 GB                                           │
│  • Disco: 500 GB - 1.5 TB                                  │
│  • Red: 100-500 Mbps                                       │
│  • JBoss: Standalone mode (1-2 nodos para HA)              │
│  • Base de Datos: PostgreSQL local (solo logs)             │
│  • Load Balancer: F5/Netscaler (si cluster)                │
│  • Backup: Sistemas redundantes                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Matriz de Capacidad 2026-2030

### Transacciones por Segundo (TPS)

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 5 TPS | 20 TPS | 50 TPS | Base actual optimizada |
| **2027** | 6 TPS | 25 TPS | 60 TPS | +15% crecimiento +20% optimización |
| **2028** | 7 TPS | 30 TPS | 75 TPS | +15% crecimiento +20% optimización |
| **2029** | 8 TPS | 35 TPS | 90 TPS | +15% crecimiento +20% optimización |
| **2030** | 10 TPS | 45 TPS | 110 TPS | +15% crecimiento +20% optimización |

**Cálculo TPS**:
- 1 request = 1-6 consultas PostGIS (promedio 4)
- Tiempo promedio por request: 2-5 segundos
- Factor de concurrencia: 10-50 usuarios simultáneos
- **Nota**: PostGIS corre en servidor separado del MAE

### Almacenamiento (GB)

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 50 GB | 200 GB | 500 GB | Logs + configuración |
| **2027** | 60 GB | 250 GB | 650 GB | +15% requests + logs |
| **2028** | 70 GB | 320 GB | 850 GB | +15% requests + logs |
| **2029** | 85 GB | 400 GB | 1.1 TB | +15% requests + logs |
| **2030** | 100 GB | 500 GB | 1.4 TB | +15% requests + logs |

**Distribución de Almacenamiento** (solo aplicación):
- **Logs de BD**: 70% (saf_request_logs, saf_predio_logs)
- **Logs de aplicación**: 20% (JBoss server.log, gc.log)
- **Configuración y backups**: 10% (config files, snapshots)
- **Nota**: Datos PostGIS en servidor separado del MAE

### Memoria RAM (GB)

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 4 GB | 8 GB | 16 GB | Base actual |
| **2027** | 5 GB | 10 GB | 20 GB | +25% eficiencia |
| **2028** | 6 GB | 12 GB | 24 GB | +25% eficiencia |
| **2029** | 7 GB | 15 GB | 30 GB | +25% eficiencia |
| **2030** | 8 GB | 18 GB | 36 GB | +25% eficiencia |

**Uso de Memoria** (por servidor):
- **JBoss EAP**: 2-4 GB por instancia
- **Cache de reglas**: 1-2 GB (ConcurrentHashMap)
- **Conexiones DB**: 0.5-1 GB por pool de conexiones
- **Overhead**: 20-30% reserva para picos
- **Nota**: Sin datos PostGIS locales

### CPU (Núcleos)

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 2 cores | 4 cores | 8 cores | Base actual |
| **2027** | 2 cores | 6 cores | 10 cores | +25% eficiencia |
| **2028** | 4 cores | 8 cores | 12 cores | +25% eficiencia |
| **2029** | 4 cores | 10 cores | 16 cores | +25% eficiencia |
| **2030** | 6 cores | 12 cores | 20 cores | +25% eficiencia |

**Distribución CPU** (por servidor):
- **Procesamiento SOAP**: 30% (parsing XML, serialización)
- **Cache management**: 20% (ConcurrentHashMap operations)
- **Consultas DB**: 25% (preparación y envío de queries)
- **Logging**: 15% (escritura a BD local)
- **Overhead**: 10% (JBoss, OS, monitoring)

### Red (Mbps)

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 10 Mbps | 50 Mbps | 100 Mbps | Base actual |
| **2027** | 15 Mbps | 65 Mbps | 130 Mbps | +15% crecimiento |
| **2028** | 20 Mbps | 85 Mbps | 170 Mbps | +15% crecimiento |
| **2029** | 25 Mbps | 110 Mbps | 220 Mbps | +15% crecimiento |
| **2030** | 30 Mbps | 140 Mbps | 280 Mbps | +15% crecimiento |

**Uso de Red** (ancho de banda real):
- **SOAP Requests/Responses**: 80% (paquetes ~11 KB promedio)
- **Consultas DB externas**: 15% (queries a PostGIS del MAE)
- **Logs externos**: 3% (syslog, monitoring)
- **Overhead**: 2% (protocolos, ACKs)
- **Pico diario**: ~200 KB/min (basado en 1000 requests/día)

### Puertos Utilizados

| Servicio | Desarrollo | QA | Producción | Protocolo |
|----------|------------|----|------------|-----------|
| **JBoss HTTP** | 8580 | 8580-8583 | 8580-8599 | HTTP/HTTPS |
| **JBoss Admin** | 10490 | 10490-10493 | 10490-10509 | HTTP/HTTPS |
| **PostgreSQL** | 5432 | 5432 | 5432/6432 | TCP |
| **PostGIS** | 5432 | 5432 | 5432/6432 | TCP |
| **Load Balancer** | - | 8580 | 8580 | HTTP/HTTPS |
| **Monitoring** | 9090 | 9090-9093 | 9090-9109 | HTTP |

### Volumen de Envíos de Correo

| Año | Desarrollo | QA | Producción | Notas |
|-----|------------|----|------------|-------|
| **2026** | 10/día | 50/día | 500/día | Alertas técnicas |
| **2027** | 12/día | 65/día | 650/día | +15% crecimiento |
| **2028** | 15/día | 85/día | 850/día | +15% crecimiento |
| **2029** | 20/día | 110/día | 1,100/día | +15% crecimiento |
| **2030** | 25/día | 140/día | 1,400/día | +15% crecimiento |

**Tipos de Correo**:
- **Alertas Críticas**: 10% (errores de sistema, downtime)
- **Reportes Diarios**: 30% (estadísticas, KPIs)
- **Alertas de Validación**: 50% (predios rechazados, umbrales excedidos)
- **Notificaciones**: 10% (mantenimiento, actualizaciones)

---

## 📈 Proyecciones de Carga por Año

### 2026 (Base Actual)
- **Requests/día**: 1,000 (estado actual)
- **Predios procesados/día**: 4,000 (promedio 4 predios/request)
- **Capas verificadas/día**: 16,000 (4 capas × 4 predios × 1,000 requests)
- **Tamaño promedio paquete**: 3 KB (request) + 8 KB (response) = 11 KB total
- **Tráfico diario**: ~11 MB (1,000 requests × 11 KB)

### 2027 (+15% crecimiento)
- **Requests/día**: 1,150
- **Predios procesados/día**: 4,600
- **Capas verificadas/día**: 18,400
- **Tráfico diario**: ~12.7 MB
- **Optimizaciones**: Cache mejorado, índices optimizados

### 2028 (+15% crecimiento)
- **Requests/día**: 1,323
- **Predios procesados/día**: 5,292
- **Capas verificadas/día**: 21,168
- **Tráfico diario**: ~14.6 MB
- **Optimizaciones**: Consultas PostGIS optimizadas

### 2029 (+15% crecimiento)
- **Requests/día**: 1,521
- **Predios procesados/día**: 6,084
- **Capas verificadas/día**: 24,336
- **Tráfico diario**: ~16.8 MB
- **Optimizaciones**: Cluster JBoss implementado

### 2030 (+15% crecimiento)
- **Requests/día**: 1,749
- **Predios procesados/día**: 6,996
- **Capas verificadas/día**: 27,984
- **Tráfico diario**: ~19.3 MB
- **Optimizaciones**: Arquitectura cloud-native

---

## 🔧 Estrategias de Escalabilidad

### Escalabilidad Horizontal
1. **Cluster JBoss**: Añadir nodos para aumentar TPS
2. **BD Cluster**: PostgreSQL con réplicas de lectura
3. **Load Balancing**: Distribución automática de carga

### Escalabilidad Vertical
1. **CPU**: Aumentar cores por servidor
2. **RAM**: Más memoria para cache y conexiones
3. **Disco**: SSD de alta velocidad para BD

### Optimizaciones de Software
1. **Cache**: Estrategias avanzadas de cache
2. **Pooling**: Optimización de conexiones
3. **Async**: Procesamiento asíncrono para operaciones pesadas

---

## 📋 Checklist de Monitoreo

### Métricas Críticas
- [ ] TPS actual vs. proyectado
- [ ] Latencia promedio (< 5 segundos)
- [ ] Uso de CPU (< 80%)
- [ ] Uso de RAM (< 85%)
- [ ] Espacio en disco (> 20% libre)
- [ ] Errores por hora (< 1%)
- [ ] Tiempo de respuesta BD (< 2 segundos)

### Alertas Automáticas
- [ ] CPU > 90% por 5 minutos
- [ ] RAM > 95% por 2 minutos
- [ ] Disco > 90% utilizado
- [ ] TPS < 50% del esperado
- [ ] Errores > 5% de requests
- [ ] Servicio no responde > 30 segundos

---

## 🎯 Recomendaciones

### Para Desarrollo
- Mantener configuración básica de 2-4 cores, 4-8 GB RAM
- Enfoque en debugging y testing unitario
- Actualizaciones de hardware cada 2-3 años

### Para QA
- Servidor dedicado con 4-8 cores, 8-16 GB RAM
- Pruebas de carga automatizadas con JMeter
- Monitoreo continuo de rendimiento

### Para Producción
- Servidor empresarial con 8-16 cores, 16-32 GB RAM
- Alta disponibilidad con 1-2 nodos JBoss
- Monitoreo 24/7 con alertas automáticas
- Plan de contingencia y backup de logs

### Plan de Implementación
1. **2026**: Optimización del código actual (2-8 cores, 4-16 GB)
2. **2027**: Implementación en QA dedicada (4-10 cores, 8-20 GB)
3. **2028**: Migración a producción (8-12 cores, 16-24 GB)
4. **2029**: Optimizaciones avanzadas (10-16 cores, 20-30 GB)
5. **2030**: Arquitectura de alta disponibilidad (12-20 cores, 24-36 GB)

---

*Documento generado: Enero 2026*
*Próxima revisión: Enero 2027*