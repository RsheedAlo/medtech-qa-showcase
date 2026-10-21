# Fremdsoftware (SOUP nach IEC 62304)

SOUP ("Software of Unknown Provenance") ist Software, die nicht für dieses Produkt entwickelt wurde. Aufgelistet sind die **direkten** Abhängigkeiten. Was diese selbst mitbringen (z. B. Tomcat, Jackson, JUnit), ist nicht einzeln aufgeführt und wird über die Elternprojekte mitgeführt. Die Risikoeinschätzung ist vereinfacht und fiktiv.

| Name | Version | Zweck | Wo | Kurze Risikoeinschätzung |
|---|---|---|---|---|
| Spring Boot (spring-boot-starter-parent, -web) | 3.5.16 | Web-Framework, eingebetteter Server, JSON | Produktion | Mittel. Kernbaustein, aber weit verbreitet und gut gepflegt. **Offener Punkt:** Der Zweig 3.5 hat seit 30.06.2026 keinen kostenfreien Support mehr, 3.5.16 war das letzte freie Release (siehe Spring-Release-Hinweis zu 3.5.16). Dependabot hat ein Update auf 4.x vorgeschlagen. Ein Wechsel ist ein größerer Schritt und wäre ein eigenes Inkrement mit Risikobewertung. |
| Spring Boot Starter Test | 3.5.16 (über Parent) | JUnit 5, Spring Test, MockMvc, Mockito | nur Test | Gering. Läuft nicht in der Produktion. |
| Playwright for Java | 1.63.0 | API- und Browser-Tests (System, Abnahme) | nur Test | Gering. Lädt beim ersten Lauf einen Browser (Chromium) aus dem Internet. Läuft nicht in der Produktion. |
| Java (Temurin JDK) | 21 | Laufzeit und Compiler | Produktion und Build | Mittel. LTS-Version, Updates über den Hersteller. |
| Maven (über Wrapper) | 3.10.0 | Build | nur Build | Gering. Gleiche Version lokal und in CI. |
| GitHub Actions (checkout, setup-java, setup-python, cache, upload-artifact) | siehe `.github/workflows/ci.yml` | Pipeline | nur CI | Gering bis mittel. Fremder Code läuft mit Zugriff auf das Repo. Deshalb sind die Rechte auf `contents: read` beschränkt, und Dependabot hält die Versionen aktuell. |
| exec-maven-plugin | 3.5.0 | Aufruf der Playwright-Installation in CI | nur CI | Gering. |

## Überwachung

Die Überwachung besteht aus zwei getrennten Teilen, die man leicht verwechselt:

- **Versions-Updates** werden in `.github/dependabot.yml` konfiguriert. Dependabot prüft wöchentlich die Maven-Abhängigkeiten und die GitHub Actions und öffnet pro veralteter Abhängigkeit einen Pull Request.
- **Sicherheitswarnungen** (Dependabot alerts) sind keine Einstellung in dieser Datei, sondern ein Schalter am Repository. GitHub gleicht die Abhängigkeiten gegen seine Advisory-Datenbank ab und meldet bekannte Schwachstellen. Der Schalter ist aktiv.

Beim Aufsetzen war nur der erste Teil aktiv. Dass der zweite fehlte, ist erst bei einer späteren Durchsicht aufgefallen. Die Lehre daraus: Eine SOUP-Überwachung ist erst belegt, wenn man geprüft hat, dass sie wirklich eingeschaltet ist, nicht schon dann, wenn eine Konfigurationsdatei im Repo liegt.

Ein Pull Request wird erst übernommen, wenn die Pipeline grün ist, also die Regressionstests und der Traceability-Check bestehen. Dadurch zeigt jedes Update sofort, ob es etwas bricht.

## Grenzen

Diese Liste ist eine Bestandsaufnahme, keine Bewertung. Es fehlt:

- **Bewertung gemeldeter Schwachstellen.** Dependabot meldet, dass eine Lücke existiert. Niemand hat beurteilt, ob sie in diesem Produkt überhaupt erreichbar ist und was sie für einen Patienten bedeuten würde. Eine Lücke in der Upload-Verarbeitung wäre hier relevant, dieselbe Lücke in einem nie genutzten Teil nicht. Diese Unterscheidung samt Entscheidung (beheben, später beheben, begründet akzeptieren) gehört in einem echten Projekt dokumentiert.
- **Lizenzprüfung.** Die Lizenzen der Abhängigkeiten wurden nicht geprüft und nicht festgehalten. Für ein kommerzielles Produkt ist das Pflicht, weil manche Lizenzen die Offenlegung des eigenen Quellcodes verlangen.
