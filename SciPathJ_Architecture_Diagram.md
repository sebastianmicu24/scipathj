# SciPathJ Complete Architecture Diagram

## System Overview

```mermaid
graph TB
    %% User Interface Layer
    subgraph "User Interface Layer"
        UI[Main Application Window]
        Config[Pipeline Configuration]
        Progress[Progress Monitor]
        Results[Results Viewer]
        Settings[Settings Dialog]
    end

    %% Core Engine Layer
    subgraph "Core Engine Layer"
        Engine[SciPathJ Engine]
        Pipeline[Pipeline Executor]
        Resource[Resource Manager]
        Event[Event System]
    end

    %% Processing Modules
    subgraph "Image Processing Layer"
        subgraph "Segmentation Module"
            Nuclear[Nuclear Segmentation<br/>StarDist Integration]
            Cyto[Cytoplasm Segmentation<br/>Voronoi Tessellation]
            Background[Background Segmentation<br/>Vessel Detection]
        end
        
        subgraph "Feature Extraction"
            Morpho[Morphological Features<br/>17 shape metrics]
            Intensity[Intensity Features<br/>16 per channel]
            Spatial[Spatial Features<br/>5 distance metrics]
        end
        
        subgraph "Classification"
            XGBoost[XGBoost Classifier]
            Models[Model Management]
            Training[Training Workflows]
        end
    end

    %% Data Layer
    subgraph "Data Management Layer"
        subgraph "Data Models"
            Cell[Cell Objects]
            Features[Feature Vectors]
            ROI[ROI Management]
            Results_Data[Classification Results]
        end
        
        subgraph "Storage & Export"
            CSV[CSV Export<br/>US/EU Formats]
            ROI_Export[ROI Export]
            Cache[Data Caching]
            Config_Store[Configuration Storage]
        end
    end

    %% External Integrations
    subgraph "External Systems"
        ImageJ[ImageJ Ecosystem]
        StarDist_Model[StarDist Models]
        XGBoost_Lib[XGBoost4J Library]
        File_System[File System]
    end

    %% Plugin System
    subgraph "Plugin Architecture"
        Plugin_API[Plugin API]
        Plugin_Loader[Plugin Loader]
        Plugin_Registry[Plugin Registry]
        Custom_Plugins[Custom Plugins]
    end

    %% Connections
    UI --> Engine
    Config --> Pipeline
    Progress --> Event
    Results --> Results_Data
    Settings --> Config_Store

    Engine --> Pipeline
    Engine --> Resource
    Engine --> Event

    Pipeline --> Nuclear
    Pipeline --> Cyto
    Pipeline --> Background
    Pipeline --> Morpho
    Pipeline --> Intensity
    Pipeline --> Spatial
    Pipeline --> XGBoost

    Nuclear --> Cell
    Cyto --> Cell
    Background --> ROI
    
    Morpho --> Features
    Intensity --> Features
    Spatial --> Features
    
    XGBoost --> Results_Data
    Models --> XGBoost_Lib
    Training --> XGBoost_Lib

    Cell --> CSV
    ROI --> ROI_Export
    Features --> Cache
    Results_Data --> CSV

    Nuclear --> StarDist_Model
    XGBoost --> XGBoost_Lib
    Background --> ImageJ
    
    Plugin_API --> Plugin_Loader
    Plugin_Loader --> Plugin_Registry
    Plugin_Registry --> Custom_Plugins
    Custom_Plugins --> Pipeline
```

## Detailed Package Structure

```mermaid
graph LR
    subgraph "com.scipath.scipathj"
        subgraph "core"
            core_engine[engine/]
            core_pipeline[pipeline/]
            core_config[config/]
            core_events[events/]
        end
        
        subgraph "ui"
            ui_main[main/]
            ui_components[components/]
            ui_dialogs[dialogs/]
            ui_themes[themes/]
        end
        
        subgraph "segmentation"
            seg_nucleus[nucleus/]
            seg_cytoplasm[cytoplasm/]
            seg_background[background/]
            seg_common[common/]
        end
        
        subgraph "features"
            feat_morphological[morphological/]
            feat_intensity[intensity/]
            feat_spatial[spatial/]
            feat_extraction[extraction/]
        end
        
        subgraph "classification"
            class_xgboost[xgboost/]
            class_models[models/]
            class_training[training/]
            class_prediction[prediction/]
        end
        
        subgraph "data"
            data_model[model/]
            data_io[io/]
            data_export[export/]
            data_storage[storage/]
        end
        
        subgraph "imaging"
            img_processing[processing/]
            img_tiling[tiling/]
            img_deconvolution[deconvolution/]
            img_roi[roi/]
        end
        
        subgraph "plugins"
            plugin_api[api/]
            plugin_loader[loader/]
            plugin_registry[registry/]
        end
    end
```

## Processing Pipeline Flow

```mermaid
flowchart TD
    Start([User Starts Analysis]) --> SelectPipeline[Select Pipeline<br/>Liver H&E]
    SelectPipeline --> SelectFolder[Select Image Folder]
    SelectFolder --> LoadImages[Load Images<br/>Batch Processing]
    
    LoadImages --> ProcessImage{For Each Image}
    
    ProcessImage --> Deconvolution[Color Deconvolution<br/>H&E Separation]
    Deconvolution --> VesselSeg[Vessel Segmentation<br/>Background Detection]
    VesselSeg --> NuclearSeg[Nuclear Segmentation<br/>StarDist 2D]
    NuclearSeg --> CytoSeg[Cytoplasm Segmentation<br/>Voronoi Tessellation]
    
    CytoSeg --> CellCreation[Cell Object Creation<br/>Nucleus + Cytoplasm]
    CellCreation --> FeatureExtraction[Feature Extraction<br/>50+ Features per Cell]
    
    FeatureExtraction --> MorphoFeatures[Morphological Features<br/>Area, Perimeter, Shape]
    FeatureExtraction --> IntensityFeatures[Intensity Features<br/>H&E Channel Statistics]
    FeatureExtraction --> SpatialFeatures[Spatial Features<br/>Vessel Distance, Neighbors]
    
    MorphoFeatures --> FeatureVector[Combine Feature Vector]
    IntensityFeatures --> FeatureVector
    SpatialFeatures --> FeatureVector
    
    FeatureVector --> Classification[XGBoost Classification<br/>Cell Type Prediction]
    Classification --> CellResults[Cell Classification Results]
    
    CellResults --> MoreImages{More Images?}
    MoreImages -->|Yes| ProcessImage
    MoreImages -->|No| AggregateResults[Aggregate Results<br/>Calculate Averages]
    
    AggregateResults --> ExportData[Export Data]
    ExportData --> IndividualCSV[Individual Cell Data<br/>CSV Export]
    ExportData --> AveragesCSV[Cell Type Averages<br/>CSV Export]
    ExportData --> ROIExport[ROI Export<br/>ZIP Files]
    
    IndividualCSV --> Complete([Analysis Complete])
    AveragesCSV --> Complete
    ROIExport --> Complete
```

## Data Flow Architecture

```mermaid
graph TD
    subgraph "Input Layer"
        ImageFolder[Image Folder]
        ConfigFile[Configuration Files]
        ModelFiles[ML Model Files]
    end
    
    subgraph "Processing Core"
        ImageLoader[Image Loader]
        ConfigManager[Configuration Manager]
        PipelineEngine[Pipeline Engine]
        
        subgraph "Analysis Pipeline"
            Segmentation[Segmentation Engine]
            FeatureEngine[Feature Extraction Engine]
            ClassificationEngine[Classification Engine]
        end
        
        subgraph "Data Management"
            CellDatabase[Cell Data Store]
            FeatureCache[Feature Cache]
            ResultsAggregator[Results Aggregator]
        end
    end
    
    subgraph "Output Layer"
        CSVExporter[CSV Exporter]
        ROIExporter[ROI Exporter]
        ReportGenerator[Report Generator]
        
        subgraph "Output Files"
            IndividualData[individual_data.csv]
            AverageData[averages.csv]
            ROIFiles[roi_sets.zip]
            ProcessingLog[processing.log]
        end
    end
    
    %% Data Flow
    ImageFolder --> ImageLoader
    ConfigFile --> ConfigManager
    ModelFiles --> ClassificationEngine
    
    ImageLoader --> PipelineEngine
    ConfigManager --> PipelineEngine
    
    PipelineEngine --> Segmentation
    Segmentation --> FeatureEngine
    FeatureEngine --> ClassificationEngine
    
    Segmentation --> CellDatabase
    FeatureEngine --> FeatureCache
    ClassificationEngine --> ResultsAggregator
    
    CellDatabase --> CSVExporter
    FeatureCache --> CSVExporter
    ResultsAggregator --> CSVExporter
    ResultsAggregator --> ReportGenerator
    
    CellDatabase --> ROIExporter
    
    CSVExporter --> IndividualData
    CSVExporter --> AverageData
    ROIExporter --> ROIFiles
    ReportGenerator --> ProcessingLog
```

## Class Relationship Diagram

```mermaid
classDiagram
    class SciPathJEngine {
        -PipelineExecutor executor
        -ResourceManager resources
        -EventSystem events
        +processImages(List~String~ paths)
        +getProgress() ProgressInfo
        +shutdown()
    }
    
    class Pipeline {
        <<interface>>
        +getName() String
        +getSteps() List~PipelineStep~
        +execute(ImagePlus image) PipelineResult
    }
    
    class LiverHEPipeline {
        -SegmentationConfig segConfig
        -ClassificationConfig classConfig
        +execute(ImagePlus image) PipelineResult
    }
    
    class Cell {
        -String id
        -NucleusROI nucleus
        -CytoplasmROI cytoplasm
        -FeatureVector features
        -ClassificationResult classification
        +getArea() double
        +getFeature(String name) double
    }
    
    class FeatureVector {
        -Map~String,Double~ features
        -FeatureMetadata metadata
        +getFeature(String name) double
        +setFeature(String name, double value)
        +size() int
    }
    
    class NuclearSegmenter {
        -StarDistConfig config
        -ImageProcessor processor
        +segment(ImagePlus image) List~NucleusROI~
        -filterBySize(List~NucleusROI~ rois) List~NucleusROI~
    }
    
    class CytoplasmSegmenter {
        -VoronoiConfig config
        +createCells(ImagePlus image, List~NucleusROI~ nuclei) List~Cell~
        -performVoronoiTessellation(ImagePlus image) ImagePlus
    }
    
    class FeatureExtractor {
        -List~FeatureCalculator~ calculators
        +extractFeatures(Cell cell, ImageContext context) FeatureVector
        +getAvailableFeatures() List~String~
    }
    
    class XGBoostClassifier {
        -Booster model
        -FeatureSelector selector
        +classify(FeatureVector features) ClassificationResult
        +loadModel(String path)
    }
    
    class CSVExporter {
        -CSVFormat format
        -ExportConfiguration config
        +exportIndividualCells(List~Cell~ cells, File output)
        +exportAverages(Map~String,CellGroup~ groups, File output)
    }
    
    %% Relationships
    SciPathJEngine --> Pipeline : uses
    Pipeline <|-- LiverHEPipeline : implements
    LiverHEPipeline --> NuclearSegmenter : uses
    LiverHEPipeline --> CytoplasmSegmenter : uses
    LiverHEPipeline --> FeatureExtractor : uses
    LiverHEPipeline --> XGBoostClassifier : uses
    
    NuclearSegmenter --> Cell : creates
    CytoplasmSegmenter --> Cell : modifies
    FeatureExtractor --> FeatureVector : creates
    XGBoostClassifier --> ClassificationResult : creates
    
    Cell --> FeatureVector : contains
    Cell --> ClassificationResult : contains
    
    CSVExporter --> Cell : exports
```

## Technology Stack Integration

```mermaid
graph TB
    subgraph "Application Layer"
        SciPathJ[SciPathJ Application]
    end
    
    subgraph "UI Framework"
        Swing[Java Swing]
        FlatLaf[FlatLaf Look & Feel]
        Ikonli[Ikonli Icons]
    end
    
    subgraph "Image Processing"
        ImageJ[ImageJ2 Core]
        SCIFIO[SCIFIO I/O]
        ImageJOps[ImageJ Ops]
        StarDist[StarDist Plugin]
    end
    
    subgraph "Machine Learning"
        XGBoost4J[XGBoost4J Library]
        Models[Pre-trained Models]
    end
    
    subgraph "Data Processing"
        Jackson[Jackson JSON]
        ApacheCommons[Apache Commons]
        SLF4J[SLF4J Logging]
        Logback[Logback Implementation]
    end
    
    subgraph "Build & Test"
        Maven[Maven Build System]
        JUnit5[JUnit 5 Testing]
        Mockito[Mockito Mocking]
        SpotBugs[SpotBugs Analysis]
    end
    
    subgraph "Runtime Environment"
        Java23[Java 23 Runtime]
        JVM[JVM with Preview Features]
    end
    
    %% Connections
    SciPathJ --> Swing
    SciPathJ --> ImageJ
    SciPathJ --> XGBoost4J
    SciPathJ --> Jackson
    SciPathJ --> SLF4J
    
    Swing --> FlatLaf
    Swing --> Ikonli
    
    ImageJ --> SCIFIO
    ImageJ --> ImageJOps
    ImageJ --> StarDist
    
    XGBoost4J --> Models
    
    SLF4J --> Logback
    
    Maven --> JUnit5
    Maven --> Mockito
    Maven --> SpotBugs
    
    SciPathJ --> Java23
    Java23 --> JVM
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Development Environment"
        IDE[IntelliJ IDEA]
        Git[Git Repository]
        LocalBuild[Local Maven Build]
    end
    
    subgraph "Build Pipeline"
        CI[Continuous Integration]
        Tests[Automated Testing]
        QualityGate[Quality Gate]
        Artifacts[Build Artifacts]
    end
    
    subgraph "Distribution"
        FatJAR[Fat JAR Distribution]
        Installer[JPackage Installer]
        Portable[Portable Version]
    end
    
    subgraph "Target Systems"
        Windows[Windows 10/11]
        Linux[Linux Distributions]
        MacOS[macOS]
    end
    
    subgraph "Runtime Dependencies"
        JavaRuntime[Java 23 Runtime]
        NativeLibs[Native Libraries]
        ModelFiles[ML Model Files]
        ConfigFiles[Configuration Files]
    end
    
    %% Flow
    IDE --> Git
    Git --> CI
    CI --> Tests
    Tests --> QualityGate
    QualityGate --> Artifacts
    
    Artifacts --> FatJAR
    Artifacts --> Installer
    Artifacts --> Portable
    
    FatJAR --> Windows
    FatJAR --> Linux
    FatJAR --> MacOS
    
    Installer --> Windows
    Installer --> Linux
    Installer --> MacOS
    
    Windows --> JavaRuntime
    Linux --> JavaRuntime
    MacOS --> JavaRuntime
    
    JavaRuntime --> NativeLibs
    JavaRuntime --> ModelFiles
    JavaRuntime --> ConfigFiles
```

This comprehensive architecture diagram shows:

1. **System Overview**: High-level component relationships
2. **Package Structure**: Detailed code organization
3. **Processing Pipeline**: Step-by-step workflow
4. **Data Flow**: How information moves through the system
5. **Class Relationships**: Object-oriented design structure
6. **Technology Integration**: External dependencies and frameworks
7. **Deployment Architecture**: Build and distribution strategy

The diagrams provide a complete visual representation of the SciPathJ architecture, from user interface down to the underlying technology stack and deployment strategy.