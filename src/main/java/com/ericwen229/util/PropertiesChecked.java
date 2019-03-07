package com.ericwen229.util;

import lombok.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertiesChecked {

    private final Properties properties;

    public PropertiesChecked(@NonNull String filePath) {
        try (FileInputStream fStream = new FileInputStream(filePath)) {
            properties = new java.util.Properties();
            properties.load(fStream);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(
                    String.format("PropertiesChecked file not found at %s.", filePath));
        }
        catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error reading properties from %s.", filePath));
        }
    }

    public String getPropertyChecked(@NonNull String key) {
        String result = properties.getProperty(key);
        if (result == null) {
            throw new RuntimeException(String.format("No properties associated with %s are found.", key));
        }
        return result;
    }

}
