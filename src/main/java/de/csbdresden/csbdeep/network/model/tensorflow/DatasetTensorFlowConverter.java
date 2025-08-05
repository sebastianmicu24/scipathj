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

import org.tensorflow.DataType;
import org.tensorflow.Tensor;

import de.csbdresden.csbdeep.converter.*;
import net.imagej.tensorflow.Tensors;
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

	public static <T extends RealType<T>, U extends RealType<U>>
		RandomAccessibleInterval<T> tensorToDataset(final Tensor<U> tensor,
			final T res, final int[] mapping, final boolean dropSingletonDims)
	{
		DirectFileLogger.logTensorFlow("=== INIZIO CONVERSIONE TENSOR TO DATASET ===");
		DirectFileLogger.logTensorFlow("Tensor presente: " + (tensor != null));
		if (tensor != null) {
			DirectFileLogger.logTensorFlow("Tensor DataType: " + tensor.dataType());
			DirectFileLogger.logTensorFlow("Tensor Shape: " + java.util.Arrays.toString(tensor.shape()));
		}
		DirectFileLogger.logTensorFlow("Result type: " + (res != null ? res.getClass().getSimpleName() : "null"));
		DirectFileLogger.logTensorFlow("Mapping: " + (mapping != null ? java.util.Arrays.toString(mapping) : "null"));
		DirectFileLogger.logTensorFlow("Drop singleton dims: " + dropSingletonDims);

		final RandomAccessibleInterval<T> outImg;

		if (tensor.dataType().equals(DataType.DOUBLE)) {
			DirectFileLogger.logTensorFlow("Processando tensor DOUBLE");
			if (res instanceof DoubleType) {
				outImg = Tensors.imgDouble((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert(
					(RandomAccessibleInterval<DoubleType>) Tensors.imgDouble(
						(Tensor) tensor, mapping), new DoubleRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.FLOAT)) {
			DirectFileLogger.logTensorFlow("Processando tensor FLOAT");
			if (res instanceof FloatType) {
				outImg = Tensors.imgFloat((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert(
					(RandomAccessibleInterval<FloatType>) Tensors.imgFloat(
						(Tensor) tensor, mapping), new FloatRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.INT64)) {
			DirectFileLogger.logTensorFlow("Processando tensor INT64");
			if (res instanceof LongType) {
				outImg = Tensors.imgLong((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<LongType>) Tensors
					.imgLong((Tensor) tensor, mapping), new LongRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.INT32)) {
			DirectFileLogger.logTensorFlow("Processando tensor INT32");
			if (res instanceof IntType) {
				outImg = Tensors.imgInt((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<IntType>) Tensors
					.imgInt((Tensor) tensor, mapping), new IntRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.UINT8)) {
			DirectFileLogger.logTensorFlow("Processando tensor UINT8");
			if (res instanceof ByteType) {
				outImg = Tensors.imgByte((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<ByteType>) Tensors
					.imgByte((Tensor) tensor, mapping), new ByteRealConverter<T>(), res);
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
			DirectFileLogger.logTensorFlow("Tentativo conversione diretta con Tensors.tensor()");
			tensor = Tensors.tensor(image, mapping);
			DirectFileLogger.logTensorFlow("Conversione diretta riuscita");
		}
		catch (IllegalArgumentException e) {
			DirectFileLogger.logTensorFlowException("Conversione diretta fallita", e);
			DirectFileLogger.logTensorFlow("Tentativo conversione con tipo specifico...");
			
			if (image.randomAccess().get() instanceof UnsignedShortType) {
				DirectFileLogger.logTensorFlow("Conversione UnsignedShort -> Int");
				tensor = Tensors.tensor(Converters.convert(image,
					new RealIntConverter<T>(), new IntType()), mapping);
			}
			else {
				DirectFileLogger.logTensorFlow("Conversione generica -> Float");
				tensor = Tensors.tensor(Converters.convert(image,
					new RealFloatConverter<T>(), new FloatType()), mapping);
			}
			DirectFileLogger.logTensorFlow("Conversione con tipo specifico riuscita");
		}
		
		DirectFileLogger.logTensorFlow("Tensor creato: " + (tensor != null));
		if (tensor != null) {
			DirectFileLogger.logTensorFlow("Tensor DataType: " + tensor.dataType());
			DirectFileLogger.logTensorFlow("Tensor Shape: " + java.util.Arrays.toString(tensor.shape()));
		}
		DirectFileLogger.logTensorFlow("=== FINE CONVERSIONE DATASET TO TENSOR ===");
		
		return tensor;
	}

}
