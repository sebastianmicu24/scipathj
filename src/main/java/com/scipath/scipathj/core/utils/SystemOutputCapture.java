package com.scipath.scipathj.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to capture System.out and System.err output and redirect it to SLF4J logging.
 * This is particularly useful for packaged applications where console output is not visible.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SystemOutputCapture {

  private static final Logger LOGGER = LoggerFactory.getLogger(SystemOutputCapture.class);
  private static final Logger SYSTEM_OUT_LOGGER = LoggerFactory.getLogger("SYSTEM_OUT");
  private static final Logger SYSTEM_ERR_LOGGER = LoggerFactory.getLogger("SYSTEM_ERR");

  private static PrintStream originalOut;
  private static PrintStream originalErr;
  private static boolean captured = false;

  /**
   * Start capturing System.out and System.err output and redirect to SLF4J loggers.
   */
  public static synchronized void startCapture() {
    if (captured) {
      LOGGER.debug("System output capture already started");
      return;
    }

    try {
      LOGGER.info("Starting system output capture for packaged application");

      // Store original streams
      originalOut = System.out;
      originalErr = System.err;

      // Create custom print streams that log to SLF4J
      System.setOut(new LoggingPrintStream(SYSTEM_OUT_LOGGER, false, originalOut));
      System.setErr(new LoggingPrintStream(SYSTEM_ERR_LOGGER, true, originalErr));

      captured = true;
      LOGGER.info("System output capture started successfully");

    } catch (Exception e) {
      LOGGER.error("Failed to start system output capture", e);
    }
  }

  /**
   * Stop capturing system output and restore original streams.
   */
  public static synchronized void stopCapture() {
    if (!captured) {
      LOGGER.debug("System output capture not active");
      return;
    }

    try {
      LOGGER.info("Stopping system output capture");

      // Restore original streams
      if (originalOut != null) {
        System.setOut(originalOut);
      }
      if (originalErr != null) {
        System.setErr(originalErr);
      }

      captured = false;
      LOGGER.info("System output capture stopped");

    } catch (Exception e) {
      LOGGER.error("Failed to stop system output capture", e);
    }
  }

  /**
   * Check if system output capture is active.
   */
  public static boolean isCaptured() {
    return captured;
  }

  /**
   * Custom PrintStream that redirects output to SLF4J logger.
   */
  private static class LoggingPrintStream extends PrintStream {

    private final Logger logger;
    private final boolean isError;
    private final PrintStream originalStream;
    private final ByteArrayOutputStream buffer;

    @SuppressWarnings("DM_DEFAULT_ENCODING")
    public LoggingPrintStream(Logger logger, boolean isError, PrintStream originalStream) {
      super(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
      this.logger = logger;
      this.isError = isError;
      this.originalStream = originalStream;
      this.buffer = (ByteArrayOutputStream) super.out;
    }

    @Override
    public void println(String message) {
      if (message != null && !message.trim().isEmpty()) {
        if (isError) {
          logger.error(message);
        } else {
          logger.info(message);
        }
      }

      // Also write to original stream if available (for development)
      if (originalStream != null) {
        originalStream.println(message);
      }
    }

    @Override
    public void print(String message) {
      if (message != null && !message.trim().isEmpty()) {
        if (isError) {
          logger.error(message);
        } else {
          logger.info(message);
        }
      }

      // Also write to original stream if available (for development)
      if (originalStream != null) {
        originalStream.print(message);
      }
    }

    @Override
    public void flush() {
      try {
        String content = buffer.toString("UTF-8");
        if (!content.isEmpty()) {
          // Split by lines and log each line
          String[] lines = content.split("\r?\n");
          for (String line : lines) {
            if (!line.trim().isEmpty()) {
              if (isError) {
                logger.error(line);
              } else {
                logger.info(line);
              }
            }
          }
          buffer.reset();
        }
      } catch (UnsupportedEncodingException e) {
        logger.error("Failed to flush logging print stream", e);
      }

      if (originalStream != null) {
        originalStream.flush();
      }
    }

    @Override
    public void close() {
      flush();
      if (originalStream != null) {
        originalStream.close();
      }
    }
  }
}
