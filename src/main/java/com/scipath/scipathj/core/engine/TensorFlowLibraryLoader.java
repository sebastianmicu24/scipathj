package com.scipath.scipathj.core.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * Custom TensorFlow native library loader that works with Java 21.
 * This class handles the native library loading process that fails
 * in the standard ImageJ-TensorFlow integration due to ClassLoader issues.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TensorFlowLibraryLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TensorFlowLibraryLoader.class);
    
    private static boolean libraryLoaded = false;
    private static String loadedLibraryPath = null;
    private static Throwable lastLoadException = null;
    
    // Common TensorFlow library names for different platforms
    private static final String[] TENSORFLOW_LIBRARY_NAMES = {
        "tensorflow_jni",      // Standard name
        "libtensorflow_jni",   // Linux/Mac with lib prefix
        "tensorflow_jni.dll",  // Windows with .dll extension
        "libtensorflow_jni.so", // Linux with .so extension
        "libtensorflow_jni.dylib" // Mac with .dylib extension
    };
    
    /**
     * Attempts to load the TensorFlow native library using multiple strategies.
     * 
     * @return true if the library was loaded successfully, false otherwise
     */
    public static synchronized boolean loadTensorFlowLibrary() {
        if (libraryLoaded) {
            LOGGER.debug("TensorFlow library already loaded from: {}", loadedLibraryPath);
            return true;
        }
        
        LOGGER.info("Attempting to load TensorFlow native library with Java 21 compatibility");
        
        // Apply ClassLoader fixes before attempting to load
        Java21ClassLoaderFix.applyFix();
        
        // Strategy 1: Try to load using System.loadLibrary (standard approach)
        if (trySystemLoadLibrary()) {
            return true;
        }
        
        // Strategy 2: Try to find and load from classpath
        if (tryLoadFromClasspath()) {
            return true;
        }
        
        // Strategy 3: Try to load from java.library.path
        if (tryLoadFromLibraryPath()) {
            return true;
        }
        
        // Strategy 4: Try to extract and load from JAR
        if (tryExtractAndLoad()) {
            return true;
        }
        
        // Strategy 5: Try to use TensorFlow's own loading mechanism with fixes
        if (tryTensorFlowNativeLoad()) {
            return true;
        }
        
        LOGGER.error("Failed to load TensorFlow native library using all strategies");
        if (lastLoadException != null) {
            LOGGER.error("Last exception:", lastLoadException);
        }
        
        return false;
    }
    
    /**
     * Strategy 1: Try standard System.loadLibrary approach.
     */
    private static boolean trySystemLoadLibrary() {
        LOGGER.debug("Strategy 1: Trying System.loadLibrary");
        
        for (String libName : TENSORFLOW_LIBRARY_NAMES) {
            try {
                // Remove file extensions for System.loadLibrary
                String cleanName = libName.replaceAll("\\.(dll|so|dylib)$", "")
                                         .replaceAll("^lib", "");
                
                LOGGER.debug("Attempting to load library: {}", cleanName);
                System.loadLibrary(cleanName);
                
                libraryLoaded = true;
                loadedLibraryPath = "System library: " + cleanName;
                LOGGER.info("Successfully loaded TensorFlow library: {}", cleanName);
                return true;
                
            } catch (UnsatisfiedLinkError e) {
                LOGGER.debug("Failed to load {}: {}", libName, e.getMessage());
                lastLoadException = e;
            }
        }
        
        return false;
    }
    
    /**
     * Strategy 2: Try to find and load from classpath.
     */
    private static boolean tryLoadFromClasspath() {
        LOGGER.debug("Strategy 2: Trying to load from classpath");
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = TensorFlowLibraryLoader.class.getClassLoader();
        }
        
        for (String libName : TENSORFLOW_LIBRARY_NAMES) {
            try {
                URL libUrl = classLoader.getResource(libName);
                if (libUrl == null) {
                    // Try with platform-specific paths
                    String osName = System.getProperty("os.name").toLowerCase();
                    String arch = System.getProperty("os.arch").toLowerCase();
                    String platformPath = getPlatformPath(osName, arch) + "/" + libName;
                    libUrl = classLoader.getResource(platformPath);
                }
                
                if (libUrl != null) {
                    LOGGER.debug("Found library in classpath: {}", libUrl);
                    
                    // Extract to temporary file and load
                    Path tempLib = extractLibraryToTemp(libUrl, libName);
                    if (tempLib != null) {
                        System.load(tempLib.toAbsolutePath().toString());
                        
                        libraryLoaded = true;
                        loadedLibraryPath = tempLib.toAbsolutePath().toString();
                        LOGGER.info("Successfully loaded TensorFlow library from classpath: {}", libUrl);
                        return true;
                    }
                }
                
            } catch (Exception e) {
                LOGGER.debug("Failed to load {} from classpath: {}", libName, e.getMessage());
                lastLoadException = e;
            }
        }
        
        return false;
    }
    
    /**
     * Strategy 3: Try to load from java.library.path.
     */
    private static boolean tryLoadFromLibraryPath() {
        LOGGER.debug("Strategy 3: Trying to load from java.library.path");
        
        String libraryPath = System.getProperty("java.library.path");
        if (libraryPath == null) {
            return false;
        }
        
        String[] paths = libraryPath.split(File.pathSeparator);
        
        for (String path : paths) {
            for (String libName : TENSORFLOW_LIBRARY_NAMES) {
                try {
                    Path libFile = Paths.get(path, libName);
                    if (Files.exists(libFile)) {
                        LOGGER.debug("Found library at: {}", libFile);
                        System.load(libFile.toAbsolutePath().toString());
                        
                        libraryLoaded = true;
                        loadedLibraryPath = libFile.toAbsolutePath().toString();
                        LOGGER.info("Successfully loaded TensorFlow library from library path: {}", libFile);
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to load {} from {}: {}", libName, path, e.getMessage());
                    lastLoadException = e;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Strategy 4: Try to extract and load from JAR.
     */
    private static boolean tryExtractAndLoad() {
        LOGGER.debug("Strategy 4: Trying to extract and load from JAR");
        
        // This would involve extracting TensorFlow libraries from the JAR
        // For now, we'll skip this complex implementation
        return false;
    }
    
    /**
     * Strategy 5: Try to use TensorFlow's own loading mechanism with ClassLoader fixes.
     */
    private static boolean tryTensorFlowNativeLoad() {
        LOGGER.debug("Strategy 5: Trying TensorFlow's native loading mechanism");
        
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    // Try to use TensorFlow's NativeLibrary class directly
                    Class<?> nativeLibClass = Class.forName("org.tensorflow.NativeLibrary");
                    Method loadMethod = nativeLibClass.getDeclaredMethod("load");
                    loadMethod.setAccessible(true);
                    loadMethod.invoke(null);
                    
                    libraryLoaded = true;
                    loadedLibraryPath = "TensorFlow NativeLibrary.load()";
                    LOGGER.info("Successfully loaded TensorFlow library using NativeLibrary.load()");
                    return true;
                    
                } catch (Exception e) {
                    LOGGER.debug("Failed to load using TensorFlow NativeLibrary: {}", e.getMessage());
                    lastLoadException = e;
                    
                    // Try alternative approach with reflection
                    try {
                        Class<?> tensorFlowClass = Class.forName("org.tensorflow.TensorFlow");
                        Method versionMethod = tensorFlowClass.getDeclaredMethod("version");
                        String version = (String) versionMethod.invoke(null);
                        
                        libraryLoaded = true;
                        loadedLibraryPath = "TensorFlow.version() - " + version;
                        LOGGER.info("TensorFlow library appears to be loaded (version: {})", version);
                        return true;
                        
                    } catch (Exception e2) {
                        LOGGER.debug("Failed to verify TensorFlow loading: {}", e2.getMessage());
                        lastLoadException = e2;
                    }
                }
                
                return false;
            }
        });
    }
    
    /**
     * Extracts a library from a URL to a temporary file.
     */
    private static Path extractLibraryToTemp(URL libUrl, String libName) {
        try {
            // Create temporary file
            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFile = Paths.get(tempDir, "tensorflow_" + System.currentTimeMillis() + "_" + libName);
            
            // Copy from URL to temporary file
            try (InputStream in = libUrl.openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Make executable
            tempFile.toFile().setExecutable(true);
            
            // Delete on exit
            tempFile.toFile().deleteOnExit();
            
            LOGGER.debug("Extracted library to temporary file: {}", tempFile);
            return tempFile;
            
        } catch (IOException e) {
            LOGGER.warn("Failed to extract library to temporary file: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the platform-specific path for native libraries.
     */
    private static String getPlatformPath(String osName, String arch) {
        String platform;
        
        if (osName.contains("windows")) {
            platform = "windows";
        } else if (osName.contains("linux")) {
            platform = "linux";
        } else if (osName.contains("mac")) {
            platform = "darwin";
        } else {
            platform = "unknown";
        }
        
        String architecture;
        if (arch.contains("64")) {
            architecture = "x86_64";
        } else if (arch.contains("86")) {
            architecture = "x86";
        } else if (arch.contains("arm")) {
            architecture = "arm";
        } else {
            architecture = arch;
        }
        
        return "native/" + platform + "/" + architecture;
    }
    
    /**
     * Checks if the TensorFlow library is loaded.
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }
    
    /**
     * Gets the path of the loaded library.
     */
    public static String getLoadedLibraryPath() {
        return loadedLibraryPath;
    }
    
    /**
     * Gets the last exception that occurred during loading.
     */
    public static Throwable getLastLoadException() {
        return lastLoadException;
    }
    
    /**
     * Gets detailed information about the TensorFlow library loading status.
     */
    public static String getLoadingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("TensorFlow Library Loading Status:\n");
        status.append("- Loaded: ").append(libraryLoaded).append("\n");
        status.append("- Library Path: ").append(loadedLibraryPath != null ? loadedLibraryPath : "N/A").append("\n");
        status.append("- Java Version: ").append(System.getProperty("java.version")).append("\n");
        status.append("- OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.arch")).append("\n");
        status.append("- Library Path Property: ").append(System.getProperty("java.library.path")).append("\n");
        
        if (lastLoadException != null) {
            status.append("- Last Exception: ").append(lastLoadException.getMessage()).append("\n");
        }
        
        return status.toString();
    }
}