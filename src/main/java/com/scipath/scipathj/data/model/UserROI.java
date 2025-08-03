package com.scipath.scipathj.data.model;

import ij.gui.Roi;
import java.awt.Rectangle;
import java.awt.Color;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user-created Region of Interest (ROI) for manual selection and analysis.
 * This is different from analysis-generated ROIs like NucleusROI and CytoplasmROI.
 */
public class UserROI {
    
    public enum ROIType {
        RECTANGLE("Rectangle"),
        SQUARE("Square"),
        CIRCLE("Circle"),
        POLYGON("Polygon"),
        COMPLEX_SHAPE("Complex Shape"); // For vessel ROIs with complex shapes
        
        private final String displayName;
        
        ROIType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final String id;
    private final ROIType type;
    private final Rectangle bounds;
    private final String name;
    private final LocalDateTime createdAt;
    private final String imageFileName;
    private Color displayColor;
    private String notes;
    
    // For complex shapes (like vessels), store the actual ImageJ ROI
    private final Roi imageJRoi;
    
    /**
     * Creates a new UserROI with simple shape
     * @param type The type of ROI
     * @param bounds The bounding rectangle of the ROI
     * @param imageFileName The name of the image this ROI belongs to
     * @param name Optional name for the ROI (can be null for auto-generated names)
     */
    public UserROI(ROIType type, Rectangle bounds, String imageFileName, String name) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.bounds = new Rectangle(bounds); // Create defensive copy
        this.imageFileName = imageFileName;
        this.name = name != null ? name : generateDefaultName();
        this.createdAt = LocalDateTime.now();
        this.displayColor = generateDefaultColor();
        this.notes = "";
        this.imageJRoi = null; // No complex shape for simple ROIs
    }
    
    /**
     * Creates a new UserROI with complex shape (for vessels)
     * @param imageJRoi The ImageJ ROI containing the complex shape
     * @param imageFileName The name of the image this ROI belongs to
     * @param name Optional name for the ROI (can be null for auto-generated names)
     */
    public UserROI(Roi imageJRoi, String imageFileName, String name) {
        this.id = UUID.randomUUID().toString();
        this.type = ROIType.COMPLEX_SHAPE;
        this.bounds = imageJRoi.getBounds(); // Get bounds from ImageJ ROI
        this.imageFileName = imageFileName;
        this.name = name != null ? name : generateDefaultName();
        this.createdAt = LocalDateTime.now();
        this.displayColor = Color.RED; // Default color for vessels
        this.notes = "";
        this.imageJRoi = (Roi) imageJRoi.clone(); // Store a copy of the ImageJ ROI
    }
    
    /**
     * Creates a square ROI (convenience constructor)
     */
    public static UserROI createSquareROI(int x, int y, int size, String imageFileName) {
        return new UserROI(ROIType.SQUARE, new Rectangle(x, y, size, size), imageFileName, null);
    }
    
    /**
     * Creates a rectangle ROI (convenience constructor)
     */
    public static UserROI createRectangleROI(int x, int y, int width, int height, String imageFileName) {
        return new UserROI(ROIType.RECTANGLE, new Rectangle(x, y, width, height), imageFileName, null);
    }
    
    private String generateDefaultName() {
        return type.getDisplayName() + "_" + System.currentTimeMillis() % 10000;
    }
    
    private Color generateDefaultColor() {
        // Generate a bright, visible color for ROI display
        Color[] defaultColors = {
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.GREEN,
            Color.ORANGE,
            Color.PINK
        };
        return defaultColors[(int) (Math.random() * defaultColors.length)];
    }
    
    // Getters
    public String getId() { return id; }
    public ROIType getType() { return type; }
    public Rectangle getBounds() { return new Rectangle(bounds); } // Return defensive copy
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getImageFileName() { return imageFileName; }
    public Color getDisplayColor() { return displayColor; }
    public String getNotes() { return notes; }
    
    // Additional getter for complex shapes
    public Roi getImageJRoi() {
        return imageJRoi != null ? (Roi) imageJRoi.clone() : null;
    }
    
    public boolean hasComplexShape() {
        return imageJRoi != null;
    }
    
    // Setters for mutable properties
    public void setDisplayColor(Color color) { this.displayColor = color; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
    
    // Utility methods
    public int getX() { return bounds.x; }
    public int getY() { return bounds.y; }
    public int getWidth() { return bounds.width; }
    public int getHeight() { return bounds.height; }
    public int getCenterX() { return bounds.x + bounds.width / 2; }
    public int getCenterY() { return bounds.y + bounds.height / 2; }
    
    public double getArea() {
        if (hasComplexShape()) {
            return imageJRoi.getStatistics().area;
        }
        return bounds.width * bounds.height;
    }
    
    public boolean contains(int x, int y) {
        if (hasComplexShape()) {
            return imageJRoi.contains(x, y);
        }
        return bounds.contains(x, y);
    }
    
    public boolean intersects(Rectangle other) {
        return bounds.intersects(other);
    }
    
    @Override
    public String toString() {
        return String.format("%s [%d,%d %dx%d] on %s", 
                           name, bounds.x, bounds.y, bounds.width, bounds.height, imageFileName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserROI userROI = (UserROI) obj;
        return id.equals(userROI.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}