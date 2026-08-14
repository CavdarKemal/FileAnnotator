@echo off
setlocal enabledelayedexpansion
rem ---------------------------------------------------------------------------
rem  Erzeugt eine eigenstaendige Windows-App (App-Image) fuer PicTree mit
rem  gebuendeltem JRE - kein separates JDK noetig, Start per Doppelklick.
rem
rem  Voraussetzung: JAVA_HOME zeigt auf ein JDK 26 (enthaelt jpackage).
rem  Nutzung (in cmd.exe):
rem    set JAVA_HOME=C:\Program Files\AdoptOpenJDK\jdk-26
rem    scripts\jpackage.cmd            -^> App-Image unter target\dist\PicTree
rem    scripts\jpackage.cmd msi        -^> .msi-Installer (benoetigt WiX Toolset)
rem ---------------------------------------------------------------------------

set "TYPE=%~1"
if "%TYPE%"=="" set "TYPE=app-image"

set "APP_NAME=PicTree"
set "APP_VERSION=0.1.0"
set "MAIN_CLASS=de.hasil.pictree.App"
set "MAIN_JAR=pictree.jar"

rem JDK mit jpackage bestimmen: JAVA26_HOME bevorzugen (auch fuer Build mit
rem release 26 noetig), sonst JAVA_HOME - aber nur, wenn es jpackage enthaelt
rem (ein system-weites JAVA_HOME zeigt oft auf ein altes JDK 8 ohne jpackage).
set "JDK="
if defined JAVA26_HOME if exist "%JAVA26_HOME%\bin\jpackage.exe" set "JDK=%JAVA26_HOME%"
if not defined JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
if not defined JDK (
  echo [jpackage] FEHLER: Kein JDK 26 mit jpackage gefunden. Bitte JAVA26_HOME oder JAVA_HOME setzen.
  exit /b 1
)
set "JAVA_HOME=%JDK%"
set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
echo [jpackage] Verwende JDK: %JAVA_HOME%

rem In das Projekt-Wurzelverzeichnis wechseln (Elternordner dieses Skripts).
cd /d "%~dp0.."

echo [jpackage] Baue Fat-JAR ...
rem Maven 4 finden (Compiler-Plugin verlangt >= 3.6.3): MAVEN4_HOME, sonst
rem bekannter Pfad, sonst mvn aus dem PATH. MAVEN_HOME wird bewusst ignoriert,
rem da es haeufig auf ein aelteres Maven 3.x zeigt.
set "MVN="
if defined MAVEN4_HOME if exist "%MAVEN4_HOME%\bin\mvn.cmd" set "MVN=%MAVEN4_HOME%\bin\mvn.cmd"
if not defined MVN if exist "C:\Program Files\ApacheMaven\apache-maven-4.0.0\bin\mvn.cmd" set "MVN=C:\Program Files\ApacheMaven\apache-maven-4.0.0\bin\mvn.cmd"
if not defined MVN set "MVN=mvn"
call "%MVN%" -q package -DskipTests
if errorlevel 1 (
  echo [jpackage] FEHLER: Maven-Build fehlgeschlagen ^(Maven 4 noetig - MAVEN4_HOME setzen^).
  exit /b 1
)

echo [jpackage] Bereite Eingabeordner vor ...
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
if exist "target\dist" rmdir /s /q "target\dist"
if exist "target\dist" (
  echo [jpackage] FEHLER: target\dist ist gesperrt. Laeuft PicTree.exe noch? Bitte die App schliessen und erneut ausfuehren.
  exit /b 1
)
mkdir "target\jpackage-input"
copy /y "target\%MAIN_JAR%" "target\jpackage-input\" >nul
if errorlevel 1 (
  echo [jpackage] FEHLER: "target\%MAIN_JAR%" nicht gefunden - Build unvollstaendig?
  exit /b 1
)

set "EXTRA="
if /I "%TYPE%"=="msi" set "EXTRA=--win-menu --win-shortcut --file-associations scripts\pictree-fileassoc.properties"

echo [jpackage] Erzeuge %TYPE% ...
"%JPACKAGE%" ^
  --type "%TYPE%" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input target\jpackage-input ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest target\dist ^
  --java-options "-Dsun.java2d.uiScale=1.0" ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --vendor "hasil" ^
  %EXTRA%
if errorlevel 1 (
  echo [jpackage] FEHLER: jpackage fehlgeschlagen.
  exit /b 1
)

echo [jpackage] Fertig. Ergebnis unter target\dist\
dir /b "target\dist"
endlocal
