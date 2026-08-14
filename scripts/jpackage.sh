#!/usr/bin/env bash
#
# Erzeugt eine eigenständige Windows-App (App-Image) für PicTree mit
# gebündeltem JRE – kein separates JDK nötig, Start per Doppelklick.
#
# Voraussetzung: ein JDK 26 (enthält jpackage), erkannt über JAVA26_HOME oder
# JAVA_HOME. Maven 4 wird über MAVEN4_HOME (bzw. bekannten Pfad) gefunden.
# Nutzung:
#   bash scripts/jpackage.sh            # -> App-Image unter target/dist/PicTree
#   bash scripts/jpackage.sh msi        # -> .msi-Installer (benötigt WiX Toolset)
#
set -euo pipefail

TYPE="${1:-app-image}"
APP_NAME="PicTree"
APP_VERSION="0.1.0"
MAIN_CLASS="de.hasil.pictree.App"
MAIN_JAR="pictree.jar"

# Windows-Pfad (C:\...) ggf. nach Unix-Form (/c/...) wandeln (Git Bash).
to_unix() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else echo "$1"; fi
}

# JDK mit jpackage bestimmen: JAVA26_HOME bevorzugt (auch für Build mit
# release 26 nötig), sonst JAVA_HOME – aber nur, wenn es jpackage enthält
# (ein system-weites JAVA_HOME zeigt oft auf ein altes JDK 8 ohne jpackage).
JDK=""
for cand in "${JAVA26_HOME:-}" "${JAVA_HOME:-}"; do
  [ -n "$cand" ] || continue
  u="$(to_unix "$cand")"
  if [ -e "$u/bin/jpackage.exe" ] || [ -e "$u/bin/jpackage" ]; then
    JDK="$u"
    break
  fi
done
if [ -z "$JDK" ]; then
  echo "[jpackage] FEHLER: Kein JDK 26 mit jpackage gefunden. Bitte JAVA26_HOME oder JAVA_HOME setzen." >&2
  exit 1
fi
export JAVA_HOME="$JDK"
JPACKAGE="$JAVA_HOME/bin/jpackage"
echo "[jpackage] Verwende JDK: $JAVA_HOME"

# Maven 4 finden (Compiler-Plugin verlangt >= 3.6.3). MAVEN_HOME zeigt oft auf
# ein älteres Maven 3.x und wird daher bewusst nicht verwendet.
MVN=""
for cand in "${MAVEN4_HOME:-}" "C:/Program Files/ApacheMaven/apache-maven-4.0.0"; do
  [ -n "$cand" ] || continue
  u="$(to_unix "$cand")"
  if [ -x "$u/bin/mvn" ]; then
    MVN="$u/bin/mvn"
    break
  fi
done
[ -n "$MVN" ] || MVN="mvn"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[jpackage] Baue Fat-JAR ..."
"$MVN" -q package -DskipTests

echo "[jpackage] Bereite Eingabeordner vor ..."
rm -rf target/jpackage-input
if ! rm -rf target/dist 2>/dev/null; then
  echo "[jpackage] FEHLER: target/dist ist gesperrt. Läuft PicTree.exe noch? Bitte die App schließen und erneut ausführen." >&2
  exit 1
fi
mkdir -p target/jpackage-input
cp "target/${MAIN_JAR}" target/jpackage-input/

echo "[jpackage] Erzeuge ${TYPE} ..."
"$JPACKAGE" \
  --type "$TYPE" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input target/jpackage-input \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --dest target/dist \
  --java-options "-Dsun.java2d.uiScale=1.0" \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --vendor "hasil" \
  $( [ "$TYPE" = "msi" ] && echo "--win-menu --win-shortcut --file-associations scripts/pictree-fileassoc.properties" )

echo "[jpackage] Fertig. Ergebnis unter target/dist/"
ls -la target/dist/
