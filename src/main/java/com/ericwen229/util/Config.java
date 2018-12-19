package com.ericwen229.util;

import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {

	private static final Logger logger = Logger.getLogger(Config.class.getName());

	// ========== singleton instance ==========

	private static Properties properties = null;

	// ========== constructor ==========

	private Config() {}

	// ========== loaders ==========

	public static void loadConfig(@NonNull InputStream iStream) {
		properties = new Properties();
		try {
			properties.load(iStream);
		}
		catch (IOException e) {
			throw new RuntimeException("Error occurred while reading properties.");
		}
		catch (IllegalArgumentException e) {

		}
	}

	// ========== getters ==========

	public static int getIntProperty(String key) {
		return Integer.parseInt(properties.getProperty(key));
	}

	public static String getStringProperty(String key) {
		return properties.getProperty(key);
	}

}
