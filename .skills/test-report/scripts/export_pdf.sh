#!/usr/bin/env bash
# Exports the informe to PDF: every docs/informe/secciones/*.md in name order, with the carátula first.
#
# Usage (from the project root):
#   export_pdf.sh [--docx]
#
# Output: docs/informe/informe.pdf (and docs/informe/informe.docx with --docx; Mermaid diagrams are not
# rendered in the .docx). The intermediate HTML is in docs/informe/build/.
# Needs pandoc and a Chromium-family browser (see capture.py). Mermaid diagrams are rendered by
# mermaid.js loaded from a CDN, so the export needs network access for them.
set -euo pipefail

readonly SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly INFORME="docs/informe"
readonly BUILD="$INFORME/build"

command -v pandoc >/dev/null || { echo "Falta pandoc" >&2; exit 2; }
[[ -f "$INFORME/caratula.html" ]] || { echo "Falta $INFORME/caratula.html (inicializar el informe)" >&2; exit 2; }
shopt -s nullglob
sections=("$INFORME"/secciones/*.md)
[[ ${#sections[@]} -gt 0 ]] || { echo "No hay secciones en $INFORME/secciones" >&2; exit 2; }

chrome="$(python3 -I -B - "$SKILL_DIR/scripts" <<'EOF'
import sys
sys.path.insert(0, sys.argv[1])
from capture import find_chrome
print(find_chrome() or "")
EOF
)"
[[ -n "$chrome" ]] || { echo "No se encontró Chromium/Chrome (definir \$CHROME)" >&2; exit 2; }

mkdir -p "$BUILD"
cp "$SKILL_DIR/assets/informe.css" "$BUILD/informe.css"

# Lenient Markdown: a heading or a list right after a paragraph line still counts as one.
common=(--from markdown-blank_before_header+lists_without_preceding_blankline --number-sections --toc --toc-depth=2
        --metadata pagetitle="Informe – Estrategia de Testing" --metadata lang=es
        --metadata toc-title="Índice" --resource-path "$INFORME/secciones")

# The HTML lives in build/, one level below docs/informe, so the "../capturas/x.png" links written
# in secciones/*.md resolve to docs/informe/capturas from there too.
pandoc "${common[@]}" --standalone --to html5 --css informe.css \
    --include-before-body "$INFORME/caratula.html" \
    --include-in-header "$SKILL_DIR/assets/mermaid.html" \
    --output "$BUILD/informe.html" "${sections[@]}"

"$chrome" --headless --no-sandbox --disable-gpu --no-pdf-header-footer \
    --virtual-time-budget=20000 --run-all-compositor-stages-before-draw \
    --print-to-pdf="$(realpath "$INFORME")/informe.pdf" "file://$(realpath "$BUILD/informe.html")" 2>/dev/null
echo "$INFORME/informe.pdf"

if [[ "${1:-}" == "--docx" ]]; then
    pandoc "${common[@]}" --to docx --output "$INFORME/informe.docx" "${sections[@]}"
    echo "$INFORME/informe.docx"
fi
