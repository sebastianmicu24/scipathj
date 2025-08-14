@echo off
REM Script per analizzare un singolo file Java con tutti e tre gli strumenti di qualità del codice
REM Uso: analyze_single_file.bat <percorso\file.java>

if "%~1"=="" (
    echo Uso: %0 ^<percorso\file.java^>
    echo Esempio: %0 src\main\java\com\scipath\scipathj\SciPathJApplication.java
    exit /b 1
)

set FILE_PATH=%~1
set FILE_NAME=%~nx1

if not exist "%FILE_PATH%" (
    echo Errore: Il file %FILE_PATH% non esiste
    exit /b 1
)

echo ==========================================================
echo ANALISI COMPLETA DEL FILE: %FILE_NAME%
echo ==========================================================
echo.

REM 1. Spotless - Formattazione e controllo stile
echo 1. SPOTLESS - Formattazione e controllo stile
echo -------------------------------------------
echo Controllo formattazione...
call mvn spotless:check -Dspotless.includes="%FILE_PATH%"

echo.
echo Applicazione formattazione (se necessario)...
call mvn spotless:apply -Dspotless.includes="%FILE_PATH%"
echo.

REM 2. Checkstyle - Analisi stile codice
echo 2. CHECKSTYLE - Analisi stile codice
echo ------------------------------------
echo Esecuzione analisi Checkstyle...
call mvn checkstyle:check -Dcheckstyle.includes="%FILE_PATH%"
echo.

REM 3. SpotBugs - Analisi statica bug
echo 3. SPOTBUGS - Analisi statica bug
echo ----------------------------------
echo Compilazione del progetto (necessaria per SpotBugs)...
call mvn clean compile -q

echo Esecuzione analisi SpotBugs...
call mvn spotbugs:check -Dspotbugs.includeFilterFile=spotbugs-single-file.xml
echo.
echo Creazione filtro per il singolo file...
echo ^<?xml version="1.0" encoding="UTF-8"?^> > spotbugs-single-file.xml
echo ^<FindBugsFilter^> >> spotbugs-single-file.xml
echo   ^<Match^> >> spotbugs-single-file.xml
echo     ^<Class name="~.*\.%~nx1"/^> >> spotbugs-single-file.xml
echo   ^</Match^> >> spotbugs-single-file.xml
echo ^</FindBugsFilter^> >> spotbugs-single-file.xml
call mvn spotbugs:check -Dspotbugs.includeFilterFile=spotbugs-single-file.xml
del spotbugs-single-file.xml
echo.

REM 4. Report combinato
echo ==========================================================
echo RIEPILOGO ANALISI PER: %FILE_NAME%
echo ==========================================================
echo.

REM Controlla se ci sono problemi con Spotless
echo ✅ SPOTLESS:
call mvn spotless:check -Dspotless.includes="%FILE_PATH%" -q >nul 2>&1
if %errorlevel% equ 0 (
    echo    ✓ Formattazione corretta
) else (
    echo    ⚠️  Problemi di formattazione rilevati
    echo    Esegui: mvn spotless:apply -Dspotless.includes="%FILE_PATH%"
)

echo.

REM Controlla se ci sono problemi con Checkstyle
echo ✅ CHECKSTYLE:
call mvn checkstyle:check -Dcheckstyle.includes="%FILE_PATH%" -q >nul 2>&1
if %errorlevel% equ 0 (
    echo    ✓ Nessuna violazione dello stile rilevata
) else (
    echo    ⚠️  Violazioni dello stile rilevate
    echo    Controlla l'output sopra per i dettagli
)

echo.

REM Controlla se ci sono problemi con SpotBugs
echo ✅ SPOTBUGS:
call mvn spotbugs:check -Dspotbugs.includes="%FILE_PATH%" -q >nul 2>&1
if %errorlevel% equ 0 (
    echo    ✓ Nessun bug rilevato
) else (
    echo    ⚠️  Bug potenziali rilevati
    echo    Controlla l'output sopra per i dettagli
    echo    Per report dettagliato: mvn spotbugs:spotbugs -Dspotbugs.includes="%FILE_PATH%"
)

echo.
echo ==========================================================
echo ANALISI COMPLETATA
echo ==========================================================
echo.
echo Comandi utili per approfondire:
echo   Formattazione:          mvn spotless:apply -Dspotless.includes="%FILE_PATH%"
echo   Report Checkstyle:      mvn checkstyle:checkstyle -Dcheckstyle.includes="%FILE_PATH%"
echo   Report SpotBugs:        mvn spotbugs:spotbugs -Dspotbugs.includes="%FILE_PATH%"
echo   Tutti i report:         mvn clean site -Dcheckstyle.includes="%FILE_PATH%" -Dspotbugs.includes="%FILE_PATH%"