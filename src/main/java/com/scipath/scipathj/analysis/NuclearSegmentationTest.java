package com.scipath.scipathj.analysis;

import com.scipath.scipathj.data.model.NucleusROI;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Simple test class for nuclear segmentation functionality.
 * Creates synthetic H&E-like images and tests the segmentation pipeline.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class NuclearSegmentationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NuclearSegmentationTest.class);
    
    /**
     * Test the simplified nuclear segmentation with a synthetic H&E-like image.
     */
    public static void testSimplifiedSegmentation() {
        LOGGER.info("Starting nuclear segmentation test");
        
        try {
            // Create a synthetic H&E-like test image
            ImagePlus testImage = createSyntheticHEImage();
            
            // Test direct segmentation
            testDirectSegmentation(testImage);
            
            // Test full segmentation pipeline
            testFullSegmentationPipeline(testImage);
            
            LOGGER.info("Nuclear segmentation test completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Nuclear segmentation test failed", e);
        }
    }
    
    /**
     * Test the simple H&E StarDist segmentation approach.
     */
    private static void testDirectSegmentation(ImagePlus testImage) {
        LOGGER.info("Testing simple H&E StarDist segmentation");
        
        try {
            SimpleHENuclearSegmentation simpleHESegmentation =
                new SimpleHENuclearSegmentation(testImage, "test_image.tif");
            
            // Check availability
            boolean isAvailable = simpleHESegmentation.isAvailable();
            LOGGER.info("Simple H&E segmentation available: {}", isAvailable);
            
            if (isAvailable) {
                // Perform segmentation
                List<NucleusROI> nuclei = simpleHESegmentation.segmentNuclei();
                
                // Log results
                LOGGER.info("Simple H&E segmentation results: {} nuclei detected", nuclei.size());
                LOGGER.info("Statistics: {}", simpleHESegmentation.getStatistics(nuclei));
                
                // Log individual nuclei
                for (int i = 0; i < Math.min(5, nuclei.size()); i++) {
                    NucleusROI nucleus = nuclei.get(i);
                    LOGGER.info("Nucleus {}: {} (area: {:.1f} px, circularity: {:.3f})",
                               i + 1, nucleus.getName(), nucleus.getNucleusArea(), nucleus.getCircularity());
                }
            }
            
            // Cleanup
            simpleHESegmentation.close();
            
        } catch (Exception e) {
            LOGGER.error("Simple H&E segmentation test failed", e);
        }
    }
    
    /**
     * Test the full segmentation pipeline with fallback.
     */
    private static void testFullSegmentationPipeline(ImagePlus testImage) {
        LOGGER.info("Testing full nuclear segmentation pipeline");
        
        NucleusSegmentation segmentation = null;
        
        try {
            segmentation = new NucleusSegmentation(testImage, "test_image.tif");
            
            // Check availability
            boolean isAvailable = segmentation.isStarDistAvailable();
            LOGGER.info("StarDist available: {}", isAvailable);
            LOGGER.info("Status: {}", segmentation.getStarDistStatus());
            
            if (isAvailable) {
                // Perform segmentation
                List<NucleusROI> nuclei = segmentation.segmentNuclei();
                
                // Log results
                LOGGER.info("Full pipeline results: {} nuclei detected", nuclei.size());
                LOGGER.info("Statistics: {}", segmentation.getNucleiStatistics());
                
                // Test additional methods
                LOGGER.info("Nuclei count: {}", segmentation.getNucleiCount());
                
                // Log individual nuclei
                for (int i = 0; i < Math.min(3, nuclei.size()); i++) {
                    NucleusROI nucleus = nuclei.get(i);
                    LOGGER.info("Nucleus {}: {} (method: {}, notes: {})",
                               i + 1, nucleus.getName(), nucleus.getSegmentationMethod(), nucleus.getNotes());
                }
            }
            
        } catch (NucleusSegmentation.NucleusSegmentationException e) {
            LOGGER.error("Full pipeline segmentation failed", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in full pipeline test", e);
        } finally {
            if (segmentation != null) {
                segmentation.close();
            }
        }
    }
    
    /**
     * Create a synthetic H&E-like image for testing.
     * Simulates nuclei as dark purple/blue circular regions on a pink background.
     */
    private static ImagePlus createSyntheticHEImage() {
        LOGGER.debug("Creating synthetic H&E-like test image");
        
        int width = 512;
        int height = 512;
        
        // Create 8-bit grayscale image (typical for nuclear segmentation)
        ImagePlus image = new ImagePlus("Synthetic_HE_Test", 
                                       new ij.process.ByteProcessor(width, height));
        
        ImageProcessor processor = image.getProcessor();
        
        // Fill with light background (simulating H&E cytoplasm/stroma)
        processor.setColor(200); // Light gray background
        processor.fill();
        
        // Add synthetic nuclei as dark circular regions
        Random random = new Random(42); // Fixed seed for reproducible results
        int numNuclei = 15 + random.nextInt(10); // 15-25 nuclei
        
        processor.setColor(60); // Dark gray for nuclei
        
        for (int i = 0; i < numNuclei; i++) {
            // Random position (avoid edges)
            int x = 50 + random.nextInt(width - 100);
            int y = 50 + random.nextInt(height - 100);
            
            // Random size (typical nucleus size range)
            int radius = 8 + random.nextInt(12); // 8-20 pixel radius
            
            // Draw filled circle (nucleus)
            processor.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            
            // Add some texture/noise to make it more realistic
            for (int j = 0; j < 5; j++) {
                int noiseX = x + random.nextInt(radius) - radius/2;
                int noiseY = y + random.nextInt(radius) - radius/2;
                if (noiseX >= 0 && noiseX < width && noiseY >= 0 && noiseY < height) {
                    int currentValue = processor.getPixel(noiseX, noiseY);
                    processor.putPixel(noiseX, noiseY, Math.max(40, currentValue - 10 + random.nextInt(20)));
                }
            }
        }
        
        // Add some background noise
        for (int i = 0; i < 1000; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int currentValue = processor.getPixel(x, y);
            processor.putPixel(x, y, Math.max(0, Math.min(255, currentValue + random.nextInt(20) - 10)));
        }
        
        LOGGER.debug("Created synthetic H&E image: {}x{} pixels with {} synthetic nuclei", 
                    width, height, numNuclei);
        
        return image;
    }
    
    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        LOGGER.info("Nuclear Segmentation Test - Standalone Execution");
        
        try {
            testSimplifiedSegmentation();
            LOGGER.info("All tests completed successfully");
        } catch (Exception e) {
            LOGGER.error("Test execution failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Quick availability check without full segmentation.
     */
    public static boolean checkStarDistAvailability() {
        try {
            // Create minimal test image
            ImagePlus testImage = new ImagePlus("test", new ij.process.ByteProcessor(100, 100));
            
            SimpleHENuclearSegmentation simpleHESegmentation =
                new SimpleHENuclearSegmentation(testImage, "availability_test.tif");
            
            boolean available = simpleHESegmentation.isAvailable();
            simpleHESegmentation.close();
            
            LOGGER.info("StarDist availability check: {}", available);
            return available;
            
        } catch (Exception e) {
            LOGGER.warn("StarDist availability check failed", e);
            return false;
        }
    }
}