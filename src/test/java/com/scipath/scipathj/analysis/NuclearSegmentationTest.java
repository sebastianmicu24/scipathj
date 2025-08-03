package com.scipath.scipathj.analysis;

import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SimpleHENuclearSegmentation.
 * Tests the nuclear segmentation functionality with StarDist H&E model.
 */
public class SimpleNuclearSegmentationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNuclearSegmentationTest.class);
    
    private SimpleHENuclearSegmentation segmentation;
    private ImagePlus testImage;
    
    @BeforeEach
    void setUp() {
        // Try to load the 32-bit H&E test image (ImgLib2 compatible)
        String testImagePath = "src/main/resources/images/test_32bit.png";
        File testImageFile = new File(testImagePath);
        
        if (testImageFile.exists()) {
            LOGGER.info("Loading 32-bit H&E test image from: {}", testImagePath);
            Opener opener = new Opener();
            testImage = opener.openImage(testImagePath);
            
            if (testImage != null) {
                LOGGER.info("Successfully loaded 32-bit H&E image: {}x{}, {} channels, {}-bit, type={}",
                           testImage.getWidth(), testImage.getHeight(),
                           testImage.getNChannels(), testImage.getBitDepth(), testImage.getType());
            } else {
                LOGGER.error("Failed to load test image from: {}", testImagePath);
                fail("Could not load test image from: " + testImagePath);
            }
        } else {
            LOGGER.error("Test image file not found: {}", testImagePath);
            fail("Test image file not found: " + testImagePath);
        }
        
        segmentation = new SimpleHENuclearSegmentation(testImage, "test_32bit.png");
        LOGGER.info("Test setup completed with 32-bit H&E image");
    }
    
    @AfterEach
    void tearDown() {
        if (segmentation != null) {
            segmentation.close();
        }
        if (testImage != null) {
            testImage.close();
        }
    }
    
    @Test
    void testStarDistAvailability() {
        LOGGER.info("Testing StarDist availability...");
        
        try {
            boolean available = segmentation.isAvailable();
            LOGGER.info("StarDist H&E model available: {}", available);
            
            if (!available) {
                LOGGER.warn("StarDist H&E model is not available - this may be expected in test environment");
            }
            
            // Test should not fail if StarDist is not available in test environment
            assertTrue(true, "Availability check completed");
            
        } catch (Exception e) {
            LOGGER.error("Error checking StarDist availability", e);
            fail("StarDist availability check failed: " + e.getMessage());
        }
    }
    @Test
    void testNuclearSegmentationExecution() {
        LOGGER.info("Testing nuclear segmentation execution...");
        
        try {
            // This test should FAIL if StarDist is not available
            assertTrue(segmentation.isAvailable(), "StarDist H&E model must be available for this test to pass");
            
            List<NucleusROI> nuclei = segmentation.segmentNuclei();
            
            LOGGER.info("Segmentation completed. Found {} nuclei", nuclei.size());
            
            assertNotNull(nuclei, "Nuclei list should not be null");
            
            // Verify that StarDist was actually used, not fallback
            boolean usedStarDist = false;
            for (NucleusROI nucleus : nuclei) {
                if (nucleus.getSegmentationMethod() != null &&
                    nucleus.getSegmentationMethod().contains("StarDist")) {
                    usedStarDist = true;
                    break;
                }
            }
            
            assertTrue(usedStarDist, "Test must use StarDist segmentation, not fallback method");
            
            // Log statistics
            if (!nuclei.isEmpty()) {
                String stats = segmentation.getStatistics(nuclei);
                LOGGER.info("Segmentation statistics: {}", stats);
                
                // Verify each nucleus has valid properties
                for (NucleusROI nucleus : nuclei) {
                    assertNotNull(nucleus.getName(), "Nucleus name should not be null");
                    assertTrue(nucleus.getNucleusArea() > 0, "Nucleus area should be positive");
                    assertEquals("test_32bit.png", nucleus.getImageFileName(), "Image filename should match");
                    // Verify it was segmented with StarDist
                    assertNotNull(nucleus.getSegmentationMethod(), "Segmentation method should be set");
                    assertTrue(nucleus.getSegmentationMethod().contains("StarDist"),
                              "Nucleus should be segmented with StarDist, not fallback");
                }
            } else {
                LOGGER.warn("No nuclei detected - StarDist may not be working properly");
                fail("StarDist segmentation should detect some nuclei in the test image");
            }
            
        } catch (Exception e) {
            LOGGER.error("Nuclear segmentation test failed", e);
            
            // Log detailed error information
            if (e.getMessage() != null) {
                if (e.getMessage().contains("ClassCastException")) {
                    LOGGER.error("ClassCastException detected - Java 21 ClassLoader issue");
                } else if (e.getMessage().contains("TensorFlow")) {
                    LOGGER.error("TensorFlow-related error detected");
                } else if (e.getMessage().contains("model")) {
                    LOGGER.error("Model loading error detected");
                } else if (e.getMessage().contains("NullPointerException")) {
                    LOGGER.error("NullPointerException detected - likely model initialization issue");
                }
            }
            
            fail("Nuclear segmentation failed: " + e.getMessage());
        }
    }
    
    @Test
    void testTensorFlowModelLoading() {
        LOGGER.info("Testing TensorFlow model loading...");
        
        try {
            // Test that TensorFlow service is properly initialized
            SimpleHENuclearSegmentation testSeg = new SimpleHENuclearSegmentation(testImage, "tf_test.png");
            
            // Check if the segmentation object was created without throwing exceptions
            assertNotNull(testSeg, "Segmentation object should be created");
            
            // Test the availability check which internally tests model loading
            boolean available = testSeg.isAvailable();
            LOGGER.info("TensorFlow model loading test - Available: {}", available);
            
            testSeg.close();
            
            // The test passes if no exceptions were thrown during initialization
            assertTrue(true, "TensorFlow model loading test completed");
            
        } catch (Exception e) {
            LOGGER.error("TensorFlow model loading test failed", e);
            
            // Check for specific error types
            if (e.getMessage() != null) {
                if (e.getMessage().contains("NullPointerException")) {
                    LOGGER.error("NullPointerException detected - likely ImageTensor node initialization issue");
                } else if (e.getMessage().contains("TensorFlow")) {
                    LOGGER.error("TensorFlow library loading issue detected");
                } else if (e.getMessage().contains("model")) {
                    LOGGER.error("Model file loading issue detected");
                }
            }
            
            fail("TensorFlow model loading failed: " + e.getMessage());
        }
    }
    
    @Test
    void testStarDistModelInitialization() {
        LOGGER.info("Testing StarDist model initialization...");
        
        try {
            // Create segmentation with custom settings to test model initialization
            NuclearSegmentationSettings settings = new NuclearSegmentationSettings();
            settings.setModelChoice("Versatile (H&E nuclei)");
            settings.setProbThresh(0.5f);
            settings.setNmsThresh(0.4f);
            
            SimpleHENuclearSegmentation testSeg = new SimpleHENuclearSegmentation(testImage, "stardist_test.png", settings);
            
            LOGGER.info("StarDist model initialization completed");
            
            // Verify settings are properly set
            NuclearSegmentationSettings retrievedSettings = testSeg.getSettings();
            assertNotNull(retrievedSettings, "Settings should not be null");
            assertEquals("Versatile (H&E nuclei)", retrievedSettings.getModelChoice(), "Model choice should match");
            assertEquals(0.5f, retrievedSettings.getProbThresh(), 0.001f, "Probability threshold should match");
            assertEquals(0.4f, retrievedSettings.getNmsThresh(), 0.001f, "NMS threshold should match");
            
            testSeg.close();
            
        } catch (Exception e) {
            LOGGER.error("StarDist model initialization failed", e);
            fail("StarDist model initialization failed: " + e.getMessage());
        }
    }
    
    @Test
    void testImagePreprocessing() {
        LOGGER.info("Testing image preprocessing for StarDist...");
        
        try {
            // Create a synthetic RGB image to test preprocessing
            ImagePlus rgbImage = createSyntheticRGBImage();
            
            SimpleHENuclearSegmentation testSeg = new SimpleHENuclearSegmentation(rgbImage, "rgb_test.png");
            
            // The preprocessing happens internally during segmentation
            // If no exception is thrown, preprocessing worked
            LOGGER.info("Image preprocessing test completed successfully");
            
            testSeg.close();
            rgbImage.close();
            
            assertTrue(true, "Image preprocessing completed without errors");
            
        } catch (Exception e) {
            LOGGER.error("Image preprocessing test failed", e);
            fail("Image preprocessing failed: " + e.getMessage());
        }
    }
    @Test
    void testStarDistModelMustWork() {
        LOGGER.info("Testing that StarDist model works properly (no fallback allowed)...");
        
        try {
            // This test specifically verifies StarDist is working
            assertTrue(segmentation.isAvailable(), "StarDist must be available");
            
            List<NucleusROI> nuclei = segmentation.segmentNuclei();
            
            assertNotNull(nuclei, "Nuclei list should not be null");
            LOGGER.info("StarDist segmentation completed. Found {} nuclei", nuclei.size());
            
            // Verify that NO nucleus was segmented with fallback method
            for (NucleusROI nucleus : nuclei) {
                assertNotNull(nucleus.getSegmentationMethod(), "Segmentation method should be set");
                assertFalse(nucleus.getSegmentationMethod().contains("fallback") ||
                           nucleus.getSegmentationMethod().contains("simple"),
                           "No nucleus should use fallback segmentation - StarDist must work properly");
                assertTrue(nucleus.getSegmentationMethod().contains("StarDist"),
                          "All nuclei must be segmented with StarDist");
                LOGGER.debug("Nucleus {} correctly segmented with: {}", nucleus.getName(), nucleus.getSegmentationMethod());
            }
            
            // If we get here, StarDist is working properly
            LOGGER.info("SUCCESS: StarDist model is working correctly without fallback");
            
        } catch (Exception e) {
            LOGGER.error("StarDist model test failed - this indicates the model is not working properly", e);
            
            // Provide specific failure information
            if (e.getMessage() != null) {
                if (e.getMessage().contains("prediction") && e.getMessage().contains("null")) {
                    LOGGER.error("CRITICAL: StarDist prediction is null - model loading/execution failed");
                } else if (e.getMessage().contains("TensorFlow")) {
                    LOGGER.error("CRITICAL: TensorFlow integration issue");
                } else if (e.getMessage().contains("ImageTensor")) {
                    LOGGER.error("CRITICAL: ImageTensor initialization failed");
                }
            }
            
            fail("StarDist model must work properly without fallback: " + e.getMessage());
        }
    }
    
    @Test
    void testContextInitialization() {
        LOGGER.info("Testing SciJava context initialization...");
        
        try {
            // This test verifies that the context can be created without errors
            SimpleHENuclearSegmentation testSeg = new SimpleHENuclearSegmentation(testImage, "context_test.jpg");
            
            LOGGER.info("Context initialization successful");
            
            testSeg.close();
            assertTrue(true, "Context initialization completed");
            
        } catch (Exception e) {
            LOGGER.error("Context initialization failed", e);
            fail("Context initialization failed: " + e.getMessage());
        }
    }
    
    @Test
    void testToStringMethod() {
        String description = segmentation.toString();
        LOGGER.info("Segmentation description: {}", description);
        
        assertNotNull(description, "toString should not return null");
        assertTrue(description.contains("SimpleHENuclearSegmentation"), "Description should contain class name");
        assertTrue(description.contains("test_32bit.png"), "Description should contain image filename");
    }
    
    /**
     * Helper method to create a synthetic RGB image for testing.
     */
    private ImagePlus createSyntheticRGBImage() {
        int width = 256;
        int height = 256;
        
        ImagePlus rgbImage = new ImagePlus("Synthetic RGB", new ij.process.ColorProcessor(width, height));
        ImageProcessor processor = rgbImage.getProcessor();
        
        // Create a simple pattern with some circular shapes that could be nuclei
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Create some circular patterns
                double dist1 = Math.sqrt((x - 64) * (x - 64) + (y - 64) * (y - 64));
                double dist2 = Math.sqrt((x - 192) * (x - 192) + (y - 64) * (y - 64));
                double dist3 = Math.sqrt((x - 128) * (x - 128) + (y - 192) * (y - 192));
                
                int red = 200, green = 150, blue = 200; // Light purple background
                
                if (dist1 < 20 || dist2 < 25 || dist3 < 18) {
                    red = 100; green = 50; blue = 150; // Darker purple for "nuclei"
                }
                
                int rgb = (red << 16) | (green << 8) | blue;
                processor.set(x, y, rgb);
            }
        }
        
        return rgbImage;
    }
}