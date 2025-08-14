# Piano Strategico di Miglioramento per SciPathJ

Questo documento delinea una strategia per il refactoring e il miglioramento del codebase di SciPathJ. L'obiettivo è modernizzare il codice, migliorare la manutenibilità e allinearlo alle best practice per i progetti open source. Il piano è suddiviso in fasi sequenziali per rendere il processo gestibile.

---

## Fase 0: Preparazione dell'Ambiente e Standard di Progetto ✅ **COMPLETATA**

**Obiettivo:** Stabilire una solida base per lo sviluppo, aggiornando le dipendenze, automatizzando la qualità del codice e rendendo il progetto accessibile a un pubblico internazionale.

**Note aggiuntive:** Sono stati creati file di configurazione per gli strumenti di qualità del codice:
- `spotbugs-exclude.xml` - Configurazione personalizzata per SpotBugs
- `checkstyle.xml` - Regole di stile per Checkstyle
- `QUALITY_TOOLS.md` - Documentazione completa degli strumenti di qualità del codice

1.  **Aggiornamento del `pom.xml`:** ✅ **COMPLETATO**
    *   **Aggiornamento Dipendenze:** ✅ Verificato e aggiornato tutte le dipendenze alle ultime versioni stabili (ImageJ 2.14.0, Jackson 2.18.2, FlatLaf 3.5.4, ecc.).
    *   Configurare il `maven-compiler-plugin` per usare `maven.compiler.release` con la versione Java corretta (`23`). ✅ Configurato con supporto completo per Java 23.
    *   Aggiungere plugin Maven per la qualità del codice:
        *   **Spotless** ✅ Implementato per la formattazione automatica del codice Java e del POM.
        *   **SpotBugs** ✅ Aggiunto per l'analisi statica con configurazione personalizzata.
        *   **Checkstyle** ✅ Aggiunto come alternativa a PMD per migliore compatibilità con Java 23.
    *   Compilare le sezioni `<licenses>`, `<developers>` e `<scm>` ✅ Formalizzato lo status open source del progetto con licenza Apache 2.0.

2.  **Internazionalizzazione del `README.md`:**
    *   Tradurre l'intero [`README.md`](README.md) in inglese.
    *   Rivedere le sezioni di installazione e utilizzo per massima chiarezza.

---

## Fase 1: Refactoring del Core e Pulizia Profonda

**Obiettivo:** Rimuovere il codice obsoleto e fragile, modernizzare la sintassi e applicare i principi SOLID al cuore dell'applicazione.

1.  **Rimozione dei Workaround e del Codice di Debug:**
    *   Eliminare completamente le classi di workaround: `ClassLoaderDebugger`, `Java21ClassLoaderFix`, `TensorFlowLibraryLoader`, `TensorFlowNetworkWrapper`.
    *   Rimuovere tutto il logging di debug a basso livello (es. `DirectFileLogger`) e le chiamate a `System.out.println` o `e.printStackTrace()`, sostituendole con un logging SLF4J appropriato a livelli `DEBUG` o `TRACE`.

2.  **Applicazione del Dependency Injection (DIP):**
    *   Rimuovere il pattern Singleton (`getInstance()`) da tutte le classi manager e di configurazione (es. `SciPathJEngine`, `ConfigurationManager`, `MainSettings`, etc.).
    *   Configurare un semplice meccanismo di Dependency Injection (manuale o tramite un micro-framework) per fornire le dipendenze tramite i costruttori.

3.  **Modernizzazione della Sintassi (Java 16+):**
    *   Convertire tutte le classi che agiscono come "data carrier" in `record` Java. Questo include:
        *   Classi di impostazioni (`NuclearSegmentationSettings`, `VesselSegmentationSettings`, etc.).
        *   Classi di metadati e risultati (`PipelineInfo`, `PipelineMetadata`, `ValidationResult`, etc.).
    *   Utilizzare `List.of()` e `Map.of()` per creare collezioni immutabili dove appropriato.

4.  **Rinominazione e Semplificazione:**
    *   Rinominare `SimpleHENuclearSegmentation` in `NuclearSegmentation` e aggiornare tutti i riferimenti.

---

## Fase 2: Internazionalizzazione e Refactoring dell'UI

**Obiettivo:** Disaccoppiare la logica UI dal testo e applicare i principi SOLID anche ai componenti dell'interfaccia utente.

1.  **Internazionalizzazione (i18n):**
    *   Creare file di risorse (es. `messages_en.properties`, `messages_it.properties`).
    *   Estrarre sistematicamente tutte le stringhe hardcoded dall'interfaccia utente (dialoghi, menu, pulsanti, etichette) e dai messaggi di errore mostrati all'utente, sostituendole con chiavi che caricano il testo dai file di risorse.

2.  **Refactoring dei Controller e Dialoghi:**
    *   Applicare la Dependency Injection anche ai controller e ai dialoghi dell'UI, come suggerito nel file di elenco.
    *   Rimuovere qualsiasi logica non-UI dalle classi della UI (es. chiamate a `JOptionPane` da classi non-UI come `TensorFlowNetwork`). La UI dovrebbe essere notificata tramite eventi o callback per mostrare errori.

---

## Fase 3: Isolamento e Pulizia delle Dipendenze Forkate

**Obiettivo:** Pulire il codice proveniente dalle librerie CSBDeep e StarDist, assicurandosi che sia coerente con gli standard del progetto SciPathJ.

1.  **Traduzione e Logging:**
    *   Tradurre tutti i commenti e i messaggi di log dall'italiano all'inglese nei package `de.csbdresden` e correlati.
    *   Sostituire i `System.out.println` e `e.printStackTrace()` con il logger SLF4J del progetto.

2.  **Risoluzione dei `TODO` e Pulizia:**
    *   Analizzare i `TODO` presenti in queste classi. Se sono critici, pianificarne la risoluzione. Altrimenti, se sono note di upstream, lasciarli ma assicurarsi che non impattino la stabilità di SciPathJ.
    *   Rimuovere i metodi `main` di test da queste classi.

---

## Fase 4: Rimozione del Codice Inutilizzato

**Obiettivo:** Rendere il codebase più snello eliminando le classi che sono solo placeholder.

1.  **Eliminazione delle Classi Placeholder:**
    *   Rimuovere le classi di analisi che sono vuote o contengono solo `TODO` e non sono implementate:
        *   `CellClassification`
        *   `FeatureExtraction`
        *   `StatisticalAnalysis`
    *   Queste funzionalità potranno essere reintrodotte in futuro quando saranno effettivamente implementate.

---

## Fase 5: Documentazione Finale

**Obiettivo:** Assicurare che il codice sia facile da capire e da mantenere per futuri contributori.

1.  **Revisione Completa del Javadoc:**
    *   Eseguire una passata su tutto il progetto per assicurarsi che ogni classe e metodo pubblico abbia una documentazione Javadoc chiara, completa e aggiornata, specialmente dopo i refactoring delle fasi precedenti.
    *   Usare il plugin Maven Javadoc per generare la documentazione e identificare le parti mancanti.
