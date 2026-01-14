#!/bin/bash

# Script para generar diagramas desde archivos Mermaid
# Fecha: 13 de enero de 2026

echo "=== Generador de Diagramas Mermaid - SAF ==="
echo "Generando diagramas desde archivos .mmd..."
echo

# Verificar si mermaid-cli está instalado
if ! command -v mmdc &> /dev/null; then
    echo "❌ Mermaid CLI no está instalado."
    echo "Para instalar:"
    echo "  npm install -g @mermaid-js/mermaid-cli"
    echo "  # o"
    echo "  yarn global add @mermaid-js/mermaid-cli"
    echo
    echo "Para instalación local en el proyecto:"
    echo "  npm install @mermaid-js/mermaid-cli --save-dev"
    echo "  npx mmdc -i diagrama.mmd -o diagrama.png"
    exit 1
fi

echo "✅ Mermaid CLI encontrado: $(mmdc --version)"

# Directorio actual
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "📁 Directorio de trabajo: $DIR"

# Lista de archivos mermaid
MERMAID_FILES=(
    "diagrama_flujo.mmd"
    "modelo_logico_datos.mmd"
    "modelo_fisico_datos.mmd"
    "modelo_relacional_datos.mmd"
    "diagrama_componentes.mmd"
    "diagrama_clases.mmd"
)

# Generar diagramas
for file in "${MERMAID_FILES[@]}"; do
    if [ -f "$DIR/$file" ]; then
        output_file="${file%.mmd}.png"
        echo
        echo "🔄 Generando $output_file..."
        mmdc -i "$DIR/$file" -o "$DIR/$output_file" -t default -b transparent
        if [ $? -eq 0 ]; then
            echo "✅ $output_file generado exitosamente"
        else
            echo "❌ Error generando $output_file"
        fi
    else
        echo "❌ Archivo $file no encontrado"
    fi
done

echo
echo "=== Generación Completada ==="
echo "Archivos generados:"
ls -la "$DIR"/*.png 2>/dev/null | grep -E "(diagrama|modelo)" || echo "Ningún archivo PNG encontrado"

echo
echo "=== Opciones Adicionales ==="
echo "Para generar con diferentes temas:"
echo "  mmdc -i diagrama.mmd -o diagrama.png -t dark"
echo "  mmdc -i diagrama.mmd -o diagrama.png -t forest"
echo
echo "Para generar SVG en lugar de PNG:"
echo "  mmdc -i diagrama.mmd -o diagrama.svg"
echo
echo "Para visualizar online:"
echo "  https://mermaid.live"
echo
echo "Para integración en VS Code:"
echo "  Instalar extensión 'Mermaid Markdown Syntax Highlighting'"</content>
<parameter name="filePath">/home/linkmaedev/Proyecto_Interconeccion/SAF_Services/Documentos/generar_diagramas_mermaid.sh