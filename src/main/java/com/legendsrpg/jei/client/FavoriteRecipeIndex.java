package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.data.CraftingRecipeData;
import com.legendsrpg.jei.data.DropRecipeData;
import com.legendsrpg.jei.data.ForgeRecipeData;
import com.legendsrpg.jei.data.ItemInfoRecipeData;
import com.legendsrpg.jei.data.OreForgeRecipeData;
import com.legendsrpg.jei.data.RawMaterialsRecipeData;
import com.legendsrpg.jei.data.ResearchPageRecipeData;
import com.legendsrpg.jei.data.ShopRecipeData;
import com.legendsrpg.jei.data.SourceRecipeData;
import com.legendsrpg.jei.data.TreasureRecipeData;
import com.legendsrpg.jei.recipe.OreForgeCategory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FavoriteRecipeIndex {
	private FavoriteRecipeIndex() {}

	public static List<?> prioritize(List<?> recipes) {
		if (recipes == null || recipes.size() < 2 || LegendsConfig.get().favoriteCount() == 0) {
			if (recipes != null && !recipes.isEmpty()) applyVariant(recipes.get(0));
			return recipes;
		}
		List<Object> sorted = new ArrayList<>(recipes);
		sorted.sort(Comparator.comparingInt(FavoriteRecipeIndex::rank).reversed());
		if (!sorted.isEmpty()) applyVariant(sorted.get(0));
		return List.copyOf(sorted);
	}

	public static int categoryRank(IRecipeManager manager, IFocusGroup focuses, IRecipeCategory<?> category) {
		LegendsConfig config = LegendsConfig.get();
		if (config.favoriteCount() == 0 || !config.hasFavoriteCategory(favoriteCategoryPath(category))) return -1;
		try {
			return categoryRankUnchecked(manager, focuses, category);
		} catch (RuntimeException ignored) {
			return -1;
		}
	}

	private static String favoriteCategoryPath(IRecipeCategory<?> category) {
		String path = category.getRecipeType().getUid().getPath();
		if (path.startsWith("shop/")) return path;
		return switch (path) {
			case "raw_materials" -> "raw";
			case "mining_research" -> "research";
			case "item_info" -> "item";
			default -> path;
		};
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static int categoryRankUnchecked(IRecipeManager manager, IFocusGroup focuses, IRecipeCategory category) {
		IRecipeType type = category.getRecipeType();
		return manager.createRecipeLookup(type)
			.limitFocus(focuses.getAllFocuses())
			.get()
			.mapToInt(FavoriteRecipeIndex::rank)
			.max()
			.orElse(-1);
	}

	public static int defaultCategoryPriority(IRecipeCategory<?> category) {
		String path = category.getRecipeType().getUid().getPath();
		String preference = LegendsConfig.get().defaultRecipePage();
		if (LegendsConfig.PAGE_ORIGINAL.equals(preference)) return 0;
		if (path.startsWith("shop/")) return LegendsConfig.PAGE_SHOP_FIRST.equals(preference) ? 0 : 1;
		if (path.equals("source")) return LegendsConfig.PAGE_SOURCE_FIRST.equals(preference) ? 0 : 1;
		if (path.equals("item_info")) return 100;
		if (path.equals("mob_drop") || path.equals("mining_treasure")) return 3;
		return 2;
	}

	public static int rank(Object recipe) {
		LegendsConfig config = LegendsConfig.get();
		String key = key(recipe);
		if (key == null) return -1;
		if (!(recipe instanceof OreForgeRecipeData)) return config.favoriteRank(key);

		int best = -1;
		String prefix = key + "/tier_";
		for (String favorite : config.favoriteRecipeKeys()) {
			if (favorite.startsWith(prefix)) best = Math.max(best, config.favoriteRank(favorite));
		}
		return best;
	}

	public static String key(Object recipe) {
		if (recipe instanceof CraftingRecipeData value) return "crafting/" + value.id();
		if (recipe instanceof RawMaterialsRecipeData value) return "raw/" + value.id();
		if (recipe instanceof ForgeRecipeData value) return "smithing/" + value.id();
		if (recipe instanceof OreForgeRecipeData value) return "ore_forge/" + value.id();
		if (recipe instanceof ShopRecipeData value) return "shop/" + value.sourceKey() + "/" + value.id();
		if (recipe instanceof TreasureRecipeData value) return "mining_treasure/" + value.id();
		if (recipe instanceof DropRecipeData value) return "mob_drop/" + value.id();
		if (recipe instanceof SourceRecipeData value) return "source/" + sanitize(value.id());
		if (recipe instanceof ItemInfoRecipeData value) return "item/" + sanitize(value.id());
		if (recipe instanceof ResearchPageRecipeData value) return "research/tier_" + value.tier() + "/" + value.pageId();
		return null;
	}

	private static void applyVariant(Object recipe) {
		if (!(recipe instanceof OreForgeRecipeData value)) return;
		String prefix = "ore_forge/" + value.id() + "/tier_";
		List<String> favorites = LegendsConfig.get().favoriteRecipeKeys();
		for (int index = favorites.size() - 1; index >= 0; index--) {
			String favorite = favorites.get(index);
			if (!favorite.startsWith(prefix)) continue;
			try {
				OreForgeCategory.setSelectedTier(Integer.parseInt(favorite.substring(prefix.length())));
			} catch (NumberFormatException ignored) { }
			return;
		}
	}

	private static String sanitize(String id) {
		return id.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
	}
}
