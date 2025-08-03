package com.scipath.scipathj.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event bus for application-wide event handling.
 * 
 * <p>This is a stub implementation for the initial skeleton version.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class EventBus {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);
    
    public EventBus() {
        LOGGER.debug("EventBus initialized (stub implementation)");
    }
}