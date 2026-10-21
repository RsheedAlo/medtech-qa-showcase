# Fremdsoftware (SOUP nach IEC 62304)

SOUP ("Software of Unknown Provenance") ist Software, die nicht für dieses Produkt entwickelt wurde. Aufgelistet sind die **direkten** Abhängigkeiten. Was diese selbst mitbringen (z. B. Tomcat, Jackson, JUnit), ist nicht einzeln aufgeführt und wird über die Elternprojekte mitgeführt. Die Risikoeinschätzung ist vereinfacht und fiktiv.

| Name | Version | Zweck | Wo | Kurze Risikoeinschätzung |
|---|---|---|---|---|
| Spring Boot (spring-boot-starter-parent, -web) | 3.5.16 | Web-Framework, eingebetteter Server, JSON | Produktion | Mittel. Kernbaustein, aber weit verbreitet und gut gepflegt. **Offener Punkt:** Der Zweig 3.5 hat seit 30.06.2026 keinen kostenfreien Support mehr, 3.5.16 war das letzte freie Release (siehe Spring-Release-Hinweis zu 3.5.16). Dependabot hat ein Update auf 4.x vorgeschlagen. Ein Wechsel ist ein größerer Schritt und wäre ein eigenes Inkrement mit Risikobewertung. |
| Spring Boot Starter Test | 3.5.16 (über Parent) | JUnit 5, Spring Test, MockMvc, Mockito | nur Test | Gering. Läuft nicht in der Produktion. |
| Playwright for Java | 1.62.0 | API- und Browser-Tests (System, Abnahme) | nur Test | Gering. Lädt beim ersten Lauf einen Browser (Chromium) aus dem Internet. Läuft nicht in der Produktion. |
| Java (Temurin JDK) | 21 | Laufzeit und Compiler | Produktion und Build | Mittel. LTS-Version, Updates über den Hersteller. |
| Maven (über Wrapper) | 3.10.0 | Build | nur Build | Gering. Gleiche Version lokal und in CI. |
| GitHub Actions (checkout, setup-java, setup-python, cache, upload-artifact) | siehe `.github/workflows/ci.yml` | Pipeline | nur CI | Gering bis mittel. Fremder Code läuft mit Zugriff auf das Repo. Deshalb sind die Rechte auf `contents: read` beschränkt, und Dependabot hält die Versionen aktuell. |
| exec-maven-plugin | 3.5.0 | Aufruf der Playwright-Installation in CI | nur CI | Gering. |

## Überwachung

Dependabot (`.github/dependabot.yml`) prüft wöchentlich Maven-Abhängigkeiten und GitHub Actions und öffnet Pull Requests bei neuen Versionen und bei Sicherheitsupdates. Ein Pull Request wird erst übernommen, wenn die Pipeline grün ist, also die Regressionstests und der Traceability-Check bestehen.

## Grenzen

Es gibt keine eigene Bewertung der bekannten Schwachstellen einzelner Bibliotheken und keine Prüfung der Lizenzen. In einem echten Projekt gehört beides zur SOUP-Bewertung.
