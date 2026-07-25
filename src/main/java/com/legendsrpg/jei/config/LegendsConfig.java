package com.legendsrpg.jei.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.legendsrpg.jei.LegendsRpgJeiClient;
import com.legendsrpg.jei.data.RecipeSnapshot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LegendsConfig {
	public static final String SIDE_LEFT = "LEFT";
	public static final String SIDE_RIGHT = "RIGHT";
	public static final String THEME_CRIMSON = "CRIMSON";
	public static final String THEME_SLATE = "SLATE";
	public static final String THEME_DARK = "DARK";
	public static final String THEME_MINECRAFT = "MINECRAFT";
	public static final String PAGE_SHOP_FIRST = "SHOP_FIRST";
	public static final String PAGE_SOURCE_FIRST = "SOURCE_FIRST";
	public static final String PAGE_ORIGINAL = "ORIGINAL";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("legendsrpg-jei.json");
	private static LegendsConfig instance = load();

	private boolean enabled = true;
	private boolean rawRecipesEnabled = true;
	private boolean abbreviatePinnedAmounts = true;
	private boolean showJeiButtons = false;
	private String pinnedHudSide = SIDE_LEFT;
	private double pinnedHudScale = 0.9D;
	private String theme = THEME_CRIMSON;
	private String defaultRecipePage = PAGE_SHOP_FIRST;
	private List<String> favoriteRecipes = new ArrayList<>();
	private RecipeSnapshot pinnedRecipe;
	private String lastTreeRootKey = "";
	private long lastTreeTargetAmount = 1L;
	private Map<String, String> treeRecipeSelections = new LinkedHashMap<>();

	private transient List<String> favoriteView = List.of();
	private transient Set<String> favoriteSet = Set.of();
	private transient Set<String> favoriteCategories = Set.of();
	private transient Map<String, Integer> favoriteRanks = Map.of();

	private LegendsConfig() {}

	public static LegendsConfig get() { return instance; }
	public static void reload() { instance = load(); }
	public boolean enabled() { return enabled; }
	public boolean rawRecipesEnabled() { return rawRecipesEnabled; }
	public boolean abbreviatePinnedAmounts() { return abbreviatePinnedAmounts; }
	public boolean showJeiButtons() { return showJeiButtons; }
	public String pinnedHudSide() { return pinnedHudSide; }
	public boolean pinnedHudOnLeft() { return SIDE_LEFT.equals(pinnedHudSide); }
	public double pinnedHudScale() { return pinnedHudScale; }
	public String theme() { return theme; }
	public String defaultRecipePage() { return defaultRecipePage; }
	public boolean crimsonTheme() { return THEME_CRIMSON.equals(theme); }
	public RecipeSnapshot pinnedRecipe() { return pinnedRecipe; }
	public int favoriteCount() { return favoriteRecipes.size(); }
	public List<String> favoriteRecipeKeys() { return favoriteView; }
	public boolean isFavorite(String key) { return key != null && favoriteSet.contains(key); }
	public boolean hasFavoriteCategory(String categoryPath) { return categoryPath != null && favoriteCategories.contains(categoryPath); }
	public int favoriteRank(String key) { return key == null ? -1 : favoriteRanks.getOrDefault(key, -1); }
	public String lastTreeRootKey() { return lastTreeRootKey == null ? "" : lastTreeRootKey; }
	public long lastTreeTargetAmount() { return Math.max(1L, lastTreeTargetAmount); }
	public Map<String, String> treeRecipeSelections() { return treeRecipeSelections == null ? Map.of() : Map.copyOf(treeRecipeSelections); }

	public String themeDisplayName() {
		return switch (theme) {
			case THEME_SLATE -> "Dark Slate";
			case THEME_DARK -> "True Dark";
			case THEME_MINECRAFT -> "Minecraft Classic";
			default -> "Inventory Crimson";
		};
	}

	public String defaultRecipePageDisplayName() {
		return switch (defaultRecipePage) {
			case PAGE_SOURCE_FIRST -> "Source First";
			case PAGE_ORIGINAL -> "JEI Original";
			default -> "Shop First";
		};
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		save();
	}

	public void setRawRecipesEnabled(boolean rawRecipesEnabled) {
		this.rawRecipesEnabled = rawRecipesEnabled;
		save();
	}

	public void setAbbreviatePinnedAmounts(boolean abbreviatePinnedAmounts) {
		this.abbreviatePinnedAmounts = abbreviatePinnedAmounts;
		save();
	}

	public void toggleAbbreviatePinnedAmounts() {
		setAbbreviatePinnedAmounts(!abbreviatePinnedAmounts);
	}

	public void setShowJeiButtons(boolean showJeiButtons) {
		this.showJeiButtons = showJeiButtons;
		save();
	}

	public void toggleShowJeiButtons() {
		setShowJeiButtons(!showJeiButtons);
	}

	public void setPinnedHudSide(String pinnedHudSide) {
		this.pinnedHudSide = normalizeSide(pinnedHudSide);
		save();
	}

	public void togglePinnedHudSide() {
		setPinnedHudSide(pinnedHudOnLeft() ? SIDE_RIGHT : SIDE_LEFT);
	}

	public void setPinnedHudScale(double pinnedHudScale) {
		this.pinnedHudScale = clampScale(pinnedHudScale);
		save();
	}

	public void setTheme(String theme) {
		this.theme = normalizeTheme(theme);
		save();
	}

	public void cycleTheme() {
		setTheme(switch (theme) {
			case THEME_CRIMSON -> THEME_SLATE;
			case THEME_SLATE -> THEME_DARK;
			case THEME_DARK -> THEME_MINECRAFT;
			default -> THEME_CRIMSON;
		});
	}

	public void setDefaultRecipePage(String defaultRecipePage) {
		this.defaultRecipePage = normalizeDefaultRecipePage(defaultRecipePage);
		save();
	}

	public void cycleDefaultRecipePage() {
		setDefaultRecipePage(switch (defaultRecipePage) {
			case PAGE_SHOP_FIRST -> PAGE_SOURCE_FIRST;
			case PAGE_SOURCE_FIRST -> PAGE_ORIGINAL;
			default -> PAGE_SHOP_FIRST;
		});
	}

	public void toggleFavorite(String key) {
		if (key == null || key.isBlank()) return;
		if (favoriteSet.contains(key)) favoriteRecipes.remove(key);
		else favoriteRecipes.add(key);
		rebuildFavoriteCache();
		save();
	}

	public void clearFavorites() {
		if (favoriteRecipes.isEmpty()) return;
		favoriteRecipes.clear();
		rebuildFavoriteCache();
		save();
	}

	public void setPinnedRecipe(RecipeSnapshot pinnedRecipe) {
		this.pinnedRecipe = pinnedRecipe;
		save();
	}

	public void saveTreeState(String rootKey, long targetAmount, Map<String, String> selections) {
		this.lastTreeRootKey = rootKey == null ? "" : rootKey;
		this.lastTreeTargetAmount = Math.max(1L, targetAmount);
		this.treeRecipeSelections = selections == null ? new LinkedHashMap<>() : new LinkedHashMap<>(selections);
		save();
	}

	private static LegendsConfig load() {
		if (!Files.exists(PATH)) {
			LegendsConfig config = new LegendsConfig();
			config.normalize();
			config.save();
			return config;
		}
		try {
			LegendsConfig config = GSON.fromJson(Files.readString(PATH), LegendsConfig.class);
			if (config == null) config = new LegendsConfig();
			config.normalize();
			return config;
		} catch (IOException | RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not read {}, using defaults", PATH, exception);
			LegendsConfig config = new LegendsConfig();
			config.normalize();
			return config;
		}
	}

	private void normalize() {
		if (favoriteRecipes == null) favoriteRecipes = new ArrayList<>();
		LinkedHashSet<String> cleanedFavorites = new LinkedHashSet<>();
		for (String key : favoriteRecipes) {
			if (key != null && !key.isBlank()) cleanedFavorites.add(key);
		}
		favoriteRecipes = new ArrayList<>(cleanedFavorites);
		pinnedHudSide = normalizeSide(pinnedHudSide);
		pinnedHudScale = clampScale(pinnedHudScale);
		theme = normalizeTheme(theme);
		defaultRecipePage = normalizeDefaultRecipePage(defaultRecipePage);
		if (lastTreeRootKey == null) lastTreeRootKey = "";
		lastTreeTargetAmount = Math.max(1L, lastTreeTargetAmount);
		if (treeRecipeSelections == null) treeRecipeSelections = new LinkedHashMap<>();
		treeRecipeSelections.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank());
		rebuildFavoriteCache();
	}

	private void rebuildFavoriteCache() {
		LinkedHashSet<String> set = new LinkedHashSet<>();
		LinkedHashSet<String> categories = new LinkedHashSet<>();
		LinkedHashMap<String, Integer> ranks = new LinkedHashMap<>();
		for (int index = 0; index < favoriteRecipes.size(); index++) {
			String key = favoriteRecipes.get(index);
			if (key == null || key.isBlank()) continue;
			set.add(key);
			ranks.put(key, index);
			String category = favoriteCategory(key);
			if (category != null) categories.add(category);
		}
		favoriteView = List.copyOf(set);
		favoriteSet = Set.copyOf(set);
		favoriteCategories = Set.copyOf(categories);
		favoriteRanks = Map.copyOf(ranks);
	}

	private static String favoriteCategory(String key) {
		if (key.startsWith("shop/")) {
			int slash = key.indexOf('/', "shop/".length());
			return slash > 0 ? key.substring(0, slash) : "shop";
		}
		int slash = key.indexOf('/');
		return slash > 0 ? key.substring(0, slash) : key;
	}

	private static String normalizeSide(String value) {
		if (value == null) return SIDE_LEFT;
		String side = value.toUpperCase(Locale.ROOT).trim();
		return SIDE_LEFT.equals(side) ? SIDE_LEFT : SIDE_RIGHT;
	}

	private static String normalizeTheme(String value) {
		if (value == null) return THEME_CRIMSON;
		String normalized = value.toUpperCase(Locale.ROOT).trim();
		return switch (normalized) {
			case THEME_SLATE -> THEME_SLATE;
			case THEME_DARK -> THEME_DARK;
			case THEME_MINECRAFT -> THEME_MINECRAFT;
			default -> THEME_CRIMSON;
		};
	}

	private static String normalizeDefaultRecipePage(String value) {
		if (value == null) return PAGE_SHOP_FIRST;
		String normalized = value.toUpperCase(Locale.ROOT).trim();
		return switch (normalized) {
			case PAGE_SOURCE_FIRST -> PAGE_SOURCE_FIRST;
			case PAGE_ORIGINAL -> PAGE_ORIGINAL;
			default -> PAGE_SHOP_FIRST;
		};
	}

	private static double clampScale(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0D) return 0.9D;
		return Math.max(0.1D, Math.min(2.0D, value));
	}

	private void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not save {}", PATH, exception);
		}
	}
}
