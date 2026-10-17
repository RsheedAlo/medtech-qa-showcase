# Traceability-Matrix

Automatisch erzeugt von `tools/traceability.py`. Nicht von Hand ändern.

## Anforderungen

| REQ-ID | Kurzbeschreibung | Risiken | Tests (Ebene) | Ergebnis |
|---|---|---|---|---|
| REQ-001 | Fall anlegen | RISK-03 | unit (28), integration (8), system (1), acceptance (1) | BESTANDEN |
| REQ-002 | Erlaubte Dateitypen beim Upload | RISK-05 | unit (17), integration (10), system (8), acceptance (1) | BESTANDEN |
| REQ-003 | Maximale Dateigröße | RISK-05 | unit (9), integration (3), system (4) | BESTANDEN |
| REQ-004 | Fester Status-Workflow | RISK-01, RISK-06 | unit (6), integration (5), system (1) | BESTANDEN |
| REQ-005 | Freigabe nur mit Planungsdatei | RISK-01 | unit (3), integration (3), system (2), acceptance (1) | BESTANDEN |
| REQ-006 | Vier-Augen-Prinzip | RISK-02 | unit (9), integration (5), system (2), acceptance (1) | BESTANDEN |
| REQ-007 | Freigegebener Fall ist unveränderlich | RISK-06 | unit (2), integration (2), system (1) | BESTANDEN |
| REQ-008 | Audit-Trail pro Fall | RISK-04 | unit (8), integration (5), system (3) | BESTANDEN |
| REQ-009 | Audit-Einträge sind unveränderlich | RISK-04 | unit (2), integration (2), system (1) | BESTANDEN |
| REQ-010 | Patient-ID bei Freigabe bestätigen | RISK-03 | unit (8), integration (5), system (3), acceptance (1) | BESTANDEN |

## Risiken

Ein Risiko gilt als getestet, wenn alle Anforderungen seiner Risikokontrolle mindestens einen Test haben und alle bestehen.

| Risiko | Titel | Schweregrad | Maßnahme (REQ) | Status |
|---|---|---|---|---|
| RISK-01 | Freigabe ohne Planungsdaten | hoch | REQ-005, REQ-004 | getestet |
| RISK-02 | Freigabe durch dieselbe Person ohne zweite Prüfung | hoch | REQ-006 | getestet |
| RISK-03 | Falscher Fall wird freigegeben (Verwechslung) | hoch | REQ-010, REQ-001 | getestet |
| RISK-04 | Fehlende oder manipulierte Nachvollziehbarkeit von Änderungen | mittel | REQ-008, REQ-009 | getestet |
| RISK-05 | Upload einer falschen oder beschädigten Datei | mittel | REQ-002, REQ-003 | getestet |
| RISK-06 | Änderung nach der Freigabe | hoch | REQ-007, REQ-004 | getestet |

## Prüfergebnis

Keine Lücken gefunden.
