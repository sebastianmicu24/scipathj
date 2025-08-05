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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.tiling.AdvancedTiledView;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import com.scipath.scipathj.core.utils.DirectFileLogger;

public class DefaultModelExecutor<T extends RealType<T>> extends DefaultTask
	implements ModelExecutor<T>
{

	private static String PROGRESS_CANCELED = "Canceled";
	private ExecutorService pool = null;
	private Network network = null;
	private boolean canceled = false;

	@Override
	public List<AdvancedTiledView<T>> run(final List<AdvancedTiledView<T>> input,
		final Network network) throws OutOfMemoryError, ExecutionException {
		DirectFileLogger.logStarDist("INFO", "=== INIZIO ESECUZIONE MODELLO ===");
		DirectFileLogger.logStarDist("INFO", "Input presente: " + (input != null));
		DirectFileLogger.logStarDist("INFO", "Network presente: " + (network != null));
		DirectFileLogger.logStarDist("INFO", "Canceled: " + isCanceled());
		
		if (input != null) {
			DirectFileLogger.logStarDist("INFO", "Numero di input tiles: " + input.size());
		}
		
		if (network != null) {
			DirectFileLogger.logStarDist("INFO", "Network class: " + network.getClass().getSimpleName());
			DirectFileLogger.logStarDist("INFO", "Network library loaded: " + network.libraryLoaded());
		}
		
		if(!isCanceled()) {
			setStarted();
			this.network = network;
			if (input.size() > 0) {
				DirectFileLogger.logStarDist("INFO", "Logging dimensioni input...");
				DatasetHelper.logDim(this, "Network input size", input.get(0)
						.randomAccess().get());
			}

			setCurrentStep(0);
			network.resetTileCount();
			int numSteps = getSteps(input);
			DirectFileLogger.logStarDist("INFO", "Numero di steps calcolati: " + numSteps);
			setNumSteps(numSteps);

			DirectFileLogger.logStarDist("INFO", "Creazione thread pool...");
			pool = Executors.newWorkStealingPool();
			final List<AdvancedTiledView<T>> output = new ArrayList<>();
			
			DirectFileLogger.logStarDist("INFO", "Inizio elaborazione tiles...");
			for (int i = 0; i < input.size(); i++) {
				AdvancedTiledView<T> tile = input.get(i);
				DirectFileLogger.logStarDist("INFO", "Elaborazione tile " + (i + 1) + "/" + input.size());
				
				try {
					AdvancedTiledView<T> result = run(tile, network);
					if (result != null) {
						output.add(result);
						DirectFileLogger.logStarDist("INFO", "Tile " + (i + 1) + " elaborato con successo");
					} else {
						DirectFileLogger.logStarDist("ERROR", "ERRORE: Tile " + (i + 1) + " ha restituito null");
					}
				} catch (ExecutionException e) {
					DirectFileLogger.logStarDistException("ECCEZIONE durante elaborazione tile " + (i + 1), e);
					throw e;
				}
				if(isCanceled()) {
					DirectFileLogger.logStarDist("WARN", "Esecuzione cancellata durante elaborazione tile " + (i + 1));
					return null;
				}
			}
			
			DirectFileLogger.logStarDist("INFO", "Shutdown thread pool...");
			pool.shutdown();
			if(isCanceled()) {
				DirectFileLogger.logStarDist("WARN", "Esecuzione cancellata dopo elaborazione tiles");
				return null;
			}
			
			DirectFileLogger.logStarDist("INFO", "Output tiles prodotti: " + output.size());
			if (output.size() > 0) {
				DirectFileLogger.logStarDist("INFO", "Logging dimensioni output...");
				DatasetHelper.logDim(this, "Network output size", output.get(0)
						.getProcessedTiles().get(0));
			}
			setFinished();
			DirectFileLogger.logStarDist("INFO", "=== FINE ESECUZIONE MODELLO ===");
			return output;
		}
		DirectFileLogger.logStarDist("WARN", "Esecuzione saltata perché cancellata");
		return null;
	}

	private int getSteps(List<AdvancedTiledView<T>> input) {
		int numSteps = 0;
		for (AdvancedTiledView<T> tile : input) {
			int steps = 1;
			for (int i = 0; i < tile.numDimensions(); i++) {
				steps *= tile.dimension(i);
			}
			numSteps += steps;
		}
		return numSteps;
	}

	private AdvancedTiledView<T> run(final AdvancedTiledView<T> input,
		final Network network) throws OutOfMemoryError, IllegalArgumentException, ExecutionException {
		DirectFileLogger.logStarDist("DEBUG", "--- Inizio elaborazione singolo tile ---");
		DirectFileLogger.logStarDist("DEBUG", "Input tile presente: " + (input != null));
		DirectFileLogger.logStarDist("DEBUG", "Network presente: " + (network != null));

		if (input != null) {
			DirectFileLogger.logStarDist("DEBUG", "Input tile dimensions: " + input.numDimensions());
			long[] dims = new long[input.numDimensions()];
			input.dimensions(dims);
			DirectFileLogger.logStarDist("DEBUG", "Input tile shape: " + java.util.Arrays.toString(dims));
		}

		input.getProcessedTiles().clear();
		DirectFileLogger.logStarDist("DEBUG", "Cleared processed tiles");

		try {
			DirectFileLogger.logStarDist("DEBUG", "Setting tiled view su network...");
			network.setTiledView(input);
			DirectFileLogger.logStarDist("DEBUG", "Tiled view impostata, submitting al pool...");
			
			Future<List<RandomAccessibleInterval<T>>> resultFuture = pool.submit(network);
			DirectFileLogger.logStarDist("DEBUG", "Task submitted, future presente: " + (resultFuture != null));
			
			if(resultFuture != null) {
				DirectFileLogger.logStarDist("DEBUG", "Waiting for result...");
				List<RandomAccessibleInterval<T>> result = resultFuture.get();
				DirectFileLogger.logStarDist("DEBUG", "Result ottenuto: " + (result != null));
				
				if(result != null) {
					DirectFileLogger.logStarDist("DEBUG", "Result size: " + result.size());
					input.getProcessedTiles().addAll(result);
					DirectFileLogger.logStarDist("DEBUG", "Result aggiunto ai processed tiles");
				} else {
					DirectFileLogger.logStarDist("WARN", "ATTENZIONE: Result è null!");
				}
			} else {
				DirectFileLogger.logStarDist("ERROR", "ERRORE: resultFuture è null!");
			}

		}
		catch(final CancellationException | RejectedExecutionException | InterruptedException e) {
			DirectFileLogger.logStarDistException("Esecuzione cancellata", e);
			//canceled
			setFailed();
			log(PROGRESS_CANCELED);
			cancel(PROGRESS_CANCELED);
			return null;
		}
		catch(final IllegalArgumentException e) {
			DirectFileLogger.logStarDistException("Argomento illegale", e);
			setFailed();
			throw e;
		}
		catch (final ExecutionException | IllegalStateException exc) {
			DirectFileLogger.logStarDistException("Eccezione durante esecuzione", exc);
			
			if(exc.getMessage() != null && exc.getMessage().contains("OOM")) {
				DirectFileLogger.logStarDist("ERROR", "Out of Memory rilevato");
				setIdle();
				throw new OutOfMemoryError();
			}
			exc.printStackTrace();
			setFailed();
			throw exc;
		}

		DirectFileLogger.logStarDist("DEBUG", "--- Fine elaborazione singolo tile ---");
		return input;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		if (pool != null && !pool.isShutdown()) {
			pool.shutdownNow();
		}
		if(network != null) {
			network.cancel(reason);
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}
