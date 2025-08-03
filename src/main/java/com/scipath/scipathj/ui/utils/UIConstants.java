package com.scipath.scipathj.ui.utils;

import java.awt.*;

/**
 * Constants for consistent UI styling across the application.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UIConstants {
    
    // Prevent instantiation
    private UIConstants() {}
    
    // Common dimensions
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 900;
    public static final int THUMBNAIL_SIZE = 120;
    public static final int PANEL_WIDTH = 140;
    public static final int PANEL_HEIGHT = 160;
    public static final int GALLERY_WIDTH = 180;
    
    // Border radius
    public static final int BORDER_RADIUS = 12;
    public static final int LARGE_BORDER_RADIUS = 16;
    
    // Spacing
    public static final int TINY_SPACING = 5;
    public static final int SMALL_SPACING = 5;
    public static final int MEDIUM_SPACING = 10;
    public static final int LARGE_SPACING = 20;
    public static final int EXTRA_LARGE_SPACING = 30;
    
    // Button dimensions
    public static final Dimension BUTTON_SIZE = new Dimension(150, 40);
    public static final Dimension SMALL_BUTTON_SIZE = new Dimension(80, 30);
    public static final Dimension LARGE_BUTTON_SIZE = new Dimension(190, 45);
    
    // Colors
    public static final Color ACCENT_COLOR = new Color(70, 130, 180);
    public static final Color SELECTION_COLOR = new Color(70, 130, 180);
    public static final Color ERROR_COLOR = Color.RED;
    public static final Color WARNING_COLOR = Color.ORANGE;
    public static final Color BORDER_COLOR = new Color(200, 200, 200);
    
    // Font sizes
    public static final float TITLE_FONT_SIZE = 20f;
    public static final float SUBTITLE_FONT_SIZE = 18f;
    public static final float LARGE_FONT_SIZE = 16f;
    public static final float MEDIUM_FONT_SIZE = 15f;
    public static final float NORMAL_FONT_SIZE = 14f;
    public static final float SMALL_FONT_SIZE = 13f;
    public static final float TINY_FONT_SIZE = 12f;
    
    // Icon sizes
    public static final int ICON_SIZE_SMALL = 16;
    public static final int ICON_SIZE_MEDIUM = 24;
    public static final int ICON_SIZE_LARGE = 32;
    
    // ROI display settings
    public static final int ROI_STROKE_WIDTH = 2;
    public static final int SELECTION_STROKE_WIDTH = 3;
    public static final float[] DASH_PATTERN = {5.0f, 5.0f};
    public static final Color ROI_COLOR = new Color(255, 255, 0, 180); // Semi-transparent yellow
    public static final Color ROI_SELECTION_COLOR = new Color(255, 0, 0, 200); // Semi-transparent red
    // Vessel segmentation constants
    public static final int VESSEL_THRESHOLD = 220;
    public static final double MIN_VESSEL_SIZE = 50.0;  // minimum area in pixels
    public static final double MAX_VESSEL_SIZE = Double.MAX_VALUE;  // no maximum limit
    public static final double GAUSSIAN_BLUR_SIGMA = 2.0;  // Blur parameter to reduce noise
    public static final Color VESSEL_ROI_COLOR = Color.RED;  // Vessels displayed in red
    
    // Nuclear segmentation constants (StarDist)
    public static final String DEFAULT_STARDIST_MODEL = "Versatile (H&E nuclei)";
    public static final float DEFAULT_PROB_THRESHOLD = 0.5f;  // Probability threshold for nucleus detection
    public static final float DEFAULT_NMS_THRESHOLD = 0.4f;   // Non-maximum suppression threshold
    public static final float DEFAULT_PERCENTILE_LOW = 1.0f;  // Bottom percentile for normalization
    public static final float DEFAULT_PERCENTILE_HIGH = 99.8f; // Top percentile for normalization
    public static final int DEFAULT_N_TILES = 1;              // Number of tiles for processing
    public static final double MIN_NUCLEUS_SIZE = 10.0;       // Minimum nucleus area in pixels
    public static final double MAX_NUCLEUS_SIZE = 1000.0;     // Maximum nucleus area in pixels
    public static final Color NUCLEUS_ROI_COLOR = new Color(0, 255, 0, 128); // Semi-transparent green
    
    // Segmentation workflow constants
    public static final int SEGMENTATION_PROGRESS_UPDATE_INTERVAL = 100; // milliseconds
    public static final boolean DEFAULT_SHOW_SEGMENTATION_PROGRESS = false;
    public static final boolean DEFAULT_VERBOSE_SEGMENTATION = false;
}