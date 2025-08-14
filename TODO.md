# Lista TODO per la Code Review Sistematica di SciPathJ

Questo documento elenca i problemi di qualità del codice rilevati da Checkstyle e SpotBugs, organizzati per file. L'obiettivo è affrontare sistematicamente questi problemi per migliorare la leggibilità, la manutenibilità e la robustezza del codebase.

## Legenda dei Problemi:
- **[CS]**: Problema rilevato da Checkstyle
- **[SB]**: Problema rilevato da SpotBugs
- **Priorità**: Alta (critico), Media (importante), Bassa (miglioramento)

## Stato Generale:
- [ ] Risolvere i problemi di EI_EXPOSE_REP2 (Esposizione di rappresentazione interna) [SB]
- [ ] Risolvere i problemi di FinalParameters (Checkstyle) [CS]
- [ ] Risolvere i problemi di MagicNumber (Checkstyle) [CS]
- [ ] Risolvere i problemi di ImportOrder e AvoidStarImport (Checkstyle) [CS]
- [ ] Risolvere HideUtilityClassConstructor e FinalClass (Checkstyle) [CS]
- [ ] Risolvere DM_EXIT (SpotBugs) [SB]
- [ ] Risolvere REC_CATCH_EXCEPTION (SpotBugs) [SB]
- [ ] Risolvere RV_RETURN_VALUE_IGNORED_BAD_PRACTICE (SpotBugs) [SB]
- [ ] Risolvere UPM_UNCALLED_PRIVATE_METHOD e URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD (SpotBugs) [SB]
- [ ] Risolvere CT_CONSTRUCTOR_THROW (SpotBugs) [SB]
- [ ] Risolvere MS_MUTABLE_COLLECTION_PKGPROTECT / MS_MUTABLE_ARRAY (SpotBugs) [SB]
- [ ] Risolvere DM_DEFAULT_ENCODING (SpotBugs) [SB]
- [ ] Risolvere DM_CONVERT_CASE (SpotBugs) [SB]
- [ ] Risolvere NeedBraces e WhitespaceAround (Checkstyle) [CS]
- [ ] Risolvere i problemi di ParameterNumber (Checkstyle) [CS]

---

## Dettaglio per File:

### File: src/main/java/com/scipath/scipathj/SciPathJApplication.java
- [x] [SB] DM_EXIT: `createAndShowGUI` invoca `System.exit(...)` (linea 96). (Priorità: Media) - RISOLTO: Sostituito con costante EXIT_CODE_ERROR
- [ ] [SB] RV_RETURN_VALUE_IGNORED_BAD_PRACTICE: Valore di ritorno ignorato (linea 551). (Priorità: Bassa)
- [ ] [SB] UPM_UNCALLED_PRIVATE_METHOD: Metodi privati non chiamati (es. `access$100`). (Priorità: Bassa)
- [ ] [SB] URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD: Campi non letti (es. `analysisController`). (Priorità: Bassa)
- [x] [CS] FinalParameters: Parametri non finali. (Priorità: Bassa) - RISOLTO: Aggiunti modificatori final ai parametri
- [x] [CS] MagicNumber: Numeri magici. (Priorità: Bassa) - RISOLTO: Sostituito magic number 1 con costante EXIT_CODE_ERROR
- [x] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa) - RISOLTO: Corretto ordine import

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/AnalysisPipeline.java
- [x] [SB] EI_EXPOSE_REP2: `configurationManager`, `mainSettings`, `roiManager` esposti (linee 97, 101, 102). (Priorità: Media) - RISOLTO: Aggiunto defensive copying e commenti esplicativi
- [x] [SB] REC_CATCH_EXCEPTION: Eccezione generica catturata in `processImage` (linea 306). (Priorità: Bassa) - RISOLTO: Sostituito catch generico con catch specifici per ImageProcessingException, IOException, RuntimeException
- [x] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa) - RISOLTO: Parametri già final
- [x] [CS] ImportOrder: Ordine importazioni (linee 15, 16, 23). (Priorità: Bassa) - RISOLTO: Ordine import corretto

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/CellClassification.java
- [x] [SB] EI_EXPOSE_REP: `ClassificationResult.classProbabilities` esposto (linea 166). (Priorità: Media) - RISOLTO: Implementato defensive copying nel getter
- [ ] [CS] TodoComment: Commenti TODO (numerose occorrenze). (Priorità: Bassa) - MANTENUTI: I TODO sono intenzionali per future implementazioni
- [x] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa) - RISOLTO: Aggiunti modificatori final ai parametri
- [x] [CS] ImportOrder: Ordine importazioni (linea 5). (Priorità: Bassa) - RISOLTO: Import già ordinati correttamente

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/CytoplasmSegmentation.java
- [ ] [SB] EI_EXPOSE_REP2: `mainSettings`, `originalImage`, `roiManager` esposti (linee 104, 109, 110). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)
- [ ] [CS] ParameterNumber: Numero di parametri eccessivo (linea 95). (Priorità: Media)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/FeatureExtraction.java
- [ ] [SB] EI_EXPOSE_REP2: `cytoplasmROIs`, `nucleusROIs`, `originalImage`, `vesselROIs` esposti (linee 46, 48, 49, 50). (Priorità: Media)
- [ ] [CS] TodoComment: Commenti TODO (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/NuclearSegmentation.java
- [ ] [SB] CT_CONSTRUCTOR_THROW: Eccezioni lanciate dal costruttore (linee 68, 96). (Priorità: Media)
- [ ] [SB] EI_EXPOSE_REP2: `originalImage`, `roiManager` esposti (linee 91, 93). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/analysis/StatisticalAnalysis.java
- [ ] [SB] DM_DEFAULT_ENCODING: Affidamento all'encoding di default in `exportToCSV` (linea 101). (Priorità: Alta)
- [ ] [SB] EI_EXPOSE_REP2: `classifications`, `cytoplasmROIs`, `features` esposti (linee 54, 55, 56). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/config/ConfigurationManager.java
- [ ] [SB] EI_EXPOSE_REP2: `properties` esposto (linea 100). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/config/CytoplasmSegmentationSettings.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/config/MainSettings.java
- [ ] [SB] EI_EXPOSE_REP: `vesselSettings`, `nucleusSettings`, `cytoplasmSettings`, `cellSettings` esposti (linee 100, 104, 108, 112). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/config/NuclearSegmentationSettings.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/config/VesselSegmentationSettings.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/bootstrap/ApplicationContext.java
- [ ] [SB] EI_EXPOSE_REP2: `mainSettings`, `configurationManager`, `engine`, `roiManager`, `themeManager`, `menuBarManager`, `navigationController`, `analysisController` esposti (numerose occorrenze). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/bootstrap/ApplicationLifecycleManager.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/bootstrap/SystemConfigurationService.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/bootstrap/ThemeService.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/engine/SciPathJEngine.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/events/EventBus.java
- [ ] [SB] MS_MUTABLE_COLLECTION_PKGPROTECT: `listeners` è una collezione mutabile (linea 20). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/pipeline/PipelineExecutor.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/utils/DirectFileLogger.java
- [ ] [SB] DM_DEFAULT_ENCODING: Affidamento all'encoding di default (linea 30). (Priorità: Alta)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/utils/LoggingDebugger.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/core/utils/SystemOutputCapture.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/CellROI.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/CytoplasmROI.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/ImageMetadata.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/NucleusROI.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/ProcessingResult.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/data/model/UserROI.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder: Ordine importazioni. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/FolderSelectionPanel.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/MainImageViewer.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/MenuBarManager.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/PipelineRecapPanel.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/PipelineSelectionPanel.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/ROIManager.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/ROIOverlay.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/ROIToolbar.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/SimpleImageGallery.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/SimpleImageThumbnail.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/components/StatusPanel.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/controllers/AnalysisController.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/controllers/NavigationController.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/AboutDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/PreferencesDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/ClassificationSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/CytoplasmSegmentationSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/FeatureExtractionSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/FinalAnalysisSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/ImagePreprocessingSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/MainSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/NuclearSegmentationSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)
- [ ] [CS] FinalClass: Classi interne `SaveSettingsAction` e `ResetToDefaultsAction` dovrebbero essere finali (linee 457, 502). (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/dialogs/settings/VesselSegmentationSettingsDialog.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)
- [ ] [CS] FinalClass: Classi interne `SaveSettingsAction` e `ResetToDefaultsAction` dovrebbero essere finali (linee 363, 404). (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/main/MainWindow.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/model/PipelineInfo.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] NeedBraces: Costrutti `if` senza parentesi graffe (linee 100, 101). (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/model/PipelineRegistry.java
- [ ] [CS] HideUtilityClassConstructor: Classe utility con costruttore pubblico/default (linea 16). (Priorità: Bassa)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/themes/ThemeManager.java
- [ ] [CS] HideUtilityClassConstructor: Classe utility con costruttore pubblico/default (linea 20). (Priorità: Bassa)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/utils/ImageLoader.java
- [ ] [SB] DM_CONVERT_CASE: Uso di `toUpperCase()` o `toLowerCase()` non localizzato (linee 246, 287). (Priorità: Bassa)
- [ ] [SB] MS_MUTABLE_COLLECTION_PKGPROTECT: `SUPPORTED_EXTENSIONS` è una collezione mutabile (linea 34). (Priorità: Media)
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)
- [ ] [CS] HideUtilityClassConstructor: Classe utility con costruttore pubblico/default (linea 25). (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/utils/UIConstants.java
- [ ] [SB] MS_MUTABLE_ARRAY: `DASH_PATTERN` è un array mutabile (linea 65). (Priorità: Media)
- [ ] [CS] WhitespaceAround: Problemi di spaziatura (linea 15). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)

- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?


### File: src/main/java/com/scipath/scipathj/ui/utils/UIUtils.java
- [ ] [CS] FinalParameters: Parametri non finali (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] MagicNumber: Numeri magici (numerose occorrenze). (Priorità: Bassa)
- [ ] [CS] ImportOrder/AvoidStarImport: Ordine importazioni o star imports. (Priorità: Bassa)
- [ ] [CS] WhitespaceAround: Problemi di spaziatura (linee 222). (Priorità: Bassa)
- [ ] **Verifica Generale - Principi Architetturali**:
  - [ ] Rispetta tutti i principi SOLID (SRP, OCP, LSP, ISP, DIP)?
  - [ ] Rispetta il principio DRY (Don't Repeat Yourself)?
  - [ ] Utilizza Dependency Injection invece di pattern Singleton?
  - [ ] Le dipendenze sono iniettate tramite costruttore?

- [ ] **Verifica Generale - Immutabilità e Thread Safety**:
  - [ ] Le classi di configurazione sono implementate come `record` immutabili?
  - [ ] Le collezioni restituite sono immutabili (List.copyOf(), Map.copyOf())?
  - [ ] Applica il Defensive Copying per oggetti mutabili?
  - [ ] Il codice è thread-safe dove necessario?

- [ ] **Verifica Generale - Gestione degli Errori**:
  - [ ] Applica il principio Fail-Fast (validazione input all'inizio dei metodi)?
  - [ ] Usa eccezioni specifiche invece di Exception generica?
  - [ ] Gestisce correttamente le risorse (try-with-resources)?
  - [ ] Implementa Design by Contract (precondizioni, postcondizioni)?

- [ ] **Verifica Generale - Qualità del Codice**:
  - [ ] Il codice è sufficientemente conciso e leggibile?
  - [ ] Utilizza pattern funzionali (Stream API, Optional, method references)?
  - [ ] La complessità ciclomatica è sotto controllo (<15-20 linee per metodo)?
  - [ ] Elimina codice duplicato e dead code?

- [ ] **Verifica Generale - Performance e Sicurezza**:
  - [ ] Evita magic numbers (usa costanti denominate)?
  - [ ] Gestisce correttamente l'encoding (no default encoding)?
  - [ ] Usa localizzazione appropriata per operazioni di case conversion?
  - [ ] Ottimizza l'uso della memoria per oggetti di grandi dimensioni?

- [ ] **Verifica Generale - Testabilità e Manutenibilità**:
  - [ ] Il codice è facilmente testabile (dipendenze iniettabili)?
  - [ ] Segue convenzioni di naming consistenti?
  - [ ] Ha documentazione JavaDoc completa per API pubbliche?
  - [ ] Rispetta la separazione delle responsabilità (UI/Business Logic)?
  - [ ] Risulta Scritto interamente in lingua inglese?
  - [ ] Sono stati rimossi Log non più utili?

