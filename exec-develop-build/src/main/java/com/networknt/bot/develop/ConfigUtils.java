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
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

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

	/** Merge inputs are repository files, never bundled defaults or service-provider fallbacks. */
	static Map<String, Object> loadRepositoryMapConfig(String configName, File filePath) {
		return ConfigUtilsImpl.loadMapConfig(configName, filePath, false);
	}

	private static final class ConfigUtilsImpl extends ConfigUtils {
		private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

		private static final String CONFIG_EXT_JSON = ".json";
		private static final String CONFIG_EXT_YAML = ".yaml";
		private static final String CONFIG_EXT_YML = ".yml";

		private static final ConfigUtils DEFAULT = initialize();


		private static ConfigUtils initialize() {
			Iterator<ConfigUtils> it;
			it = ServiceLoader.load(ConfigUtils.class).iterator();
			return it.hasNext() ? it.next() : new ConfigUtilsImpl();

		}

		/**
		 * Load a config file as a map. Application loading permits classpath fallback;
		 * merge inputs use the repository-only loader instead.
		 */
		public Map<String, Object> loadMapConfig(String configName, File filePath) {
			return loadMapConfig(configName, filePath, true);
		}

		@SuppressWarnings("unchecked")
		private static Map<String, Object> loadMapConfig(String configName, File filePath, boolean classpathFallback) {
			for (String extension : new String[]{CONFIG_EXT_YML, CONFIG_EXT_YAML, CONFIG_EXT_JSON}) {
				String filename = configName + extension;
				try (InputStream input = getConfigStream(filename, filePath, classpathFallback)) {
					if (input == null) {
						continue;
					}
					if (CONFIG_EXT_JSON.equals(extension)) {
						Map<String, Object> config = Config.getInstance().getMapper().readValue(input,
								new TypeReference<HashMap<String, Object>>() {});
						if (config != null) {
							return config;
						}
					} else {
						logger.info("Loading {} using YAML 1.2 Core; legacy boolean, numeric and timestamp values may resolve differently", filename);
						Object parsed;
						try {
							parsed = BuildYaml.newLoad().loadFromInputStream(input);
						} catch (YamlEngineException e) {
							throw new IllegalStateException("Unable to parse " + filename + " using YAML 1.2 Core", e);
						}
						if (parsed == null) {
							continue;
						}
						if (!(parsed instanceof Map)) {
							throw new IllegalStateException("Configuration " + filename + " must have a mapping root");
						}
						return (Map<String, Object>) parsed;
					}
				} catch (IOException e) {
					logger.error("IOException loading " + filename, e);
				}
			}
			return null;
		}

		/**
		 * Get an input stream for a config file as a map, specifying the file name as well as the directory from where it is loaded
		 */
		private static InputStream getConfigStream(String configFilename, File filePath, boolean classpathFallback) {

			InputStream inStream = null;
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
			if (!classpathFallback) {
				return null;
			}
			if (logger.isInfoEnabled()) {
				logger.info(
						"Trying to load config from classpath directory for file " + Encode.forJava(configFilename));
			}
			inStream = ConfigUtils.class.getClassLoader().getResourceAsStream(configFilename);
			if (inStream != null) {
				if (logger.isInfoEnabled()) {
					logger.info("config loaded from classpath for " + Encode.forJava(configFilename));
				}
				return inStream;
			}
			inStream = ConfigUtils.class.getClassLoader().getResourceAsStream("config/" + configFilename);
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
