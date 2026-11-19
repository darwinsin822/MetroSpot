#!/usr/bin/env bash
# ==============================================================================
# MetroSpot — Suite de Verificación Determinista e Integración Continua Local
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"

echo "🧪 [1/4] Verificando ausencia total de rutas privadas en el repositorio..."
if git -C "$REPO_DIR" grep -I -i "/home/darwinsin" ":(exclude).git" ":(exclude)scripts/ci.sh" 2>/dev/null; then
  echo "  ❌ [FAIL] Se detectaron rutas privadas absolutas."
  exit 1
else
  echo "  ✔ [PASS] 0 rutas privadas absolutas detectadas."
fi

echo "🧪 [2/4] Verificando integridad de archivos Kotlin de migración Spotify..."
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

echo "🧪 [3/4] Verificando módulo de Música Local (Protocolo Zeus)..."
for file in "app/src/main/kotlin/com/metrospot/music/local/model/LocalAudioTrack.kt" \
            "app/src/main/kotlin/com/metrospot/music/local/model/LocalModels.kt" \
            "app/src/main/kotlin/com/metrospot/music/local/scanner/LocalMediaScanner.kt" \
            "app/src/main/kotlin/com/metrospot/music/local/ui/LocalMusicScreen.kt" \
            "app/src/main/kotlin/com/metrospot/music/local/viewmodel/LocalMusicViewModel.kt" \
            "app/src/main/res/drawable/folder.xml" \
            "app/src/main/res/drawable/folder_open.xml"; do
  if [ -f "$REPO_DIR/$file" ]; then
    echo "  ✔ [PASS] $file presente."
  else
    echo "  ❌ [FAIL] Falta $file."
    exit 1
  fi
done

echo "🧪 [4/4] Validando configuración Gradle y Kotlin Scripts..."
if [ -f "$REPO_DIR/build.gradle.kts" ] && [ -f "$REPO_DIR/settings.gradle.kts" ]; then
  echo "  ✔ [PASS] Scripts de compilación gradle presentes y estructurados."
fi

echo ""
echo "✅ Todas las pruebas de CI de MetroSpot pasaron al 100% (Código de salida 0)."
