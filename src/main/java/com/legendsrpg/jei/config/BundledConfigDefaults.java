package com.legendsrpg.jei.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class BundledConfigDefaults {
	private static final Logger LOGGER = LoggerFactory.getLogger("legendsrpg_jei/config-defaults");
	private static final String RESOURCE_ROOT = "legendsrpg_jei/default-config/";
	private static final String VERSION_MARKER = ".legendsrpg-jei-defaults-version";
	private static final int DEFAULTS_VERSION = 9;
	private static final List<String> INSTALL_ONLY_FILES = List.of(
		"legendsrpg-jei-updater.json"
	);
	private static final List<String> DEFAULT_FILES = List.of(
		"legendsrpg-jei.json",
		"jei/blacklist.json",
		"jei/ingredient-list-mod-sort-order.ini",
		"jei/ingredient-list-type-sort-order.ini",
		"jei/jei-client.ini",
		"jei/jei-colors.ini",
		"jei/jei-debug.ini",
		"jei/jei-mod-id-format.ini",
		"jei/recipe-category-sort-order.ini"
	);

	private BundledConfigDefaults() {}

	public static void installMissingDefaults() {
		Path configDirectory = FabricLoader.getInstance().getConfigDir();
		for (String relativePath : DEFAULT_FILES) {
			copyDefault(configDirectory, relativePath, false);
		}
		for (String relativePath : INSTALL_ONLY_FILES) {
			copyDefault(configDirectory, relativePath, false);
		}
		applyMigrations(configDirectory);
	}

	public static RestoreResult restoreAll() {
		Path configDirectory = FabricLoader.getInstance().getConfigDir();
		int restored = 0;
		List<String> failures = new ArrayList<>();

		for (String relativePath : DEFAULT_FILES) {
			if (copyDefault(configDirectory, relativePath, true)) {
				restored++;
			} else {
				failures.add(relativePath);
			}
		}

		if (!writeVersionMarker(configDirectory)) {
			failures.add(VERSION_MARKER);
		}

		return new RestoreResult(restored, List.copyOf(failures));
	}

	private static boolean copyDefault(Path configDirectory, String relativePath, boolean replaceExisting) {
		Path target = configDirectory.resolve(relativePath);
		if (!replaceExisting && Files.exists(target)) {
			return true;
		}

		String resourcePath = RESOURCE_ROOT + relativePath;
		try (InputStream input = BundledConfigDefaults.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (input == null) {
				LOGGER.warn("Bundled default config resource is missing: {}", resourcePath);
				return false;
			}

			Path parent = target.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			if (replaceExisting) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.copy(input, target);
			}
			LOGGER.info("{} default config {}", replaceExisting ? "Restored" : "Installed", target);
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not {} default config {}", replaceExisting ? "restore" : "install", target, exception);
			return false;
		}
	}

	private static void applyMigrations(Path configDirectory) {
		int installedVersion = readVersionMarker(configDirectory);
		if (installedVersion >= DEFAULTS_VERSION) {
			return;
		}

		Path jeiClientConfig = configDirectory.resolve("jei/jei-client.ini");
		if (installedVersion < 2) {
			setIngredientListBackgroundEnabled(jeiClientConfig);
		}
		if (installedVersion < 8) {
			setIniValue(jeiClientConfig, "lookupHistory", "enabled", "false");
			setIniValue(jeiClientConfig, "ingredientList", "buttonNavigationVisibility", "AUTO_HIDE");
		}
		if (installedVersion < 9) {
			setIniValue(jeiClientConfig, "bookmarkList", "drawBackground", "false");
		}

		writeVersionMarker(configDirectory);
	}

	private static void setIngredientListBackgroundEnabled(Path configPath) {
		if (!Files.isRegularFile(configPath)) {
			return;
		}

		try {
			List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
			boolean inIngredientList = false;
			boolean changed = false;

			for (int index = 0; index < lines.size(); index++) {
				String trimmed = lines.get(index).trim();
				if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
					inIngredientList = trimmed.equalsIgnoreCase("[ingredientList]");
					continue;
				}

				if (inIngredientList && trimmed.startsWith("drawBackground")) {
					int equalsIndex = lines.get(index).indexOf('=');
					String indentation = lines.get(index).substring(0, lines.get(index).indexOf('d'));
					if (equalsIndex >= 0 && !trimmed.equalsIgnoreCase("drawBackground = true")) {
						lines.set(index, indentation + "drawBackground = true");
						changed = true;
					}
					break;
				}
			}

			if (changed) {
				Files.write(configPath, lines);
				LOGGER.info("Updated JEI ingredient-list background default in {}", configPath);
			}
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("Could not migrate JEI ingredient-list background setting in {}", configPath, exception);
		}
	}

	private static void setIniValue(Path configPath, String section, String key, String value) {
		if (!Files.isRegularFile(configPath)) return;
		try {
			List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
			boolean inSection = false;
			for (int index = 0; index < lines.size(); index++) {
				String trimmed = lines.get(index).trim();
				if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
					inSection = trimmed.equalsIgnoreCase("[" + section + "]");
					continue;
				}
				if (!inSection || trimmed.startsWith("#")) continue;
				int equals = trimmed.indexOf('=');
				if (equals < 0 || !trimmed.substring(0, equals).trim().equalsIgnoreCase(key)) continue;
				String original = lines.get(index);
				int first = 0;
				while (first < original.length() && Character.isWhitespace(original.charAt(first))) first++;
				lines.set(index, original.substring(0, first) + key + " = " + value);
				Files.write(configPath, lines);
				LOGGER.info("Updated JEI setting {}.{} in {}", section, key, configPath);
				return;
			}
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("Could not migrate JEI setting {}.{} in {}", section, key, configPath, exception);
		}
	}

	private static int readVersionMarker(Path configDirectory) {
		Path marker = configDirectory.resolve(VERSION_MARKER);
		if (!Files.isRegularFile(marker)) {
			return 0;
		}
		try {
			return Integer.parseInt(Files.readString(marker).trim());
		} catch (IOException | NumberFormatException exception) {
			return 0;
		}
	}

	private static boolean writeVersionMarker(Path configDirectory) {
		Path marker = configDirectory.resolve(VERSION_MARKER);
		try {
			Files.createDirectories(configDirectory);
			Files.writeString(marker, Integer.toString(DEFAULTS_VERSION));
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not write default-config version marker {}", marker, exception);
			return false;
		}
	}

	public record RestoreResult(int restoredFiles, List<String> failedFiles) {
		public boolean successful() {
			return failedFiles.isEmpty();
		}
	}
}
