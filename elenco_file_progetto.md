# Documentazione Architetturale e Stato dei File - SciPathJ

Questo documento descrive lo stato attuale del progetto SciPathJ, riflettendo il completamento di un significativo refactoring architetturale. Il software ora aderisce rigorosamente ai principi SOLID, utiliza pattern moderni come la Dependency Injection e sfrutta le funzionalità avanzate di Java 16+ come i `record` per garantire un codice pulito, testabile e production-ready.

## Principi Guida dell'Architettura

L'architettura attuale è fondata sui seguenti pilastri:

*   **Principi SOLID**: Ogni componente del sistema è stato progettato per aderire ai cinque principi SOLID, garantendo un'alta manutenibilità e flessibilità.
*   **Dependency Injection (DI)**: Il pattern Singleton è stato completamente eradicato in favore della DI. Un `ApplicationContext` centrale gestisce il ciclo di vita e l'iniezione delle dipendenze, promuovendo disaccoppiamento e testabilità.
*   **Immutabilità**: Le classi di configurazione e i modelli di dati sono stati trasformati in `record` immutabili, garantendo la thread-safety e la prevedibilità dello stato.
*   **Modernizzazione di Java**: Il codice sfrutta pienamente le funzionalità moderne di Java (record, API concise, programmazione funzionale) per migliorare la leggibilità e la sicurezza.
*   **Internazionalizzazione (i18n)**: Le stringhe visibili all'utente sono state estratte in file di risorse per facilitare la traduzione.
*   **Gestione Robusta degli Errori**: Le eccezioni generiche sono state sostituite con eccezioni custom e specifiche per dominio, migliorando l'affidabilità del sistema.

---

## Stato dei File di Progetto

Di seguito è riportata un'analisi dello stato attuale dei file principali del progetto, post-refactoring.

### File di Configurazione e Build

- **`.gitignore`**: Configurazioni standard per ignorare file di build di Maven, log, file di configurazione degli IDE e file di sistema operativo.
- **`pom.xml`**:
    *   **Stato Attuale**: Configurato per **Java 23**. Le dipendenze (incluse ImageJ, StarDist, FlatLaf) sono aggiornate. I plugin Maven per compilazione, packaging, code quality (SpotBugs, PMD) e test coverage (JaCoCo) sono integrati e correttamente configurati. Include metadati essenziali per un progetto open source (`<licenses>`, `<developers>`, `<scm>`).
    *   **SOLID/DI**: Non direttamente applicabile, ma supporta l'architettura DI attraverso una gestione pulita delle dipendenze.
- **`README.md`**:
    *   **Stato Attuale**: Completamente tradotto in inglese e aggiornato per riflettere l'architettura basata su Dependency Injection, i principi SOLID e le tecnologie più recenti. Le sezioni sull'architettura e sullo sviluppo descrivono accuratamente lo stato attuale del progetto.

### Codice Sorgente (`src/main/java`)

#### Package `com.scipath.scipathj.core`

- **`bootstrap/`**: Contiene le classi per l'avvio dell'applicazione.
    - **`ApplicationContext.java`**: **Componente Chiave**. Gestisce l'intero ciclo di vita dell'applicazione. Inizializza e inietta tutte le dipendenze necessarie (`ConfigurationManager`, `SciPathJEngine`, etc.), eliminando la necessità di Singleton.
- **`analysis/`**: Contiene la logica di segmentazione e analisi. **Questa parte è stata completamente refattorizzata**.
    - **`AnalysisPipeline.java`**: Orchestra i passi di analisi. Riceve le configurazioni tramite DI. Utilizza `record` immutabili per i risultati (`AnalysisResults`, `ImageAnalysisResult`). La logica asincrona è gestita in modo robusto.
    - **`NuclearSegmentation.java`**: (Precedentemente `SimpleHENuclearSegmentation`). Completamente ripulita dai workaround per il ClassLoader di TensorFlow. Riceve le sue dipendenze (`ConfigurationManager`, `ROIManager`) tramite DI. Usa eccezioni specifiche (`NuclearSegmentationException`).
    - **`VesselSegmentation.java` / `CytoplasmSegmentation.java`**: Seguono lo stesso pattern di `NuclearSegmentation`. Sono classi focalizzate (SRP), ricevono dipendenze tramite DI e utilizzano record immutabili per le loro configurazioni.
    - **Placeholder Rimanenti**: `CellClassification.java`, `FeatureExtraction.java`, `StatisticalAnalysis.java` contengono ancora logica `TODO`. Sono state identificate e sono pronte per essere implementate o rimosse in cicli di sviluppo futuri.
- **`config/`**: Contiene le classi per la gestione della configurazione. **Questa parte è stata completamente refattorizzata**.
    - **`ConfigurationManager.java`**: Responsabilità unica di caricare e salvare le configurazioni da file `.properties`. **Non è più un Singleton**. Viene istanziato e iniettato dall'`ApplicationContext`. Restituisce `record` immutabili.
    - **`*Settings.java` (es. `VesselSegmentationSettings.java`, `NuclearSegmentationSettings.java`, `CytoplasmSegmentationSettings.java`)**: Convertite in **`record` Java 16+ immutabili**. Contengono la validazione dei dati nei loro costruttori compatti, garantendo che un'impostazione non valida non possa mai essere creata.
    - **`MainSettings.java`**: Classe che gestisce le impostazioni globali. È immutabile ma non un record, per permettere una maggiore flessibilità e contenere configurazioni nested.
- **`engine/`**:
    - **`SciPathJEngine.java`**: Motore di elaborazione centrale. **Non è più un Singleton**. Viene iniettato dove necessario.
    - **Classi Obsolete e Rimosse**: `ClassLoaderDebugger.java`, `Java21ClassLoaderFix.java`, `TensorFlowLibraryLoader.java` e `TensorFlowNetworkWrapper.java` sono state **rimosse o drasticamente semplificate**, poiché i problemi di compatibilità di TensorFlow sono stati risolti o gestiti a un livello inferiore.
- **`pipeline/`**: Contiene le interfacce e le classi per la gestione delle pipeline.
    - Le classi come `PipelineConfiguration.java` e `PipelineMetadata.java` sono state convertite in **`record` immutabili**.

#### Package `com.scipath.scipathj.ui`

- **`controllers/`**:
    - **`AnalysisController.java` / `NavigationController.java`**: Ricevono le dipendenze (es. `ConfigurationManager`, pannelli UI) tramite Dependency Injection. Gestiscono la logica dell'interfaccia utente e comunicano con il core del sistema.
- **`dialogs/`** e **`dialogs/settings/`**:
    - Tutte le finestre di dialogo, specialmente quelle delle impostazioni (es. `VesselSegmentationSettingsDialog.java`), sono state aggiornate per ricevere il `ConfigurationManager` e i record di impostazioni tramite DI. Caricano e salvano le configurazioni attraverso il `ConfigurationManager` invece di accedere a Singleton globali.
- **`main/`**:
    - **`MainWindow.java`**: La finestra principale dell'applicazione. Riceve tutte le sue dipendenze principali (engine, manager, controller) dall'`ApplicationContext` all'avvio.
- **`model/`**:
    - **`PipelineInfo.java`**: Convertita in un **`record` immutabile**.
    - **`PipelineRegistry.java`**: Le stringhe di testo (nomi delle pipeline, descrizioni) sono state preparate per l'internazionalizzazione.

#### Package `de.csbdresden` e `net.imagej.tensorflow`

- **Stato Attuale**: Questo codice proviene da librerie esterne (CSBDeep, StarDist) ed è stato parzialmente personalizzato per il logging e per alcuni workaround. Il refactoring ha **rimosso la maggior parte delle personalizzazioni invasive**.
- **`TensorFlowNetwork.java`**: La logica di caricamento del modello è stata semplificata. Le chiamate a `JOptionPane` (UI in logica core) sono state rimosse.
- **Logging**: I log in lingua italiana (`DirectFileLogger`) sono stati rimossi o tradotti, e il logging di debug eccessivo è stato ridotto a livelli appropriati per la produzione (`DEBUG`/`TRACE`).

---

## Conclusione

Il progetto SciPathJ ha raggiunto uno stato di maturità architetturale. L'eliminazione dei Singleton, l'adozione della Dependency Injection e l'uso di `record` immutabili hanno reso il codebase significativamente più **robusto, testabile e manutenibile**, allineandolo agli standard di un software professionale e "production-ready".
