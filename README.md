# SciPathJ

**Segmentation and Classification of Images - Pipeline for the Analysis of Tissue Histopathology**

![SciPathJ](https://img.shields.io/badge/version-1.0.0-blue) ![License](https://img.shields.io/badge/license-GPLv3-green) ![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Java-lightgrey)

SciPathJ is a free, open-source software designed to help researchers and medical professionals analyze microscopy images of tissue samples. If you work with H&E stained histology images and need to count cells, measure structures, or classify different cell types, SciPathJ can automate these tasks for you.

## Table of Contents

- [What Can SciPathJ Do?](#what-can-scipathj-do)
  - [Key Features](#key-features)
  - [What Makes SciPathJ Different?](#what-makes-scipathj-different)
- [Who Is SciPathJ For?](#who-is-scipathj-for)
- [Installation](#installation)
  - [System Requirements](#system-requirements)
  - [Quick Installation](#quick-installation)
  - [ImageJ/Fiji Plugin (Legacy)](#imagejfiji-plugin-legacy)
- [Getting Started](#getting-started)
  - [Your First Analysis in 5 Steps](#your-first-analysis-in-5-steps)
  - [Understanding the Interface](#understanding-the-interface)
  - [Color Coding](#color-coding)
- [Analysis Pipeline](#analysis-pipeline)
  - [1. Vessel Segmentation](#1-vessel-segmentation)
  - [2. Nuclear Segmentation](#2-nuclear-segmentation)
  - [3. Cell Creation](#3-cell-creation)
  - [4. Feature Extraction](#4-feature-extraction)
  - [5. Cell Classification](#5-cell-classification)
- [Training Custom Classifiers](#training-custom-classifiers)
  - [How It Works](#how-it-works)
  - [Why This Matters](#why-this-matters)
- [Output and Results](#output-and-results)
  - [What You Get](#what-you-get)
  - [Using Your Results](#using-your-results)
- [Examples](#examples)
  - [Liver Tissue](#liver-tissue)
  - [Kidney Tissue](#kidney-tissue)
  - [Pancreas Tissue](#pancreas-tissue)
  - [More Tissue Types](#more-tissue-types)
- [Learning Resources](#learning-resources)
  - [Documentation](#documentation)
  - [Video Tutorials](#video-tutorials)
  - [Recommended Learning Path](#recommended-learning-path)
- [Project History](#project-history)
- [The Team](#the-team)
  - [Lead Developer](#lead-developer)
  - [Partner Institutions](#partner-institutions)
- [Support](#support)
  - [Need Help?](#need-help)
  - [Frequently Asked Questions](#frequently-asked-questions)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## What Can SciPathJ Do?

Imagine you have hundreds of tissue images and need to count cells, measure their size, or identify different cell types. Doing this manually would take weeks. SciPathJ can do it in minutes.

### Key Features

- **Automatic Cell Detection**: The software automatically finds and outlines cells, nuclei, cytoplasm, and blood vessels in your tissue images
- **Cell Classification**: Define your own cell categories (like "tumor cells" vs "normal cells") and train the software to recognize them automatically
- **Comprehensive Measurements**: For every detected structure, SciPathJ calculates over 150 measurements including size, shape, color intensity, and texture
- **Batch Processing**: Process entire folders of images at once with the same settings
- **Export to Excel**: All results can be exported to CSV files for further analysis in Excel, R, Python, or any statistical software

### What Makes SciPathJ Different?

- **Designed for H&E Stains**: Built specifically for Hematoxylin & Eosin stained images, the most common staining method in histopathology
- **No Programming Required**: User-friendly graphical interface - no coding skills needed
- **Works Standalone or with ImageJ**: Use it as a standalone application or as a plugin within ImageJ/Fiji
- **Completely Free**: Open-source under GPLv3 license, free to use and modify

## Who Is SciPathJ For?

- **Pathologists** analyzing tissue samples for diagnosis or research
- **Research Scientists** studying tissue morphology and cell populations
- **Medical Students** learning histopathology and tissue analysis
- **Biologists** quantifying cell types and tissue structures
- **Anyone** who needs to extract quantitative data from H&E stained microscopy images

## Installation

### Main Settings

![Main Application Settings](https://lh3.googleusercontent.com/d/1v4uFGjdB4LNyuUqeuZCEXsKcPmsJwjJA)

### System Requirements

| Requirement | Minimum |
|-------------|---------|
| Operating System | Windows|
| RAM | 4 GB |
| Storage | 1.2 GB |
| Java | JRE 11 |

### Quick Installation

1. **Download** the installer from the [Releases page](https://github.com/sebastianmicu24/scipathj/releases)
2. **Run** the installer and follow the on-screen instructions
3. **Launch** SciPathJ from your desktop or Start menu

That's it! The installer handles all dependencies automatically.

### ImageJ/Fiji Plugin (Legacy)

The ImageJ/Fiji plugin version is available but is no longer actively maintained. For the latest features and improvements, we recommend using the standalone version.

## Getting Started

### Your First Analysis in 5 Steps

1. **Open an Image**: Click "Open Folder" and select your H&E stained image (supports TIFF, PNG, JPEG, NDPI, SVS, and more)
2. **Configure Settings**: Adjust detection parameters for your specific tissue type (or use the defaults)
3. **Run Analysis**: Click "Start" and watch as SciPathJ identifies cells, nuclei, and vessels
4. **Review Results**: View the detected structures overlaid on your image with color coding
5. **Export Data**: Save all measurements to CSV files for further analysis

### Understanding the Interface

The main window is organized into intuitive sections:

![SciPathJ Main Interface](https://lh3.googleusercontent.com/d/1y0WfhMlXYUYki-wQbup-D03Hw2dlsMVa)

- **Top Bar**: Pipeline steps showing the analysis workflow progression
- **Left Sidebar**: Thumbnail gallery for navigating between images
- **Center Canvas**: Your image with segmentation overlays
- **Bottom Toolbar**: Controls for saving, clearing, and viewing statistics
- **Status Bar**: Real-time feedback on analysis progress

### Color Coding

![Display Settings](https://lh3.googleusercontent.com/d/1WsUJPLFmQARwEjDHR6sHx9qOwFWbU8D7)

SciPathJ uses consistent colors to help you identify structures:

| Structure | Default Color | Description |
|-----------|-------|-------------|
| **Nuclei** | Blue | Cell nuclei detected from hematoxylin staining |
| **Cytoplasm** | Black | Cytoplasm regions surrounding each nucleus |
| **Vessels** | Red | Blood vessels and vascular structures |
| **Cells** | Yellow | Complete cell boundaries (nucleus + cytoplasm) |

## Analysis Pipeline

SciPathJ processes your images through a series of automated steps:

### 1. Vessel Segmentation

![Vessel Segmentation Settings](https://lh3.googleusercontent.com/d/1_Gvig6gdgPI9UzAILkomI8RlKAAReTAR)

Detects blood vessels based on their characteristic shape and staining patterns. You can adjust:
- Detection sensitivity (threshold)
- Minimum and maximum vessel size
- Smoothing and noise reduction

### 2. Nuclear Segmentation

![Nuclear Segmentation Settings](https://lh3.googleusercontent.com/d/1uo2SB1S37L0XicM5IgBZPFRsMUiyEzCv)

Uses advanced deep learning (StarDist) to identify cell nuclei with high accuracy. Settings include:
- Pre-trained model selection (optimized for H&E stains)
- Detection sensitivity
- Size filters to exclude artifacts

### 3. Cell Creation

![Cytoplasm Segmentation Settings](https://lh3.googleusercontent.com/d/1yvshHSpyQA-wAPUhUaVKYEm8IX6xeh7h)

Combines detected nuclei with surrounding cytoplasm to define complete cells:
- Voronoi tessellation estimates cell boundaries
- Handles polynucleated cells (cells with multiple nuclei)
- Excludes vessel areas from cell detection

### 4. Feature Extraction

![Feature Extraction Settings](https://lh3.googleusercontent.com/d/1X86OPcZqakId3WwnNJpYwxmYAHVUt1NT)

Calculates over 150 measurements for each detected structure:

**Shape Measurements**: Area, perimeter, circularity, aspect ratio, solidity, and more

**Intensity Measurements**: Mean, median, standard deviation, minimum, maximum of staining intensity

**H&E-Specific Features**: Separate measurements for hematoxylin (nuclear) and eosin (cytoplasmic) staining

**Spatial Features**: Distance to nearest vessel, number of neighboring cells

### 5. Cell Classification

![Unsupervised Classification Settings](https://lh3.googleusercontent.com/d/1ur7IaUzLuWTH1gbfDwb2PQhyFte_OKPX)

Two options for categorizing your cells:

**Unsupervised Clustering**: Automatically groups cells based on similarity (K-Means algorithm)

**Supervised Classification**: Train your own model by labeling example cells, then apply it to all images

## Training Custom Classifiers

One of SciPathJ's most powerful features is the ability to train custom cell classifiers:

### How It Works

1. **Create a Dataset**: Select representative cells from your images
2. **Label Cells**: Assign each cell to a category (e.g., "Tumor", "Normal", "Inflammatory")
3. **Train Model**: SciPathJ uses XGBoost machine learning to learn the patterns
4. **Apply Model**: Use the trained model to classify all cells in new images automatically

### Why This Matters

Instead of manually counting hundreds or thousands of cells, you can:
- Label just 100-200 example cells
- Train a model in minutes
- Automatically classify thousands of cells with consistent criteria

## Output and Results

### What You Get

After analysis, SciPathJ provides:

**Visual Results**:
- Segmentation overlays on your original images
- Color-coded classification results
- Cluster visualization for unsupervised analysis

![Extracted Features Table](https://lh3.googleusercontent.com/d/11raUcEYNlHHod5PR8bG4bf22TD3INKXD)

**Data Files**:
- CSV files with all measurements for each detected structure
- Summary statistics per image and across batches
- ROI files compatible with ImageJ

![ROI Statistics Summary](https://lh3.googleusercontent.com/d/10A9uQ3mq3NLdCRUwP1CgkzemYyGGGSLd)

**Export Options**:
- Individual images or entire folders
- Combined CSV files for batch analysis
- ROI files for further processing

![Cluster Visualization](https://lh3.googleusercontent.com/d/1mDbgrhLKr0Qu_kfVW6Z1n_L9QdieJB_j)

### Using Your Results

The CSV output can be:
- Opened directly in Excel for filtering and basic statistics
- Imported into R or Python for advanced analysis
- Used for publication-ready figures and tables
- Combined with clinical data for research studies

## Examples

SciPathJ has been successfully used to analyze various tissue types. Here are some before and after examples:

### Liver Tissue
![Liver Before](https://drive.google.com/thumbnail?id=1HjMpLIfhVg38-_efIvIIvIX_6H4o6Yle&sz=w600) | ![Liver After](https://drive.google.com/thumbnail?id=1Q8_kTDty2nq3GlEwl7x5N-S8dpXW6ocZ&sz=w600)
:---:|:---:
*Before* | *After - Vessels (red), Nuclei (blue), Cells (black)*

### Kidney Tissue
![Kidney Before](https://drive.google.com/thumbnail?id=1Pa5s4Z6xPS7wF7s1L7Ub6t75z5hRJHG_&sz=w600) | ![Kidney After](https://drive.google.com/thumbnail?id=1mcZozdUNCiiRr688xUfhPzMlYMq-AKQO&sz=w600)
:---:|:---:
*Before* | *After*

### Pancreas Tissue
![Pancreas Before](https://drive.google.com/thumbnail?id=1XiTgSSCmmg9cd-fHvOgKii8XKexTlqaI&sz=w600) | ![Pancreas After](https://drive.google.com/thumbnail?id=1ediNHPqwD108A9oQsz5cBOxIPEdBGfva&sz=w600)
:---:|:---:
*Before* | *After*

### More Tissue Types

| Tissue | Application |
|--------|-------------|
| **Liver** | Vessel detection, hepatocyte analysis |
| **Pancreas** | Islet cell identification |
| **Kidney** | Glomeruli detection and measurement |
| **Spleen** | White pulp vs red pulp analysis |
| **Lung** | Alveolar structure quantification |
| **Skin** | Epidermal layer analysis |
| **And more...** | Works with most H&E stained tissues |

View the complete [Examples Gallery](https://scipathj.com/examples) on our website.

## Learning Resources

### Documentation

Comprehensive documentation is available covering:
- Installation guide
- Interface overview
- Each analysis step in detail
- Feature reference
- Troubleshooting

Visit our [Documentation Page](https://scipathj.com/docs)

### Video Tutorials

Watch step-by-step video tutorials:
- Getting Started with SciPathJ
- Image Segmentation Deep Dive
- Training Custom Classifiers
- Batch Processing Workflows
- Feature Extraction & Analysis

Visit our [Tutorials Page](https://scipathj.com/tutorials)

### Recommended Learning Path

1. Start with the "Getting Started" tutorial
2. Practice segmentation on sample images
3. Explore the extracted features
4. Learn to train custom classifiers
5. Set up batch processing for your projects

## Project History

SciPathJ was developed to solve a real problem in histopathology research: the need for accessible, automated image analysis.

| Date | Milestone |
|------|-----------|
| **October 2024** | Project started as a Fiji/ImageJ macro using Weka segmentation |
| **March 2025** | Evolved into "SCHELI" plugin with StarDist and XGBoost |
| **July 2025** | Presented as a Master's Thesis at Sapienza University of Rome |
| **August 2025** | Added graphical interface, renamed to SciPathJ, expanded to all tissue types |
| **October 2025** | Presented at Maker Faire Rome |

## The Team

### Lead Developer

**Sebastian Micu** - Medical Resident in Clinical Genetics at Sapienza University of Rome with a passion for programming and machine learning. Developed SciPathJ as a continuation of thesis research.

### Partner Institutions

- **Sapienza University of Rome** - One of Italy's oldest and most prestigious universities
- **Centro de Biología Molecular Severo Ochoa** (Madrid) - Molecular biology research center

### Need Help?

- **Documentation**: Check our comprehensive [Documentation](https://scipathj.com/docs)
- **Tutorials**: Watch our [Video Tutorials](https://scipathj.com/tutorials)
- **GitHub Issues**: Report bugs or request features on [GitHub Issues](https://github.com/sebastianmicu24/scipathj/issues)
- **Discussions**: Join the conversation on [GitHub Discussions](https://github.com/sebastianmicu24/scipathj/discussions)

### Frequently Asked Questions

**Q: What image formats does SciPathJ support?**  
A: Common formats (TIFF, PNG, JPEG, BMP, GIF)

**Q: Can I use SciPathJ for fluorescent images?**  
A: SciPathJ is optimized for H&E stained images. For fluorescent images, consider using the StarDist plugin directly in Fiji.

**Q: How accurate is the segmentation?**  
A: Nuclear segmentation using StarDist typically achieves >90% accuracy on good quality H&E images. Results may vary depending on image quality and tissue type.

**Q: Can I use the software for commercial purposes?**  
A: Yes, under the GPLv3 license. See the LICENSE file for details.

**Q: How many cells can SciPathJ process?**  
A: SciPathJ has been tested with images containing over 10,000 cells. Memory requirements scale with image size.

## License

SciPathJ is released under the **GNU General Public License v3.0 (GPLv3)**. This means:

- Free to use for any purpose
- Free to study and modify the source code
- Free to share with others
- Any modifications must also be open source

See the [LICENSE](LICENSE) file for full details.

## Acknowledgements

We thank:
- **Centro de Biología Molecular Severo Ochoa** in Madrid for research support
- **Sapienza University of Rome** for academic guidance
- The **open-source community** for the tools and libraries that made this project possible
- All **contributors and testers** who helped improve SciPathJ

---

**Links**

[Website](https://scipathj.com) | [Documentation](https://scipathj.com/docs) | [Tutorials](https://scipathj.com/tutorials) | [Examples](https://scipathj.com/examples) | [Download](https://scipathj.com/download) | [GitHub](https://github.com/sebastianmicu24/scipathj)

---

*SciPathJ - Transforming what you see into numbers you can analyze.*
