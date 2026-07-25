package com.legendsrpg.jei;

import com.legendsrpg.jei.client.JeiRuntimeController;
import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.ShopGroupData;
import com.legendsrpg.jei.recipe.CraftingCategory;
import com.legendsrpg.jei.recipe.ForgeCategory;
import com.legendsrpg.jei.recipe.ItemInfoCategory;
import com.legendsrpg.jei.recipe.LegendsRecipeTypes;
import com.legendsrpg.jei.recipe.CompactItemStackRenderer;
import com.legendsrpg.jei.recipe.OreForgeCategory;
import com.legendsrpg.jei.recipe.MiningResearchCategory;
import com.legendsrpg.jei.recipe.MiningTreasureCategory;
import com.legendsrpg.jei.recipe.MobDropCategory;
import com.legendsrpg.jei.recipe.RawMaterialsCategory;
import com.legendsrpg.jei.recipe.ShopCategory;
import com.legendsrpg.jei.recipe.SourceCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@JeiPlugin
public final class LegendsRpgJeiPlugin implements IModPlugin {
	private static final Identifier UID = Identifier.fromNamespaceAndPath(LegendsRpgJeiClient.MOD_ID, "plugin");

	@Override
	public Identifier getPluginUid() { return UID; }

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		Set<Item> bases = new LinkedHashSet<>();
		for (ItemDefinition item : AddonData.get().items()) {
			bases.add(ItemStackFactory.baseItem(item));
		}
		bases.add(Items.KNOWLEDGE_BOOK);

		for (Item base : bases) {
			registration.registerSubtypeInterpreter(base, (stack, context) -> ItemStackFactory.customItemId(stack));
		}
		LegendsRpgJeiClient.LOGGER.info(
			"Registered custom_item_id subtype matching for {} vanilla item bases",
			bases.size()
		);
	}

	@Override
	public void registerExtraIngredients(IExtraIngredientRegistration registration) {
		AddonData data = AddonData.get();
		Set<String> obtainableItemIds = data.itemInfoRecipes().stream()
			.filter(recipe -> !recipe.methods().isEmpty())
			.map(recipe -> recipe.id())
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

		List<ItemStack> items = data.items().stream()
			.filter(item -> obtainableItemIds.contains(item.id()))
			.map(item -> ItemStackFactory.create(item.id(), 1))
			.toList();

		registration.addExtraItemStacks(items);
		LegendsRpgJeiClient.LOGGER.info(
			"Registered {} obtainable LegendsRPG items; hid {} items with no acquisition methods and all research-tier helper stacks",
			items.size(),
			data.items().size() - items.size()
		);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(Items.KNOWLEDGE_BOOK), LegendsRecipeTypes.MINING_RESEARCH);
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		LegendsRpgJeiClient.LOGGER.info("JEI plugin registered: adding LegendsRPG categories");
		var helpers = registration.getJeiHelpers();
		var guiHelper = helpers.getGuiHelper();
		CompactItemStackRenderer.install(helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK));
		List<IRecipeCategory<?>> categories = new ArrayList<>();
		categories.add(new CraftingCategory(guiHelper));
		categories.add(new RawMaterialsCategory(guiHelper));
		categories.add(new ForgeCategory(guiHelper));
		categories.add(new OreForgeCategory(guiHelper));
		categories.add(new MiningResearchCategory(guiHelper));
		for (ShopGroupData group : AddonData.get().shopGroups()) {
			categories.add(new ShopCategory(guiHelper, group));
		}
		categories.add(new MiningTreasureCategory(guiHelper));
		categories.add(new MobDropCategory(guiHelper));
		categories.add(new SourceCategory(guiHelper));
		categories.add(new ItemInfoCategory(guiHelper));
		registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
		LegendsRpgJeiClient.LOGGER.info("Registered {} individual shop tabs", AddonData.get().shopGroups().size());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		LegendsRpgJeiClient.LOGGER.info("JEI plugin registered: adding LegendsRPG recipes");
		AddonData data = AddonData.get();
		registration.addRecipes(LegendsRecipeTypes.CRAFTING, data.craftingRecipes());
		registration.addRecipes(LegendsRecipeTypes.RAW_MATERIALS, data.rawMaterialsRecipes());
		registration.addRecipes(LegendsRecipeTypes.SMITHING, data.forgeRecipes());
		registration.addRecipes(LegendsRecipeTypes.ORE_FORGE, data.oreForgeRecipes());
		registration.addRecipes(LegendsRecipeTypes.MINING_RESEARCH, data.researchPages());
		for (ShopGroupData group : data.shopGroups()) {
			registration.addRecipes(LegendsRecipeTypes.shop(group.key()), group.recipes());
		}
		registration.addRecipes(LegendsRecipeTypes.MINING_TREASURE, data.treasureRecipes());
		registration.addRecipes(LegendsRecipeTypes.MOB_DROP, data.dropRecipes());
		registration.addRecipes(LegendsRecipeTypes.SOURCE, data.sourceRecipes());
		registration.addRecipes(
			LegendsRecipeTypes.ITEM_INFO,
			data.itemInfoRecipes().stream().filter(recipe -> !recipe.methods().isEmpty()).toList()
		);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) { JeiRuntimeController.onAvailable(jeiRuntime); }
	@Override
	public void onRuntimeUnavailable() { JeiRuntimeController.onUnavailable(); }
}
