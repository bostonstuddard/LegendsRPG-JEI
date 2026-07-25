package com.legendsrpg.jei.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.legendsrpg.jei.LegendsRpgJeiClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class UpdateManager {
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(REQUEST_TIMEOUT)
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private static final String MOD_JAR_PREFIX = "legendsrpg-jei";

	private static volatile State state = State.DISABLED;
	private static volatile UpdateManifest availableManifest;
	private static volatile String statusMessage = "Updater not configured";

	private UpdateManager() {}

	public static void init() {
		UpdaterConfig.reload();
		if (!UpdaterConfig.get().enabled()) {
			state = State.DISABLED;
			statusMessage = "Updater disabled";
			return;
		}
		if (UpdaterConfig.get().manifestUrl().isBlank()) {
			state = State.DISABLED;
			statusMessage = "Updater not configured";
			return;
		}
		checkForUpdates();
	}

	public static void checkForUpdates() {
		String manifestUrl = UpdaterConfig.get().manifestUrl();
		if (!UpdaterConfig.get().enabled() || manifestUrl.isBlank()) {
			state = State.DISABLED;
			statusMessage = manifestUrl.isBlank() ? "Updater not configured" : "Updater disabled";
			return;
		}
		if (state == State.CHECKING || state == State.DOWNLOADING || state == State.STAGED) return;

		state = State.CHECKING;
		statusMessage = "Checking for updates...";
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(manifestUrl))
				.timeout(REQUEST_TIMEOUT)
				.header("Accept", "application/json")
				.GET()
				.build();
		} catch (RuntimeException exception) {
			failCheck("Invalid updater manifest URL", exception);
			return;
		}

		HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
			.thenAccept(UpdateManager::handleManifestResponse)
			.exceptionally(throwable -> {
				failCheck("Could not check for updates", throwable);
				return null;
			});
	}

	public static Component getButtonText() {
		return switch (state) {
			case DISABLED -> Component.literal("LegendsRPG JEI: " + statusMessage);
			case CHECKING -> Component.literal("LegendsRPG JEI: Checking...");
			case UP_TO_DATE -> Component.literal("LegendsRPG JEI: Up to date");
			case AVAILABLE -> Component.literal("LegendsRPG JEI: Update " + safeVersion());
			case DOWNLOADING -> Component.literal("LegendsRPG JEI: Downloading...");
			case STAGED -> Component.literal("LegendsRPG JEI: Restarting...");
			case FAILED -> Component.literal("LegendsRPG JEI: Retry update check");
		};
	}

	public static Component getCompactButtonText() {
		return switch (state) {
			case DISABLED -> Component.literal("JEI: " + statusMessage);
			case CHECKING -> Component.literal("JEI: Checking...");
			case UP_TO_DATE -> Component.literal("JEI: Up to date");
			case AVAILABLE -> Component.literal("JEI: Update " + safeVersion());
			case DOWNLOADING -> Component.literal("JEI: Downloading...");
			case STAGED -> Component.literal("JEI: Restarting...");
			case FAILED -> Component.literal("JEI: Retry update");
		};
	}

	public static boolean isButtonActive() {
		return state == State.AVAILABLE || state == State.FAILED;
	}

	public static boolean canDownloadUpdate() {
		return state == State.AVAILABLE && availableManifest != null;
	}

	public static UpdateManifest getAvailableManifest() {
		return availableManifest;
	}

	public static String getInstalledVersion() {
		return FabricLoader.getInstance().getModContainer(LegendsRpgJeiClient.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("0.0.0");
	}

	public static String getStatusMessage() {
		return statusMessage;
	}

	public static void downloadAndInstall(Minecraft minecraft) {
		UpdateManifest manifest = availableManifest;
		if (manifest == null || state == State.DOWNLOADING || state == State.STAGED) return;
		String downloadUrl = trim(manifest.downloadUrl());
		if (downloadUrl.isBlank()) {
			failInstall("Manifest is missing a download URL", null);
			return;
		}

		state = State.DOWNLOADING;
		statusMessage = "Downloading " + manifest.version();
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(downloadUrl))
				.timeout(Duration.ofMinutes(2))
				.header("Accept", "application/java-archive,application/octet-stream,*/*")
				.GET()
				.build();
		} catch (RuntimeException exception) {
			failInstall("Invalid update download URL", exception);
			return;
		}

		HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
			.thenAccept(response -> {
				try {
					if (response.statusCode() < 200 || response.statusCode() >= 300) {
						throw new IOException("Update download returned HTTP " + response.statusCode());
					}
					stageUpdate(manifest, response.body());
					state = State.STAGED;
					statusMessage = "Update staged; closing Minecraft";
					minecraft.execute(minecraft::stop);
				} catch (Exception exception) {
					failInstall("Could not install update", exception);
				}
			})
			.exceptionally(throwable -> {
				failInstall("Could not download update", throwable);
				return null;
			});
	}

	private static void handleManifestResponse(HttpResponse<String> response) {
		try {
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IOException("Manifest returned HTTP " + response.statusCode());
			}
			UpdateManifest manifest = parseManifest(response.body());
			String gameVersion = FabricLoader.getInstance().getRawGameVersion();
			if (!trim(manifest.minecraftVersion()).isBlank() && !trim(manifest.minecraftVersion()).equals(gameVersion)) {
				availableManifest = null;
				state = State.UP_TO_DATE;
				statusMessage = "No update for Minecraft " + gameVersion;
				return;
			}
			if (isRemoteVersionNewer(manifest.version())) {
				availableManifest = manifest;
				state = State.AVAILABLE;
				statusMessage = "Update " + manifest.version() + " available";
			} else {
				availableManifest = null;
				state = State.UP_TO_DATE;
				statusMessage = "Up to date";
			}
		} catch (Exception exception) {
			failCheck("Could not read update manifest", exception);
		}
	}

	private static UpdateManifest parseManifest(String json) throws IOException {
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		UpdateManifest manifest = new UpdateManifest(
			getString(object, "version"),
			getString(object, "minecraftVersion"),
			getString(object, "jarName"),
			getString(object, "downloadUrl"),
			getString(object, "sha256"),
			getString(object, "releasePage")
		);
		if (trim(manifest.version()).isBlank()) throw new IOException("Manifest is missing version");
		if (trim(manifest.jarName()).isBlank()) throw new IOException("Manifest is missing jarName");
		if (trim(manifest.downloadUrl()).isBlank()) throw new IOException("Manifest is missing downloadUrl");
		return manifest;
	}

	private static String getString(JsonObject object, String key) {
		return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
	}

	private static boolean isRemoteVersionNewer(String remote) {
		try {
			return Version.parse(trim(remote)).compareTo(Version.parse(getInstalledVersion())) > 0;
		} catch (VersionParsingException exception) {
			return compareLooseVersions(remote, getInstalledVersion()) > 0;
		}
	}

	private static int compareLooseVersions(String left, String right) {
		String[] leftParts = normalizeVersion(left).split("\\.");
		String[] rightParts = normalizeVersion(right).split("\\.");
		int count = Math.max(leftParts.length, rightParts.length);
		for (int index = 0; index < count; index++) {
			int a = index < leftParts.length ? parseVersionPart(leftParts[index]) : 0;
			int b = index < rightParts.length ? parseVersionPart(rightParts[index]) : 0;
			if (a != b) return Integer.compare(a, b);
		}
		return 0;
	}

	private static String normalizeVersion(String value) {
		return trim(value).toLowerCase(Locale.ROOT).replaceFirst("^[^0-9]*", "").replaceAll("[^0-9.].*$", "");
	}

	private static int parseVersionPart(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	private static void stageUpdate(UpdateManifest manifest, byte[] bytes) throws IOException {
		verifySha256(manifest, bytes);
		validateDownloadedJar(manifest, bytes);

		Path gameDirectory = FabricLoader.getInstance().getGameDir();
		Path modsDirectory = gameDirectory.resolve("mods");
		Files.createDirectories(modsDirectory);
		Path stagingDirectory = gameDirectory.resolve(".legendsrpg-jei-update");
		Files.createDirectories(stagingDirectory);

		String jarName = safeJarName(manifest.jarName());
		Path stagedJar = stagingDirectory.resolve(jarName + ".download");
		Path targetJar = modsDirectory.resolve(jarName);
		Files.write(stagedJar, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

		List<Path> activeJars = findActiveJars(modsDirectory);
		Path script = createDeferredInstallScript(stagingDirectory, stagedJar, targetJar, activeJars);
		launchDeferredInstallScript(script);
	}

	private static void validateDownloadedJar(UpdateManifest manifest, byte[] bytes) throws IOException {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!"fabric.mod.json".equals(entry.getName())) continue;
				String json = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
				JsonObject object = JsonParser.parseString(json).getAsJsonObject();
				String id = getString(object, "id");
				String version = getString(object, "version");
				if (!LegendsRpgJeiClient.MOD_ID.equals(id)) throw new IOException("Downloaded JAR has unexpected mod id: " + id);
				if (!trim(manifest.version()).equals(trim(version))) {
					throw new IOException("Downloaded JAR version " + version + " does not match manifest " + manifest.version());
				}
				return;
			}
		}
		throw new IOException("Downloaded JAR is missing fabric.mod.json");
	}

	private static void verifySha256(UpdateManifest manifest, byte[] bytes) throws IOException {
		String expected = trim(manifest.sha256()).replaceAll("[^A-Fa-f0-9]", "").toLowerCase(Locale.ROOT);
		if (expected.isBlank()) return;
		try {
			String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
			if (!actual.equals(expected)) throw new IOException("Downloaded JAR SHA-256 does not match manifest");
		} catch (NoSuchAlgorithmException exception) {
			throw new IOException("SHA-256 is unavailable", exception);
		}
	}

	private static String safeJarName(String value) throws IOException {
		String name = trim(value);
		if (name.isBlank() || !name.toLowerCase(Locale.ROOT).endsWith(".jar") || name.contains("/") || name.contains("\\") || name.contains("..")) {
			throw new IOException("Unsafe update JAR name: " + value);
		}
		return name;
	}

	private static List<Path> findActiveJars(Path modsDirectory) throws IOException {
		List<Path> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDirectory, "*.jar")) {
			for (Path path : stream) {
				String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.startsWith(MOD_JAR_PREFIX)) result.add(path.toAbsolutePath());
			}
		}
		return List.copyOf(result);
	}

	private static Path createDeferredInstallScript(Path directory, Path stagedJar, Path targetJar, List<Path> oldJars) throws IOException {
		long pid = ProcessHandle.current().pid();
		boolean windows = isWindows();
		Path script = directory.resolve(windows ? "apply-update.cmd" : "apply-update.sh");
		StringBuilder content = new StringBuilder();

		if (windows) {
			content.append("@echo off\r\nsetlocal\r\n");
			content.append(":wait\r\ntasklist /FI \"PID eq ").append(pid).append("\" 2>NUL | find \"").append(pid).append("\" >NUL\r\n");
			content.append("if not errorlevel 1 (timeout /t 1 /nobreak >NUL & goto wait)\r\n");
			for (Path old : oldJars) {
				Path disabled = old.resolveSibling(old.getFileName() + ".disabled");
				content.append("move /Y ").append(quoteWindows(old)).append(' ').append(quoteWindows(disabled)).append(" >NUL\r\n");
			}
			content.append("move /Y ").append(quoteWindows(stagedJar)).append(' ').append(quoteWindows(targetJar)).append(" >NUL\r\n");
			for (Path old : oldJars) {
				Path disabled = old.resolveSibling(old.getFileName() + ".disabled");
				content.append("del /Q ").append(quoteWindows(disabled)).append(" 2>NUL\r\n");
			}
			content.append("del /Q \"%~f0\"\r\n");
		} else {
			content.append("#!/bin/sh\n");
			content.append("while kill -0 ").append(pid).append(" 2>/dev/null; do sleep 1; done\n");
			for (Path old : oldJars) {
				Path disabled = old.resolveSibling(old.getFileName() + ".disabled");
				content.append("mv -f ").append(quoteShell(old)).append(' ').append(quoteShell(disabled)).append("\n");
			}
			content.append("mv -f ").append(quoteShell(stagedJar)).append(' ').append(quoteShell(targetJar)).append("\n");
			for (Path old : oldJars) {
				Path disabled = old.resolveSibling(old.getFileName() + ".disabled");
				content.append("rm -f ").append(quoteShell(disabled)).append("\n");
			}
			content.append("rm -f -- \"$0\"\n");
		}
		Files.writeString(script, content.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (!windows) script.toFile().setExecutable(true);
		return script;
	}

	private static void launchDeferredInstallScript(Path script) throws IOException {
		ProcessBuilder builder;
		if (isWindows()) {
			builder = new ProcessBuilder("cmd.exe", "/c", "start", "", "/min", script.toAbsolutePath().toString());
		} else {
			builder = new ProcessBuilder("sh", script.toAbsolutePath().toString());
		}
		builder.directory(script.getParent().toFile());
		builder.redirectErrorStream(true);
		builder.start();
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static String quoteWindows(Path path) {
		return "\"" + path.toAbsolutePath() + "\"";
	}

	private static String quoteShell(Path path) {
		return "'" + path.toAbsolutePath().toString().replace("'", "'\\''") + "'";
	}

	private static void failCheck(String message, Throwable throwable) {
		availableManifest = null;
		state = State.FAILED;
		statusMessage = message;
		if (throwable == null) LegendsRpgJeiClient.LOGGER.warn(message);
		else LegendsRpgJeiClient.LOGGER.warn(message, throwable);
	}

	private static void failInstall(String message, Throwable throwable) {
		state = State.FAILED;
		statusMessage = message;
		if (throwable == null) LegendsRpgJeiClient.LOGGER.warn(message);
		else LegendsRpgJeiClient.LOGGER.warn(message, throwable);
	}

	private static String safeVersion() {
		return availableManifest == null ? "available" : availableManifest.version();
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	public enum State {
		DISABLED,
		CHECKING,
		UP_TO_DATE,
		AVAILABLE,
		DOWNLOADING,
		STAGED,
		FAILED
	}
}
