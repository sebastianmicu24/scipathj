package com.scipath.scipathj.core.pipeline;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Metadata information about a pipeline
 */
public class PipelineMetadata {
    private final String id;
    private final String name;
    private final String description;
    private final String version;
    private final String author;
    private final LocalDateTime createdDate;
    private final String organType;
    private final String stainingType;
    private final Map<String, Object> customProperties;
    
    public PipelineMetadata(String id, String name, String description, String version, 
                           String author, String organType, String stainingType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.organType = organType;
        this.stainingType = stainingType;
        this.createdDate = LocalDateTime.now();
        this.customProperties = new HashMap<>();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public String getOrganType() { return organType; }
    public String getStainingType() { return stainingType; }
    public Map<String, Object> getCustomProperties() { return new HashMap<>(customProperties); }
    
    public void setCustomProperty(String key, Object value) {
        customProperties.put(key, value);
    }
    
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    public boolean hasCustomProperty(String key) {
        return customProperties.containsKey(key);
    }
    
    @Override
    public String toString() {
        return String.format("PipelineMetadata{id='%s', name='%s', version='%s', organ='%s', staining='%s'}", 
                           id, name, version, organType, stainingType);
    }
}