# Comandi Utili per gli Strumenti di Qualità del Codice

Questo documento contiene una lista di comandi Maven utili per lavorare con Spotless, SpotBugs e Checkstyle nel progetto SciPathJ.

## Spotless (Formattazione del Codice)

### Comandi Base
```bash
# Controlla la formattazione del codice senza modificare i file
mvn spotless:check

# Applica automaticamente la formattazione del codice
mvn spotless:apply

# Controlla solo la formattazione Java
mvn spotless:java:check

# Applica la formattazione solo al codice Java
mvn spotless:java:apply

# Controlla solo la formattazione del POM
mvn spotless:pom:check

# Applica la formattazione solo al POM
mvn spotless:pom:apply
```

### Comandi Avanzati
```bash
# Esegui Spotless saltando la cache (utile per problemi di caching)
mvn spotless:check -Dspotless.cache.skip=true

# Mostra dettagli aggiuntivi durante l'esecuzione
mvn spotless:check -X

# Esegui Spotless su un profilo specifico
mvn spotless:apply -Pquality
```

## SpotBugs (Analisi Statica)

### Comandi Base
```bash
# Esegui l'analisi SpotBugs
mvn spotbugs:check

# Genera il report SpotBugs in formato HTML
mvn spotbugs:spotbugs

# Genera il report in formato XML
mvn spotbugs:spotbugs -Dspotbugs.xml.output=true
```

### Comandi Avanzati
```bash
# Esegui SpotBugs con soglia personalizzata
mvn spotbugs:check -Dspotbugs.effort=Max -Dspotbugs.threshold=Low

# Salta l'analisi se non ci sono cambiamenti
mvn spotbugs:check -Dspotbugs.skip=false

# Genera report con inclusione di filtri personalizzati
mvn spotbugs:spotbugs -Dspotbugs.includeFilterFile=custom-include.xml

# Esegui solo su moduli specifici (in progetti multi-modulo)
mvn spotbugs:check -pl module-name
```

## Checkstyle (Analisi dello Stile del Codice)

### Comandi Base
```bash
# Esegui il controllo Checkstyle
mvn checkstyle:check

# Genera il report Checkstyle
mvn checkstyle:checkstyle

# Usa un file di configurazione personalizzato
mvn checkstyle:check -Dcheckstyle.config.location=my-checkstyle.xml
```

### Comandi Avanzati
```bash
# Genera report in formato XML
mvn checkstyle:checkstyle -Dcheckstyle.output.format=xml

# Genera report in formato HTML
mvn checkstyle:checkstyle -Dcheckstyle.output.format=html

# Salta il controllo Checkstyle
mvn checkstyle:check -Dcheckstyle.skip=true

# Esegui con inclusione/esclusione di file specifici
mvn checkstyle:check -Dcheckstyle.includes=**/core/**/*.java
mvn checkstyle:check -Dcheckstyle.excludes=**/test/**/*.java
```

## Comandi Combinati per il Workflow Sviluppo

### Prima del Commit
```bash
# 1. Applica la formattazione del codice
mvn spotless:apply

# 2. Esegui i controlli di qualità
mvn clean compile spotless:check spotbugs:check checkstyle:check
```

### Per Build Complete
```bash
# Build completa con tutti i controlli di qualità
mvn clean verify spotbugs:spotbugs checkstyle:checkstyle

# Build con profilo quality (se configurato)
mvn clean verify -Pquality
```

### Per CI/CD
```bash
# Esegui tutti i controlli senza modificare i file
mvn clean compile spotless:check spotbugs:check checkstyle:check

# Genera tutti i report per la documentazione
mvn clean site spotbugs:spotbugs checkstyle:checkstyle
```

## Comandi per Debug e Risoluzione Problemi

### Spotless
```bash
# Mostra i file che verrebbero modificati
mvn spotless:check -Dspotless.diff=console

# Pulisci la cache di Spotless
mvn spotless:apply -Dspotless.cache.skip=true
```

### SpotBugs
```bash
# Mostra configurazione corrente
mvn spotbugs:help

# Esegui con logging dettagliato
mvn spotbugs:check -Dspotbugs.debug=true
```

### Checkstyle
```bash
# Mostra configurazione corrente
mvn checkstyle:help

# Verifica la sintassi del file di configurazione
mvn checkstyle:check -Dcheckstyle.verify=true
```

## Shortcut per Sviluppo Rapido

### Formattazione Rapida
```bash
# Formatta tutto il progetto
mvn spotless:apply
```

### Controllo Rapido
```bash
# Controlla solo la formattazione e la compilazione
mvn clean compile spotless:check
```

### Analisi Rapida
```bash
# Esegui solo SpotBugs (l'analisi più veloce)
mvn spotbugs:check
```

## Integrazione con IDE

### IntelliJ IDEA
```bash
# Configura Spotless come build step
# File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner
# Aggiungi: spotless:apply
```

### VS Code
```bash
# Configura task nel file .vscode/tasks.json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Format Code",
      "type": "shell",
      "command": "mvn spotless:apply"
    },
    {
      "label": "Check Quality",
      "type": "shell", 
      "command": "mvn spotless:check spotbugs:check checkstyle:check"
    }
  ]
}
```

## Note Importanti

1. **Ordine di esecuzione consigliato**: Spotless → Checkstyle → SpotBugs
2. **Spotless** modifica i file, mentre gli altri strumenti sono solo di analisi
3. **SpotBugs** richiede che il codice sia compilato prima di essere eseguito
4. **Checkstyle** può essere eseguito anche su codice non compilato
5. Tutti gli strumenti possono essere configurati nel `pom.xml` per eseguirsi automaticamente durante la build