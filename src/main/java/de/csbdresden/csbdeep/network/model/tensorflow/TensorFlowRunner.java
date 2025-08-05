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

package de.csbdresden.csbdeep.network.model.tensorflow;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TensorFlowRunner {

	/*
	 * runs graph on input tensor
	 *
	 */
	public static Tensor executeGraph(final SavedModelBundle model,
		final Tensor image, final String inputTensorName,
		final String outputTensorName) throws IllegalArgumentException, ExecutionException
	{

		final Tensor output_t = model.session().runner() //
			.feed(inputTensorName, image) //
			.fetch(outputTensorName) //
			.run().get(0);

		if (output_t != null) {

			if (output_t.shape().numDimensions() == 0) {
				showError("Output tensor has no dimensions");
				throw new ExecutionException("Output tensor has no dimensions", null);
			}
		}
		else {
			throw new NullPointerException("Output tensor is null");
		}
		return output_t;
	}

	/**
	 * Overloaded method for Session-based execution (TensorFlow 2.x compatibility)
	 * with timeout support to prevent infinite blocking
	 */
	public static Tensor executeGraph(final Session session,
		final Tensor image, final String inputTensorName,
		final String outputTensorName) throws IllegalArgumentException, ExecutionException
	{
		System.out.println("Executing TensorFlow 2.x with timeout for output: " + outputTensorName);
		System.out.println("Image tensor shape: " + image.shape().toString());
		
		// Forza garbage collection prima dell'esecuzione per liberare memoria
		System.gc();
		
		// Mostra memoria disponibile
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = totalMemory - freeMemory;
		
		System.out.println("Memory status before TensorFlow execution:");
		System.out.println("  Max memory: " + (maxMemory / 1024 / 1024) + " MB");
		System.out.println("  Used memory: " + (usedMemory / 1024 / 1024) + " MB");
		System.out.println("  Free memory: " + (freeMemory / 1024 / 1024) + " MB");
		
		if (usedMemory > maxMemory * 0.8) {
			System.out.println("WARNING: Memory usage is high (" + (usedMemory * 100 / maxMemory) + "%)");
		}
		
		try {
			// Esegui con timeout per evitare blocchi infiniti
			CompletableFuture<Tensor> future = CompletableFuture.supplyAsync(() -> {
				try {
					System.out.println("Starting session.runner() execution...");
					long startTime = System.currentTimeMillis();
					
					// Configura opzioni per ridurre uso memoria
					Tensor result = session.runner()
						.feed(inputTensorName, image)
						.fetch(outputTensorName)
						.run().get(0);
					
					long endTime = System.currentTimeMillis();
					System.out.println("TensorFlow execution completed in " + (endTime - startTime) + " ms");
					
					return result;
				} catch (OutOfMemoryError oom) {
					System.out.println("=== TENSORFLOW OUTOFMEMORYERROR DIAGNOSTICS ===");
					System.out.println("OutOfMemoryError during TensorFlow execution!");
					
					// Detailed memory analysis
					Runtime runtimeOOM = Runtime.getRuntime();
					long maxMemoryOOM = runtimeOOM.maxMemory();
					long totalMemoryOOM = runtimeOOM.totalMemory();
					long freeMemoryOOM = runtimeOOM.freeMemory();
					long usedMemoryOOM = totalMemoryOOM - freeMemoryOOM;
					
					System.out.println("JVM Memory Status:");
					System.out.println("  Max memory: " + (maxMemoryOOM / 1024 / 1024) + " MB");
					System.out.println("  Total memory: " + (totalMemoryOOM / 1024 / 1024) + " MB");
					System.out.println("  Used memory: " + (usedMemoryOOM / 1024 / 1024) + " MB");
					System.out.println("  Free memory: " + (freeMemoryOOM / 1024 / 1024) + " MB");
					System.out.println("  Memory usage: " + (usedMemoryOOM * 100 / maxMemoryOOM) + "%");
					
					System.out.println("Tensor Information:");
					System.out.println("  Input tensor shape: " + image.shape().toString());
					System.out.println("  Input tensor dataType: " + image.dataType());
					
					// Calculate tensor memory usage
					long tensorElements = 1;
					for (int i = 0; i < image.shape().numDimensions(); i++) {
						tensorElements *= image.shape().size(i);
					}
					long tensorMemoryMB = (tensorElements * 4) / 1024 / 1024; // 4 bytes per float
					System.out.println("  Estimated tensor memory: " + tensorMemoryMB + " MB");
					
					System.out.println("Possible Solutions:");
					System.out.println("  1. This is likely a TensorFlow native memory issue, not JVM heap");
					System.out.println("  2. TensorFlow 2.x may require more native memory than TensorFlow 1.x");
					System.out.println("  3. Try reducing image size or implementing tiling");
					System.out.println("  4. Check TensorFlow memory configuration");
					System.out.println("=== END DIAGNOSTICS ===");
					
					throw new RuntimeException("TensorFlow native OutOfMemoryError - not JVM heap issue", oom);
				} catch (Exception e) {
					System.out.println("Exception in TensorFlow execution: " + e.getMessage());
					throw new RuntimeException(e);
				}
			});
			
			// Timeout di 60 secondi (era molto più veloce con TF 1.x)
			final Tensor output_t = future.get(60, TimeUnit.SECONDS);
			
			System.out.println("TensorFlow execution successful!");
			return validateAndReturnOutput(output_t);
			
		} catch (TimeoutException timeoutEx) {
			System.out.println("TIMEOUT after 60 seconds - TensorFlow 2.x performance issue detected");
			System.out.println("This was much faster with TensorFlow 1.x - consider optimization");
			throw new ExecutionException("TensorFlow execution timeout after 60 seconds", timeoutEx);
		} catch (Exception e) {
			System.out.println("TensorFlow execution failed: " + e.getMessage());
			throw new ExecutionException("TensorFlow execution failed", e);
		}
	}
	private static Tensor validateAndReturnOutput(Tensor output_t) throws ExecutionException {

		if (output_t != null) {
			if (output_t.shape().numDimensions() == 0) {
				showError("Output tensor has no dimensions");
				throw new ExecutionException("Output tensor has no dimensions", null);
			}
		}
		else {
			throw new NullPointerException("Output tensor is null");
		}

		return output_t;
	}
	
	/**
		* Helper method to inspect available operations in the TensorFlow graph
		*/
	private static void inspectGraphOperations(Session session) {
		try {
			// Try to get graph operations using reflection or direct access
			System.out.println("=== AVAILABLE TENSORFLOW OPERATIONS ===");
			
			// Try some common inspection methods
			try {
				// Method 1: Try to run with a dummy fetch to see error details
				session.runner().run();
			} catch (Exception e1) {
				System.out.println("Graph inspection method 1 failed: " + e1.getMessage());
			}
			
			// Method 2: Try common serving signatures
			String[] servingSignatures = {
				"serving_default",
				"predict",
				"inference",
				"__saved_model_init_op"
			};
			
			for (String sig : servingSignatures) {
				try {
					System.out.println("Checking signature: " + sig);
					// This will fail but might give us useful error info
					session.runner().fetch(sig + ":0").run();
				} catch (Exception e2) {
					if (e2.getMessage() != null && e2.getMessage().contains("No Operation named")) {
						System.out.println("  - " + sig + " not found");
					} else {
						System.out.println("  - " + sig + " error: " + e2.getMessage());
					}
				}
			}
			
			System.out.println("=== END GRAPH INSPECTION ===");
			
		} catch (Exception e) {
			System.out.println("Graph inspection failed: " + e.getMessage());
		}
	}
	/**
	 * Helper method to clean tensor names (removes :0 suffix if present)
	 */
	public static String cleanTensorName(final String tensorName) {
		if (tensorName != null && tensorName.endsWith(":0")) {
			return tensorName.substring(0, tensorName.lastIndexOf(":0"));
		}
		return tensorName;
	}

	public static void showError(final String errorMsg) {
		JOptionPane.showMessageDialog(null, errorMsg, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

}
