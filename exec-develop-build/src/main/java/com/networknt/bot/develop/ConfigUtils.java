package com.networknt.bot.develop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;

/**
 * A singleton config utility class for loading configurations in map format
 *
 */
public abstract class ConfigUtils {
	
	// abstract methods that need be implemented by all implementations
    public abstract Map<String, Object> loadMapConfig(String configName, File filePath);
	
	protected ConfigUtils() {
	}

	public static ConfigUtils getInstance() {
		return ConfigUtilsImpl.DEFAULT;
	}

	private static final class ConfigUtilsImpl extends ConfigUtils {
		private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
		
		String CONFIG_EXT_JSON = ".json";
		String CONFIG_EXT_YAML = ".yaml";
		String CONFIG_EXT_YML = ".yml";

		private static final ConfigUtils DEFAULT = initialize();

		private static ConfigUtils initialize() {
			Iterator<ConfigUtils> it;
			it = ServiceLoader.load(ConfigUtils.class).iterator();
			return it.hasNext() ? it.next() : new ConfigUtilsImpl();

		}

		/**
		 * Load a config file as a map, specifying the file name as well as the directory from where it is loaded
		 */
		public Map<String, Object> loadMapConfig(String configName, File filePath) {
			Map<String, Object> config = null;
			Yaml yaml = new Yaml();

			String ymlFilename = configName + CONFIG_EXT_YML;
			try (InputStream inStream = getConfigStream(ymlFilename, filePath)) {
				if (inStream != null) {
					config = (Map<String, Object>) yaml.load(inStream);
				}
			} catch (IOException ioe) {
				logger.error("IOException", ioe);
			}
			if (config != null)
				return config;

			String yamlFilename = configName + CONFIG_EXT_YAML;
			try (InputStream inStream = getConfigStream(yamlFilename, filePath)) {
				if (inStream != null) {
					config = (Map<String, Object>) yaml.load(inStream);
				}
			} catch (IOException ioe) {
				logger.error("IOException", ioe);
			}
			if (config != null)
				return config;

			String configFilename = configName + CONFIG_EXT_JSON;
			try (InputStream inStream = getConfigStream(configFilename, filePath)) {
				if (inStream != null) {
					config = Config.getInstance().getMapper().readValue(inStream,
							new TypeReference<HashMap<String, Object>>() {
							});
				}
			} catch (IOException ioe) {
				logger.error("IOException", ioe);
			}
			return config;
		}

		/**
		 * Get an input stream for a config file as a map, specifying the file name as well as the directory from where it is loaded
		 */		
		private InputStream getConfigStream(String configFilename, File filePath) {

			FileInputStream inStream = null;
			try {
				inStream = new FileInputStream(filePath + "/"+ configFilename);
			} catch (FileNotFoundException ex) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Unable to load config from externalized folder %s for file %s", filePath, Encode.forJava(configFilename)));
				}
			}
			if (inStream != null) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Config loaded from externalized folder %s for file: %s", filePath, Encode.forJava(configFilename)));
				}
				return inStream;
			}
			if (logger.isInfoEnabled()) {
				logger.info(
						"Trying to load config from classpath directory for file " + Encode.forJava(configFilename));
			}
			inStream = (FileInputStream) getClass().getClassLoader().getResourceAsStream(configFilename);
			if (inStream != null) {
				if (logger.isInfoEnabled()) {
					logger.info("config loaded from classpath for " + Encode.forJava(configFilename));
				}
				return inStream;
			}
			inStream = (FileInputStream) getClass().getClassLoader().getResourceAsStream("config/" + configFilename);
			if (inStream != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Config loaded from default folder for " + Encode.forJava(configFilename));
				}
				return inStream;
			}
			if (configFilename.endsWith(CONFIG_EXT_YML)) {
				logger.info("Unable to load config " + Encode.forJava(configFilename)
						+ ". Looking for the same file name with extension yaml...");
			} else if (configFilename.endsWith(CONFIG_EXT_JSON)) {
				logger.info("Unable to load config " + Encode.forJava(configFilename)
						+ ". Looking for the same file name with extension json...");
			} else {
				System.out.println("Unable to load config '"
						+ Encode.forJava(configFilename.substring(0, configFilename.indexOf(".")))
						+ "' with extension yml, yaml and json from external config, application config and module config. Please ignore this message if you are sure that your application is not using this config file.");
			}
			return null;
		}
	}
}
