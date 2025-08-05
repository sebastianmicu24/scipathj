# SciPathJ

**Segmentation and Classification of Images, Pipelines for the Analysis of Tissue Histopathology**

![SciPathJ Logo](src/main/resources/icon.png)

SciPathJ is a modern Java-based software for histopathological image analysis, designed to provide automated segmentation and classification of histological images. The project builds upon proven methodologies while incorporating a modern user interface and an extensible architecture.

## Table of Contents

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technologies Used](#technologies-used)
- [Installation](#installation)
- [Usage](#usage)
- [Analysis Pipelines](#analysis-pipelines)
- [ROI Management](#roi-management)
- [StarDist Integration](#stardist-integration)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Contact](#contact)

## Project Overview

SciPathJ is a professional desktop application for histopathological image analysis that combines advanced image processing algorithms with an intuitive user interface. The software is designed for researchers and professionals in the digital pathology field who need automated tools for tissue analysis.

### Project Vision

- **Automated Analysis**: Highly automated software for histopathological analysis.
- **Batch Processing**: Process entire folders of images with comprehensive results.
- **Modern Interface**: A clean and intuitive user interface with a professional design.
- **Extensibility**: A plugin-ready architecture for future enhancements.

## Key Features

### 🖼️ Advanced Image Management
- Support for common formats: JPG, PNG, GIF, BMP, TIFF
- Support for scientific formats: LSM, CZI, ND2, OIB, OIF, VSI
- Support for microscopy formats: IMS, LIF, SCN, SVS, NDPI
- Thumbnail gallery for efficient navigation
- Main image viewer with metadata display

### 🔍 Intelligent Segmentation
- **Nuclear Segmentation**: Integration with StarDist for nucleus detection.
- **Vascular Segmentation**: Thresholding algorithms for vessel detection.
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
- Professional FontAwesome icons.
- Responsive design and smooth transitions.
- Intuitive tab-based navigation.
- Status bar with real-time feedback.

### ⚙️ Extendable Pipelines
- Modular pipeline architecture.
- Step-by-step configuration.
- Support for adding new algorithms.
- Batch execution with progress monitoring.

## System Architecture

### Project Structure

```
com.scipath.scipathj/
├── core/                 # Main processing engine
│   ├── engine/          # Main processing coordinator
│   ├── pipeline/        # Pipeline management system
│   ├── config/          # Configuration management
│   └── events/          # Event system for UI updates
├── ui/                  # User Interface components
│   ├── main/           # Main application window
│   ├── components/     # Reusable UI components
│   ├── dialogs/        # Settings and dialogs
│   ├── themes/         # Theme management
│   ├── model/          # UI data models
│   └── utils/          # UI utilities
├── analysis/           # Analysis algorithms
├── data/               # Data models and management
│   └── model/          # Core data structures
└── SciPathJApplication.java # Main application class
```

### Core Components

#### Core Engine
- **SciPathJEngine**: Central processing coordinator.
- **ConfigurationManager**: Manages settings and preferences.
- **EventBus**: Event-based communication system.
- **Pipeline System**: Extendable pipeline architecture with interfaces.

#### UI System
- **MainWindow**: Main application interface with tabbed navigation.
- **PipelineSelectionPanel**: Interactive pipeline selection with visual cards.
- **PipelineRecapPanel**: Displays pipeline information.
- **FolderSelectionPanel**: Folder selection with drag-and-drop.
- **ImageGallery**: Vertical thumbnail gallery.
- **MainImageViewer**: Main image viewer.

#### ROI Management
- **ROIManager**: Centralized ROI management system.
- **ROIOverlay**: Overlay system for interactive drawing.
- **ROIToolbar**: Toolbar for ROI management.
- Full support for ImageJ formats (.roi, .zip).

## Technologies Used

### Core Java
- **Java 23**: Latest version with preview features enabled.
- **Maven**: Dependency management and build tool.
- **SLF4J + Logback**: Professional logging framework.

### Image Processing
- **ImageJ 2.9.0**: Comprehensive ecosystem for scientific image processing.
- **ImgLib2**: Library for multidimensional image processing.
- **CSBDeep 0.3.5-SNAPSHOT**: Deep learning for biological analysis.
- **StarDist**: Deep learning-based nucleus detection.

### Machine Learning
- **XGBoost4J 2.1.4**: Machine learning algorithms for classification.
- **TensorFlow 1.15.0**: Backend for neural networks.

### User Interface
- **FlatLaf 3.4.1**: Modern Look and Feel for Swing applications.
- **Ikonli 12.3.1**: Professional FontAwesome icons.
- **Swing**: Main Java UI framework.

### Data
- **Jackson 2.15.2**: JSON processing.
- **Apache Commons**: Various utilities.

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

To create an executable JAR file:
```bash
mvn clean package
```

The executable will be created at `target/scipathj-1.0.0.jar`.

## Usage

### Starting the Application

1.  Launch SciPathJ:
    ```bash
    java -jar target/scipathj-1.0.0.jar
    ```

2.  Select an analysis pipeline from the main screen.

3.  Choose a folder containing the images to be analyzed.

4.  Navigate the gallery and select the images of interest.

### Main Workflow

1.  **Pipeline Selection**
    - Choose from available pipelines.
    - View the planned analysis steps.
    - Configure specific parameters.

2.  **Image Selection**
    - Select a folder via drag-and-drop.
    - Browse the thumbnail gallery.
    - View images in the main viewer.

3.  **Analysis**
    - Start the analysis with the "Start" button.
    - Monitor progress in the status bar.
    - View the results upon completion.

4.  **ROI Management**
    - Manually create ROIs on images.
    - Use automatically generated ROIs.
    - Export ROIs for external analysis.

## Analysis Pipelines

### Available Pipelines

#### 1. H&E Liver Analysis
- Nuclear segmentation with StarDist.
- Vascular segmentation.
- Morphological feature extraction.
- Tissue classification.

#### 2. Nuclear Segmentation
- Nucleus detection with StarDist.
- Filtering by size and shape.
- Nuclear statistics.
- Export results.

#### 3. Vascular Analysis
- Blood vessel segmentation.
- Vascular density analysis.
- Morphological measurements.
- Results visualization.

### Pipeline Configuration

Each pipeline offers specific configurations:

#### Nuclear Segmentation Settings
- StarDist model selection.
- Probability and NMS thresholds.
- Input normalization.
- Tiling parameters.

#### Vascular Segmentation Settings
- Intensity threshold.
- Gaussian blur sigma.
- Morphological closing.
- Minimum/maximum sizes.

## ROI Management

### Creating ROIs

1.  Select a drawing tool from the toolbar:
    - Square
    - Rectangle
    - Circle

2.  Click and drag on the image to create the ROI.

3.  The ROI will be automatically associated with the current image.

### Managing Existing ROIs

- **Save ROIs**: Export the ROIs of the current image.
- **Save All**: Export all ROIs into a master ZIP file.
- **Delete All**: Remove all ROIs from the current image.

### Supported Formats

- **Single ROI**: .roi file (ImageJ compatible).
- **Multiple ROIs**: .zip file (set of ImageJ ROIs).
- **Master ZIP**: ZIP file organized by image.

## StarDist Integration

SciPathJ integrates StarDist, a state-of-the-art algorithm for cell nucleus detection based on deep learning.

### Supported Models

- **Versatile (fluorescent)**: General model for fluorescent images.
- **Versatile (H&E)**: Specific model for H&E histopathological images.
- **DSB 2018**: Model trained on the DSB 2018 dataset.
- **Tissue Net**: Model for various tissues.

### StarDist Configuration

```java
// Example configuration
NuclearSegmentationSettings settings = new NuclearSegmentationSettings();
settings.setModelChoice("Versatile (H&E)");
settings.setProbThresh(0.5);
settings.setNmsThresh(0.4);
settings.setNormalizeInput(true);
settings.setPercentileBottom(1.0);
settings.setPercentileTop(99.8);
```

### Automatic Pre-processing

- Conversion to 8-bit for compatibility.
- Percentile-based normalization.
- Management of RGB images with separate channels.
- Fallback to traditional methods in case of errors.

## Development

### Development Environment

1.  Recommended IDE: IntelliJ IDEA
2.  Useful Plugins:
    - Maven Integration
    - Git Integration
    - SonarLint

### Code Structure

The project follows professional Java development guidelines:

- **Single Responsibility Principle**: Each class has a single responsibility.
- **Comprehensive Documentation**: JavaDoc for all public classes and methods.
- **Error Handling**: Custom exceptions and detailed logging.
- **Unit Testing**: JUnit 5 with Mockito for mocking.

### Building and Testing

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Code analysis
mvn spotbugs:check
mvn pmd:check

# Test coverage
mvn jacoco:report
```

### Coding Standards

- Maximum file length: 400 lines.
- Maximum method length: 30 lines.
- Descriptive names for classes, methods, and variables.
- Comments that explain the "why," not the "what."

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