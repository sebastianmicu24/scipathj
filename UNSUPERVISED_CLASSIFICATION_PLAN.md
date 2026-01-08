# Unsupervised Classification Implementation Plan

## 1. Overview
Implement an unsupervised classification (clustering) pipeline for cells, allowing users to discover cell types based on morphological and intensity features without labeled training data.

## 2. Core Strategy
*   **Algorithm:** K-Means Clustering (via Smile library).
*   **Data Unit:** Single Cell (Aggregated).
*   **Polynucleated Handling:** "Aggregated Approach" - Nuclei features are summarized (count, mean area, total area) and appended to the cell's feature vector.

## 3. Dependencies
*   Add **Smile (Statistical Machine Intelligence and Learning Engine)** to `pom.xml`.
    *   `com.github.haifengl:smile-core:3.0.0`

## 4. Data Structure Changes

### 4.1. `CellFeatureAggregator` (New Class)
Responsible for creating a single, flat feature vector for a `CellROI`.
*   **Input:** `CellROI` (which links to `CytoplasmROI` and `NucleusROI`).
*   **Output:** `double[]` feature vector.
*   **Logic:**
    *   Extract Cell features (Area, Perimeter, Shape).
    *   Extract Cytoplasm features (Intensity, Texture).
    *   **Aggregate Nuclei:**
        *   If single nucleus: Use its features directly.
        *   If multiple nuclei (future-proofing): Calculate `count`, `mean_area`, `total_area`, `mean_intensity`.
        *   *Current Implementation Note:* Since `CellROI` currently links to a single `NucleusROI`, we will implement the aggregator to support the *concept* of multiple nuclei by checking if the cell has a list (if we modify `CellROI`) or just using the single nucleus for now but naming features in a way that supports aggregation (e.g., `nucleus_count` = 1).

## 5. UI Components

### 5.1. `UnsupervisedClusteringPanel` (New Class)
A new main panel accessible from the Main Menu.
*   **Controls:**
    *   **Cluster Count (K):** Spinner/Slider (2-20).
    *   **Feature Selection:** Checkboxes for feature groups (Morphology, Intensity, Texture).
    *   **Run Button:** Triggers clustering.
*   **Visualization:**
    *   **Scatter Plot:** PCA-reduced 2D projection of clusters.
    *   **Image Overlay:** Color-code cells in the main viewer based on their assigned cluster.

### 5.2. Main Menu Update
*   Add "Unsupervised Classification" button to `MainMenuPanel`.

## 6. Implementation Steps

1.  **Dependency:** Add Smile to `pom.xml`.
2.  **Backend Logic:**
    *   Create `CellFeatureAggregator`.
    *   Create `UnsupervisedClassifier` service class (wraps Smile's K-Means).
3.  **UI Implementation:**
    *   Create `UnsupervisedClusteringPanel`.
    *   Integrate into `NavigationController` and `MainMenuPanel`.
4.  **Visualization:**
    *   Implement result visualization (coloring ROIs by cluster ID).

## 7. Polynucleated Cell Handling Detail
Since `CellROI` currently has `private NucleusROI associatedNucleus;` (singular), we will treat it as a "1-nucleus cell" for now but structure the feature vector to be ready for N-nuclei.
*   Feature: `nucleus_count` (Always 1 for now, unless we detect overlaps).
*   Feature: `total_nuclear_area` (= `associatedNucleus.getArea()`).
*   Feature: `mean_nuclear_circularity` (= `associatedNucleus.getCircularity()`).

This naming convention allows us to seamlessly upgrade `CellROI` to `List<NucleusROI>` later without breaking the classifier model.