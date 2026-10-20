# KI im Testing: Vorschläge prüfen, nicht glauben

Ich habe eine KI zusätzliche Testfälle vorschlagen lassen und jeden Vorschlag selbst geprüft. Die KI entscheidet nichts. Ein Test kommt nur ins Repo, wenn ich ihn gegen Anforderungen und Code geprüft habe und seine Aufnahme begründen kann.

## Werkzeug und Prompt

- **Werkzeug:** Claude Code (Modell Claude Sonnet 5.5).
- **Grundlage:** `docs/requirements.md` und `docs/risk-analysis.md`.
- **Prompt, wörtlich:**

> Schlag mir 12-15 zusätzliche Testfälle bzw. Edge Cases vor, nur auf Basis von requirements.md und risk-analysis.md. Bewerte deine eigenen Vorschläge NICHT und filtere sie nicht vor. Ich will sie roh sehen.

Ergebnis: 15 Vorschläge (KI-01 bis KI-15), roh und ungefiltert.

**Ehrlicher Hinweis zum Ablauf:** Die Prüfung kam nachträglich. Die Tests existierten schon. Normalerweise würde man die Vorschläge besprechen, bevor man die Tests schreibt. Außerdem kannte die KI in dieser Sitzung auch den Code und die vorhandenen Tests, obwohl der Prompt nur die zwei Dokumente nennt.

## Entscheidungen

Die Begründungen zu KI-01 bis KI-05 stammen von mir. Zu KI-06 bis KI-15 hat die KI einen Entwurf gemacht. Ich habe ihn gegen Anforderungen und Code gelesen und übernommen.

| ID | Vorschlag | Entscheidung | Begründung | Umsetzung |
|---|---|---|---|---|
| KI-01 | Patient-ID mit Ziffern eines anderen Zeichensatzes | übernommen | Im Code schon vorhanden, nichts neu gebaut | bereits getestet (`CaseCreationTest`) |
| KI-02 | Patient-ID mit angehängtem Zeilenumbruch | verworfen | Dublette, die Formatprüfung deckt es schon ab | – |
| KI-03 | Benutzername nur aus Leerzeichen | übernommen | Im Code schon vorhanden, nichts neu gebaut | bereits getestet (`CaseCreationTest`) |
| KI-04 | Datei `plan.exe.stl` | übernommen | Die Endung ist `.stl`, die Datei soll angenommen werden. Nur die letzte Endung zählt | neuer Unit-Test `onlyLastExtensionCounts` |
| KI-05 | Datei `plan.stl.` | verworfen | Dublette, `plan.` wird schon geprüft | – |
| KI-06 | Endung in gemischter Schreibweise `Plan.StL` | verworfen | Dublette, Groß-/Kleinschreibung ist schon getestet | – |
| KI-07 | Gleichen Dateinamen zweimal hochladen | verworfen | In den Anforderungen nicht geregelt, die KI hat etwas angenommen. Wäre eine Rückfrage an die Fachseite | – |
| KI-08 | Datei mit 10 MB − 1 Byte | verworfen | Dublette, schon in den Grenzwerttests | – |
| KI-09 | Zwei Dateien mit je 10 MB in einem Fall | übernommen | REQ-003 gilt pro Datei, nicht pro Fall. Bisher nicht getestet | neuer Unit-Test `limitAppliesPerFileNotPerCase` |
| KI-10 | Ersteller gibt mit falscher Bestätigung frei (zwei Verstöße) | übernommen | Es muss genau ein Audit-Eintrag entstehen. Bisher nur Einzelverstöße getestet (REQ-008) | neuer Unit-Test `twoViolationsAtOnceAreLoggedOnce` |
| KI-11 | Zwei Freigaben gleichzeitig | übernommen | Abgeleitet aus RISK-06, bisher nicht getestet. Ergebnis eindeutig: genau eine Freigabe | neuer Unit-Test `ConcurrentApprovalTest` (ohne Sleeps) |
| KI-12 | Upload und Freigabe gleichzeitig | verworfen | Überschneidet sich mit KI-11, Ausgang hängt vom Zeitpunkt ab und ist schwer eindeutig prüfbar | – |
| KI-13 | Eigenen Zeitstempel im Request mitsenden | übernommen | REQ-008 verlangt einen systemseitigen Zeitstempel. Bisher nicht getestet | neuer Integrationstest `clientTimestampIsIgnored` |
| KI-14 | Bestätigung der Patient-ID in Kleinbuchstaben | verworfen | Dublette, in Unit- und Integrationstests schon vorhanden | – |
| KI-15 | Benutzer „Praktikant“ darf nicht freigeben | verworfen | Widerspricht einer bewussten Entscheidung: keine Rollenprüfung (RISK-02). Die KI kannte das nicht | – |

## Zusammenfassung

**15 vorgeschlagen, 7 übernommen, 8 verworfen.**

- Von den 7 übernommenen waren 2 (KI-01, KI-03) schon durch vorhandene Tests abgedeckt, 5 sind neue Tests (KI-04, KI-09, KI-10, KI-11, KI-13).
- Von den 8 verworfenen waren 6 Dubletten (KI-02, 05, 06, 08, 14 und im Kern KI-12), 2 beruhten auf Annahmen, die nicht in den Anforderungen stehen (KI-07, KI-15).

Jeder neue Test hat ein REQ-Tag und im Kommentar den Vermerk "Origin: AI proposal KI-xx". Das Traceability-Skript behandelt ihn wie jeden anderen Test.

## Was ich daraus gelernt habe

**Typische Schwächen der KI-Vorschläge:**
- **Annahmen, die nicht in den Anforderungen stehen.** KI-15 nimmt eine Rollenprüfung an, die wir bewusst nicht haben. KI-07 nimmt eine Regel zu doppelten Dateinamen an, die niemand festgelegt hat. KI-04 sprach von einer „gefährlichen Endung“, die Anforderung sagt nur, dass die letzte Endung zählt.
- **Dubletten.** Die KI wusste nicht, was schon getestet ist. Mehr als die Hälfte der Vorschläge war schon abgedeckt oder doppelt.
- **Schwer prüfbare Formulierungen.** Bei KI-12 hängt das Ergebnis vom Zeitpunkt ab. Ein Test dafür wäre unzuverlässig.
- **Gut waren** die Vorschläge zu Dingen, die ich nicht bedacht hatte: Gleichzeitigkeit (KI-11), zwei Verstöße auf einmal (KI-10), ein vom Client gesendeter Zeitstempel (KI-13).

**Auch ich bin nicht unfehlbar:** Beim Prüfen habe ich `plan.exe.stl` kurz mit `plan.stl.exe` verwechselt. Erst der Blick in den Code hat es geklärt. Das zeigt, warum man mit dem echten Verhalten abgleicht und nicht aus dem Kopf entscheidet.

**Warum im regulierten Umfeld immer ein Mensch prüft und alles nachvollziehbar bleibt:**
- Eine KI übernimmt keine Verantwortung. Für die Verifikation einer Medizinprodukte-Software (IEC 62304, ISO 13485) muss eine Person einstehen, die nachweisen kann, warum etwas getestet wurde und warum nicht.
- KI-Vorschläge können plausibel klingen und trotzdem falsch sein. Ein ungeprüft übernommener Test, der etwas Falsches festschreibt, wäre ein falscher Nachweis.
- Nachvollziehbarkeit heißt: Jeder Test lässt sich bis zur Anforderung zurückverfolgen, und für jeden Vorschlag ist dokumentiert, wer ihn mit welcher Begründung angenommen oder verworfen hat. Genau das steht in dieser Datei.
