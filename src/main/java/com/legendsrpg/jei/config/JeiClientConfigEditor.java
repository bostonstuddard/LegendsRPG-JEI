package com.legendsrpg.jei.config;

import com.legendsrpg.jei.LegendsRpgJeiClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JeiClientConfigEditor {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("jei/jei-client.ini");

	private JeiClientConfigEditor() {}

	public static synchronized String get(String section, String key, String fallback) {
		try {
			for (String line : Files.readAllLines(PATH)) {
				String trimmed = line.trim();
				if (trimmed.equalsIgnoreCase("[" + section + "]")) {
					return readValueInSection(Files.readAllLines(PATH), section, key, fallback);
				}
			}
		} catch (IOException | RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.debug("Could not read JEI setting {}.{}", section, key, exception);
		}
		return fallback;
	}

	public static synchronized boolean getBoolean(String section, String key, boolean fallback) {
		return Boolean.parseBoolean(get(section, key, Boolean.toString(fallback)));
	}

	public static synchronized int getInt(String section, String key, int fallback) {
		try {
			return Integer.parseInt(get(section, key, Integer.toString(fallback)));
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	public static synchronized boolean setInt(String section, String key, int value) {
		return set(section, key, Integer.toString(value));
	}

	public static synchronized boolean set(String section, String key, String value) {
		try {
			List<String> lines = Files.exists(PATH) ? new ArrayList<>(Files.readAllLines(PATH)) : new ArrayList<>();
			int sectionStart = -1;
			int sectionEnd = lines.size();
			for (int index = 0; index < lines.size(); index++) {
				String trimmed = lines.get(index).trim();
				if (trimmed.equalsIgnoreCase("[" + section + "]")) {
					sectionStart = index;
					continue;
				}
				if (sectionStart >= 0 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
					sectionEnd = index;
					break;
				}
			}

			if (sectionStart < 0) {
				if (!lines.isEmpty() && !lines.getLast().isBlank()) lines.add("");
				lines.add("[" + section + "]");
				lines.add("\t" + key + " = " + value);
			} else {
				boolean replaced = false;
				for (int index = sectionStart + 1; index < sectionEnd; index++) {
					String trimmed = lines.get(index).trim();
					int equals = trimmed.indexOf('=');
					if (equals < 0 || !trimmed.substring(0, equals).trim().equalsIgnoreCase(key)) continue;
					String original = lines.get(index);
					int firstNonWhitespace = 0;
					while (firstNonWhitespace < original.length() && Character.isWhitespace(original.charAt(firstNonWhitespace))) firstNonWhitespace++;
					String indent = original.substring(0, firstNonWhitespace);
					lines.set(index, indent + key + " = " + value);
					replaced = true;
					break;
				}
				if (!replaced) lines.add(sectionEnd, "\t" + key + " = " + value);
			}

			Files.createDirectories(PATH.getParent());
			Files.write(PATH, lines);
			return true;
		} catch (IOException | RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not write JEI setting {}.{}", section, key, exception);
			return false;
		}
	}

	public static synchronized boolean toggleBoolean(String section, String key, boolean fallback) {
		boolean next = !getBoolean(section, key, fallback);
		return set(section, key, Boolean.toString(next));
	}

	private static String readValueInSection(List<String> lines, String section, String key, String fallback) {
		boolean inSection = false;
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
				inSection = trimmed.equalsIgnoreCase("[" + section + "]");
				continue;
			}
			if (!inSection || trimmed.startsWith("#")) continue;
			int equals = trimmed.indexOf('=');
			if (equals < 0 || !trimmed.substring(0, equals).trim().equalsIgnoreCase(key)) continue;
			return trimmed.substring(equals + 1).trim();
		}
		return fallback;
	}
}
