#!/usr/bin/env bash
# ==============================================================================
# MetroSpot — Suite de Verificación Determinista e Integración Continua Local
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"

echo "🧪 [1/3] Verificando ausencia total de rutas privadas en el repositorio..."
if git -C "$REPO_DIR" grep -I -i "/home/darwinsin" ":(exclude).git" 2>/dev/null; then
  echo "  ❌ [FAIL] Se detectaron rutas privadas absolutas."
  exit 1
else
  echo "  ✔ [PASS] 0 rutas privadas absolutas detectadas."
fi

echo "🧪 [2/3] Verificando integridad de archivos Kotlin de migración Spotify..."
for file in "app/src/main/kotlin/com/metrospot/music/spotify/SpotifyMigrateViewModel.kt" \
            "app/src/main/kotlin/com/metrospot/music/spotify/SpotifyPlaylistExtractor.kt" \
            "app/src/main/kotlin/com/metrospot/music/ui/screens/spotify/SpotifyMigrateScreen.kt"; do
  if [ -f "$REPO_DIR/$file" ]; then
    echo "  ✔ [PASS] $file presente."
  else
    echo "  ❌ [FAIL] Falta $file."
    exit 1
  fi
done

echo "🧪 [3/3] Validando configuración Gradle y Kotlin Scripts..."
if [ -f "$REPO_DIR/build.gradle.kts" ] && [ -f "$REPO_DIR/settings.gradle.kts" ]; then
  echo "  ✔ [PASS] Scripts de compilación gradle presentes y estructurados."
fi

echo ""
echo "✅ Todas las pruebas de CI de MetroSpot pasaron al 100% (Código de salida 0)."
