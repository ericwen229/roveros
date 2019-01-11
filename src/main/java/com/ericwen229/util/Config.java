package com.ericwen229.util;

import lombok.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class manages application configurations.
 */
public class Config {

	/**
	 * Properties shared within the application.
	 */
	private static Properties properties = null;

	/**
	 * Load configuration from given path.
	 *
	 * @param filePath path of configuration file
	 */
	public static void loadConfig(@NonNull String filePath) {
		Logger.getGlobal().info(String.format("Config file path: %s", filePath));
		try (FileInputStream fStream = new FileInputStream(filePath)) {
			properties = new Properties();
			properties.load(fStream);
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(
					String.format("Configuration file not found at %s.", filePath));
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Error reading configuration from %s.", filePath));
		}
	}

	/**
	 * Get property associated with given key.
	 *
	 * @param key key with which property required is associated
	 * @return property associated with given key
	 */
	public static String getProperty(String key) {
		String result = properties.getProperty(key);
		if (result == null)
			throw new IllegalArgumentException(
					String.format("Property %s not found in configuration.", key));
		return result;
	}

	/**
	 * An alias of {@link #getProperty(String)}
	 *
	 * @param key key with which property required is associated
	 * @return property associated with given key
	 */
	public static String getPropertyAsString(String key) {
		return getProperty(key);
	}

	/**
	 * Get property associated with given key and try converting it to integer.
	 *
	 * @param key key with which property required is associated
	 * @return property associated with given key
	 */
	public static int getPropertyAsInt(String key) {
		return Integer.parseInt(getProperty(key));
	}

}
