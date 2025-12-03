package com.bhojpurri;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple .env file loader for API keys and configuration.
 * Reads key=value pairs from .env file in the project root.
 */
public class EnvLoader {
    private static final Logger logger = LoggerFactory.getLogger(EnvLoader.class);
    private static final Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Loads environment variables from .env file.
     * Call this once at application startup.
     */
    public static void load() {
        if (loaded) {
            return; // Already loaded
        }

        Path envFile = Paths.get(".env");
        
        if (!Files.exists(envFile)) {
            logger.info("No .env file found - using system environment variables only");
            loaded = true;
            return;
        }

        logger.info("Loading environment variables from .env file");

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile.toFile()))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    envVars.put(key, value);
                    logger.debug("Loaded: {} = {}", key, maskValue(value));
                } else {
                    logger.warn("Invalid line {} in .env file: {}", lineNumber, line);
                }
            }

            logger.info("Successfully loaded {} environment variables from .env", envVars.size());
            loaded = true;

        } catch (IOException e) {
            logger.error("Failed to load .env file", e);
        }
    }

    /**
     * Gets an environment variable value.
     * Checks .env file first, then falls back to system environment.
     * 
     * @param key The environment variable name
     * @return The value, or null if not found
     */
    public static String get(String key) {
        // Load .env if not already loaded
        if (!loaded) {
            load();
        }

        // Check .env file first
        String value = envVars.get(key);
        
        // Fall back to system environment
        if (value == null || value.isEmpty()) {
            value = System.getenv(key);
        }

        return value;
    }

    /**
     * Gets an environment variable with a default fallback.
     * 
     * @param key The environment variable name
     * @param defaultValue Default value if key is not found
     * @return The value, or defaultValue if not found
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Checks if a key exists and has a non-empty value.
     * 
     * @param key The environment variable name
     * @return true if key exists and is not empty
     */
    public static boolean has(String key) {
        String value = get(key);
        return value != null && !value.isEmpty();
    }

    /**
     * Masks sensitive values for logging.
     */
    private static String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
