package org.osbo.bots.util;

import java.io.*;
import java.util.Properties;

public class CustomProperties {
    private static final Properties properties = new Properties();
    private static File file;

    public CustomProperties() {
        CustomProperties.file = new File("/opt/db/custom.properties");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key, "false");
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static void save() {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Custom Properties File");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
