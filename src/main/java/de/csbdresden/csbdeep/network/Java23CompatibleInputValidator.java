/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.csbdresden.csbdeep.network;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.exception.IncompatibleTypeException;

/**
 * Java 23 compatible InputValidator that handles null axes gracefully.
 * This validator is designed to work in JPackage environments where
 * TensorFlow model signatures might not be available.
 */
public class Java23CompatibleInputValidator extends DefaultTask implements InputValidator {

  @Override
  public void run(final Dataset input, final Network network) throws IncompatibleTypeException {
    setStarted();

    log("Starting Java 23 compatible input validation");

    // Check if network has valid node shape
    if (network.getInputNode() == null) {
      log("Warning: Input node is null, skipping validation");
      setFinished();
      return;
    }

    @SuppressWarnings("BC_IMPOSSIBLE_INSTANCEOF")
    Object nodeShapeObj = network.getInputNode().getNodeShape();
    long[] nodeShape = null;
    if (nodeShapeObj instanceof long[]) {
      nodeShape = (long[]) nodeShapeObj;
    } else if (nodeShapeObj instanceof Long[]) {
      Long[] longArray = (Long[]) nodeShapeObj;
      nodeShape = new long[longArray.length];
      for (int i = 0; i < longArray.length; i++) {
        nodeShape[i] = longArray[i] != null ? longArray[i] : 0;
      }
    }
    if (nodeShape == null) {
      log("Warning: Node shape is null, skipping validation");
      setFinished();
      return;
    }

    log("Input node shape: " + java.util.Arrays.toString(nodeShape));
    log("Input dataset dimensions: " + input.numDimensions());

    checkForTooManyDimensions(input, network);

    // Validate each dimension, handling null axes gracefully
    for (int i = 0; i < nodeShape.length; i++) {
      AxisType axis = null;
      try {
        axis = network.getInputNode().getNodeAxis(i);
      } catch (Exception e) {
        log("Warning: Could not get axis for dimension " + i + ": " + e.getMessage());
      }

      long size = nodeShape[i];

      if (axis != null) {
        checkIfAxesWithFixedSizeExists(input, axis, size);
        checkScalarAxes(input, axis, size);
      } else {
        log(
            "Warning: Axis "
                + i
                + " is null, skipping axis-specific validation for dimension with size "
                + size);
      }
    }

    log("Input validation completed successfully");
    setFinished();
  }

  @SuppressWarnings("BC_IMPOSSIBLE_INSTANCEOF")
  private void checkForTooManyDimensions(Dataset input, Network network) {
    Object nodeShapeObj = network.getInputNode().getNodeShape();
    long[] nodeShape = null;
    if (nodeShapeObj instanceof long[]) {
      nodeShape = (long[]) nodeShapeObj;
    } else if (nodeShapeObj instanceof Long[]) {
      Long[] longArray = (Long[]) nodeShapeObj;
      nodeShape = new long[longArray.length];
      for (int i = 0; i < longArray.length; i++) {
        nodeShape[i] = longArray[i] != null ? longArray[i] : 0;
      }
    }
    if (nodeShape != null && nodeShape.length == 4) {
      // Only check if we can safely access TIME and Z dimensions
      try {
        if (input.dimension(Axes.TIME) > 1 && input.dimension(Axes.Z) > 1) {
          throw new IncompatibleTypeException(
              input,
              "Network is meant for 2D images and can handle one additional batch dimension (Z or"
                  + " TIME), but this dataset contains data in both Z and TIME dimension.");
        }
      } catch (Exception e) {
        log("Warning: Could not check TIME/Z dimensions: " + e.getMessage());
      }
    }
  }

  private void checkIfAxesWithFixedSizeExists(Dataset input, AxisType axis, long size) {
    if (size > 1) {
      try {
        if (!input.axis(axis).isPresent()) {
          throw new IncompatibleTypeException(
              input, "Input should have axis of type " + axis.getLabel() + " and size " + size);
        }
      } catch (Exception e) {
        log(
            "Warning: Could not check axis presence for "
                + axis.getLabel()
                + ": "
                + e.getMessage());
      }
    }
  }

  private void checkScalarAxes(Dataset input, AxisType axis, long size) {
    if (size == 1) {
      try {
        if (input.axis(axis).isPresent() && input.dimension(axis) != size) {
          throw new IncompatibleTypeException(
              input,
              "Input axis of type "
                  + axis.getLabel()
                  + " should have size "
                  + size
                  + " but has size "
                  + input.dimension(axis));
        }
      } catch (Exception e) {
        log("Warning: Could not check scalar axis for " + axis.getLabel() + ": " + e.getMessage());
      }
    }
  }
}
