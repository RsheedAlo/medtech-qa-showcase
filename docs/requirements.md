# Anforderungen

Fiktive, stark vereinfachte Fallverwaltung für die Planung chirurgischer Eingriffe. Kein Medizinprodukt, keine echten Patientendaten.

Dieses Dokument ist die Quelle für die Traceability: Jede `REQ-xxx`-Überschrift wird später von einem Skript gelesen und mit den Tests verknüpft. Das Überschriftenformat (`## REQ-xxx – Titel`) deshalb nicht ändern.

## Rollen

- **Planer** (Techniker): legt Fälle an, lädt Planungsdateien hoch, reicht Fälle zur Prüfung ein.
- **Chirurg**: prüft Fälle und gibt sie frei.

## Festlegungen, die über die reine Aufgabenstellung hinausgehen

- **Benutzer:** Es gibt keine Authentifizierung. Der handelnde Benutzer wird bei jeder Aktion als Parameter mitgegeben (Vereinfachung, siehe `risk-analysis.md`, RISK-02). Die Rolle wird nicht technisch erzwungen; Planer und Chirurg unterscheiden sich nur in dem, was sie laut Prozess tun.
- **Benutzernamen** werden ohne führende/nachfolgende Leerzeichen und ohne Beachtung der Groß-/Kleinschreibung verglichen.
- **Patient-ID:** `P-` gefolgt von genau 6 Ziffern, z. B. `P-004711`.
- **Eingriffstyp:** feste Auswahl `KNEE`, `HIP`, `CRANIAL`.
- **10 MB** bedeutet 10 × 1024 × 1024 = 10 485 760 Byte.
- **Workflow:** Es gibt nur die Vorwärtsübergänge `DRAFT → IN_REVIEW → APPROVED`. Einen Rückweg (Prüfer gibt zur Überarbeitung zurück) gibt es bewusst nicht.
- **Uploads** sind in `DRAFT` und `IN_REVIEW` möglich, nach der Freigabe nicht mehr.
- **Audit-Trail:** Protokolliert werden Änderungen am Fall und abgelehnte Freigabeversuche. Ablehnungen wegen ungültiger Eingaben (z. B. falscher Dateityp) sind keine Änderung und werden nicht protokolliert.

---

## REQ-001 – Fall anlegen

Als Planer möchte ich einen Fall mit Patient-ID und Eingriffstyp anlegen, damit Planungsdaten eindeutig einem Patienten und einem Eingriff zugeordnet sind.

Akzeptanzkriterien:
- Given eine gültige Patient-ID, ein gültiger Eingriffstyp und ein angegebener Benutzer, When der Fall angelegt wird, Then entsteht ein Fall mit eindeutiger Fall-ID im Status `DRAFT`, und der Benutzer ist als Ersteller gespeichert
- Given eine Patient-ID, die nicht dem Format `P-` + 6 Ziffern entspricht (z. B. `P-12345`, `P-1234567`, `X-123456`, `P-12345A`), When der Fall angelegt wird, Then wird die Anlage mit einer verständlichen Fehlermeldung abgelehnt und kein Fall erzeugt
- Given ein Eingriffstyp außerhalb der festen Auswahl, When der Fall angelegt wird, Then wird die Anlage mit einer verständlichen Fehlermeldung abgelehnt
- Given kein Benutzer, When der Fall angelegt wird, Then wird die Anlage abgelehnt, weil sonst der Ersteller unbekannt wäre

## REQ-002 – Erlaubte Dateitypen beim Upload

Als Planer möchte ich Planungsdateien nur in den erlaubten Formaten hochladen können, damit keine unbrauchbaren Dateien als Planungsgrundlage im Fall landen.

Akzeptanzkriterien:
- Given ein Fall im Status `DRAFT`, When eine Datei mit der Endung `.stl`, `.pdf` oder `.png` hochgeladen wird, Then wird sie angenommen und dem Fall zugeordnet
- Given ein Fall im Status `DRAFT`, When eine Datei mit einer anderen Endung (z. B. `.exe`, `.jpg`, `.txt`), ohne Endung oder mit unzulässiger letzter Endung (z. B. `plan.stl.exe`) hochgeladen wird, Then wird sie mit einer verständlichen Fehlermeldung abgelehnt und nicht dem Fall zugeordnet
- Given eine Datei mit erlaubter Endung in Großschreibung (z. B. `PLAN.STL`), When sie hochgeladen wird, Then wird sie wie `.stl` behandelt und angenommen

## REQ-003 – Maximale Dateigröße

Als Planer möchte ich, dass Planungsdateien auf 10 MB begrenzt sind und leere Dateien nicht angenommen werden, damit übergroße oder offensichtlich unvollständige Uploads nicht unbemerkt als Planungsgrundlage gelten.

Akzeptanzkriterien:
- Given eine Datei mit exakt 10 MB (10 485 760 Byte), When sie hochgeladen wird, Then wird sie angenommen
- Given eine Datei mit 10 MB + 1 Byte, When sie hochgeladen wird, Then wird sie abgelehnt, und die Fehlermeldung nennt das Limit
- Given eine leere Datei (0 Byte), When sie hochgeladen wird, Then wird sie abgelehnt

## REQ-004 – Fester Status-Workflow

Als Chirurg möchte ich, dass ein Fall die Status nur in der festen Reihenfolge `DRAFT → IN_REVIEW → APPROVED` durchläuft, damit jeder freigegebene Fall vorher zur Prüfung vorgelegt wurde.

Akzeptanzkriterien:
- Given ein Fall im Status `DRAFT`, When er zur Prüfung eingereicht wird, Then wechselt er auf `IN_REVIEW`
- Given ein Fall im Status `DRAFT`, When die Freigabe versucht wird, Then wird sie abgelehnt (Status `IN_REVIEW` ist Pflicht) und der Status bleibt `DRAFT`
- Given ein Fall im Status `IN_REVIEW`, When er erneut eingereicht wird, Then wird das abgelehnt und der Status bleibt `IN_REVIEW`
- Given ein Fall im Status `APPROVED`, When ein beliebiger Statuswechsel versucht wird (erneutes Einreichen, erneute Freigabe), Then wird er abgelehnt und der Status bleibt `APPROVED`

## REQ-005 – Freigabe nur mit Planungsdatei

Als Chirurg möchte ich, dass ein Fall nur mit mindestens einer Planungsdatei freigegeben werden kann, damit kein Eingriff auf unvollständiger Grundlage geplant wird.

Akzeptanzkriterien:
- Given ein Fall im Status `IN_REVIEW` ohne Planungsdatei, When ein anderer Benutzer als der Ersteller die Freigabe mit korrekter Patient-ID-Bestätigung versucht, Then wird sie mit einer verständlichen Fehlermeldung abgelehnt und der Status bleibt `IN_REVIEW`
- Given ein Fall im Status `IN_REVIEW`, dessen einziger Upload-Versuch abgelehnt wurde (z. B. falscher Dateityp), When die Freigabe versucht wird, Then wird sie abgelehnt, weil abgelehnte Uploads keine Planungsdatei sind
- Given ein Fall im Status `IN_REVIEW` mit mindestens einer Planungsdatei, When ein anderer Benutzer als der Ersteller die Freigabe mit korrekter Patient-ID-Bestätigung durchführt, Then wechselt der Fall auf `APPROVED`

## REQ-006 – Vier-Augen-Prinzip

Als Chirurg möchte ich, dass ein Fall nie von der Person freigegeben werden kann, die ihn angelegt hat, damit jede Planung von einer zweiten, unabhängigen Person geprüft wurde.

Akzeptanzkriterien:
- Given ein vollständiger Fall im Status `IN_REVIEW`, When der Ersteller selbst die Freigabe versucht, Then wird sie mit einer verständlichen Fehlermeldung abgelehnt und der Status bleibt `IN_REVIEW`
- Given derselbe Fall, When ein anderer Benutzer die Freigabe durchführt, Then wechselt er auf `APPROVED`, und der Freigebende ist am Fall gespeichert
- Given ein Ersteller `mueller`, When `Mueller` oder ` mueller ` die Freigabe versucht, Then gilt das als dieselbe Person und wird abgelehnt
- Given kein Benutzer, When die Freigabe versucht wird, Then wird sie abgelehnt

## REQ-007 – Freigegebener Fall ist unveränderlich

Als Chirurg möchte ich, dass ein freigegebener Fall nicht mehr verändert werden kann, damit der Stand, den ich freigegeben habe, auch der Stand ist, nach dem geplant wird.

Akzeptanzkriterien:
- Given ein Fall im Status `APPROVED`, When eine Datei hochgeladen wird, Then wird der Upload abgelehnt
- Given ein Fall im Status `APPROVED`, When Änderungen versucht und abgelehnt wurden, Then sind Dateiliste, Status, Ersteller und Freigebender unverändert

## REQ-008 – Audit-Trail pro Fall

Als Chirurg möchte ich pro Fall nachvollziehen können, wer wann was geändert hat, auch bei abgelehnten Freigabeversuchen, damit ich vor einer Freigabe sehen kann, was an diesem Fall passiert ist.

Akzeptanzkriterien:
- Given ein Fall, der angelegt, mit einer Datei versehen, eingereicht und freigegeben wurde, When der Audit-Trail abgerufen wird, Then enthält er je einen Eintrag für Anlage, Upload und beide Statuswechsel, jeweils mit systemseitig gesetztem Zeitstempel, Benutzer und Aktion, in zeitlicher Reihenfolge
- Given ein abgelehnter Freigabeversuch (egal aus welchem Grund: fehlende Datei, falscher Status, gleicher Benutzer, falsche Patient-ID-Bestätigung), When der Audit-Trail abgerufen wird, Then enthält er dazu einen Eintrag mit dem Benutzer, der Aktion „Freigabe abgelehnt“ und dem Ablehnungsgrund
- Given ein Upload, der wegen unzulässigem Dateityp oder Größe abgelehnt wurde, When der Audit-Trail abgerufen wird, Then enthält er keinen Eintrag dafür (keine Änderung am Fall)
- Given zwei Fälle mit jeweils eigenen Aktionen, When der Audit-Trail eines Falls abgerufen wird, Then enthält er ausschließlich Einträge dieses Falls

## REQ-009 – Audit-Einträge sind unveränderlich

Als Chirurg möchte ich, dass Audit-Einträge nachträglich weder geändert noch gelöscht werden können, damit der Verlauf eines Falls verlässlich bleibt.

Akzeptanzkriterien:
- Given ein Fall mit Audit-Einträgen, When über die API versucht wird, einen Eintrag zu ändern oder zu löschen, Then wird das abgelehnt und die Einträge bleiben unverändert
- Given die Audit-Liste eines Falls im Domänenmodell, When der Aufrufer die zurückgegebene Liste zu ändern versucht, Then hat das keinen Effekt auf den gespeicherten Bestand
- Given ein Audit-Eintrag, When der Audit-Trail später erneut abgerufen wird, Then sind Zeitstempel, Benutzer und Aktion dieses Eintrags unverändert

## REQ-010 – Patient-ID bei Freigabe bestätigen

Als Chirurg möchte ich bei der Freigabe die Patient-ID des Falls bestätigen müssen, damit ich nicht versehentlich den Fall eines anderen Patienten freigebe.

Akzeptanzkriterien:
- Given ein vollständiger Fall im Status `IN_REVIEW`, When die Freigabe mit einer falschen Patient-ID als Bestätigung versucht wird, Then wird sie abgelehnt und der Status bleibt `IN_REVIEW`
- Given derselbe Fall, When die Freigabe ohne Bestätigung versucht wird, Then wird sie abgelehnt
- Given derselbe Fall, When die Freigabe mit der korrekten Patient-ID und durch einen anderen Benutzer als den Ersteller erfolgt, Then wechselt er auf `APPROVED`
- Given eine Fall-ID, die nicht existiert, When ein Abruf, Upload oder eine Freigabe versucht wird, Then antwortet das System mit „nicht gefunden“ und ändert keinen anderen Fall
