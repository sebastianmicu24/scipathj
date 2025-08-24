package com.scipath.scipathj.infrastructure.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of pipeline validation
 */
public class ValidationResult {
  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  public ValidationResult(boolean valid) {
    this.valid = valid;
    this.errors = new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
    this.valid = valid;
    this.errors = new ArrayList<>(errors);
    this.warnings = new ArrayList<>(warnings);
  }

  public static ValidationResult success() {
    return new ValidationResult(true);
  }

  public static ValidationResult failure(String error) {
    ValidationResult result = new ValidationResult(false);
    result.addError(error);
    return result;
  }

  public static ValidationResult failure(List<String> errors) {
    return new ValidationResult(false, errors, new ArrayList<>());
  }

  public void addError(String error) {
    errors.add(error);
  }

  public void addWarning(String warning) {
    warnings.add(warning);
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public List<String> getWarnings() {
    return new ArrayList<>(warnings);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ValidationResult{valid=").append(valid);
    if (!errors.isEmpty()) {
      sb.append(", errors=").append(errors.size());
    }
    if (!warnings.isEmpty()) {
      sb.append(", warnings=").append(warnings.size());
    }
    sb.append("}");
    return sb.toString();
  }
}
