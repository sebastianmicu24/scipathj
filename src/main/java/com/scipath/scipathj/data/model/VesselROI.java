package com.scipath.scipathj.data.model;

import ij.gui.Roi;
import java.awt.Color;
import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a vessel ROI detected through automated segmentation.
 * This class preserves the actual shape of the vessel as detected by ImageJ's ParticleAnalyzer.
 */
public class VesselROI {
    
    private final String id;
    private final String name;
    private final LocalDateTime createdAt;
    private final String imageFileName;
    private final Roi imageJRoi; // Store the actual ImageJ ROI with complex shape
    private Color displayColor;
    private String notes;
    
    /**
     * Creates a new VesselROI from an ImageJ ROI
     * 
     * @param imageJRoi The ImageJ ROI containing the vessel shape
     * @param imageFileName The name of the image this ROI belongs to
     * @param name Optional name for the ROI (can be null for auto-generated names)
     */
    public VesselROI(Roi imageJRoi, String imageFileName, String name) {
        this.id = UUID.randomUUID().toString();
        this.imageJRoi = (Roi) imageJRoi.clone(); // Create a copy to avoid external modifications
        this.imageFileName = imageFileName;
        this.name = name != null ? name : generateDefaultName();
        this.createdAt = LocalDateTime.now();
        this.displayColor = Color.RED; // Vessels displayed in red by default
        this.notes = "";
    }
    
    /**
     * Creates a vessel ROI with auto-generated name
     */
    public static VesselROI createFromImageJROI(Roi imageJRoi, String imageFileName, int vesselNumber) {
        String vesselName = "Vessel_" + vesselNumber;
        return new VesselROI(imageJRoi, imageFileName, vesselName);
    }
    
    private String generateDefaultName() {
        return "Vessel_" + System.currentTimeMillis() % 10000;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getImageFileName() { return imageFileName; }
    public Color getDisplayColor() { return displayColor; }
    public String getNotes() { return notes; }
    public Roi getImageJRoi() { return (Roi) imageJRoi.clone(); } // Return a copy to prevent external modifications
    
    // Setters for mutable properties
    public void setDisplayColor(Color color) { this.displayColor = color; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
    
    // Utility methods that delegate to the ImageJ ROI
    public Rectangle getBounds() { 
        return imageJRoi.getBounds(); 
    }
    
    public int getX() { return imageJRoi.getBounds().x; }
    public int getY() { return imageJRoi.getBounds().y; }
    public int getWidth() { return imageJRoi.getBounds().width; }
    public int getHeight() { return imageJRoi.getBounds().height; }
    public int getCenterX() { 
        Rectangle bounds = imageJRoi.getBounds();
        return bounds.x + bounds.width / 2; 
    }
    public int getCenterY() { 
        Rectangle bounds = imageJRoi.getBounds();
        return bounds.y + bounds.height / 2; 
    }
    
    /**
     * Get the actual area of the vessel (not just bounding box area)
     */
    public double getArea() {
        return imageJRoi.getStatistics().area;
    }
    
    /**
     * Get the perimeter of the vessel
     */
    public double getPerimeter() {
        return imageJRoi.getLength();
    }
    
    /**
     * Check if a point is contained within the actual vessel shape
     */
    public boolean contains(int x, int y) {
        return imageJRoi.contains(x, y);
    }
    
    /**
     * Check if this vessel intersects with a rectangle
     */
    public boolean intersects(Rectangle other) {
        return imageJRoi.getBounds().intersects(other);
    }
    
    /**
     * Get the type of ImageJ ROI (for display purposes)
     */
    public String getRoiType() {
        return imageJRoi.getTypeAsString();
    }
    
    @Override
    public String toString() {
        Rectangle bounds = getBounds();
        return String.format("%s [%s] [%d,%d %dx%d] area=%.1f on %s", 
                           name, getRoiType(), bounds.x, bounds.y, bounds.width, bounds.height, 
                           getArea(), imageFileName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VesselROI vesselROI = (VesselROI) obj;
        return id.equals(vesselROI.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}