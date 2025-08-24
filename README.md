# SciPathJ

**Segmentation and Classification of Images, Pipelines for the Analysis of Tissue Histopathology**

![SciPathJ Logo](src/main/resources/icon.png)

SciPathJ is a modern, professional-grade Java software for histopathological image analysis, designed to provide automated segmentation and classification of histological images. The project builds upon proven methodologies while incorporating a modern user interface and an extensible, SOLID-compliant architecture ready for production environments.

## Table of Contents

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Usage](#usage)
- [Main Functions](#main-functions)
- [ROI Management](#roi-management)
- [StarDist Integration](#stardist-integration)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Contact](#contact)

## Project Overview

SciPathJ is a professional desktop application for histopathological image analysis that combines advanced image processing algorithms with an intuitive user interface. The software is designed for researchers and professionals in the digital pathology field who need automated, reliable, and extensible tools for tissue analysis.

### Project Vision

- **Automated Analysis**: Highly automated software for histopathological analysis.
- **Batch Processing**: Process entire folders of images with comprehensive results.
- **Modern Interface**: A clean and intuitive user interface with a professional design.
- **Extensibility**: A plugin-ready architecture built on SOLID principles.

## Key Features

### 🖼️ Advanced Image Management
- Support for common formats: JPG, PNG, GIF, BMP, TIFF
- Support for scientific formats: LSM, CZI, ND2, OIB, OIF, VSI
- Support for microscopy formats: IMS, LIF, SCN, SVS, NDPI
- Thumbnail gallery for efficient navigation
- Main image viewer with metadata display

### 🔍 Intelligent Segmentation
- **Nuclear Segmentation**: Integration with StarDist for state-of-the-art nucleus detection.
- **Vascular Segmentation**: Thresholding algorithms for vessel detection.
- **Cytoplasm Segmentation**: Advanced Voronoi tessellation using nucleus center points as seeds for accurate cell boundary detection, including proper handling of touching nuclei with perpendicular bisector separation.
- **H&E Color Deconvolution**: Separates H&E stained images into Hematoxylin, Eosin, and Background channels using Ruifrok & Johnston method.
- Configurable parameters for different tissue types.
- Automatic image pre-processing.

### 🎯 ROI (Region of Interest) System
- Interactive ROI creation: Square, Rectangle, Circle.
- Multi-image management with automatic association.
- Export to ImageJ-compatible formats (.roi, .zip).
- Import from existing ROI files.
- Overlay display on images.

### 🎨 Modern User Interface
- Light/Dark themes with FlatLaf.
- Professional FontAwesome icons via Ikonli.
- Responsive design and smooth transitions.
- Intuitive tab-based navigation.
- Status bar with real-time feedback.

### ⚙️ Extendable Pipelines
- Modular, SOLID-compliant pipeline architecture.
- Step-by-step configuration with clear settings dialogs.
- Support for adding new algorithms and processing steps.
- Batch execution with progress monitoring.

## System Architecture

The architecture of SciPathJ is built on modern software engineering principles, emphasizing modularity, testability, and maintainability. The system avoids Singleton patterns in favor of a centralized **Application Context** that manages object lifecycles and handles **Dependency Injection (DI)**.

### Advanced Voronoi Tessellation Implementation

SciPathJ features an advanced Voronoi tessellation implementation specifically designed for accurate cytoplasm segmentation in histopathological images:

- **Point-Based Seeds**: Uses individual nucleus center coordinates as Voronoi seeds rather than filled regions, ensuring each nucleus gets its own distinct seed point.
- **Touching Nuclei Handling**: Properly separates touching nuclei with perpendicular bisector boundaries, eliminating overlapping ROI regions.
- **Performance Optimized**: Streamlined algorithm that processes 1992+ nuclei efficiently without complex distance calculations.
- **Mathematically Correct**: Implements the fundamental Voronoi principle where each seed point generates its own Voronoi cell with proper geometric boundaries.

This approach ensures accurate cell segmentation even in dense tissue regions where nuclei frequently touch or overlap, providing reliable cytoplasm ROI generation for downstream analysis.

### Project Structure

```
com.scipath.scipathj/
├── analysis/
│   ├── algorithms/     # Analysis algorithms (classification, segmentation, statistics)
│   │   ├── classification/  # Feature extraction and cell classification
│   │   ├── segmentation/    # Nuclear, vascular, cytoplasm segmentation
│   │   └── statistics/      # Statistical analysis tools
│   ├── config/         # Analysis-specific configuration (feature extraction, segmentation settings)
│   └── pipeline/       # Analysis pipeline orchestration
├── core/
│   ├── analysis/       # Core analysis utilities (H&E deconvolution, etc.)
│   ├── bootstrap/      # Application startup and context management
│   ├── config/         # Configuration records and constants
│   ├── engine/         # Core processing engine
│   ├── events/         # Event handling system
│   ├── pipeline/       # Pipeline system components
│   └── utils/          # Core utility classes
├── infrastructure/
│   ├── bootstrap/      # Application bootstrap services (context, theme, system config)
│   ├── config/         # Infrastructure configuration (main settings, configuration manager)
│   ├── engine/         # Infrastructure engine components (TensorFlow wrapper, resource manager)
│   ├── events/         # Event bus system
│   ├── pipeline/       # Pipeline infrastructure (executor, validation, etc.)
│   └── utils/          # Infrastructure utilities (logging, system output capture)
├── roi/
│   ├── model/          # ROI data models (UserROI, CellROI, etc.)
│   └── operations/     # ROI manipulation operations
├── ui/
│   ├── main/           # Main application window and controllers
│   ├── analysis/       # Analysis UI components
│   ├── common/         # Common UI components (image viewer, ROI overlay, etc.)
│   ├── controllers/    # UI controllers and state management
│   ├── dataset/        # Dataset creation UI
│   ├── model/          # UI data models
│   ├── themes/         # Theme management
│   ├── utils/          # UI utilities
│   └── visualization/  # Results visualization components
└── SciPathJApplication.java # Main application entry point
```

### Core Components

#### Core Engine & Configuration
- **ApplicationContext**: Manages the lifecycle of all major components. It instantiates services like `ConfigurationManager` and `SciPathJEngine` and injects them where needed.
- **SciPathJEngine**: Central coordinator for processing tasks. It receives analysis requests from the UI and delegates them to the appropriate pipelines.
- **ConfigurationManager**: Handles the persistence (loading and saving) of application settings. It produces immutable configuration objects (Java Records).
- **Immutable Settings Records**: Classes like `VesselSegmentationSettings`, `NuclearSegmentationSettings`, and `MainSettings` are implemented as immutable Java Records to ensure thread safety and predictability.

#### UI System
- **MainWindow**: The main application Frame, which orchestrates all UI panels.
- **MainMenuPanel**: New streamlined entry point with three main options (Perform Analysis, Create Dataset, Visualize Results).
- **NavigationController**: Manages transitions between different application workflows and panels.
- **DatasetCreationPanel**: Comprehensive interface for dataset creation with ROI loading, class management, and file selection.
- **FolderSelectionPanel**: Enhanced file selection component supporting both single files and folders with drag-and-drop.
- **UI Panels** (`PipelineRecapPanel`, `ImageGallery`, `StatusPanel`, etc.): Self-contained Swing components responsible for displaying information. They receive dependencies, like configuration objects, via their constructors.

#### ROI Management
- **ROIManager**: Centralized service for managing ROIs across all images.
- **ROIOverlay**: Renders ROIs on top of the `MainImageViewer`.
- **ROIToolbar**: Provides tools for creating and deleting ROIs.

## Technologies Used

### Core Java
- **Java 23**: Latest version with modern features like Records, Sealed Classes, and Pattern Matching.
- **Maven**: Dependency management and build automation.
- **SLF4J + Logback**: Professional logging framework.

### Image Processing
- **ImageJ**: A comprehensive ecosystem for scientific image processing.
- **ImgLib2**: Core library for n-dimensional image representation.
- **CSBDeep**: Deep learning framework for microscopy.
- **StarDist**: State-of-the-art deep learning model for nucleus detection.

### Machine Learning
- **XGBoost4J**: Machine learning algorithms for classification tasks.
- **TensorFlow**: Backend for running neural networks (used by StarDist).

### User Interface
- **Java Swing**: The core framework for the desktop UI.
- **FlatLaf**: A modern, clean Look and Feel for Swing applications.
- **Ikonli**: Library for using high-quality icon fonts like FontAwesome.

### Data
- **Jackson**: High-performance JSON processing for configuration and data export.
- **Apache Commons**: A suite of utilities for common developer tasks.

## Installation

### Prerequisites
- Java 23 or later
- Maven 3.6 or later
- 4GB of RAM recommended
- 500MB of disk space

### Build from Source

1.  Clone the repository:
    ```bash
    git clone https://github.com/sebastianmicu24/scipathj.git
    cd scipathj
    ```

2.  Compile the project:
    ```bash
    mvn clean compile
    ```

3.  Run the application:
    ```bash
    mvn exec:java
    ```

### Creating an Executable

To create a standalone executable JAR file:
```bash
mvn clean package
```
The executable will be located at `target/scipathj-1.0.0.jar`.

## Usage

### Starting the Application

1.  Launch SciPathJ:
    ```bash
    java -jar target/scipathj-1.0.0.jar
    ```
2.  Choose from three main options on the main menu:
   - **Perform Analysis**: Run segmentation and classification on tissue images
   - **Create Dataset**: Select cells and create custom classification models
   - **Visualize Results**: View and analyze previously processed data

### Main Workflow

1.  **Option Selection**: Choose one of the three main functions from the main menu:
    - **Perform Analysis**: Run comprehensive segmentation and classification
    - **Create Dataset**: Build custom datasets with ROI management
    - **Visualize Results**: View and analyze processed data

2.  **Perform Analysis Workflow**:
    - Select a folder containing images to be analyzed
    - Browse the thumbnail gallery and select images of interest
    - Configure analysis parameters (nuclear, vascular, cytoplasm segmentation)
    - Monitor progress with real-time status updates
    - View results with interactive ROI overlays
    - Export results and ROI data

3.  **Create Dataset Workflow**:
    - Select ROI ZIP files for loading existing annotations
    - Choose image folders for processing
    - Create and manage classification classes
    - Process large datasets (5,000+ ROIs supported)
    - Export structured datasets for machine learning

4.  **Visualize Results Workflow**:
    - Load previously processed analysis results
    - Browse images with thumbnail gallery
    - View interactive ROI overlays
    - Access statistical analysis tools
    - Export visualizations and reports

## Main Functions

SciPathJ provides three main functions accessible from the main menu:

### 1. Perform Analysis
**Status: Available**

Run comprehensive segmentation and classification on tissue images using advanced algorithms:

- **Nuclear Segmentation**: StarDist deep learning for accurate nucleus detection
- **Vascular Segmentation**: Adaptive thresholding for blood vessel identification
- **Cytoplasm Segmentation**: Advanced Voronoi tessellation using nucleus center points as seeds
- **Feature Extraction**: Morphological analysis of segmented regions
- **Tissue Classification**: Automated classification based on extracted features

**Configuration Options**:
- StarDist model selection (e.g., "Versatile (H&E)")
- Probability and NMS thresholds for nucleus detection
- Input normalization and percentile adjustments
- Tiling parameters for large images
- Vascular segmentation thresholds and morphological operations

### 2. Create Dataset
**Status: Available**

Interactive tools for creating custom classification datasets:

- **ZIP File Support**: Load ROIs from nested ZIP files with automatic filename matching
- **Flexible File Selection**: Choose between single files or entire folders for image processing
- **Class Management**: Create and manage classification classes with intuitive UI
- **ROI Processing**: Handle large ROI sets (5,000+ ROIs) with robust error handling
- **Image-ROI Association**: Automatic matching of ROI files to corresponding images
- **Dataset Export**: Export processed datasets in standard formats for machine learning

### 3. Visualize Results
**Status: Available**

Tools for analyzing and visualizing previously processed data:

- **Interactive ROI Display**: View and interact with ROI overlays on images
- **Results Management**: Load and display analysis results with comprehensive metadata
- **Statistical Analysis**: Built-in statistical tools for data analysis
- **Export Capabilities**: Generate reports and export visualizations
- **Image Gallery**: Browse processed images with thumbnail navigation

## ROI Management

### Creating ROIs

1.  Select a drawing tool from the toolbar (Square, Rectangle, Circle).
2.  Click and drag on the image to create the ROI.
3.  The ROI is automatically associated with the current image and managed by the `ROIManager`.

### Managing Existing ROIs
- **Save ROIs**: Export the ROIs of the current image.
- **Save All**: Export all ROIs from all images into a master ZIP file.
- **Delete All**: Remove all ROIs from the current image.

### Supported Formats
- **Single ROI**: `.roi` file (ImageJ compatible).
- **Multiple ROIs**: `.zip` file (a set of ImageJ ROIs).
- **Nested ZIP Support**: Handles ZIP files containing subdirectories with ROI files.
- **Large Dataset Processing**: Efficiently processes datasets with 5,000+ ROIs.
- **Flexible Naming**: Automatic filename matching with support for variations (spaces vs. underscores).

## StarDist Integration

SciPathJ integrates StarDist, a state-of-the-art algorithm for cell nucleus detection based on deep learning.

### Supported Models
- **Versatile (fluorescent)**: General model for fluorescent images.
- **Versatile (H&E)**: Specific model for H&E histopathological images.
- **DSB 2018**: Model trained on the DSB 2018 dataset.

### StarDist Configuration (Example)

Configuration is managed through immutable Java Records, ensuring settings are thread-safe and predictable.

```java
// Example of creating a configuration record
var nuclearSettings = new NuclearSegmentationSettings(
    "Versatile (H&E)", // modelChoice
    0.5,               // probThresh
    0.4,               // nmsThresh
    true,              // normalizeInput
    1.0,               // percentileBottom
    99.8,              // percentileTop
    1024,              // normTileSize
    "ROI Manager"      // outputType
);
```

### Automatic Pre-processing
- Conversion to 8-bit for compatibility.
- Percentile-based normalization.
- Management of RGB images with separate channels.
- Fallback to traditional methods in case of errors.

## H&E Color Deconvolution

SciPathJ includes a high-performance implementation of H&E color deconvolution based on the Ruifrok & Johnston method, identical to Fiji's Color Deconvolution plugin.

### Features
- **Ultra-fast Processing**: Optimized matrix operations with pre-computed inverse matrices
- **Standard Stain Vectors**: Uses identical vectors as Fiji for guaranteed compatibility
- **Three Channel Output**: Hematoxylin, Eosin, and Background channels
- **Direct Pixel Access**: Bypasses ImageJ overhead for maximum performance

### Algorithm
The deconvolution process follows these steps:
1. **RGB to Optical Density**: Convert RGB values to optical density using `OD = -log10(RGB/255.0)`
2. **Matrix Multiplication**: Apply the inverse stain matrix to separate stains
3. **Back to RGB**: Convert optical densities back to transmittance values

### Stain Matrix
```
Hematoxylin: [0.650, 0.704, 0.286] (Red, Green, Blue)
Eosin:       [0.072, 0.990, 0.105] (Red, Green, Blue)
Background:  [0.000, 0.000, 0.000] (computed as cross product)
```

### Usage Example
```java
// Create deconvolution instance
HEDeconvolution deconvolution = new HEDeconvolution(image);

// Perform deconvolution
deconvolution.performDeconvolution();

// Get individual channels
ImagePlus hematoxylin = deconvolution.getHematoxylinImage();
ImagePlus eosin = deconvolution.getEosinImage();
ImagePlus background = deconvolution.getBackgroundImage();
```

### Testing
Run the test to verify deconvolution works correctly:
```bash
mvn exec:java -Dexec.mainClass=com.scipath.scipathj.core.analysis.HEDeconvolutionTest
```

### Documentation
For detailed technical information, see [`HE_DECONVOLUTION.md`](HE_DECONVOLUTION.md).

## Development

The project adheres to professional Java development guidelines to ensure a high-quality, maintainable, and extensible codebase.

### Development Environment

- **Recommended IDE**: IntelliJ IDEA
- **Useful Plugins**:
  - Maven Integration
  - Git Integration
  - SonarLint / Checkstyle

### Architectural Principles

The codebase is built upon the **SOLID** principles:
- **(S)ingle Responsibility Principle**: Each class has a single, well-defined responsibility.
- **(O)pen/Closed Principle**: Software entities are open for extension, but closed for modification.
- **(L)iskov Substitution Principle**: Subtypes are substitutable for their base types.
- **(I)nterface Segregation Principle**: Clients are not forced to depend on interfaces they do not use.
- **(D)ependency Inversion Principle**: High-level modules do not depend on low-level modules; both depend on abstractions. Dependency Injection is used throughout the application.

### Coding Practices
- **Immutability**: Data-carrying classes are implemented as immutable Java Records where possible.
- **Comprehensive Documentation**: JavaDoc for all public classes and methods.
- **Robust Error Handling**: Specific, custom exceptions are used instead of generic ones.
- **Unit Testing**: JUnit 5 with Mockito for creating robust and isolated tests.

### Building and Testing

```bash
# Compile and run all tests
mvn clean test

# Run code quality analysis
mvn spotbugs:check pmd:check

# Generate test coverage report
mvn jacoco:report
```

## Contributing

We welcome contributions! Please follow these guidelines to get started.

### Contribution Guidelines

1.  Fork the repository.
2.  Create a feature branch: `git checkout -b feature/new-feature`
3.  Commit your changes: `git commit -m 'Add new feature'`
4.  Push to the branch: `git push origin feature/new-feature`
5.  Open a Pull Request.

### Reporting Bugs

Please use GitHub Issues to report bugs:
- Use a descriptive title.
- Provide steps to reproduce the issue.
- Explain the expected vs. observed behavior.
- Include a stack trace if available.
- Specify your Java and operating system versions.

## License

This project is distributed under the BSD 3-Clause License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **ImageJ Team**: For the outstanding image processing framework.
- **StarDist Team**: For the state-of-the-art nuclear segmentation algorithm.
- **CSBDeep Team**: For the deep learning framework for biological analysis.
- **FlatLaf Team**: For the modern Swing Look and Feels.

## Contact

- **Author**: Sebastian Micu
- **Email**: sebastian.micu@example.com
- **Repository**: https://github.com/sebastianmicu24/scipathj
- **Issues**: https://github.com/sebastianmicu24/scipathj/issues

---

**SciPathJ** - A professional tool for digital histopathological analysis.