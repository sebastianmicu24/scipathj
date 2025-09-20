package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for categorizing ROIs by type and classification status.
 * Follows Single Responsibility Principle - only handles ROI categorization.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ROICategorizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ROICategorizer.class);

    /**
     * Categorize ROIs by type and classification status.
     * 
     * @param allROIs Map of all ROIs from the overlay
     * @return Categorized ROI data
     */
    public ROICategorizedData categorizeROIs(Map<String, UserROI> allROIs) {
        LOGGER.info("Categorizing {} ROIs by type and classification status", allROIs.size());
        
        ROICategorizedData data = new ROICategorizedData();

        for (UserROI roi : allROIs.values()) {
            switch (roi.getType()) {
                case CELL:
                    data.cells.add(roi);
                    if (isClassified(roi)) data.classifiedCells.add(roi);
                    break;
                case NUCLEUS:
                    data.nuclei.add(roi);
                    if (isClassified(roi)) data.classifiedNuclei.add(roi);
                    break;
                case CYTOPLASM:
                    data.cytoplasm.add(roi);
                    if (isClassified(roi)) data.classifiedCytoplasms.add(roi);
                    break;
                case VESSEL:
                    data.vessel.add(roi);
                    break;
                case IGNORE:
                    data.ignored.add(roi);
                    break;
            }
        }

        LOGGER.info("Categorized ROIs: Cells={}, Nuclei={}, Cytoplasm={}, Vessel={}, Ignored={}",
                   data.cells.size(), data.nuclei.size(), data.cytoplasm.size(), 
                   data.vessel.size(), data.ignored.size());
        LOGGER.info("Classified ROIs: Cells={}, Nuclei={}, Cytoplasm={}",
                   data.classifiedCells.size(), data.classifiedNuclei.size(), data.classifiedCytoplasms.size());

        return data;
    }

    /**
     * Check if an ROI has been manually classified.
     * 
     * @param roi The ROI to check
     * @return true if the ROI has a valid classification
     */
    private boolean isClassified(UserROI roi) {
        String className = roi.getAssignedClass();
        return className != null && !className.trim().isEmpty() && !"Unclassified".equals(className);
    }

    /**
     * Group ROIs by cell ID for proper organization.
     * 
     * @param categorizedData The categorized ROI data
     * @return Map of cell ID to list of ROIs
     */
    public Map<String, List<UserROI>> groupROIsByCellId(ROICategorizedData categorizedData) {
        Map<String, List<UserROI>> roisByCell = new HashMap<>();
        
        // Add all ROIs to grouping
        List<UserROI> allROIs = new ArrayList<>();
        allROIs.addAll(categorizedData.cells);
        allROIs.addAll(categorizedData.nuclei);
        allROIs.addAll(categorizedData.cytoplasm);
        
        for (UserROI roi : allROIs) {
            String cellId = extractCellIdFromROIName(roi.getName());
            roisByCell.computeIfAbsent(cellId, k -> new ArrayList<>()).add(roi);
        }
        
        LOGGER.debug("Grouped {} ROIs into {} cell groups", allROIs.size(), roisByCell.size());
        return roisByCell;
    }
    
    /**
     * Extract cell ID from ROI name.
     * 
     * @param roiName The ROI name to parse
     * @return The extracted cell ID
     */
    private String extractCellIdFromROIName(String roiName) {
        if (roiName == null || roiName.isEmpty()) {
            return "unknown";
        }

        // Extract number from names like "Cell_123", "Nucleus_456", etc.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*?(\\d+)$");
        java.util.regex.Matcher matcher = pattern.matcher(roiName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return roiName;
    }

    /**
     * Get the class assignment for a specific cell ID.
     * 
     * @param cellId The cell ID to get the class for
     * @param roisByCell Map of cell ID to ROIs
     * @return The class name or "Unclassified"
     */
    public String getClassForCell(String cellId, Map<String, List<UserROI>> roisByCell) {
        List<UserROI> cellROIs = roisByCell.get(cellId);
        if (cellROIs != null) {
            for (UserROI roi : cellROIs) {
                String className = roi.getAssignedClass();
                if (className != null && !className.trim().isEmpty() && !"Unclassified".equals(className)) {
                    return className;
                }
            }
        }
        return "Unclassified";
    }

    /**
     * Data class to hold categorized ROI data.
     */
    public static class ROICategorizedData {
        public final List<UserROI> cells = new ArrayList<>();
        public final List<UserROI> nuclei = new ArrayList<>();
        public final List<UserROI> cytoplasm = new ArrayList<>();
        public final List<UserROI> vessel = new ArrayList<>();
        public final List<UserROI> ignored = new ArrayList<>();

        public final List<UserROI> classifiedCells = new ArrayList<>();
        public final List<UserROI> classifiedNuclei = new ArrayList<>();
        public final List<UserROI> classifiedCytoplasms = new ArrayList<>();
    }
}