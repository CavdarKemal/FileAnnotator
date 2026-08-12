#!/usr/bin/env bash
#
# Erzeugt eine eigenständige Windows-App (App-Image) für PicTree mit
# gebündeltem JRE – kein separates JDK nötig, Start per Doppelklick.
#
# Voraussetzung: JAVA_HOME zeigt auf ein JDK 26 (enthält jpackage).
# Nutzung:
#   export JAVA_HOME="/c/Program Files/AdoptOpenJDK/jdk-26"
#   bash scripts/jpackage.sh            # -> App-Image unter target/dist/PicTree
#   bash scripts/jpackage.sh msi        # -> .msi-Installer (benötigt WiX Toolset)
#
set -euo pipefail

TYPE="${1:-app-image}"
APP_NAME="PicTree"
APP_VERSION="0.1.0"
MAIN_CLASS="de.hasil.pictree.App"
MAIN_JAR="pictree.jar"

: "${JAVA_HOME:?Bitte JAVA_HOME auf ein JDK 26 setzen}"
JPACKAGE="$JAVA_HOME/bin/jpackage"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[jpackage] Baue Fat-JAR ..."
"$JAVA_HOME/../ApacheMaven/apache-maven-4.0.0/bin/mvn" -q package -DskipTests 2>/dev/null \
  || mvn -q package -DskipTests

echo "[jpackage] Bereite Eingabeordner vor ..."
rm -rf target/jpackage-input target/dist
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
