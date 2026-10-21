# medtech-qa-showcase

[![CI](https://github.com/RsheedAlo/medtech-qa-showcase/actions/workflows/ci.yml/badge.svg)](https://github.com/RsheedAlo/medtech-qa-showcase/actions/workflows/ci.yml)

Eine kleine, fiktive Fallverwaltung für die Planung chirurgischer Eingriffe (Fälle anlegen, Planungsdateien hochladen, Status-Workflow, Freigabe nach Vier-Augen-Prinzip, Audit-Trail). Ich habe sie gebaut, um zu zeigen, wie man Software in einem regulierten Umfeld (IEC 62304, ISO 14971, ISO 13485) nachvollziehbar testet. Die Anwendung ist Nebensache. Der Kern ist die Kette Anforderung → Risiko → Test → Ergebnis, die ein Skript bei jedem Push automatisch prüft: Fehlt ein Test oder schlägt einer fehl, wird die Pipeline rot.

**Hinweis:** Fiktives Projekt, kein Medizinprodukt, keine echten Patientendaten. Gebaut mit Claude Code als Werkzeug. Anforderungen, Risikobewertung und Testauswahl habe ich selbst geprüft und verantworte sie.

## Wo finde ich was

| Was | Wo |
|---|---|
| Anforderungen (User Stories, Given/When/Then) | [docs/requirements.md](docs/requirements.md) |
| Risikoanalyse (vereinfacht nach ISO 14971, IEC-62304-Klasse) | [docs/risk-analysis.md](docs/risk-analysis.md) |
| Traceability-Matrix (automatisch erzeugt) | [docs/traceability.md](docs/traceability.md) |
| Prozess, V-Modell und agiles Arbeiten | [docs/process.md](docs/process.md) |
| KI im Testing, meine Entscheidungen | [docs/AI-TESTING.md](docs/AI-TESTING.md) |
| Fremdsoftware (SOUP) | [docs/soup.md](docs/soup.md) |
| Unit-Tests (Domänenlogik, ohne Spring) | [src/test/java/com/medtechqa/unit](src/test/java/com/medtechqa/unit) |
| Integrationstests (Controller + Service, MockMvc) | [src/test/java/com/medtechqa/integration](src/test/java/com/medtechqa/integration) |
| Systemtests (Playwright API gegen die laufende App) | [src/test/java/com/medtechqa/system](src/test/java/com/medtechqa/system) |
| Abnahmetests (Playwright im Browser) | [src/test/java/com/medtechqa/acceptance](src/test/java/com/medtechqa/acceptance) |
| Traceability-Skript und sein Selbsttest | [tools/](tools) |
| Pipeline | [.github/workflows/ci.yml](.github/workflows/ci.yml) |

## Lokal starten und testen

Voraussetzung: JDK 21. Maven ist nicht nötig, der Wrapper liegt im Repo.

```bash
./mvnw spring-boot:run      # App starten, dann http://localhost:8080/ öffnen
./mvnw test                 # alle vier Testebenen
python tools/traceability.py   # Matrix erzeugen und Lücken prüfen (Python 3)
```

Unter Windows (PowerShell) statt `./mvnw` den Aufruf `.\mvnw.cmd` verwenden. Der erste Testlauf lädt den Browser für die Abnahmetests herunter (Playwright), das kann einige Minuten dauern.

## Bewusste Vereinfachungen

- Kein Login: Der handelnde Benutzer kommt als Header `X-User` mit (siehe RISK-02).
- Daten nur im Arbeitsspeicher, nach einem Neustart ist alles weg (siehe RISK-04).
- Dateien werden nur nach Endung und Größe geprüft, der Inhalt nicht (siehe RISK-05).
