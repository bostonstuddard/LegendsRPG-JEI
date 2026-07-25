package com.legendsrpg.jei.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.legendsrpg.jei.LegendsRpgJeiClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class UpdaterConfig {
	public static final String CHANNEL_STABLE = "STABLE";
	public static final String CHANNEL_DEV = "DEV";
	private static final String DEFAULT_STABLE_MANIFEST_URL = "https://raw.githubusercontent.com/bostonstuddard/LegendsRPG-JEI-Releases/main/update.json";
	private static final String DEFAULT_DEV_MANIFEST_URL = "https://raw.githubusercontent.com/bostonstuddard/LegendsRPG-JEI-Releases/main/update-dev.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("legendsrpg-jei-updater.json");
	private static UpdaterConfig instance = load();

	private boolean enabled = true;
	private String channel = CHANNEL_STABLE;
	private String stableManifestUrl = DEFAULT_STABLE_MANIFEST_URL;
	private String devManifestUrl = DEFAULT_DEV_MANIFEST_URL;
	// Kept so old configs migrate without losing their existing stable URL.
	private String manifestUrl = DEFAULT_STABLE_MANIFEST_URL;

	private UpdaterConfig() {}

	public static UpdaterConfig get() { return instance; }
	public static void reload() { instance = load(); }
	public boolean enabled() { return enabled; }
	public String channel() { return CHANNEL_DEV.equals(channel) ? CHANNEL_DEV : CHANNEL_STABLE; }
	public String channelDisplayName() { return CHANNEL_DEV.equals(channel()) ? "Development" : "Stable"; }

	public String manifestUrl() {
		String value = CHANNEL_DEV.equals(channel()) ? devManifestUrl : stableManifestUrl;
		value = value == null ? "" : value.trim();
		return value.isBlank() ? (CHANNEL_DEV.equals(channel()) ? DEFAULT_DEV_MANIFEST_URL : DEFAULT_STABLE_MANIFEST_URL) : value;
	}

	public void toggleChannel() {
		channel = CHANNEL_DEV.equals(channel()) ? CHANNEL_STABLE : CHANNEL_DEV;
		save();
	}

	private void normalize() {
		channel = channel == null ? CHANNEL_STABLE : channel.toUpperCase(Locale.ROOT).trim();
		if (!CHANNEL_DEV.equals(channel)) channel = CHANNEL_STABLE;
		if ((stableManifestUrl == null || stableManifestUrl.isBlank()) && manifestUrl != null && !manifestUrl.isBlank()) {
			stableManifestUrl = manifestUrl;
		}
		if (stableManifestUrl == null || stableManifestUrl.isBlank()) stableManifestUrl = DEFAULT_STABLE_MANIFEST_URL;
		if (devManifestUrl == null || devManifestUrl.isBlank()) devManifestUrl = DEFAULT_DEV_MANIFEST_URL;
		manifestUrl = stableManifestUrl;
	}

	private static UpdaterConfig load() {
		if (!Files.isRegularFile(PATH)) {
			UpdaterConfig config = new UpdaterConfig();
			config.normalize();
			config.save();
			return config;
		}
		try {
			UpdaterConfig config = GSON.fromJson(Files.readString(PATH), UpdaterConfig.class);
			if (config == null) config = new UpdaterConfig();
			config.normalize();
			return config;
		} catch (IOException | RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not read updater config {}", PATH, exception);
			UpdaterConfig config = new UpdaterConfig();
			config.normalize();
			return config;
		}
	}

	private void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not save updater config {}", PATH, exception);
		}
	}
}
