package com.xebialabs.overcast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.Resources;

import static com.google.common.base.Joiner.on;
import static com.google.common.io.Files.readLines;
import static java.nio.charset.Charset.defaultCharset;

public class PropertiesLoader {

    private static Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);

    public static final String OVERCAST_PROPERTY_FILE = "overcast.properties";


    public static Properties loadOvercastProperties() {
        try {
            Properties properties = new Properties();
            loadOvercastPropertiesFromClasspath(properties);
            loadOvercastPropertiesFromHomeDirectory(properties);
            loadOvercastPropertiesFromCurrentDirectory(properties);
            return properties;
        } catch (IOException exc) {
            throw new RuntimeException("Cannot load " + OVERCAST_PROPERTY_FILE, exc);
        }
    }


    private static void loadOvercastPropertiesFromClasspath(final Properties properties) throws IOException {
        URL resource = Resources.getResource(OVERCAST_PROPERTY_FILE);
        if (resource != null) {
            loadOvercastPropertiesFromFile(new File(resource.getFile()), properties);
        } else {
            logger.warn("File {} not found on classpath.", OVERCAST_PROPERTY_FILE);
        }
    }

    private static void loadOvercastPropertiesFromCurrentDirectory(final Properties properties) throws IOException {
        loadOvercastPropertiesFromFile(new File(OVERCAST_PROPERTY_FILE), properties);
    }

    private static void loadOvercastPropertiesFromHomeDirectory(final Properties properties) throws IOException {
        loadOvercastPropertiesFromFile(new File(System.getProperty("user.home"), ".overcast" + File.separator + OVERCAST_PROPERTY_FILE), properties);
    }

    private static void loadOvercastPropertiesFromFile(File file, Properties properties) throws IOException {
        if (file.exists()) {
            logger.info("Loading from file {}", file.getAbsolutePath());
            String fileContent = on("\n").join(readLines(file, defaultCharset()));
            properties.load(new ByteArrayInputStream(processed(fileContent).getBytes()));
        } else {
            logger.warn("File {} not found.", file.getAbsolutePath());
        }
    }

    private static String processed(String s) {
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            s = s.replace("${env." + e.getKey() + "}", e.getValue());
        }
        return s;
    }

}



