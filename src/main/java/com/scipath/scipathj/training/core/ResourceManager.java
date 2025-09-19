package com.scipath.scipathj.training.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resource manager for automatic cleanup of AutoCloseable resources.
 * Ensures proper resource disposal even when exceptions occur.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class ResourceManager implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);
    
    private final List<AutoCloseable> resources = new ArrayList<>();
    
    /**
     * Manages a resource by adding it to the cleanup list.
     * 
     * @param resource resource to manage
     * @param <T> resource type
     * @return the same resource for chaining
     */
    public <T extends AutoCloseable> T manage(T resource) {
        if (resource != null) {
            resources.add(resource);
            LOGGER.debug("Managing resource: {}", resource.getClass().getSimpleName());
        }
        return resource;
    }
    
    /**
     * Manages a resource that might not implement AutoCloseable.
     * Wraps it in a lambda for cleanup.
     * 
     * @param resource resource to manage
     * @param cleanup cleanup action
     * @param <T> resource type
     * @return the resource
     */
    public <T> T manage(T resource, Runnable cleanup) {
        if (resource != null && cleanup != null) {
            resources.add(() -> cleanup.run());
            LOGGER.debug("Managing resource with custom cleanup: {}", resource.getClass().getSimpleName());
        }
        return resource;
    }
    
    /**
     * Gets the number of managed resources.
     */
    public int getManagedResourceCount() {
        return resources.size();
    }
    
    @Override
    public void close() {
        if (resources.isEmpty()) {
            return;
        }
        
        LOGGER.debug("Closing {} managed resources", resources.size());
        
        // Close resources in reverse order (LIFO - last in, first out)
        Collections.reverse(resources);
        
        int successCount = 0;
        int errorCount = 0;
        
        for (AutoCloseable resource : resources) {
            try {
                resource.close();
                successCount++;
                LOGGER.trace("Successfully closed resource: {}", resource.getClass().getSimpleName());
            } catch (Exception e) {
                errorCount++;
                LOGGER.warn("Error closing resource: {}", resource.getClass().getSimpleName(), e);
            }
        }
        
        resources.clear();
        
        if (errorCount > 0) {
            LOGGER.warn("Resource cleanup completed: {} successful, {} errors", successCount, errorCount);
        } else {
            LOGGER.debug("Resource cleanup completed: {} resources closed successfully", successCount);
        }
    }
}