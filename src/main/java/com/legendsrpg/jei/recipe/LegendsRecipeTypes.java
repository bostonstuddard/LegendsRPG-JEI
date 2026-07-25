package com.legendsrpg.jei.recipe;

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
import mezz.jei.api.recipe.types.IRecipeType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LegendsRecipeTypes {
	public static final IRecipeType<CraftingRecipeData> CRAFTING =
		IRecipeType.create("legendsrpg_jei", "crafting", CraftingRecipeData.class);
	public static final IRecipeType<RawMaterialsRecipeData> RAW_MATERIALS =
		IRecipeType.create("legendsrpg_jei", "raw_materials", RawMaterialsRecipeData.class);
	public static final IRecipeType<ForgeRecipeData> SMITHING =
		IRecipeType.create("legendsrpg_jei", "smithing", ForgeRecipeData.class);
	public static final IRecipeType<OreForgeRecipeData> ORE_FORGE =
		IRecipeType.create("legendsrpg_jei", "ore_forge", OreForgeRecipeData.class);
	public static final IRecipeType<ResearchPageRecipeData> MINING_RESEARCH =
		IRecipeType.create("legendsrpg_jei", "mining_research", ResearchPageRecipeData.class);
	public static final IRecipeType<TreasureRecipeData> MINING_TREASURE =
		IRecipeType.create("legendsrpg_jei", "mining_treasure", TreasureRecipeData.class);
	public static final IRecipeType<DropRecipeData> MOB_DROP =
		IRecipeType.create("legendsrpg_jei", "mob_drop", DropRecipeData.class);
	public static final IRecipeType<SourceRecipeData> SOURCE =
		IRecipeType.create("legendsrpg_jei", "source", SourceRecipeData.class);
	public static final IRecipeType<ItemInfoRecipeData> ITEM_INFO =
		IRecipeType.create("legendsrpg_jei", "item_info", ItemInfoRecipeData.class);

	private static final Map<String, IRecipeType<ShopRecipeData>> SHOP_TYPES = new LinkedHashMap<>();

	public static synchronized IRecipeType<ShopRecipeData> shop(String sourceKey) {
		String safeKey = sourceKey == null || sourceKey.isBlank() ? "unknown" : CategoryUi.sanitize(sourceKey);
		return SHOP_TYPES.computeIfAbsent(
			safeKey,
			key -> IRecipeType.create("legendsrpg_jei", "shop/" + key, ShopRecipeData.class)
		);
	}

	private LegendsRecipeTypes() {}
}
