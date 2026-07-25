package com.legendsrpg.jei.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.data.RecipeSnapshot;

public final class RecipeActionManager {
	private RecipeActionManager() {}

	public static boolean isFavorite(String key) {
		return LegendsConfig.get().isFavorite(key);
	}

	public static boolean isPinned(String key) {
		RecipeSnapshot pinned = LegendsConfig.get().pinnedRecipe();
		return pinned != null && pinned.key().equals(key);
	}

	public static void toggleFavorite(RecipeSnapshot snapshot) {
		if (snapshot == null || snapshot.key().isBlank()) return;
		LegendsConfig.get().toggleFavorite(snapshot.key());
	}

	public static void togglePinned(RecipeSnapshot snapshot) {
		if (snapshot == null || snapshot.key().isBlank()) return;
		LegendsConfig config = LegendsConfig.get();
		if (isPinned(snapshot.key())) {
			config.setPinnedRecipe(null);
		} else {
			config.setPinnedRecipe(snapshot);
		}
	}

	public static void clearPinned() {
		LegendsConfig.get().setPinnedRecipe(null);
	}

	public static void openFavoriteTree(RecipeSnapshot preferredRoot) {
		Minecraft client = Minecraft.getInstance();
		Screen parent = client.screen;
		if (parent instanceof AbstractContainerScreen<?> && client.player != null) {
			client.player.closeContainer();
			parent = null;
		}
		client.setScreen(new FavoriteRecipeTreeScreen(parent, preferredRoot));
	}
}
