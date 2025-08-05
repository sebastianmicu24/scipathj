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

package de.csbdresden.csbdeep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;

import de.csbdresden.csbdeep.commands.GenericNetwork;

public class IOHelper {

	public static Location loadFileOrURL(final String path)
		throws FileNotFoundException
	{
		if (path == null) {
			throw new FileNotFoundException("No path specified");
		}
		final File file = new File(path);
		Location source;
		if (!file.exists()) {
			// For TensorFlow 2.x, we only support local files
			throw new FileNotFoundException("Could not find file: " + path);
		}
		else {
			source = new FileLocation(file);
		}
		return source;

	}

	public static boolean urlExists(String url) {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = null;
		boolean existingUrl = false;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("HEAD");
			existingUrl = con.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (IOException | IllegalArgumentException e) {
		} finally {
			if(con != null){
				con.disconnect();
			}
		}
		return existingUrl;
	}

	public static String getFileCacheName(Class<? extends GenericNetwork> parentClass, File file) throws IOException {
		FileInputStream fis = null;
		try {
			// Handle TensorFlow 2.x SavedModel directories
			File targetFile = file;
			if (file.isDirectory()) {
				// For TensorFlow 2.x SavedModel, use saved_model.pb for hash calculation
				File savedModelPb = new File(file, "saved_model.pb");
				if (savedModelPb.exists() && savedModelPb.isFile()) {
					targetFile = savedModelPb;
					System.out.println("[INFO] Using saved_model.pb for cache name calculation: " + savedModelPb.getAbsolutePath());
				} else {
					// Fallback: use directory name + modification time
					String dirName = file.getName();
					long lastModified = file.lastModified();
					String hashInput = dirName + "_" + lastModified;
					String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(hashInput.getBytes());
					System.out.println("[INFO] Using directory-based cache name for: " + file.getAbsolutePath());
					return parentClass.getSimpleName() + "_" + md5;
				}
			}
			
			fis = new FileInputStream(targetFile);
			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			return parentClass.getSimpleName() + "_" + md5;
		} catch (IOException e) {
			// If we still can't read the file, create a fallback cache name
			if (file.isDirectory()) {
				String dirName = file.getName();
				String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(dirName.getBytes());
				System.out.println("[WARN] Using fallback cache name for directory: " + file.getAbsolutePath());
				return parentClass.getSimpleName() + "_" + md5;
			}
			throw e;
		} finally {
			if(fis != null) {
				fis.close();
			}
		}
	}

	public static String getUrlCacheName(Class<? extends GenericNetwork> parentClass, String modelUrl) throws IOException {
		URL url = new URL(modelUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setReadTimeout(1000*10*1);
		connection.setConnectTimeout(1000*10*1);
		Long dateTime = connection.getLastModified();
		connection.disconnect();
		ZonedDateTime urlLastModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTime), ZoneId.of("GMT"));

		return parentClass.getSimpleName()
				+ "_" + url.getPath().replace(".zip", "").replace("/", "")
				+ "_" + DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss").format(urlLastModified);
	}
}
