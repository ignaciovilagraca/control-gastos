#!/usr/bin/env bash
set -euo pipefail

# Siempre trabajar en la carpeta del proyecto (donde está run.sh), aunque lo ejecutes desde otro directorio.
# BASH_SOURCE[0] = ruta de este script → dirname = carpeta del script → cd + pwd = ruta absoluta.
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO"

find_fat_jar() {
  local f
  shopt -s nullglob
  for f in "$REPO"/build/libs/control-gastos-*.jar; do
    if [[ -f "$f" && "$f" != *-sources.jar && "$f" != *-javadoc.jar ]]; then
      printf '%s' "$f"
      shopt -u nullglob
      return 0
    fi
  done
  shopt -u nullglob
  return 1
}

JAR=""
if existing_jar="$(find_fat_jar)"; then
  JAR="$existing_jar"
fi

HEAD_BEFORE="$(git rev-parse HEAD)"
if ! git pull; then
  echo "git pull failed (network/auth?); continuing with current checkout." >&2
fi
HEAD_AFTER="$(git rev-parse HEAD)"

if [[ -z "$JAR" ]]; then
  echo "No previous build: building and running..."
  ./gradlew --no-daemon jar
  JAR="$(find_fat_jar)"
  exec java -jar "$JAR"
fi

if [[ "$HEAD_BEFORE" == "$HEAD_AFTER" ]]; then
  echo "No changes after git pull: running existing jar..."
  exec java -jar "$JAR"
fi

echo "Repository changed: rebuilding and running..."
./gradlew --no-daemon jar
JAR="$(find_fat_jar)"
exec java -jar "$JAR"
