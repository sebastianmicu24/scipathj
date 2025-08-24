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

import com.scipath.scipathj.infrastructure.utils.DirectFileLogger;
import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import java.io.FileNotFoundException;
import net.imagej.Dataset;

public class DefaultModelLoader extends DefaultTask implements ModelLoader {

  @Override
  public void run(
      final String modelName, final Network network, final String modelFileUrl, final Dataset input)
      throws FileNotFoundException {

    setStarted();

    if (!network.isInitialized()) {
      try {
        loadNetwork(modelName, network, modelFileUrl, input);
      } catch (FileNotFoundException e) {
        setFailed();
        throw e;
      }
      if (!network.isInitialized()) {
        setFailed();
        return;
      }
      network.preprocess();
    }

    setFinished();
  }

  protected void loadNetwork(
      final String modelName, final Network network, final String modelFileUrl, final Dataset input)
      throws FileNotFoundException {

    DirectFileLogger.logTensorFlow(
        "=== INIZIO CARICAMENTO MODELLO ===");
    DirectFileLogger.logTensorFlow("Model name: " + modelName);
    DirectFileLogger.logTensorFlow(
        "Model file URL: " + modelFileUrl);
    DirectFileLogger.logTensorFlow(
        "Network class: " + network.getClass().getSimpleName());
    DirectFileLogger.logTensorFlow(
        "Input presente: " + (input != null));

    if (modelFileUrl.isEmpty()) {
      DirectFileLogger.logTensorFlow(
          "ERROR: Model file URL è vuoto!");
      return;
    }

    DirectFileLogger.logTensorFlow(
        "Chiamando network.loadModel...");
    boolean loaded = network.loadModel(modelFileUrl, modelName);
    DirectFileLogger.logTensorFlow(
        "network.loadModel result: " + loaded);

    if (!loaded) {
      DirectFileLogger.logTensorFlow(
          "ERROR: network.loadModel ha fallito!");
      return;
    }

    DirectFileLogger.logTensorFlow("Caricando input node...");
    network.loadInputNode(input);
    DirectFileLogger.logTensorFlow("Input node caricato");

    DirectFileLogger.logTensorFlow("Caricando output node...");
    network.loadOutputNode(input);
    DirectFileLogger.logTensorFlow("Output node caricato");

    DirectFileLogger.logTensorFlow("Inizializzando mapping...");
    network.initMapping();
    DirectFileLogger.logTensorFlow("Mapping inizializzato");

    DirectFileLogger.logTensorFlow(
        "=== FINE CARICAMENTO MODELLO ===");
  }
}
