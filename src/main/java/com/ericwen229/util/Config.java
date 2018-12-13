package com.ericwen229.util;

import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

	// ========== singleton instance ==========

	private static Properties properties = null;

	// ========== loaders ==========

	public static void loadConfig(@NonNull InputStream iStream) {
		properties = new Properties();
		try {
			properties.load(iStream);
		} catch (IOException e) {
			// TODO
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
