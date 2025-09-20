package com.scipath.scipathj.ui.dataset.model;

/**
 * Tracks progress state for long-running operations like ROI loading.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProgressState {
    private int current;
    private int total;
    private String operation;
    private String details;
    private boolean cancelled;

    /**
     * Creates a new ProgressState with default values.
     */
    public ProgressState() {
        this(0, 0, null, null, false);
    }

    /**
     * Creates a new ProgressState with specified values.
     */
    public ProgressState(int current, int total, String operation, String details, boolean cancelled) {
        this.current = Math.max(0, current);
        this.total = Math.max(0, total);
        this.operation = operation;
        this.details = details;
        this.cancelled = cancelled;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = Math.max(0, current);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = Math.max(0, total);
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gets the progress percentage (0.0 to 1.0).
     */
    public double getProgressPercentage() {
        if (total == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) current / total);
    }

    /**
     * Checks if the operation is complete.
     */
    public boolean isComplete() {
        return current >= total && total > 0;
    }

    /**
     * Updates both current and total values.
     */
    public void update(int current, int total) {
        setCurrent(current);
        setTotal(total);
    }

    /**
     * Creates a copy of this progress state.
     */
    public ProgressState copy() {
        return new ProgressState(current, total, operation, details, cancelled);
    }

    /**
     * Resets the progress to initial state.
     */
    public void reset() {
        current = 0;
        total = 0;
        operation = null;
        details = null;
        cancelled = false;
    }

    @Override
    public String toString() {
        return String.format("ProgressState{current=%d, total=%d, operation='%s', percentage=%.1f%%, cancelled=%s}",
                           current, total, operation, getProgressPercentage() * 100, cancelled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProgressState other)) {
            return false;
        }
        return current == other.current &&
               total == other.total &&
               cancelled == other.cancelled &&
               java.util.Objects.equals(operation, other.operation) &&
               java.util.Objects.equals(details, other.details);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(current, total, operation, details, cancelled);
    }
}