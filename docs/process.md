# Prozess

## V-Modell und Testebenen

Das V-Modell ist hier ein Denkmodell: Jede Ebene links hat eine Testebene rechts, die prüft, ob sie erfüllt ist.

| Planungsebene | Artefakt im Repo | Testebene | Testordner |
|---|---|---|---|
| Nutzeranforderungen, Akzeptanzkriterien | `docs/requirements.md` | Abnahmetest (Browser, 2 Szenarien) | `src/test/java/com/medtechqa/acceptance` |
| Systemanforderungen (Verhalten der API) | `docs/requirements.md`, API in `api/` | Systemtest (echte HTTP-Anfragen) | `src/test/java/com/medtechqa/system` |
| Architekturentwurf (Zusammenspiel Controller, Service, Domäne) | `src/main/java/com/medtechqa/api` | Integrationstest (MockMvc) | `src/test/java/com/medtechqa/integration` |
| Detailentwurf (Regeln der Domäne) | `src/main/java/com/medtechqa/domain` | Unittest (ohne Spring) | `src/test/java/com/medtechqa/unit` |

Quer dazu: Die Risikoanalyse (`docs/risk-analysis.md`) liefert Risikokontrollen, die als Anforderungen umgesetzt und getestet werden. `docs/traceability.md` zeigt die Kette und wird bei jedem Push geprüft.

### Was jede Ebene nicht prüft

- **Unit:** kein HTTP, kein JSON, keine Verdrahtung.
- **Integration:** kein echter Server, keine echte Upload-Verarbeitung.
- **System:** nicht die HTML-Seite, keine Last, keine Sicherheit, keine Nebenläufigkeit über mehrere Server.
- **Abnahme:** nur zwei Kriterien, keine weiteren Browser, kein Layout.

## Agiles Arbeiten im regulierten Umfeld

Dokumentation entsteht inkrementell pro Inkrement statt als ein großes Dokument am Anfang. Das V-Modell bleibt das Denkmodell für die Testebenen, wird aber pro Inkrement durchlaufen: kleine Änderung, Anforderung und Risiko anpassen, Tests ergänzen, Nachweis aktualisieren. Eine Orientierung dafür gibt AAMI TIR45, die Guidance zu agilen Praktiken bei Software für Medizinprodukte.

## Wie ich es im Team aufsetzen würde

Ich habe dieses Projekt allein in wenigen Tagen gebaut, deshalb gibt es keine Sprint-Struktur. Im Team würde ich:

- die **Definition of Done** festlegen: Tests grün, Traceability-Check grün, betroffene Doku (Anforderung, Risiko) aktualisiert;
- bei **jeder neuen Story** zuerst fragen, ob sie ein Risiko berührt, und die Risikoanalyse bei Bedarf erweitern;
- **Tests als Teil der Story** schreiben, nicht danach, und jeden Test einer Anforderung zuordnen;
- **KI-Vorschläge** (Tests, Code) wie jeden anderen Beitrag reviewen und die Entscheidung dokumentieren, siehe `docs/AI-TESTING.md`.
