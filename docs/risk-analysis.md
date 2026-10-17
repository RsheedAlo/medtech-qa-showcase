# Risikoanalyse (vereinfacht, nach ISO 14971)

> **Fiktiv und vereinfacht.** Dieses Projekt ist kein Medizinprodukt. Einstufung und Bewertungen unten sind angenommen, um den Ablauf zu zeigen. In einem echten Projekt gehören sie in die Risikomanagementakte und werden von qualifizierten Personen freigegeben.

## Angenommene Software-Sicherheitsklasse (IEC 62304): B

Die Software steuert kein Gerät und führt keinen Eingriff aus. Sie verwaltet Planungsdaten, auf deren Grundlage ein Chirurg entscheidet. Ein Softwarefehler (etwa ein zu Unrecht freigegebener Fall) kann zu einer fehlerhaften Planung führen, aber der Chirurg prüft Plan und Patient vor und während des Eingriffs unabhängig von der Software. Diese Prüfung ist eine Risikokontrolle außerhalb der Software; unter der Annahme, dass sie wirksam ist, ist eine Verletzung möglich, aber keine schwere, daher Klasse B und nicht C.

**Schwachstelle der Einstufung:** Die Schweregrade unten beschreiben die Folge ohne diese externe Kontrolle. Trägt die Annahme nicht, wäre die Software Klasse C. In einem echten Projekt müsste die Wirksamkeit der externen Kontrolle belegt werden.

## Bewertungsskalen

- **Schweregrad:** *gering* = keine oder vorübergehende Beeinträchtigung ohne Behandlung; *mittel* = Verzögerung oder Behandlung nötig; *hoch* = schwere oder dauerhafte Schädigung, z. B. Revisionseingriff.
- **Wahrscheinlichkeit** (ohne die Risikokontrolle): *gering*, *mittel*, *hoch*.
- **Vereinfachung:** Eine Risikoakzeptanzmatrix gibt es hier nicht. Stattdessen gilt die Regel: **Zu jedem Risiko mit Schweregrad „hoch“ muss mindestens eine Risikokontrolle durch einen automatisierten Test belegt sein.** Das prüft das Traceability-Skript.

Die Überschriften (`## RISK-xx – Titel`) und Feldnamen werden vom Traceability-Skript gelesen und sollten deshalb nicht umbenannt werden.

---

## RISK-01 – Freigabe ohne Planungsdaten

- **Gefährdung:** Ein Fall wird freigegeben, obwohl keine Planungsdatei vorliegt.
- **Mögliche Folge für den Patienten:** Der Eingriff wird auf unvollständiger Grundlage geplant (falsche oder fehlende Implantatdaten).
- **Schweregrad:** hoch
- **Wahrscheinlichkeit:** mittel
- **Risikokontrolle:** Die Freigabe wird systemseitig abgelehnt, solange der Fall keine angenommene Planungsdatei hat; der feste Workflow verhindert, dass die Prüfung übersprungen wird.
- **Umgesetzt durch:** REQ-005, REQ-004
- **Restrisiko / Grenzen:** „Datei vorhanden“ heißt nicht „Datei korrekt und vollständig“. Die inhaltliche Prüfung liegt beim Chirurgen.

## RISK-02 – Freigabe durch dieselbe Person ohne zweite Prüfung

- **Gefährdung:** Der Ersteller gibt seinen eigenen Fall frei. Ein Fehler in der Planung wird von niemandem Zweiten gesehen.
- **Mögliche Folge für den Patienten:** Ein unentdeckter Planungsfehler (z. B. falsche Implantatgröße) gelangt in den Eingriff.
- **Schweregrad:** hoch
- **Wahrscheinlichkeit:** mittel
- **Risikokontrolle:** Vier-Augen-Prinzip: Der Freigebende darf nicht der Ersteller sein (Namensvergleich ohne Groß-/Kleinschreibung und ohne Leerzeichen).
- **Umgesetzt durch:** REQ-006
- **Restrisiko / Grenzen:** Es gibt keine Authentifizierung. Die Identität ist ein mitgegebener Name und lässt sich umgehen, indem jemand unter zwei Namen auftritt. Auch eine Berechtigung „nur Chirurgen dürfen freigeben“ fehlt. Beides wäre in einem echten System Pflicht.

## RISK-03 – Falscher Fall wird freigegeben (Verwechslung)

- **Gefährdung:** Der Prüfer gibt versehentlich den Fall eines anderen Patienten frei, z. B. weil sich zwei Patient-IDs nur in einer Ziffer unterscheiden.
- **Mögliche Folge für den Patienten:** Ein Eingriff wird mit den Planungsdaten eines anderen Patienten vorbereitet.
- **Schweregrad:** hoch
- **Wahrscheinlichkeit:** mittel
- **Risikokontrolle:** Patient-IDs müssen ein striktes Format haben, damit Tippfehler früh auffallen. Bei der Freigabe muss die Patient-ID des Falls aktiv eingegeben werden.
- **Umgesetzt durch:** REQ-010, REQ-001
- **Restrisiko / Grenzen:** Die Bestätigung fängt Versehen ab, beweist aber nicht, dass die ID zum richtigen Menschen gehört. Eine Prüfsumme in der ID oder die Anbindung an ein Patientenverzeichnis gibt es nicht.

## RISK-04 – Fehlende oder manipulierte Nachvollziehbarkeit von Änderungen

- **Gefährdung:** Änderungen an einem Fall sind nicht protokolliert oder können nachträglich verändert oder gelöscht werden.
- **Mögliche Folge für den Patienten:** Ein Fehler lässt sich nicht auf seine Ursache zurückverfolgen, Korrekturmaßnahmen laufen ins Leere. Die Patientensicherheit ist nur indirekt betroffen.
- **Schweregrad:** mittel
- **Wahrscheinlichkeit:** gering
- **Risikokontrolle:** Jede Änderung und jeder abgelehnte Freigabeversuch wird mit Zeitstempel, Benutzer und Aktion protokolliert. Die API bietet keine Möglichkeit, Einträge zu ändern oder zu löschen.
- **Umgesetzt durch:** REQ-008, REQ-009
- **Restrisiko / Grenzen:** Die Daten liegen nur im Arbeitsspeicher und sind nach einem Neustart weg. Echte Manipulationssicherheit (revisionssicherer Speicher, Hash-Kette) ist nicht umgesetzt.

## RISK-05 – Upload einer falschen oder beschädigten Datei

- **Gefährdung:** Eine Datei in einem ungeeigneten Format, eine leere oder eine übergroße Datei wird als Planungsgrundlage akzeptiert.
- **Mögliche Folge für den Patienten:** Die Planung beruht auf unbrauchbaren Daten; Verzögerung oder fehlerhafte Planung.
- **Schweregrad:** mittel
- **Wahrscheinlichkeit:** mittel
- **Risikokontrolle:** Whitelist erlaubter Dateitypen (`.stl`, `.pdf`, `.png`), Größenlimit von 10 MB, leere Dateien werden abgelehnt.
- **Umgesetzt durch:** REQ-002, REQ-003
- **Restrisiko / Grenzen:** Geprüft werden nur Endung und Größe. Eine umbenannte oder inhaltlich defekte Datei mit erlaubter Endung kommt durch. Eine Inhaltsprüfung (Dateikopf, Parser) gibt es nicht.

## RISK-06 – Änderung nach der Freigabe

- **Gefährdung:** Nach der Freigabe werden noch Dateien hinzugefügt oder der Status verändert. Der freigegebene Stand ist dann nicht mehr der Stand, der geprüft wurde.
- **Mögliche Folge für den Patienten:** Der Eingriff wird nach Daten geplant, die kein Chirurg geprüft hat.
- **Schweregrad:** hoch
- **Wahrscheinlichkeit:** mittel
- **Risikokontrolle:** Ein freigegebener Fall nimmt keine Uploads und keine Statuswechsel mehr an.
- **Umgesetzt durch:** REQ-007, REQ-004
- **Restrisiko / Grenzen:** Uploads sind auch im Status `IN_REVIEW` möglich. Wird dort nach dem Beginn der Prüfung eine Datei hinzugefügt, ist das nur im Audit-Trail sichtbar (REQ-008), die Prüfung wird nicht ungültig. Ein Rückweg „zur Überarbeitung zurück“ existiert nicht.
