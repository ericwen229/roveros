package com.ericwen229.util;

import lombok.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {

	private static Properties properties = null;

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

	public static String getProperty(String key) {
		String result = properties.getProperty(key);
		if (result == null)
			throw new IllegalArgumentException(
					String.format("Property %s not found in configuration.", key));
		return result;
	}

	public static String getPropertyAsString(String key) {
		return getProperty(key);
	}

	public static int getPropertyAsInt(String key) {
		return Integer.parseInt(getProperty(key));
	}

	public static double getPropertyAsDouble(String key) {
		return Double.parseDouble(getProperty(key));
	}

}
