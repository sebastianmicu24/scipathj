# SciPathJ Project Development Summary
**Segmentation and Classification of Images, Pipelines for the Analysis of Tissue Histopathology**

*Last Updated: January 29, 2025*

## Project Overview

SciPathJ is a modern Java-based image analysis software for tissue histopathology, designed to provide automated segmentation and classification of histological images. The project builds upon proven methodologies while incorporating modern UI design and extensible architecture.

### Core Vision
- **Automated Analysis**: Highly automated software for histopathological image analysis
- **Batch Processing**: Process folder-based image batches with comprehensive results
- **Modern Interface**: Clean, intuitive user interface with professional design
- **Extensibility**: Plugin-ready architecture for future enhancements

## Development Phases Completed

### Phase 1: Project Foundation & Architecture (Completed)
**Duration**: Initial setup phase  
**Status**: ✅ Complete

#### Core Infrastructure
- **Maven Project Setup**: Java 23 with preview features enabled
- **Dependency Management**: Comprehensive dependency stack including:
  - ImageJ ecosystem (2.9.0) for image processing
  - XGBoost4J (2.1.4) for machine learning
  - FlatLaf (3.4.1) for modern UI theming
  - Ikonli (12.3.1) for professional icons
  - SLF4J + Logback for logging
  - Jackson for JSON processing

#### Package Structure
```
com.scipath.scipathj/
├── core/                 # Core processing engine
│   ├── engine/          # Main processing coordinator
│   ├── pipeline/        # Pipeline management system
│   ├── config/          # Configuration management
│   └── events/          # Event system for UI updates
├── ui/                  # User interface components
│   ├── main/           # Main application window
│   ├── components/     # Reusable UI components
│   ├── dialogs/        # Settings and dialogs
│   ├── themes/         # Theme management
│   ├── model/          # UI data models
│   └── utils/          # UI utilities
└── data/               # Data models and management
    └── model/          # Core data structures
```

#### Key Components Implemented
- **SciPathJEngine**: Central processing coordinator
- **ConfigurationManager**: Settings and preferences management
- **EventBus**: Event-driven communication system
- **Pipeline System**: Extensible pipeline architecture with interfaces
- **Data Models**: Core data structures (Cell, FeatureVector, ROI classes)

### Phase 2: User Interface Foundation (Completed)
**Duration**: UI framework development  
**Status**: ✅ Complete

#### Modern UI Framework
- **Theme System**: Dark/Light theme support with FlatLaf integration
- **Professional Design**: Modern, clean interface with consistent styling
- **Icon Integration**: FontAwesome icons throughout the application
- **Responsive Layout**: Proper component sizing and layout management

#### Core UI Components
- **MainWindow**: Primary application interface with card-based navigation
- **PipelineSelectionPanel**: Interactive pipeline selection with visual cards
- **PipelineRecapPanel**: Pipeline information display with step visualization
- **FolderSelectionPanel**: Drag-and-drop folder selection with modern styling
- **PreferencesDialog**: Comprehensive settings management interface

#### UI State Management
- **Card Layout System**: Smooth transitions between application states
- **State-Driven Navigation**: Proper state management for user workflow
- **Event-Driven Updates**: Reactive UI updates based on user actions

### Phase 3: Image Gallery Implementation (Completed)
**Duration**: Latest development phase  
**Status**: ✅ Complete

#### Image Processing Infrastructure
- **ImageLoader Utility**: Comprehensive image loading system supporting:
  - Common formats: JPG, PNG, GIF, BMP, TIFF
  - Scientific formats: LSM, CZI, ND2, OIB, OIF, VSI
  - Microscopy formats: IMS, LIF, SCN, SVS, NDPI
  - Raw formats: CR2, NEF, DNG
  - Other ImageJ-compatible formats: FITS, PGM, DICOM
- **Thumbnail Generation**: Efficient thumbnail creation with caching
- **Error Handling**: Robust error handling for unsupported formats
- **Memory Management**: Optimized memory usage for large image sets

#### Gallery Components
- **SimpleImageThumbnail**: Clean thumbnail component with:
  - Rounded borders for modern appearance
  - Filename display (extension removed for clarity)
  - Selection states with visual feedback
  - Asynchronous loading with progress indicators
  - Error state handling for failed loads

- **SimpleImageGallery**: Vertical gallery component featuring:
  - Scrollable thumbnail list
  - Auto-selection of first image
  - Loading states and error handling
  - Clean, minimal design focused on usability

- **MainImageViewer**: Primary image display component with:
  - Large format image display
  - Automatic scaling for optimal viewing
  - Image metadata display (filename, size, dimensions, format)
  - Loading states and error handling
  - Scroll support for large images

#### Workflow Integration
- **Three-State Navigation**:
  1. **Pipeline Selection**: Choose analysis pipeline
  2. **Folder Selection**: Select folder containing images
  3. **Image Gallery**: View and select images for analysis

- **Seamless Transitions**: 
  - Folder selection automatically transitions to gallery view
  - Gallery displays all supported images from selected folder
  - Main image viewer shows selected thumbnail in large format
  - "Change Folder" button allows easy folder switching

#### User Experience Enhancements
- **Intuitive Workflow**: Natural progression from pipeline → folder → images
- **Visual Feedback**: Clear loading states, selection indicators, and error messages
- **Professional Appearance**: Consistent styling with rounded corners and modern design
- **Responsive Design**: Proper component sizing and layout management

### Phase 4: ROI (Region of Interest) System Implementation (Completed)
**Duration**: Latest development phase
**Status**: ✅ Complete

#### ROI Management Infrastructure
- **UserROI Data Model**: Comprehensive ROI data structure supporting:
  - Multiple ROI types: Square, Rectangle, Circle
  - Per-image ROI association with filename tracking
  - Display properties (color, name, bounds)
  - Unique ID system for ROI identification
  - ImageJ-compatible data structure

- **ROIManager Singleton**: Centralized ROI management system featuring:
  - Per-image ROI storage and retrieval
  - Thread-safe concurrent operations
  - Event-driven ROI change notifications
  - ImageJ-compatible .roi and .zip file I/O
  - Comprehensive ROI statistics and querying

#### Interactive ROI Creation
- **ROIOverlay Component**: Transparent overlay system providing:
  - Real-time ROI drawing with mouse interaction
  - Visual feedback during ROI creation
  - ROI selection and highlighting
  - Coordinate transformation for scaled images
  - Anti-aliased rendering with professional appearance

- **Mouse Interaction System**:
  - Click-and-drag ROI creation
  - Square ROI creation (temporary feature for testing)
  - Rectangle ROI creation with flexible dimensions
  - ROI selection by clicking
  - Visual feedback during creation process

#### ROI Management UI
- **ROIToolbar**: Professional toolbar component featuring:
  - ROI creation mode buttons (Square/Rectangle)
  - Save ROIs functionality with smart file format selection
  - Save All ROIs from all images to master ZIP
  - Clear All ROIs for current image
  - Real-time ROI count display
  - Context-sensitive button states

- **Enhanced MainImageViewer**: Integrated ROI support including:
  - Layered pane architecture for ROI overlay
  - Coordinate transformation for proper ROI positioning
  - ROI display synchronized with image changes
  - Improved scrolling for tall images
  - ROI creation mode integration

#### ImageJ-Compatible File I/O
- **Flexible Save System**: Smart file format selection based on ROI count:
  - Single ROI: Saves as .roi file
  - Multiple ROIs (≥2): Saves as .zip file containing individual .roi files
  - Master ZIP export: All ROIs from all images in organized ZIP structure
  - Proper file extension handling and user interface feedback

- **ROI File Format Support**:
  - Individual .roi files for single ROIs
  - .zip ROI sets for multiple ROIs per image
  - Master .zip files with per-image ROI organization
  - Full ImageJ compatibility for seamless workflow integration

#### Advanced Features
- **Multi-Image ROI Management**:
  - ROIs properly associated with their respective images
  - ROI display only on corresponding images
  - Seamless switching between images with ROI persistence
  - Comprehensive ROI statistics across all images

- **Professional User Experience**:
  - Intuitive ROI creation workflow
  - Visual feedback for all operations
  - Error handling with user-friendly messages
  - Consistent styling with application theme
  - Responsive UI updates based on ROI operations

### Phase 5: Vessel Segmentation & Advanced Image Viewer (Completed)
**Duration**: Latest development phase
**Status**: ✅ Complete

#### Vessel Segmentation Implementation
- **VesselSegmentation Class**: Comprehensive vessel detection system featuring:
  - ImageJ Wand tool integration for shape-preserving vessel detection
  - Gaussian blur preprocessing (σ=2.0) for noise reduction
  - Threshold-based segmentation (pixel value > 150) for vessel identification
  - Individual vessel detection avoiding ImageJ ROI Manager interference
  - Minimum vessel size filtering (50.0 pixels) to exclude noise
  - Custom VesselParticleAnalyzer for precise vessel boundary detection
  - Complete isolation from ImageJ's global ROI Manager system

- **VesselROI Data Model**: Specialized vessel ROI structure supporting:
  - Complex vessel shape storage via ImageJ ROI integration
  - Vessel-specific metadata and properties
  - Integration with SciPathJ's custom ROI management system
  - Shape preservation for accurate morphological analysis

#### Advanced Image Viewer with Zoom Functionality
- **Comprehensive Zoom System**: Multi-level zoom implementation featuring:
  - Zoom range: 10% to 500% with 10% increments
  - Fit-to-window automatic scaling for optimal viewing
  - 100% actual size display for pixel-perfect analysis
  - Mouse wheel zoom with Ctrl modifier key support
  - Zoom controls UI with intuitive button interface
  - Smooth zoom transitions with proper image scaling

- **Enhanced MainImageViewer**: Major architectural improvements including:
  - Layered pane system for proper component stacking
  - Zoom-aware coordinate transformation for ROI positioning
  - Scroll synchronization with zoom level changes
  - Proper ROI overlay scaling and positioning
  - Memory-efficient image scaling with quality preservation
  - Responsive UI updates during zoom operations

#### ROI Overlay Synchronization System
- **Advanced Coordinate Transformation**: Precise positioning system featuring:
  - Zoom factor integration for accurate ROI scaling
  - Scroll offset compensation for proper ROI positioning
  - Real-time coordinate updates during pan and zoom operations
  - Anti-aliased rendering for professional appearance
  - Thread-safe overlay updates with proper synchronization

- **Enhanced ROIOverlay Component**: Improved overlay system including:
  - UIConstants integration for centralized parameter management
  - Zoom-aware ROI rendering with proper scaling
  - Improved mouse interaction handling
  - Visual feedback optimization for different zoom levels
  - Performance optimization for large image datasets

#### Constants Management & Architecture
- **UIConstants Centralization**: Comprehensive constants management featuring:
  - Vessel segmentation parameters (VESSEL_THRESHOLD=150, MIN_VESSEL_SIZE=50.0)
  - Gaussian blur configuration (GAUSSIAN_BLUR_SIGMA=2.0)
  - ROI display constants for consistent appearance
  - Zoom system parameters (MIN_ZOOM=0.1, MAX_ZOOM=5.0, ZOOM_STEP=0.1)
  - Centralized parameter management for easy configuration

- **Analysis Integration**: Seamless workflow integration including:
  - AnalysisController integration for vessel segmentation workflow
  - Progress tracking and user feedback during vessel detection
  - Error handling with user-friendly messages
  - Batch processing preparation for multiple images
  - Results management and export preparation

#### Technical Achievements
- **ImageJ Integration Without Interference**: Complete solution to ImageJ ROI Manager conflicts:
  - Custom vessel detection using Wand tool directly
  - Bypassing ImageJ's global ROI Manager completely
  - Maintaining ImageJ compatibility for shape storage
  - Preserving actual vessel contours instead of bounding boxes
  - Thread-safe operations independent of ImageJ's global state

- **Performance Optimization**: Efficient processing implementation:
  - Memory-efficient vessel detection algorithms
  - Optimized zoom rendering with proper caching
  - Responsive UI during intensive processing operations
  - Proper resource cleanup and memory management
  - Scalable architecture for large image datasets

## Technical Specifications

### Development Environment
- **Java Version**: Java 23 with preview features
- **Build System**: Maven 3.x
- **IDE Compatibility**: IntelliJ IDEA, Eclipse, VS Code
- **Operating System**: Cross-platform (Windows, macOS, Linux)

### Dependencies & Libraries
- **ImageJ Ecosystem**: Complete ImageJ integration for image processing
- **UI Framework**: Swing with FlatLaf for modern appearance
- **Machine Learning**: XGBoost4J for classification (prepared for future use)
- **Logging**: SLF4J with Logback for comprehensive logging
- **Icons**: Ikonli with FontAwesome for professional iconography

### Performance Characteristics
- **Memory Efficient**: Optimized for processing large image sets
- **Asynchronous Loading**: Non-blocking image loading and thumbnail generation
- **Scalable Architecture**: Designed to handle batch processing of hundreds of images
- **Resource Management**: Proper cleanup and memory management

## Current Application Features

### ✅ Implemented Features
1. **Modern User Interface**
   - Professional dark/light theme support
   - Intuitive navigation with card-based layout
   - Responsive design with proper component sizing

2. **Pipeline Management**
   - Visual pipeline selection interface
   - Pipeline information display with step breakdown
   - Extensible pipeline architecture for future additions

3. **Image Gallery System**
   - Support for 20+ image formats including scientific formats
   - Thumbnail generation with rounded corners
   - Large format image viewer with metadata
   - Asynchronous loading with progress feedback
   - Improved scrolling for tall images

4. **ROI (Region of Interest) System**
   - Interactive ROI creation with mouse drawing
   - Multiple ROI types: Square, Rectangle, Circle
   - Per-image ROI management and display
   - Professional ROI toolbar with creation tools
   - ImageJ-compatible file I/O (.roi and .zip formats)
   - Smart save system: single ROI → .roi file, multiple ROIs → .zip file
   - Master ZIP export for all ROIs from all images
   - Real-time ROI count display and management

5. **Vessel Segmentation System**
   - Automated vessel detection using ImageJ Wand tool
   - Threshold-based segmentation (pixel value > 150)
   - Gaussian blur preprocessing for noise reduction
   - Individual vessel shape preservation (no bounding boxes)
   - Minimum size filtering to exclude noise artifacts
   - Complete isolation from ImageJ's global ROI Manager
   - Integration with SciPathJ's custom ROI management system

6. **Advanced Image Viewer with Zoom**
   - Multi-level zoom functionality (10% to 500%)
   - Fit-to-window and 100% actual size modes
   - Mouse wheel zoom with Ctrl modifier support
   - Zoom controls UI with intuitive button interface
   - ROI overlay synchronization with zoom and scroll
   - Proper coordinate transformation for all zoom levels
   - Memory-efficient image scaling with quality preservation

7. **Folder Processing**
   - Drag-and-drop folder selection
   - Automatic image detection and filtering
   - Batch processing preparation

8. **Configuration Management**
   - User preferences with persistent storage
   - Theme selection and customization
   - Comprehensive settings management
   - Centralized constants management via UIConstants

### 🚧 Prepared for Implementation
1. **Image Analysis Pipeline**
   - StarDist integration for nuclear segmentation
   - Voronoi tessellation for cytoplasm segmentation
   - Feature extraction system (50+ morphological and intensity features)
   - XGBoost classification system

2. **Export System**
   - CSV export with US/EU format support
   - Batch processing results export
   - Analysis results visualization

3. **Advanced Features**
   - Large image tiling support
   - Plugin system architecture
   - ONNX model integration
   - ROI loading functionality from existing .roi/.zip files

### ✅ Recently Implemented
1. **Vessel Segmentation System**
   - Automated vessel detection and ROI creation
   - Shape-preserving segmentation using ImageJ Wand tool
   - Complete integration with SciPathJ's ROI management system

2. **Advanced Image Viewer**
   - Comprehensive zoom functionality (10%-500%)
   - ROI overlay synchronization with zoom and scroll
   - Professional zoom controls and mouse wheel support

## File Structure Overview

### Active Components
### Active Components
```
src/main/java/com/scipath/scipathj/
├── SciPathJApplication.java           # Main application entry point
├── analysis/
│   └── VesselSegmentation.java        # Vessel detection and segmentation
├── core/
│   ├── engine/SciPathJEngine.java     # Core processing engine
│   ├── config/ConfigurationManager.java # Settings management
│   ├── events/EventBus.java           # Event system
│   └── pipeline/                      # Pipeline interfaces and management
├── ui/
│   ├── main/MainWindow.java           # Primary application window
│   ├── components/
│   │   ├── FolderSelectionPanel.java  # Folder selection interface
│   │   ├── SimpleImageGallery.java    # Image gallery component
│   │   ├── SimpleImageThumbnail.java  # Individual thumbnail component
│   │   ├── MainImageViewer.java       # Large image display with zoom and ROI support
│   │   ├── PipelineSelectionPanel.java # Pipeline selection interface
│   │   ├── PipelineRecapPanel.java    # Pipeline information display
│   │   ├── ROIManager.java            # ROI management singleton
│   │   ├── ROIOverlay.java            # Interactive ROI overlay component
│   │   └── ROIToolbar.java            # ROI creation and management toolbar
│   ├── controllers/
│   │   └── AnalysisController.java    # Analysis workflow coordination
│   ├── dialogs/PreferencesDialog.java # Settings dialog
│   ├── themes/ThemeManager.java       # Theme management
│   ├── model/                         # UI data models
│   └── utils/
│       ├── ImageLoader.java           # Image processing utilities
│       └── UIConstants.java           # Centralized UI constants
└── data/model/
    ├── UserROI.java                   # ROI data model
    ├── VesselROI.java                 # Vessel-specific ROI model
    └── [other data structures]        # Core data structures
```
### Configuration Files
- **pom.xml**: Maven project configuration with all dependencies
- **logback.xml**: Logging configuration
- **Professional_Java_Development_Guide.md**: Development best practices
- **SciPathJ_Implementation_Plan.md**: Detailed implementation roadmap

## Development Methodology

### Code Quality Standards
- **Professional Java Practices**: Following Java 23 best practices
- **Comprehensive Documentation**: JavaDoc for all public APIs
- **Error Handling**: Robust exception handling with user-friendly messages
- **Logging**: Comprehensive logging at appropriate levels
- **Memory Management**: Proper resource cleanup and memory optimization

### Architecture Principles
- **Separation of Concerns**: Clear separation between UI, business logic, and data
- **Event-Driven Design**: Loose coupling through event system
- **Extensibility**: Plugin-ready architecture for future enhancements
- **Maintainability**: Clean, readable code with consistent patterns

## Next Development Priorities

### High Priority (Ready for Implementation)
1. **Image Analysis Integration**
   - Implement StarDist nuclear segmentation
   - Add feature extraction algorithms
   - Integrate XGBoost classification

2. **Results Management**
   - Implement CSV export functionality
   - Add analysis results visualization
   - Create batch processing workflow

### Medium Priority
1. **Advanced UI Features**
   - Implement image selection for batch processing
   - Add progress tracking for analysis operations
   - Enhance vessel segmentation with additional parameters

2. **Performance Optimization**
   - Implement image caching system
   - Add multi-threading for batch operations
   - Optimize memory usage for large datasets
   - Further optimize zoom rendering performance

### Future Enhancements
1. **Plugin System**
   - Implement plugin loading mechanism
   - Create plugin API documentation
   - Add plugin management interface

2. **Advanced Analysis**
   - ONNX model integration
   - Custom model loading
   - Advanced visualization tools

## Technical Achievements

### Code Quality Metrics
- **Architecture**: Clean, modular design with clear separation of concerns
- **Documentation**: Comprehensive JavaDoc and inline documentation
- **Error Handling**: Robust exception handling throughout the application
- **Performance**: Optimized for memory efficiency and responsive UI
- **Maintainability**: Consistent coding patterns and professional structure

### User Experience
- **Intuitive Workflow**: Natural progression through application states
- **Professional Appearance**: Modern, clean interface with consistent styling
- **Responsive Design**: Proper handling of different screen sizes and content
- **Error Feedback**: Clear, user-friendly error messages and recovery options

## Conclusion

SciPathJ has successfully established a comprehensive foundation with a modern, professional user interface, complete image gallery functionality, fully integrated ROI (Region of Interest) management system, and advanced vessel segmentation capabilities. The application demonstrates professional Java development practices with clean architecture, robust error handling, and extensible design.

The current implementation provides a complete workflow from pipeline selection through image gallery viewing to interactive ROI creation, automated vessel segmentation, and advanced image viewing with zoom functionality. This represents a significant milestone, as both ROI functionality and vessel segmentation are essential for histopathological image analysis workflows.

Key achievements in the latest development phase include:
- **Complete ROI System**: Interactive creation, management, and export of regions of interest
- **Automated Vessel Segmentation**: Shape-preserving vessel detection using ImageJ Wand tool with complete isolation from ImageJ's global ROI Manager
- **Advanced Image Viewer**: Comprehensive zoom functionality (10%-500%) with proper ROI overlay synchronization
- **ImageJ Compatibility**: Seamless integration with existing ImageJ workflows through .roi and .zip file formats
- **Professional UI**: Intuitive ROI toolbar, zoom controls, and overlay system with real-time feedback
- **Multi-Image Support**: Proper ROI association and management across multiple images
- **Performance Optimization**: Memory-efficient processing with responsive UI during intensive operations

The project is excellently positioned for continued development, with a solid technical foundation that supports both current functionality and future enhancements. The vessel segmentation system provides the first implemented analysis algorithm, demonstrating the framework's capability to integrate complex image processing workflows while maintaining clean architecture and user-friendly interfaces.

---

### Phase 6: StarDist Nuclear Segmentation Integration (Completed)
**Duration**: Latest development phase
**Status**: ✅ Complete

#### StarDist Integration Implementation
- **Complete Source Integration**: Direct integration of StarDist and CSBDeep source files into SciPathJ project:
  - All StarDist classes copied to `scipathj/src/main/java/de/csbdresden/stardist/`
  - Complete CSBDeep framework integrated to `scipathj/src/main/java/de/csbdresden/csbdeep/`
  - Full neural network execution pipeline with TensorFlow backend
  - Pre-trained model support for versatile H&E nuclear segmentation

- **TensorFlow API Compatibility Resolution**: Major dependency management improvements:
  - Updated `pom-scijava` from version `28.0.0` to `29.2.1` for latest ImageJ ecosystem
  - Added explicit `imagej-tensorflow` version `1.1.5` for proper API compatibility
  - Integrated `tensorflow` version `1.15.0` with `libtensorflow` and `proto` dependencies
  - Resolved all TensorFlow API version mismatches between CSBDeep and ImageJ-TensorFlow

#### Advanced Image Processing Pipeline
- **StarDistIntegration Class**: Comprehensive nuclear segmentation engine featuring:
  - Full SciJava context initialization with all required services (UIService, CommandService, DatasetService)
  - Automatic image bit depth conversion supporting 8, 16, and 32-bit images
  - Intelligent image format handling with automatic conversion to supported formats
  - Direct StarDist2D command execution through SciJava CommandService
  - Complete parameter mapping from SciPathJ settings to StarDist configuration
  - ROI Manager integration for seamless result extraction

- **Robust Error Handling and Recovery**: Production-ready error management including:
  - Comprehensive plugin availability checking before execution
  - Automatic image format conversion for unsupported bit depths
  - Graceful degradation with detailed error reporting
  - Memory-efficient processing with proper resource cleanup
  - Thread-safe operations with proper context management

#### Technical Achievements
- **Compilation Success**: Complete resolution of all compilation errors:
  - Fixed generic type inference issues with `DatasetService.create()` methods
  - Resolved TensorFlow API compatibility through proper dependency versions
  - Eliminated all missing class and method errors through direct source integration
  - Achieved clean compilation with Java 23 and modern syntax support

- **Runtime Stability**: Comprehensive runtime issue resolution:
  - Fixed "Unrecognized command: StarDist2D" error through proper SciJava integration
  - Resolved image bit depth compatibility issues with automatic conversion
  - Eliminated SciJava context initialization failures through full service loading
  - Achieved stable nuclear segmentation execution with proper ROI extraction

#### Integration Architecture
- **Direct Source Code Integration**: Complete StarDist ecosystem integration:
  - StarDist neural network classes with polygon detection algorithms
  - CSBDeep framework with TensorFlow model execution pipeline
  - Clipper library integration for polygon clipping operations
  - Full compatibility with existing ImageJ ROI management system

- **Modern Dependency Management**: Professional Maven configuration:
  - System-scoped Clipper JAR dependency with local library file
  - Proper ImageJ ecosystem integration through pom-scijava parent
  - TensorFlow version alignment between all components
  - Clean dependency resolution without conflicts

#### Nuclear Segmentation Capabilities
- **Pre-trained Model Support**: Ready-to-use nuclear segmentation:
  - Versatile H&E nuclei model for general histopathology
  - Configurable probability and NMS thresholds
  - Adjustable normalization parameters for different image types
  - Support for tiled processing of large images

- **Advanced Configuration System**: Comprehensive parameter control:
  - Model selection interface with multiple pre-trained options
  - Threshold adjustment for detection sensitivity
  - Normalization parameter tuning for optimal results
  - Output format selection and ROI management options

---

**Development Team**: Sebastian Micu
**Technology Stack**: Java 23, Maven, ImageJ, StarDist, CSBDeep, TensorFlow, Swing/FlatLaf
**Project Status**: Foundation Complete with Vessel Segmentation, Advanced Image Viewer, and StarDist Nuclear Segmentation Ready
**Next Milestone**: Feature Extraction and XGBoost Classification Implementation