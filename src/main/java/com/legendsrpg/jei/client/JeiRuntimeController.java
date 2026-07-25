package com.legendsrpg.jei.client;

import com.legendsrpg.jei.LegendsRpgJeiClient;
import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.recipe.LegendsRecipeTypes;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JeiRuntimeController {
	private static IJeiRuntime runtime;
	private static List<ItemStack> vanillaStacks = List.of();
	private static List<ItemStack> legendsStacks = List.of();
	private static List<IJeiFluidIngredient> fluidStacks = List.of();
	private static List<IRecipeType<?>> recipeTypes = List.of();
	private static Boolean appliedEnabled;
	private static Boolean appliedRawRecipes;
	private static boolean vanillaPresent;
	private static boolean legendsPresent;
	private static boolean fluidsPresent;

	private JeiRuntimeController() {}

	public static void onAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
		IIngredientManager manager = runtime.getIngredientManager();
		List<ItemStack> allStacks = new ArrayList<>(manager.getAllItemStacks());
		vanillaStacks = allStacks.stream()
			.filter(stack -> !ItemStackFactory.isLegendsItem(stack))
			.map(ItemStack::copy)
			.toList();
		legendsStacks = allStacks.stream()
			.filter(ItemStackFactory::isLegendsItem)
			.map(ItemStack::copy)
			.toList();

		try {
			fluidStacks = List.copyOf(manager.getAllIngredients(FabricTypes.FLUID_STACK));
			fluidsPresent = !fluidStacks.isEmpty();
		} catch (RuntimeException exception) {
			fluidStacks = List.of();
			fluidsPresent = false;
			LegendsRpgJeiClient.LOGGER.warn("Could not read JEI fluid ingredients", exception);
		}

		Map<String, IRecipeType<?>> uniqueTypes = new LinkedHashMap<>();
		runtime.getRecipeManager().createRecipeCategoryLookup().includeHidden().get()
			.map(IRecipeCategory::getRecipeType)
			.forEach(type -> uniqueTypes.put(type.getUid().toString(), type));
		recipeTypes = List.copyOf(uniqueTypes.values());
		vanillaPresent = !vanillaStacks.isEmpty();
		legendsPresent = !legendsStacks.isEmpty();
		appliedEnabled = null;
		appliedRawRecipes = null;
		apply();
	}

	public static void onUnavailable() {
		runtime = null;
		vanillaStacks = List.of();
		legendsStacks = List.of();
		fluidStacks = List.of();
		recipeTypes = List.of();
		appliedEnabled = null;
		appliedRawRecipes = null;
		vanillaPresent = false;
		legendsPresent = false;
		fluidsPresent = false;
	}

	public static boolean showRecipes(ItemStack stack) {
		return show(stack, RecipeIngredientRole.OUTPUT);
	}

	public static boolean showUses(ItemStack stack) {
		return show(stack, RecipeIngredientRole.INPUT);
	}

	private static boolean show(ItemStack stack, RecipeIngredientRole role) {
		if (runtime == null || stack == null || stack.isEmpty()) return false;
		try {
			var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.copy());
			runtime.getRecipesGui().show(focus);
			return true;
		} catch (RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.warn("Could not open JEI {} lookup for {}", role, stack, exception);
			return false;
		}
	}

	public static boolean matchesShowRecipe(KeyEvent event) {
		return matchesKey(event, true);
	}

	public static boolean matchesShowUses(KeyEvent event) {
		return matchesKey(event, false);
	}

	private static boolean matchesKey(KeyEvent event, boolean recipe) {
		if (runtime == null || event == null) return false;
		try {
			var mapping = recipe ? runtime.getKeyMappings().getShowRecipe() : runtime.getKeyMappings().getShowUses();
			return mapping.isActiveAndMatches(InputConstants.getKey(event));
		} catch (RuntimeException exception) {
			return false;
		}
	}

	public static String showRecipeKeyName() {
		return keyName(true, "R");
	}

	public static String showUsesKeyName() {
		return keyName(false, "U");
	}

	private static String keyName(boolean recipe, String fallback) {
		if (runtime == null) return fallback;
		try {
			var mapping = recipe ? runtime.getKeyMappings().getShowRecipe() : runtime.getKeyMappings().getShowUses();
			return mapping.getTranslatedKeyMessage().getString();
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	public static void toggle() {
		LegendsConfig config = LegendsConfig.get();
		config.setEnabled(!config.enabled());
		apply();
	}

	public static void toggleRawRecipes() {
		LegendsConfig config = LegendsConfig.get();
		config.setRawRecipesEnabled(!config.rawRecipesEnabled());
		apply();
	}

	public static void apply() {
		if (runtime == null) return;
		LegendsConfig config = LegendsConfig.get();
		boolean enabled = config.enabled();
		boolean rawEnabled = config.rawRecipesEnabled();
		if (appliedEnabled != null && appliedEnabled == enabled
			&& appliedRawRecipes != null && appliedRawRecipes == rawEnabled) return;

		IIngredientManager manager = runtime.getIngredientManager();
		if (enabled) {
			if (!legendsPresent && !legendsStacks.isEmpty()) {
				manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, legendsStacks);
				legendsPresent = true;
			}
			if (vanillaPresent && !vanillaStacks.isEmpty()) {
				manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, vanillaStacks);
				vanillaPresent = false;
			}
			if (fluidsPresent && !fluidStacks.isEmpty()) {
				manager.removeIngredientsAtRuntime(FabricTypes.FLUID_STACK, fluidStacks);
				fluidsPresent = false;
			}
		} else {
			if (!vanillaPresent && !vanillaStacks.isEmpty()) {
				manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, vanillaStacks);
				vanillaPresent = true;
			}
			if (!fluidsPresent && !fluidStacks.isEmpty()) {
				manager.addIngredientsAtRuntime(FabricTypes.FLUID_STACK, fluidStacks);
				fluidsPresent = true;
			}
			if (legendsPresent && !legendsStacks.isEmpty()) {
				manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, legendsStacks);
				legendsPresent = false;
			}
		}

		for (IRecipeType<?> type : recipeTypes) {
			boolean legendsType = type.getUid().getNamespace().equals(LegendsRpgJeiClient.MOD_ID);
			if (!legendsType) {
				if (enabled) runtime.getRecipeManager().hideRecipeCategory(type);
				else runtime.getRecipeManager().unhideRecipeCategory(type);
				continue;
			}

			boolean hide = !enabled
				|| (!rawEnabled && type.getUid().equals(LegendsRecipeTypes.RAW_MATERIALS.getUid()));
			if (hide) runtime.getRecipeManager().hideRecipeCategory(type);
			else runtime.getRecipeManager().unhideRecipeCategory(type);
		}

		appliedEnabled = enabled;
		appliedRawRecipes = rawEnabled;
		LegendsRpgJeiClient.LOGGER.info(
			"LegendsRPG JEI: viewer={}, raw recipes={}, indexed custom ingredients={}",
			enabled ? "enabled" : "disabled",
			rawEnabled ? "enabled" : "disabled",
			legendsStacks.size()
		);
	}
}
