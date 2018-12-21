package com.ericwen229.util;

import lombok.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Global configuration management.
 */
public class Config {

	// ========== static members ==========

	/**
	 * Logger of the class.
	 */
	private static final Logger logger = Logger.getLogger(Config.class.getName());

	/**
	 * Properties.
	 */
	private static Properties properties = null;



	// ========== static methods ==========

	/**
	 * Load configuration from .properties file.
	 *
	 * @param filePath path of configuration file
	 */
	public static void loadConfig(@NonNull String filePath) {
		logger.info(String.format("Config file path: %s", filePath));
		try (FileInputStream fStream = new FileInputStream(filePath)) {
			properties = new Properties();
			properties.load(fStream);
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException(
					String.format("Configuration file not found at %s.", filePath));
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Error reading configuration from %s.", filePath));
		}
	}

	/**
	 * Get property to which the key is mapped.
	 *
	 * @param key key whose associated property is to be returned
	 * @return associated property
	 */
	public static String getProperty(String key) {
		String result = properties.getProperty(key);
		if (result == null)
			throw new IllegalArgumentException(
					String.format("Property %s not found in configuration.", key));
		return result;
	}

	/**
	 * Get property as string.
	 *
	 * @param key key whose associated property is to be returned
	 * @return associated property as string
	 */
	public static String getPropertyAsString(String key) {
		return getProperty(key);
	}

	/**
	 * Get property as integer.
	 *
	 * @param key key whose associated property is to be returned
	 * @return associated property as integer
	 */
	public static int getPropertyAsInt(String key) {
		return Integer.parseInt(getProperty(key));
	}

	/**
	 * Get property as double.
	 *
	 * @param key key whose associated property is to be returned
	 * @return associated property as double
	 */
	public static double getPropertyAsDouble(String key) {
		return Double.parseDouble(getProperty(key));
	}

}
