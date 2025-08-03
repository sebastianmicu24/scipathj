package com.scipath.scipathj.data.model;

import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Region of Interest representing cell cytoplasm (derived from Voronoi tessellation)
 */
public class CytoplasmROI {
    private final Polygon boundary;
    private final NucleusROI nucleus;
    private final double area;
    private final double perimeter;
    private final Rectangle boundingBox;
    
    public CytoplasmROI(Polygon boundary, NucleusROI nucleus) {
        this.boundary = boundary;
        this.nucleus = nucleus;
        this.boundingBox = boundary.getBounds();
        this.area = calculateArea();
        this.perimeter = calculatePerimeter();
    }
    
    private double calculateArea() {
        // Area of cytoplasm = total area - nucleus area
        double totalArea = boundingBox.width * boundingBox.height * 0.785;
        return totalArea - nucleus.getArea();
    }
    
    private double calculatePerimeter() {
        // Simplified perimeter calculation
        return Math.PI * Math.sqrt(2 * (area + nucleus.getArea()) / Math.PI);
    }
    
    // Getters
    public Polygon getBoundary() { return boundary; }
    public NucleusROI getNucleus() { return nucleus; }
    public double getArea() { return area; }
    public double getPerimeter() { return perimeter; }
    public Rectangle getBoundingBox() { return boundingBox; }
    
    public double getCentroidX() {
        return boundingBox.getCenterX();
    }
    
    public double getCentroidY() {
        return boundingBox.getCenterY();
    }
    
    public double getCytoplasmToNucleusRatio() {
        return area / nucleus.getArea();
    }
}