package com.ericwen229.util;

import lombok.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {

	private static final Logger logger = Logger.getLogger(Config.class.getName());

	// ========== singleton instance ==========

	private static Properties properties = null;

	// ========== constructor ==========

	private Config() {}

	// ========== loaders ==========

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

	// ========== getters ==========

	public static int getIntProperty(String key) {
		return Integer.parseInt(getProperty(key));
	}

	public static String getProperty(String key) {
		String result = properties.getProperty(key);
		if (result == null)
			throw new IllegalArgumentException(
					String.format("Property %s not found in configuration.", key));
		return result;
	}

}
