# ADR-001: Refactoring del Sistema di Configurazione e Adozione della Dependency Injection

**Stato**: Accettato
**Data**: 2025-08-14

## Contesto

La versione iniziale di SciPathJ utilizzava ampiamente il pattern Singleton per la gestione della configurazione (`ConfigurationManager`, `MainSettings`, ecc.) e per i servizi principali dell'applicazione. Sebbene questo approccio semplificasse l'accesso globale, ha portato a diversi problemi architetturali:

*   **Accoppiamento Elevato**: I componenti erano strettamente accoppiati alle implementazioni concrete dei Singleton, rendendo difficile la sostituzione o la modifica.
*   **Testabilità Scarsa**: L'impossibilità di fornire implementazioni mock dei servizi rendeva gli unit test complessi e inaffidabili.
*   **Gestione dello Stato Globale**: Lo stato mutabile e globale era difficile da tracciare e soggetto a race condition.
*   **Violazione dei Principi SOLID**: In particolare, il Single Responsibility Principle e il Dependency Inversion Principle non erano rispettati.

Era necessario un refactoring completo per allineare il progetto a standard moderni e renderlo "production-ready".

## Decisione

Si è deciso di intraprendere un refactoring architetturale completo con i seguenti obiettivi chiave:

1.  **Eliminare il Pattern Singleton**: Rimuovere completamente tutte le istanze del pattern Singleton (`getInstance()`).
2.  **Adottare la Dependency Injection (DI)**: Introdurre un `ApplicationContext` centrale responsabile dell'istanziazione, configurazione e iniezione delle dipendenze (principalmente tramite costruttore).
3.  **Introdurre l'Immutabilità**: Convertire le classi di configurazione (es. `VesselSegmentationSettings`) da classi mutabili a **`record` Java 16+ immutabili**. Questo garantisce la thread-safety e la prevedibilità dello stato della configurazione.
4.  **Applicare Rigorosamente i Principi SOLID**: Ristrutturare le classi per aderire a tutti e cinque i principi SOLID.
5.  **Modernizzare il Codice**: Sfruttare le funzionalità moderne di Java per rendere il codice più conciso, leggibile e sicuro.

## Conseguenze

L'adozione di questa nuova architettura ha portato a benefici significativi in tutto il codebase:

### Positive

*   **Testabilità Migliorata**: I componenti ora possono essere testati in isolamento fornendo dipendenze mock, migliorando drasticamente la copertura e l'affidabilità dei test.
*   **Disaccoppiamento**: I componenti dipendono da astrazioni (interfacce o classi base) che vengono iniettate, non da implementazioni concrete. Questo aumenta la flessibilità e la manutenibilità.
*   **Chiarezza e Leggibilità**: L'uso dei `record` ha ridotto drasticamente il codice boilerplate (getter, setter, equals, hashCode). Il flusso delle dipendenze è ora esplicito e facile da seguire.
*   **Thread Safety**: Le configurazioni immutabili eliminano un'intera classe di bug legati alla concorrenza.
*   **Aderenza ai Principi SOLID**:
    *   **SRP**: Ogni classe ha ora una responsabilità singola e ben definita (es. `ConfigurationManager` si occupa solo della persistenza).
    *   **OCP**: Il sistema è più facile da estendere. Ad esempio, è possibile aggiungere nuove implementazioni di servizi senza modificare i client esistenti.
    *   **LSP**: Assicurato dal corretto uso delle interfacce.
    *   **ISP**: Le interfacce sono state create per essere specifiche per i client.
    *   **DIP**: Completamente rispettato grazie all'uso della DI.
*   **Manutenibilità a Lungo Termine**: L'architettura è più pulita, più facile da capire per i nuovi sviluppatori e più semplice da far evolvere nel tempo.

### Negative

*   **Curva di Apprendimento Iniziale**: Gli sviluppatori devono familiarizzare con il pattern DI e con il funzionamento dell'`ApplicationContext`.
*   **Configurazione Iniziale più Complessa**: Il "wiring" delle dipendenze nell'`ApplicationContext` richiede una configurazione iniziale più esplicita rispetto all'accesso globale dei Singleton.

Tuttavia, i benefici in termini di qualità, testabilità e manutenibilità del software superano di gran lunga questi svantaggi iniziali. L'architettura è ora robusta e pronta per future estensioni.