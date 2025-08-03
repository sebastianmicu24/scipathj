package com.scipath.scipathj.ui.utils;

import ij.ImagePlus;
import ij.io.Opener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for loading and processing images in various formats supported by ImageJ/Fiji.
 * 
 * <p>This class provides methods to load images from files, create thumbnails,
 * and filter image files based on supported formats.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ImageLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageLoader.class);
    
    /**
     * Supported image file extensions (ImageJ/Fiji compatible formats).
     */
    public static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // Common formats
        "jpg", "jpeg", "png", "gif", "bmp",
        // TIFF formats
        "tif", "tiff",
        // Scientific formats
        "lsm", "czi", "nd2", "oib", "oif", "vsi",
        // Microscopy formats
        "ims", "lif", "scn", "svs", "ndpi",
        // Raw formats
        "raw", "cr2", "nef", "dng",
        // Other ImageJ supported formats
        "fits", "pgm", "ppm", "pbm", "dcm", "dicom"
    ));
    
    /**
     * Maximum thumbnail size in pixels.
     */
    private static final int MAX_THUMBNAIL_SIZE = 150;
    
    /**
     * Loads an image from the specified file path using ImageJ.
     * 
     * @param filePath the path to the image file
     * @return ImagePlus object, or null if loading failed
     */
    public static ImagePlus loadImage(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LOGGER.warn("Invalid file path provided: {}", filePath);
            return null;
        }
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("File does not exist or is not a file: {}", filePath);
            return null;
        }
        
        if (!isImageFile(file)) {
            LOGGER.debug("File is not a supported image format: {}", filePath);
            return null;
        }
        
        try {
            LOGGER.debug("Loading image: {}", filePath);
            Opener opener = new Opener();
            ImagePlus image = opener.openImage(filePath);
            
            if (image == null) {
                LOGGER.warn("Failed to load image: {}", filePath);
                return null;
            }
            
            LOGGER.debug("Successfully loaded image: {} ({}x{})", 
                        filePath, image.getWidth(), image.getHeight());
            return image;
            
        } catch (Exception e) {
            LOGGER.error("Error loading image: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * Creates a thumbnail image from an ImagePlus object.
     * 
     * @param imagePlus the source image
     * @param maxSize maximum size for the thumbnail (width or height)
     * @return BufferedImage thumbnail, or null if creation failed
     */
    public static BufferedImage createThumbnail(ImagePlus imagePlus, int maxSize) {
        if (imagePlus == null) {
            return null;
        }
        
        try {
            // Get the original image
            Image originalImage = imagePlus.getImage();
            if (originalImage == null) {
                return null;
            }
            
            // Calculate thumbnail dimensions
            int originalWidth = imagePlus.getWidth();
            int originalHeight = imagePlus.getHeight();
            
            double scale = Math.min(
                (double) maxSize / originalWidth,
                (double) maxSize / originalHeight
            );
            
            int thumbnailWidth = (int) (originalWidth * scale);
            int thumbnailHeight = (int) (originalHeight * scale);
            
            // Create scaled image
            Image scaledImage = originalImage.getScaledInstance(
                thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH);
            
            // Convert to BufferedImage
            BufferedImage thumbnail = new BufferedImage(
                thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                               RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                               RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();
            
            return thumbnail;
            
        } catch (Exception e) {
            LOGGER.error("Error creating thumbnail for image: {}", imagePlus.getTitle(), e);
            return null;
        }
    }
    
    /**
     * Creates a thumbnail image from a file path.
     * 
     * @param filePath the path to the image file
     * @return BufferedImage thumbnail, or null if creation failed
     */
    public static BufferedImage createThumbnail(String filePath) {
        ImagePlus imagePlus = loadImage(filePath);
        if (imagePlus == null) {
            return null;
        }
        
        BufferedImage thumbnail = createThumbnail(imagePlus, MAX_THUMBNAIL_SIZE);
        
        // Clean up ImagePlus to free memory
        imagePlus.close();
        
        return thumbnail;
    }
    
    /**
     * Creates a default error thumbnail for images that failed to load.
     * 
     * @param width thumbnail width
     * @param height thumbnail height
     * @return BufferedImage with error indication
     */
    public static BufferedImage createErrorThumbnail(int width, int height) {
        BufferedImage errorImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = errorImage.createGraphics();
        
        // Set rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fill background
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, width, height);
        
        // Draw border
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(0, 0, width - 1, height - 1);
        
        // Draw error icon (X)
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        int margin = width / 4;
        g2d.drawLine(margin, margin, width - margin, height - margin);
        g2d.drawLine(width - margin, margin, margin, height - margin);
        
        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "Error";
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = height - 10;
        g2d.drawString(text, textX, textY);
        
        g2d.dispose();
        return errorImage;
    }
    
    /**
     * Checks if a file is a supported image format.
     * 
     * @param file the file to check
     * @return true if the file is a supported image format
     */
    public static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return false;
        }
        
        String extension = fileName.substring(lastDotIndex + 1);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
    
    /**
     * Filters an array of files to include only supported image files.
     * 
     * @param files array of files to filter
     * @return array containing only image files
     */
    public static File[] filterImageFiles(File[] files) {
        if (files == null) {
            return new File[0];
        }
        
        return Arrays.stream(files)
                .filter(ImageLoader::isImageFile)
                .toArray(File[]::new);
    }
    
    /**
     * Gets the file extension from a file name.
     * 
     * @param fileName the file name
     * @return the extension (without dot), or empty string if no extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * Formats file size in human-readable format.
     * 
     * @param bytes file size in bytes
     * @return formatted file size string
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}