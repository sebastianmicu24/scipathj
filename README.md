# SciPathJ

**Segmentation and Classification of Images, Pipelines for the Analysis of Tissue Histopathology**

![SciPathJ Logo](src/main/resources/icon.png)

SciPathJ è un software moderno basato su Java per l'analisi di immagini istopatologiche, progettato per fornire segmentazione e classificazione automatizzata di immagini istologiche. Il progetto si basa su metodologie comprovate incorporando un'interfaccia utente moderna e un'architettura estendibile.

## Indice

- [Panoramica del Progetto](#panoramica-del-progetto)
- [Caratteristiche Principali](#caratteristiche-principali)
- [Architettura del Sistema](#architettura-del-sistema)
- [Tecnologie Utilizzate](#tecnologie-utilizzate)
- [Installazione](#installazione)
- [Utilizzo](#utilizzo)
- [Pipeline di Analisi](#pipeline-di-analisi)
- [Gestione delle ROI](#gestione-delle-roi)
- [Integrazione StarDist](#integrazione-stardist)
- [Sviluppo](#sviluppo)
- [Contribuire](#contribuire)
- [Licenza](#licenza)

## Panoramica del Progetto

SciPathJ è un'applicazione desktop professionale per l'analisi di immagini istopatologiche che combina algoritmi avanzati di elaborazione delle immagini con un'interfaccia utente intuitiva. Il software è progettato per ricercatori e professionisti nel campo della patologia digitale che necessitano di strumenti automatizzati per l'analisi del tessuto.

### Visione del Progetto

- **Analisi Automatizzata**: Software altamente automatizzato per l'analisi istopatologica
- **Elaborazione Batch**: Processamento di cartelle di immagini con risultati completi
- **Interfaccia Moderna**: Interfaccia utente pulita e intuitiva con design professionale
- **Estendibilità**: Architettura pronta per plugin per futuri miglioramenti

## Caratteristiche Principali

### 🖼️ Gestione Avanzata delle Immagini
- Supporto per formati comuni: JPG, PNG, GIF, BMP, TIFF
- Supporto per formati scientifici: LSM, CZI, ND2, OIB, OIF, VSI
- Supporto per formati di microscopia: IMS, LIF, SCN, SVS, NDPI
- Galleria di miniature con navigazione efficiente
- Visualizzatore di immagini principale con metadati

### 🔍 Segmentazione Intelligente
- **Segmentazione Nucleare**: Integrazione con StarDist per il rilevamento dei nuclei
- **Segmentazione Vascolare**: Algoritmi di sogliatura per il rilevamento dei vasi
- Parametri configurabili per diversi tipi di tessuto
- Pre-processing automatico delle immagini

### 🎯 Sistema ROI (Region of Interest)
- Creazione interattiva di ROI: Quadrato, Rettangolo, Cerchio
- Gestione multi-immagine con associazione automatica
- Esportazione in formati compatibili con ImageJ (.roi, .zip)
- Importazione da file ROI esistenti
- Visualizzazione sovrapposta sulle immagini

### 🎨 Interfaccia Utente Moderna
- Temi Chiari/Scuri con FlatLaf
- Icone professionali FontAwesome
- Design responsivo e transizioni fluide
- Navigazione a schede intuitive
- Barra di stato con feedback in tempo reale

### ⚙️ Pipeline Estendibili
- Architettura a pipeline modulare
- Configurazione passo-passo
- Supporto per l'aggiunta di nuovi algoritmi
- Esecuzione batch con monitoraggio del progresso

## Architettura del Sistema

### Struttura del Progetto

```
com.scipath.scipathj/
├── core/                 # Motore di elaborazione principale
│   ├── engine/          # Coordinatore principale dell'elaborazione
│   ├── pipeline/        # Sistema di gestione delle pipeline
│   ├── config/          # Gestione della configurazione
│   └── events/          # Sistema di eventi per aggiornamenti UI
├── ui/                  # Componenti dell'interfaccia utente
│   ├── main/           # Finestra principale dell'applicazione
│   ├── components/     # Componenti UI riutilizzabili
│   ├── dialogs/        # Impostazioni e dialoghi
│   ├── themes/         # Gestione dei temi
│   ├── model/          # Modelli dati UI
│   └── utils/          # Utilità UI
├── analysis/           # Algoritmi di analisi
├── data/               # Modelli dati e gestione
│   └── model/          # Strutture dati principali
└── SciPathJApplication.java # Classe principale dell'applicazione
```

### Componenti Principali

#### Core Engine
- **SciPathJEngine**: Coordinatore centrale dell'elaborazione
- **ConfigurationManager**: Gestione delle impostazioni e preferenze
- **EventBus**: Sistema di comunicazione basato su eventi
- **Pipeline System**: Architettura di pipeline estendibile con interfacce

#### Sistema UI
- **MainWindow**: Interfaccia principale dell'applicazione con navigazione a schede
- **PipelineSelectionPanel**: Selezione interattiva delle pipeline con schede visive
- **PipelineRecapPanel**: Visualizzazione delle informazioni della pipeline
- **FolderSelectionPanel**: Selezione cartelle con drag-and-drop
- **ImageGallery**: Galleria verticale di miniature
- **MainImageViewer**: Visualizzatore principale di immagini

#### Gestione ROI
- **ROIManager**: Sistema centralizzato di gestione delle ROI
- **ROIOverlay**: Sistema di sovrapposizione per il disegno interattivo
- **ROIToolbar**: Barra degli strumenti per la gestione delle ROI
- Supporto completo per formati ImageJ (.roi, .zip)

## Tecnologie Utilizzate

### Core Java
- **Java 23**: Ultima versione con funzionalità preview abilitate
- **Maven**: Gestione delle dipendenze e build
- **SLF4J + Logback**: Sistema di logging professionale

### Elaborazione delle Immagini
- **ImageJ 2.9.0**: Ecosistema completo per l'elaborazione delle immagini scientifiche
- **ImgLib2**: Libreria per l'elaborazione di immagini multidimensionali
- **CSBDeep 0.3.5-SNAPSHOT**: Deep learning per l'analisi biologica
- **StarDist**: Rilevamento di nuclei basato su deep learning

### Machine Learning
- **XGBoost4J 2.1.4**: Algoritmi di machine learning per la classificazione
- **TensorFlow 1.15.0**: Backend per reti neurali

### Interfaccia Utente
- **FlatLaf 3.4.1**: Temi moderni per applicazioni Swing
- **Ikonli 12.3.1**: Icone professionali FontAwesome
- **Swing**: Framework UI principale di Java

### Dati
- **Jackson 2.15.2**: Elaborazione JSON
- **Apache Commons**: Utilità varie

## Installazione

### Prerequisiti

- Java 23 o successivo
- Maven 3.6 o successivo
- 4GB di RAM raccomandati
- Spazio su disco: 500MB

### Build da Sorgenti

1. Clonare il repository:
```bash
git clone https://github.com/sebastianmicu24/scipathj.git
cd scipathj
```

2. Compilare il progetto:
```bash
mvn clean compile
```

3. Eseguire l'applicazione:
```bash
mvn exec:java
```

### Creazione dell'Eseguibile

Per creare un JAR eseguibile:
```bash
mvn clean package
```

L'eseguibile verrà creato in `target/scipathj-1.0.0.jar`

## Utilizzo

### Avvio dell'Applicazione

1. Avviare SciPathJ:
```bash
java -jar target/scipathj-1.0.0.jar
```

2. Selezionare una pipeline di analisi dalla schermata principale

3. Scegliere una cartella contenente le immagini da analizzare

4. Navigare nella galleria e selezionare le immagini di interesse

### Flusso di Lavoro Principale

1. **Selezione della Pipeline**
   - Scegliere tra le pipeline disponibili
   - Visualizzare i passaggi di analisi previsti
   - Configurare i parametri specifici

2. **Selezione delle Immagini**
   - Selezionare una cartella tramite drag-and-drop
   - Navigare nella galleria delle miniature
   - Visualizzare le immagini nel visualizzatore principale

3. **Analisi**
   - Avviare l'analisi con il pulsante "Start"
   - Monitorare il progresso nella barra di stato
   - Visualizzare i risultati al termine

4. **Gestione delle ROI**
   - Creare ROI manualmente sugli immagini
   - Utilizzare le ROI generate automaticamente
   - Esportare le ROI per analisi esterne

## Pipeline di Analisi

### Pipeline Disponibili

#### 1. Analisi Epatica H&E
- Segmentazione nucleare con StarDist
- Segmentazione vascolare
- Estrazione delle caratteristiche morfologiche
- Classificazione del tessuto

#### 2. Segmentazione Nucleare
- Rilevamento nuclei con StarDist
- Filtraggio per dimensione e forma
- Statistiche nucleari
- Esportazione risultati

#### 3. Analisi Vascolare
- Segmentazione dei vasi sanguigni
- Analisi della densità vascolare
- Misurazioni morfologiche
- Visualizzazione risultati

### Configurazione della Pipeline

Ogni pipeline offre configurazioni specifiche:

#### Impostazioni di Segmentazione Nucleare
- Scelta del modello StarDist
- Soglie di probabilità e NMS
- Normalizzazione dell'input
- Parametri di tiling

#### Impostazioni di Segmentazione Vascolare
- Soglia di intensità
- Sigma del blur gaussiano
- Chiusura morfologica
- Dimensioni minime/massime

## Gestione delle ROI

### Creazione di ROI

1. Selezionare uno strumento di disegno dalla toolbar:
   - Quadrato
   - Rettangolo
   - Cerchio

2. Fare clic e trascinare sull'immagine per creare la ROI

3. La ROI verrà automaticamente associata all'immagine corrente

### Gestione delle ROI Esistenti

- **Salva ROI**: Esporta le ROI dell'immagine corrente
- **Salva Tutte**: Esporta tutte le ROI in un file ZIP master
- **Cancella Tutte**: Rimuove tutte le ROI dall'immagine corrente

### Formati Supportati

- **ROI singola**: File .roi (compatibile con ImageJ)
- **ROI multiple**: File .zip (set di ROI ImageJ)
- **Master ZIP**: File ZIP organizzato per immagine

## Integrazione StarDist

SciPathJ integra StarDist, un algoritmo state-of-the-art per il rilevamento dei nuclei cellulari basato su deep learning.

### Modelli Supportati

- **Versatile (fluorescent)**: Modello generale per immagini fluorescenti
- **Versatile (H&E)**: Modello specifico per immagini istopatologiche H&E
- **DSB 2018**: Modello addestrato sul dataset DSB 2018
- **Tissue Net**: Modello per tessuti vari

### Configurazione StarDist

```java
// Esempio di configurazione
NuclearSegmentationSettings settings = new NuclearSegmentationSettings();
settings.setModelChoice("Versatile (H&E)");
settings.setProbThresh(0.5);
settings.setNmsThresh(0.4);
settings.setNormalizeInput(true);
settings.setPercentileBottom(1.0);
settings.setPercentileTop(99.8);
```

### Pre-processing Automatico

- Conversione a 8-bit per compatibilità
- Normalizzazione percentile-based
- Gestione di immagini RGB a canali separati
- Fallback a metodi tradizionali in caso di errori

## Sviluppo

### Ambiente di Sviluppo

1. IDE raccomandato: IntelliJ IDEA
2. Plugin utili:
   - Maven Integration
   - Git Integration
   - SonarLint

### Struttura del Codice

Il progetto segue le linee guida di sviluppo Java professionale:

- **Principio di Responsabilità Unica**: Ogni classe ha una sola responsabilità
- **Documentazione Completa**: JavaDoc per tutte le classi e metodi pubblici
- **Gestione degli Errori**: Eccezioni personalizzate e logging dettagliato
- **Test Unitari**: JUnit 5 con Mockito per il mocking

### Compilazione e Testing

```bash
# Compilazione
mvn clean compile

# Esecuzione test
mvn test

# Analisi codice
mvn spotbugs:check
mvn pmd:check

# Copertura test
mvn jacoco:report
```

### Standard di Codifica

- Dimensione massima dei file: 400 linee
- Dimensione massima dei metodi: 30 linee
- Nomi descrittivi per classi, metodi e variabili
- Commenti che spiegano il "perché" non il "cosa"

## Contribuire

### Linee Guida per i Contributi

1. Fare fork del repository
2. Creare un branch feature: `git checkout -b feature/nuova-funzionalita`
3. Commit delle modifiche: `git commit -m 'Aggiunta nuova funzionalità'`
4. Push del branch: `git push origin feature/nuova-funzionalita`
5. Aprire una Pull Request

### Segnalazione di Bug

Utilizzare le issue di GitHub per segnalare bug:
- Titolo descrittivo
- Passi per riprodurre il problema
- Comportamento atteso vs. comportamento osservato
- Stack trace se disponibile
- Versione di Java e del sistema operativo

## Licenza

Questo progetto è distribuito sotto licenza BSD 3-Clause. Vedere il file [LICENSE](LICENSE) per i dettagli.

## Riconoscimenti

- **ImageJ Team**: Per l'eccezionale framework di elaborazione delle immagini
- **StarDist Team**: Per l'algoritmo di segmentazione nucleare state-of-the-art
- **CSBDeep Team**: Per il framework di deep learning per l'analisi biologica
- **FlatLaf Team**: Per i moderni temi Swing

## Contatti

- **Autore**: Sebastian Micu
- **Email**: sebastian.micu@example.com
- **Repository**: https://github.com/sebastianmicu24/scipathj
- **Issues**: https://github.com/sebastianmicu24/scipathj/issues

---

**SciPathJ** - Strumento professionale per l'analisi istopatologica digitale