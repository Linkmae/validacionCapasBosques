#!/bin/bash

# Script para generar diagramas UML desde archivos PlantUML
# Fecha: 13 de enero de 2026

echo "=== Generador de Diagramas SAF ==="
echo "Generando diagramas UML..."
echo

# Verificar si PlantUML está instalado
if ! command -v plantuml &> /dev/null; then
    echo "❌ PlantUML no está instalado."
    echo "Para instalar en Ubuntu/Debian:"
    echo "  sudo apt update && sudo apt install plantuml"
    echo
    echo "Para instalar en otros sistemas, visite:"
    echo "  https://plantuml.com/download"
    exit 1
fi

echo "✅ PlantUML encontrado: $(plantuml -version)"

# Directorio actual
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "📁 Directorio de trabajo: $DIR"

# Generar diagrama de clases
echo
echo "🔄 Generando diagrama de clases..."
if [ -f "$DIR/diagrama_clases.puml" ]; then
    plantuml "$DIR/diagrama_clases.puml" -o "$DIR"
    if [ $? -eq 0 ]; then
        echo "✅ Diagrama de clases generado: diagrama_clases.png"
    else
        echo "❌ Error generando diagrama de clases"
    fi
else
    echo "❌ Archivo diagrama_clases.puml no encontrado"
fi

# Generar diagrama de componentes
echo
echo "🔄 Generando diagrama de componentes..."
if [ -f "$DIR/diagrama_componentes.puml" ]; then
    plantuml "$DIR/diagrama_componentes.puml" -o "$DIR"
    if [ $? -eq 0 ]; then
        echo "✅ Diagrama de componentes generado: diagrama_componentes.png"
    else
        echo "❌ Error generando diagrama de componentes"
    fi
else
    echo "❌ Archivo diagrama_componentes.puml no encontrado"
fi

echo
echo "=== Generación Completada ==="
echo "Archivos generados:"
ls -la "$DIR"/*.png 2>/dev/null || echo "Ningún archivo PNG encontrado"

echo
echo "Para visualizar los diagramas:"
echo "  - Abrir los archivos .png con un visor de imágenes"
echo "  - Importar .puml en herramientas como Draw.io"
echo "  - Usar plugins de VS Code o IntelliJ"</content>
<parameter name="filePath">/home/linkmaedev/Proyecto_Interconeccion/SAF_Services/Documentos/generar_diagramas.sh