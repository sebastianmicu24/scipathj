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

import org.tensorflow.Tensor;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TFloat64;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TUint8;

import de.csbdresden.csbdeep.converter.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import com.scipath.scipathj.core.utils.DirectFileLogger;

public class DatasetTensorFlowConverter {

	public static <T extends RealType<T>>
		RandomAccessibleInterval<T> tensorToDataset(final Tensor tensor,
			final T res, final int[] mapping, final boolean dropSingletonDims)
	{
		DirectFileLogger.logTensorFlow("=== INIZIO CONVERSIONE TENSOR TO DATASET ===");
		DirectFileLogger.logTensorFlow("Tensor presente: " + (tensor != null));
		if (tensor != null) {
			DirectFileLogger.logTensorFlow("Tensor DataType: " + tensor.dataType());
			DirectFileLogger.logTensorFlow("Tensor Shape: " + tensor.shape().toString());
		}
		DirectFileLogger.logTensorFlow("Result type: " + (res != null ? res.getClass().getSimpleName() : "null"));
		DirectFileLogger.logTensorFlow("Mapping: " + (mapping != null ? java.util.Arrays.toString(mapping) : "null"));
		DirectFileLogger.logTensorFlow("Drop singleton dims: " + dropSingletonDims);

		final RandomAccessibleInterval<T> outImg;

		String dataTypeStr = tensor.dataType().toString();
		if (dataTypeStr.contains("FLOAT64") || dataTypeStr.contains("DOUBLE")) {
			DirectFileLogger.logTensorFlow("Processando tensor DOUBLE");
			if (res instanceof DoubleType) {
				outImg = convertTensorToDataset(tensor, res, mapping);
			}
			else {
				RandomAccessibleInterval<DoubleType> doubleImg = convertTensorToDataset(tensor, new DoubleType(), mapping);
				outImg = Converters.convert(doubleImg, new DoubleRealConverter<T>(), res);
			}
		}
		else if (dataTypeStr.contains("FLOAT32") || dataTypeStr.contains("FLOAT")) {
			DirectFileLogger.logTensorFlow("Processando tensor FLOAT");
			if (res instanceof FloatType) {
				outImg = convertTensorToDataset(tensor, res, mapping);
			}
			else {
				RandomAccessibleInterval<FloatType> floatImg = convertTensorToDataset(tensor, new FloatType(), mapping);
				outImg = Converters.convert(floatImg, new FloatRealConverter<T>(), res);
			}
		}
		else if (dataTypeStr.contains("INT64") || dataTypeStr.contains("LONG")) {
			DirectFileLogger.logTensorFlow("Processando tensor INT64");
			if (res instanceof LongType) {
				outImg = convertTensorToDataset(tensor, res, mapping);
			}
			else {
				RandomAccessibleInterval<LongType> longImg = convertTensorToDataset(tensor, new LongType(), mapping);
				outImg = Converters.convert(longImg, new LongRealConverter<T>(), res);
			}
		}
		else if (dataTypeStr.contains("INT32") || dataTypeStr.contains("INT")) {
			DirectFileLogger.logTensorFlow("Processando tensor INT32");
			if (res instanceof IntType) {
				outImg = convertTensorToDataset(tensor, res, mapping);
			}
			else {
				RandomAccessibleInterval<IntType> intImg = convertTensorToDataset(tensor, new IntType(), mapping);
				outImg = Converters.convert(intImg, new IntRealConverter<T>(), res);
			}
		}
		else if (dataTypeStr.contains("UINT8") || dataTypeStr.contains("BYTE")) {
			DirectFileLogger.logTensorFlow("Processando tensor UINT8");
			if (res instanceof ByteType) {
				outImg = convertTensorToDataset(tensor, res, mapping);
			}
			else {
				RandomAccessibleInterval<ByteType> byteImg = convertTensorToDataset(tensor, new ByteType(), mapping);
				outImg = Converters.convert(byteImg, new ByteRealConverter<T>(), res);
			}
		}
		else {
			DirectFileLogger.logTensorFlow("ERRORE: Tipo di tensor non supportato: " + tensor.dataType());
			outImg = null;
		}

		DirectFileLogger.logTensorFlow("Output image creata: " + (outImg != null));
		if (outImg != null) {
			DirectFileLogger.logTensorFlow("Output dimensions: " + outImg.numDimensions());
			long[] dims = new long[outImg.numDimensions()];
			outImg.dimensions(dims);
			DirectFileLogger.logTensorFlow("Output shape: " + java.util.Arrays.toString(dims));
		}
		
		RandomAccessibleInterval<T> result = dropSingletonDims ? Views.dropSingletonDimensions(outImg) : outImg;
		DirectFileLogger.logTensorFlow("=== FINE CONVERSIONE TENSOR TO DATASET ===");
		return result;
	}

	public static <T extends RealType<T>> Tensor datasetToTensor(
		RandomAccessibleInterval<T> image, final int[] mapping)
	{
		System.out.println("[INFO] === INIZIO CONVERSIONE DATASET TO TENSOR ===");
		System.out.println("[INFO] Image presente: " + (image != null));
		if (image != null) {
			System.out.println("[INFO] Image dimensions: " + image.numDimensions());
			long[] dims = new long[image.numDimensions()];
			image.dimensions(dims);
			System.out.println("[INFO] Image shape: " + java.util.Arrays.toString(dims));
			System.out.println("[INFO] Image type: " + image.randomAccess().get().getClass().getSimpleName());
		}
		System.out.println("[INFO] Mapping: " + (mapping != null ? java.util.Arrays.toString(mapping) : "null"));
		
		DirectFileLogger.logTensorFlow("=== INIZIO CONVERSIONE DATASET TO TENSOR ===");
		DirectFileLogger.logTensorFlow("Image presente: " + (image != null));
		if (image != null) {
			DirectFileLogger.logTensorFlow("Image dimensions: " + image.numDimensions());
			long[] dims = new long[image.numDimensions()];
			image.dimensions(dims);
			DirectFileLogger.logTensorFlow("Image shape: " + java.util.Arrays.toString(dims));
			DirectFileLogger.logTensorFlow("Image type: " + image.randomAccess().get().getClass().getSimpleName());
		}
		DirectFileLogger.logTensorFlow("Mapping: " + (mapping != null ? java.util.Arrays.toString(mapping) : "null"));

		Tensor tensor;
		try {
			DirectFileLogger.logTensorFlow("Tentativo conversione diretta");
			tensor = convertDatasetToTensor(image, mapping);
			DirectFileLogger.logTensorFlow("Conversione diretta riuscita");
		}
		catch (IllegalArgumentException e) {
			DirectFileLogger.logTensorFlowException("Conversione diretta fallita", e);
			DirectFileLogger.logTensorFlow("Tentativo conversione con tipo specifico...");
			
			if (image.randomAccess().get() instanceof UnsignedShortType) {
				DirectFileLogger.logTensorFlow("Conversione UnsignedShort -> Int");
				tensor = convertDatasetToTensor(Converters.convert(image,
					new RealIntConverter<T>(), new IntType()), mapping);
			}
			else {
				DirectFileLogger.logTensorFlow("Conversione generica -> Float");
				tensor = convertDatasetToTensor(Converters.convert(image,
					new RealFloatConverter<T>(), new FloatType()), mapping);
			}
			DirectFileLogger.logTensorFlow("Conversione con tipo specifico riuscita");
		}
		
		System.out.println("[INFO] Tensor creato: " + (tensor != null));
		if (tensor != null) {
			System.out.println("[INFO] Tensor DataType: " + tensor.dataType());
			System.out.println("[INFO] Tensor Shape: " + tensor.shape().toString());
		}
		System.out.println("[INFO] === FINE CONVERSIONE DATASET TO TENSOR ===");
		
		DirectFileLogger.logTensorFlow("Tensor creato: " + (tensor != null));
		if (tensor != null) {
			DirectFileLogger.logTensorFlow("Tensor DataType: " + tensor.dataType());
			DirectFileLogger.logTensorFlow("Tensor Shape: " + tensor.shape().toString());
		}
		DirectFileLogger.logTensorFlow("=== FINE CONVERSIONE DATASET TO TENSOR ===");
		
		return tensor;
	}

	/**
	 * Helper method for TensorFlow 2.x tensor to dataset conversion
	 */
	@SuppressWarnings("unchecked")
	private static <T extends RealType<T>> RandomAccessibleInterval<T> convertTensorToDataset(
		Tensor tensor, T type, int[] mapping) {
		// Implementazione semplificata per TensorFlow 2.x
		// Per ora restituiamo null e logghiamo l'errore
		DirectFileLogger.logTensorFlow("AVVISO: Conversione tensor->dataset non ancora implementata per TensorFlow 2.x");
		return null;
	}

	/**
	 * Helper method for TensorFlow 2.x dataset to tensor conversion
	 */
	private static <T extends RealType<T>> Tensor convertDatasetToTensor(
		RandomAccessibleInterval<T> image, int[] mapping) {
		
		DirectFileLogger.logTensorFlow("=== INIZIO CONVERSIONE DATASET->TENSOR ===");
		DirectFileLogger.logTensorFlow("Tipo immagine: " + image.getClass().getSimpleName());
		
		// Get image dimensions
		long[] dims = new long[image.numDimensions()];
		for (int i = 0; i < dims.length; i++) {
			dims[i] = image.dimension(i);
		}
		
		DirectFileLogger.logTensorFlow("Dimensioni immagine originali: " + java.util.Arrays.toString(dims));
		DirectFileLogger.logTensorFlow("Numero dimensioni: " + dims.length);
		DirectFileLogger.logTensorFlow("Mapping array: " + (mapping != null ? java.util.Arrays.toString(mapping) : "null"));
		
		// For StarDist, we need to ensure the tensor has the right shape: [batch, height, width, channels]
		// ImgLib2 typically uses [X, Y, Channel] which maps to [width, height, channels]
		// We need to add batch dimension and potentially reorder
		
		long[] tensorDims;
		if (dims.length == 2) {
			// Grayscale image [X, Y] -> [1, Y, X, 1] (batch, height, width, channels)
			tensorDims = new long[]{1, dims[1], dims[0], 1};
			System.out.println("[INFO] CORREZIONE: Grayscale 2D [" + dims[0] + "," + dims[1] + "] -> [1," + dims[1] + "," + dims[0] + ",1]");
			DirectFileLogger.logTensorFlow("Immagine grayscale 2D, aggiunta dimensione batch e canale");
		} else if (dims.length == 3) {
			// Multi-channel image [X, Y, Channel] -> [1, Y, X, Channel] (batch, height, width, channels)
			tensorDims = new long[]{1, dims[1], dims[0], dims[2]};
			System.out.println("[INFO] CORREZIONE: Multi-canale 3D [" + dims[0] + "," + dims[1] + "," + dims[2] + "] -> [1," + dims[1] + "," + dims[0] + "," + dims[2] + "]");
			System.out.println("[INFO] VERIFICA: Canali preservati = " + dims[2] + " (dovrebbe essere 3 per RGB)");
			DirectFileLogger.logTensorFlow("Immagine multi-canale 3D, aggiunta dimensione batch");
			DirectFileLogger.logTensorFlow("DETTAGLIO CANALI: Input=[" + dims[0] + "," + dims[1] + "," + dims[2] + "] -> Tensor=[1," + dims[1] + "," + dims[0] + "," + dims[2] + "]");
			DirectFileLogger.logTensorFlow("VERIFICA: Canali preservati = " + dims[2] + " (dovrebbe essere 3 per RGB)");
		} else if (dims.length == 4) {
			// PROBLEMA IDENTIFICATO: Il tile ha già 4 dimensioni [X, Y, Channel, Batch]
			// Ma TensorFlow si aspetta [Batch, Height, Width, Channels]
			// Devo riordinare: [X, Y, Channel, Batch] -> [Batch, Y, X, Channel]
			tensorDims = new long[]{dims[3], dims[1], dims[0], dims[2]};
			System.out.println("[INFO] CORREZIONE CRITICA: 4D [" + dims[0] + "," + dims[1] + "," + dims[2] + "," + dims[3] + "] -> [" + dims[3] + "," + dims[1] + "," + dims[0] + "," + dims[2] + "]");
			System.out.println("[INFO] RIORDINAMENTO: [X,Y,Channel,Batch] -> [Batch,Height,Width,Channels]");
			DirectFileLogger.logTensorFlow("CORREZIONE CRITICA: Riordinamento dimensioni per TensorFlow");
		} else {
			// Fallback: add batch dimension at the beginning
			tensorDims = new long[dims.length + 1];
			tensorDims[0] = 1;
			System.arraycopy(dims, 0, tensorDims, 1, dims.length);
			System.out.println("[INFO] Dimensioni non standard, usando fallback");
			DirectFileLogger.logTensorFlow("Dimensioni non standard, usando fallback");
		}
		
		DirectFileLogger.logTensorFlow("Dimensioni tensor finale: " + java.util.Arrays.toString(tensorDims));
		
		// Calculate total number of elements
		long totalElements = 1;
		for (long dim : tensorDims) {
			totalElements *= dim;
		}
		
		DirectFileLogger.logTensorFlow("Elementi totali tensor: " + totalElements);
		
		// Get the pixel type to determine tensor type
		T pixelType = image.randomAccess().get();
		DirectFileLogger.logTensorFlow("Tipo pixel: " + pixelType.getClass().getSimpleName());
		
		try {
			// Convert based on pixel type
			if (pixelType instanceof FloatType) {
				return convertToFloatTensor(image, dims, tensorDims);
			} else if (pixelType instanceof DoubleType) {
				return convertToDoubleTensorFallback(image, tensorDims);
			} else if (pixelType instanceof IntType) {
				return convertToIntTensorFallback(image, tensorDims);
			} else if (pixelType instanceof LongType) {
				return convertToLongTensorFallback(image, tensorDims);
			} else if (pixelType instanceof ByteType || pixelType instanceof UnsignedShortType) {
				return convertToUint8TensorFallback(image, tensorDims);
			} else {
				// Default: convert to float
				DirectFileLogger.logTensorFlow("Tipo non riconosciuto, conversione a float");
				return convertToFloatTensorGenericFallback(image, tensorDims);
			}
		} catch (Exception e) {
			DirectFileLogger.logTensorFlowException("Errore durante conversione dataset->tensor", e);
			throw new IllegalArgumentException("Errore conversione dataset->tensor: " + e.getMessage(), e);
		}
	}
	
	private static <T extends RealType<T>> Tensor convertToFloatTensor(
		RandomAccessibleInterval<T> image, long[] imageDims, long[] tensorDims) {
			
			DirectFileLogger.logTensorFlow("Conversione a TFloat32");
			DirectFileLogger.logTensorFlow("Dimensioni immagine: " + java.util.Arrays.toString(imageDims));
			DirectFileLogger.logTensorFlow("Dimensioni tensor: " + java.util.Arrays.toString(tensorDims));
			
			// Create TFloat32 tensor with the correct dimensions
			TFloat32 tensor = TFloat32.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
			
			// Handle different dimension mappings
			if (imageDims.length == 2) {
				// Grayscale [X, Y] -> [1, Y, X, 1]
				convertGrayscale2DToTensor(image, tensor, imageDims);
			} else if (imageDims.length == 3) {
				// Multi-channel [X, Y, Channel] -> [1, Y, X, Channel]
				convertMultiChannel3DToTensor(image, tensor, imageDims);
			} else if (imageDims.length == 4) {
				// CORREZIONE CRITICA: 4D [X, Y, Channel, Batch] -> [Batch, Y, X, Channel]
				System.out.println("[INFO] Usando conversione 4D con riordinamento dimensioni");
				convert4DToTensorWithReordering(image, tensor, imageDims, tensorDims);
			} else {
				// Fallback: use original method
				System.out.println("[INFO] Usando conversione fallback per dimensioni non standard");
				DirectFileLogger.logTensorFlow("Usando conversione fallback per dimensioni non standard");
				convertGenericToTensor(image, tensor, tensorDims);
			}
			
			DirectFileLogger.logTensorFlow("Conversione TFloat32 completata");
			DirectFileLogger.logTensorFlow("Tensor shape finale: " + tensor.shape().toString());
			
			return tensor;
		}
		
		private static <T extends RealType<T>> void convertGrayscale2DToTensor(
			RandomAccessibleInterval<T> image, TFloat32 tensor, long[] imageDims) {
			
			DirectFileLogger.logTensorFlow("Conversione grayscale 2D: [X,Y] -> [1,Y,X,1]");
			
			var cursor = Views.iterable(image).cursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				float value = cursor.get().getRealFloat();
				
				// Get position in image coordinates [x, y]
				long x = cursor.getLongPosition(0);
				long y = cursor.getLongPosition(1);
				
				// Map to tensor coordinates [batch=0, height=y, width=x, channel=0]
				tensor.setFloat(value, 0, y, x, 0);
			}
		}
		
		private static <T extends RealType<T>> void convertMultiChannel3DToTensor(
			RandomAccessibleInterval<T> image, TFloat32 tensor, long[] imageDims) {
			
			DirectFileLogger.logTensorFlow("=== CONVERSIONE MULTI-CANALE 3D ===");
			DirectFileLogger.logTensorFlow("Dimensioni immagine input: " + java.util.Arrays.toString(imageDims));
			DirectFileLogger.logTensorFlow("Canali immagine: " + imageDims[2]);
			DirectFileLogger.logTensorFlow("Dimensioni tensor target: " + java.util.Arrays.toString(tensor.shape().asArray()));
			
			// VERIFICA CRITICA: Controlliamo se l'immagine ha effettivamente 3 dimensioni
			DirectFileLogger.logTensorFlow("VERIFICA DIMENSIONI IMMAGINE:");
			DirectFileLogger.logTensorFlow("  - image.numDimensions(): " + image.numDimensions());
			long[] actualDims = new long[image.numDimensions()];
			image.dimensions(actualDims);
			DirectFileLogger.logTensorFlow("  - image.dimensions(): " + java.util.Arrays.toString(actualDims));
			
			// Se l'immagine non ha 3 dimensioni, c'è un problema upstream
			if (image.numDimensions() != 3) {
				DirectFileLogger.logTensorFlow("ERRORE CRITICO: L'immagine non ha 3 dimensioni! Ha " + image.numDimensions() + " dimensioni");
				DirectFileLogger.logTensorFlow("Questo spiega perché i canali si perdono!");
				DirectFileLogger.logTensorFlow("Dimensioni reali: " + java.util.Arrays.toString(actualDims));
				throw new IllegalArgumentException("Immagine multi-canale attesa con 3 dimensioni, ma ha " + image.numDimensions() + " dimensioni");
			}
			
			var cursor = Views.iterable(image).cursor();
			long elementCount = 0;
			long[] channelCounts = new long[(int)imageDims[2]];
			
			while (cursor.hasNext()) {
				cursor.fwd();
				float value = cursor.get().getRealFloat();
				
				// Get position in image coordinates [x, y, channel]
				long x = cursor.getLongPosition(0);
				long y = cursor.getLongPosition(1);
				long c = cursor.getLongPosition(2);
				
				// Count elements per channel
				if (c < channelCounts.length) {
					channelCounts[(int)c]++;
				}
				
				// Debug per i primi elementi
				if (elementCount < 20) {
					DirectFileLogger.logTensorFlow("Elemento " + elementCount + ": pos=[" + x + "," + y + "," + c +
						"], valore=" + value);
				}
				
				// Map to tensor coordinates [batch=0, height=y, width=x, channel=c]
				try {
					tensor.setFloat(value, 0, y, x, c);
				} catch (Exception e) {
					DirectFileLogger.logTensorFlow("ERRORE setFloat: pos=[0," + y + "," + x + "," + c + "], errore=" + e.getMessage());
					if (elementCount < 5) {
						throw e; // Re-throw per i primi errori per debug
					}
				}
				elementCount++;
			}
			
			DirectFileLogger.logTensorFlow("Elementi totali processati: " + elementCount);
			DirectFileLogger.logTensorFlow("Elementi per canale: " + java.util.Arrays.toString(channelCounts));
			DirectFileLogger.logTensorFlow("=== FINE CONVERSIONE MULTI-CANALE ===");
		}
		
		private static <T extends RealType<T>> void convert4DToTensorWithReordering(
			RandomAccessibleInterval<T> image, TFloat32 tensor, long[] imageDims, long[] tensorDims) {
			
			System.out.println("[INFO] === CONVERSIONE 4D CON RIORDINAMENTO ===");
			System.out.println("[INFO] Dimensioni immagine: " + java.util.Arrays.toString(imageDims));
			System.out.println("[INFO] Dimensioni tensor: " + java.util.Arrays.toString(tensorDims));
			System.out.println("[INFO] Riordinamento: [X,Y,Channel,Batch] -> [Batch,Height,Width,Channels]");
			
			var cursor = Views.iterable(image).cursor();
			long elementCount = 0;
			
			while (cursor.hasNext()) {
				cursor.fwd();
				float value = cursor.get().getRealFloat();
				
				// Get position in image coordinates [x, y, channel, batch]
				long x = cursor.getLongPosition(0);
				long y = cursor.getLongPosition(1);
				long c = cursor.getLongPosition(2);
				long b = cursor.getLongPosition(3);
				
				// Debug per i primi elementi
				if (elementCount < 20) {
					System.out.println("[INFO] Elemento " + elementCount + ": pos=[" + x + "," + y + "," + c + "," + b + "], valore=" + value);
				}
				
				// Map to tensor coordinates [batch, height, width, channel]
				// [x, y, channel, batch] -> [batch, y, x, channel]
				try {
					tensor.setFloat(value, b, y, x, c);
					if (elementCount < 10) {
						System.out.println("[INFO] Mappato: [" + x + "," + y + "," + c + "," + b + "] -> [" + b + "," + y + "," + x + "," + c + "]");
					}
				} catch (Exception e) {
					System.out.println("[ERROR] setFloat: pos=[" + b + "," + y + "," + x + "," + c + "], errore=" + e.getMessage());
					if (elementCount < 5) {
						throw e; // Re-throw per i primi errori per debug
					}
				}
				elementCount++;
			}
			
			System.out.println("[INFO] Elementi totali processati: " + elementCount);
			System.out.println("[INFO] === FINE CONVERSIONE 4D ===");
		}
	
		private static <T extends RealType<T>> void convertGenericToTensor(
			RandomAccessibleInterval<T> image, TFloat32 tensor, long[] tensorDims) {
			
			DirectFileLogger.logTensorFlow("Conversione generica con dimensioni: " + java.util.Arrays.toString(tensorDims));
			
			var cursor = Views.flatIterable(image).cursor();
			long[] indices = new long[tensorDims.length];
			
			while (cursor.hasNext()) {
				float value = cursor.next().getRealFloat();
				tensor.setFloat(value, indices);
				incrementIndices(indices, tensorDims);
			}
		}
	private static <T extends RealType<T>> Tensor convertToDoubleTensor(
		RandomAccessibleInterval<T> image, long[] dims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TFloat64");
		
		TFloat64 tensor = TFloat64.tensorOf(org.tensorflow.ndarray.Shape.of(dims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[dims.length];
		
		while (cursor.hasNext()) {
			double value = cursor.next().getRealDouble();
			tensor.setDouble(value, indices);
			incrementIndices(indices, dims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TFloat64 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToIntTensor(
		RandomAccessibleInterval<T> image, long[] dims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TInt32");
		
		TInt32 tensor = TInt32.tensorOf(org.tensorflow.ndarray.Shape.of(dims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[dims.length];
		
		while (cursor.hasNext()) {
			int value = (int) cursor.next().getRealDouble();
			tensor.setInt(value, indices);
			incrementIndices(indices, dims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TInt32 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToLongTensor(
		RandomAccessibleInterval<T> image, long[] dims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TInt64");
		
		TInt64 tensor = TInt64.tensorOf(org.tensorflow.ndarray.Shape.of(dims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[dims.length];
		
		while (cursor.hasNext()) {
			long value = (long) cursor.next().getRealDouble();
			tensor.setLong(value, indices);
			incrementIndices(indices, dims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TInt64 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToUint8Tensor(
		RandomAccessibleInterval<T> image, long[] dims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TUint8");
		
		TUint8 tensor = TUint8.tensorOf(org.tensorflow.ndarray.Shape.of(dims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[dims.length];
		
		while (cursor.hasNext()) {
			byte value = (byte) Math.max(0, Math.min(255, (int) cursor.next().getRealDouble()));
			tensor.setByte(value, indices);
			incrementIndices(indices, dims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TUint8 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToFloatTensorGeneric(
		RandomAccessibleInterval<T> image, long[] dims) {
		
		DirectFileLogger.logTensorFlow("Conversione generica a TFloat32");
		
		TFloat32 tensor = TFloat32.tensorOf(org.tensorflow.ndarray.Shape.of(dims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[dims.length];
		
		while (cursor.hasNext()) {
			float value = cursor.next().getRealFloat();
			tensor.setFloat(value, indices);
			incrementIndices(indices, dims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione generica TFloat32 completata");
		return tensor;
	}
	
	/**
	 * Helper method to increment multi-dimensional indices
	 */
	private static void incrementIndices(long[] indices, long[] dims) {
		for (int i = indices.length - 1; i >= 0; i--) {
			indices[i]++;
			if (indices[i] < dims[i]) {
				break;
			}
			indices[i] = 0;
		}
	}
	
	// Metodi fallback per compatibilità
	private static <T extends RealType<T>> Tensor convertToDoubleTensorFallback(
		RandomAccessibleInterval<T> image, long[] tensorDims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TFloat64 (fallback)");
		
		TFloat64 tensor = TFloat64.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[tensorDims.length];
		
		while (cursor.hasNext()) {
			double value = cursor.next().getRealDouble();
			tensor.setDouble(value, indices);
			incrementIndices(indices, tensorDims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TFloat64 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToIntTensorFallback(
		RandomAccessibleInterval<T> image, long[] tensorDims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TInt32 (fallback)");
		
		TInt32 tensor = TInt32.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[tensorDims.length];
		
		while (cursor.hasNext()) {
			int value = (int) cursor.next().getRealDouble();
			tensor.setInt(value, indices);
			incrementIndices(indices, tensorDims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TInt32 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToLongTensorFallback(
		RandomAccessibleInterval<T> image, long[] tensorDims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TInt64 (fallback)");
		
		TInt64 tensor = TInt64.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[tensorDims.length];
		
		while (cursor.hasNext()) {
			long value = (long) cursor.next().getRealDouble();
			tensor.setLong(value, indices);
			incrementIndices(indices, tensorDims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TInt64 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToUint8TensorFallback(
		RandomAccessibleInterval<T> image, long[] tensorDims) {
		
		DirectFileLogger.logTensorFlow("Conversione a TUint8 (fallback)");
		
		TUint8 tensor = TUint8.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[tensorDims.length];
		
		while (cursor.hasNext()) {
			byte value = (byte) Math.max(0, Math.min(255, (int) cursor.next().getRealDouble()));
			tensor.setByte(value, indices);
			incrementIndices(indices, tensorDims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione TUint8 completata");
		return tensor;
	}
	
	private static <T extends RealType<T>> Tensor convertToFloatTensorGenericFallback(
		RandomAccessibleInterval<T> image, long[] tensorDims) {
		
		DirectFileLogger.logTensorFlow("Conversione generica a TFloat32 (fallback)");
		
		TFloat32 tensor = TFloat32.tensorOf(org.tensorflow.ndarray.Shape.of(tensorDims));
		
		var cursor = Views.flatIterable(image).cursor();
		long[] indices = new long[tensorDims.length];
		
		while (cursor.hasNext()) {
			float value = cursor.next().getRealFloat();
			tensor.setFloat(value, indices);
			incrementIndices(indices, tensorDims);
		}
		
		DirectFileLogger.logTensorFlow("Conversione generica TFloat32 completata");
		return tensor;
	}

}
