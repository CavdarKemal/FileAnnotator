# PicTree – FileAnnotator

Swing-Tool zum Annotieren von Bildern mit Text-Stempeln. Bild im Explorer-Baum
auswählen, Kommentar eingeben, per Drag positionieren, stylen und als neue JPG
speichern – **das Original bleibt unangetastet**. EXIF-Daten (Aufnahmedatum,
GPS) werden übernommen, und ein ganzer Ordner lässt sich im Stapel stempeln.

## Features

- **Datei-Baum** im Windows-Explorer-Stil (System-Icons, lazy geladen).
- **Vorschau** mit Live-WYSIWYG-Overlay des Stempeltextes.
- **Drag-&-Drop-Positionierung** des Textes direkt in der Vorschau.
- **Styling**: Schriftfarbe (Color-Picker), Größe (Slider, % der Bildhöhe),
  Schriftfamilie (Roboto/SansSerif/Serif/Monospaced), Fett.
- **Speichern** als neue `.jpg` nach `~/Pictures/PicTreeAlbums` (kollisionsfrei).
- **EXIF-Übernahme** vom Original (DateTimeOriginal + GPS) via Apache Commons Imaging.
- **Stapelverarbeitung**: Ordner markieren → gleicher Stempel auf alle Bilder.
- **Persistenz**: Kommentar/Stil/Position pro Bild als Sidecar
  `*.pictree.properties`, beim erneuten Selektieren wiederhergestellt.

## Voraussetzungen

- **JDK 26** (getestet mit AdoptOpenJDK jdk-26)
- **Maven 4** (nutzt `maven-compiler-plugin` mit `release=26`)

## Bauen & Testen

```bash
mvn clean install
```

Erzeugt ein lauffähiges Fat-JAR unter `target/pictree.jar` und führt alle
JUnit-5-Tests aus.

> Im lokalen Setup alternativ mit dem Wrapper-Skript: `cit 26`
> (baut mit JDK 26 inkl. Tests).

## Starten

```bash
java -jar target/pictree.jar
# oder während der Entwicklung:
mvn exec:java
```

## Bedienung

1. Links im Baum ein Bild auswählen.
2. Unten den Kommentar-/Stempeltext eingeben – erscheint sofort in der Vorschau.
3. Text mit der Maus an die gewünschte Stelle ziehen.
4. Farbe, Größe, Schrift oben in der Werkzeugleiste anpassen.
5. **Bild speichern** → neue JPG in `~/Pictures/PicTreeAlbums` (EXIF übernommen).
6. Für Stapelverarbeitung einen **Ordner** im Baum markieren und
   **Ordner stempeln** wählen.

## Projektstruktur

```
de.hasil.pictree
├── App                    # Einstiegspunkt, Look-and-Feel
├── model                  # StampStyle, Annotation, FileTree(Node/Model)
├── ui                     # MainFrame, FileTreePanel, PreviewPanel,
│                          #   StyleToolbar, CommentPanel, TextDragModel, Geometrie
└── service                # ImageStamp-, Save-, Exif-, Batch-, Annotation-Store,
                           #   TextStampRenderer, FontRegistry, ImageSupport
```

Die Zeichenlogik (`TextStampRenderer`) wird von Vorschau **und** Speichern
gemeinsam genutzt → echtes WYSIWYG. Position und Größe werden relativ (0..1 bzw.
% der Bildhöhe) gespeichert, damit Annotationen über beliebige Bildauflösungen
und in der Stapelverarbeitung konsistent wirken.
