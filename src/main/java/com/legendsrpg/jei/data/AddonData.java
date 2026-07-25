package com.legendsrpg.jei.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.legendsrpg.jei.LegendsRpgJeiClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AddonData {
	private static final String ROOT = "assets/legendsrpg_jei/data/";
	private static final Gson GSON = new GsonBuilder().create();
	private static final AddonData INSTANCE = load();

	private final List<ItemDefinition> items;
	private final Map<String, ItemDefinition> itemById;
	private final List<CraftingRecipeData> craftingRecipes;
	private final List<ShopRecipeData> shopRecipes;
	private final List<ShopGroupData> shopGroups;
	private final List<DropRecipeData> dropRecipes;
	private final List<SourceRecipeData> sourceRecipes;
	private final List<ForgeRecipeData> forgeRecipes;
	private final List<OreForgeRecipeData> oreForgeRecipes;
	private final List<TreasureRecipeData> treasureRecipes;
	private final List<RawMaterialsRecipeData> rawMaterialsRecipes;
	private final List<ResearchTierRecipeData> researchRecipes;
	private final List<ResearchPageRecipeData> researchPages;
	private final List<ItemInfoRecipeData> itemInfoRecipes;

	private AddonData(
		List<ItemDefinition> items,
		List<CraftingRecipeData> craftingRecipes,
		List<ShopRecipeData> shopRecipes,
		List<DropRecipeData> dropRecipes,
		List<SourceRecipeData> sourceRecipes,
		List<ForgeRecipeData> forgeRecipes,
		List<OreForgeRecipeData> oreForgeRecipes,
		List<TreasureRecipeData> treasureRecipes,
		List<RawMaterialsRecipeData> rawMaterialsRecipes,
		List<ResearchTierRecipeData> researchRecipes,
		List<ItemInfoRecipeData> itemInfoRecipes
	) {
		this.items = List.copyOf(items);
		Map<String, ItemDefinition> byId = new LinkedHashMap<>();
		for (ItemDefinition item : items) {
			byId.put(item.id(), item);
		}
		this.itemById = Map.copyOf(byId);
		this.craftingRecipes = List.copyOf(craftingRecipes);
		this.shopRecipes = List.copyOf(shopRecipes);
		Map<String, List<ShopRecipeData>> groupedShops = new LinkedHashMap<>();
		Map<String, String> shopNames = new LinkedHashMap<>();
		for (ShopRecipeData recipe : shopRecipes) {
			groupedShops.computeIfAbsent(recipe.sourceKey(), key -> new ArrayList<>()).add(recipe);
			shopNames.putIfAbsent(recipe.sourceKey(), recipe.source());
		}
		List<ShopGroupData> groups = new ArrayList<>();
		for (Map.Entry<String, List<ShopRecipeData>> entry : groupedShops.entrySet()) {
			groups.add(new ShopGroupData(entry.getKey(), shopNames.get(entry.getKey()), entry.getValue()));
		}
		groups.sort(Comparator.comparing(ShopGroupData::name, String.CASE_INSENSITIVE_ORDER));
		this.shopGroups = List.copyOf(groups);
		this.dropRecipes = List.copyOf(dropRecipes);
		this.sourceRecipes = List.copyOf(sourceRecipes);
		this.forgeRecipes = List.copyOf(forgeRecipes);
		this.oreForgeRecipes = List.copyOf(oreForgeRecipes);
		this.treasureRecipes = List.copyOf(treasureRecipes);
		this.rawMaterialsRecipes = List.copyOf(rawMaterialsRecipes);
		this.researchRecipes = List.copyOf(researchRecipes);
		this.researchPages = ResearchPageRecipeData.expand(this.researchRecipes);
		this.itemInfoRecipes = List.copyOf(itemInfoRecipes);
	}

	public static AddonData get() {
		return INSTANCE;
	}

	private static AddonData load() {
		Type itemsType = new TypeToken<List<ItemDefinition>>() {}.getType();
		Type craftingType = new TypeToken<List<CraftingRecipeData>>() {}.getType();
		Type shopsType = new TypeToken<List<ShopRecipeData>>() {}.getType();
		Type dropsType = new TypeToken<List<DropRecipeData>>() {}.getType();
		Type sourcesType = new TypeToken<List<SourceRecipeData>>() {}.getType();
		Type forgeType = new TypeToken<List<ForgeRecipeData>>() {}.getType();
		Type oreForgeType = new TypeToken<List<OreForgeRecipeData>>() {}.getType();
		Type treasureType = new TypeToken<List<TreasureRecipeData>>() {}.getType();
		Type rawType = new TypeToken<List<RawMaterialsRecipeData>>() {}.getType();
		Type researchType = new TypeToken<List<ResearchTierRecipeData>>() {}.getType();
		Type itemInfoType = new TypeToken<List<ItemInfoRecipeData>>() {}.getType();
		return new AddonData(
			read("items.json", itemsType),
			read("crafting.json", craftingType),
			read("shops.json", shopsType),
			read("drops.json", dropsType),
			read("sources.json", sourcesType),
			read("forge.json", forgeType),
			read("ore_forge.json", oreForgeType),
			read("treasure.json", treasureType),
			read("raw_costs.json", rawType),
			read("research.json", researchType),
			read("item_pages.json", itemInfoType)
		);
	}

	private static <T> T read(String name, Type type) {
		String path = ROOT + name;
		InputStream stream = AddonData.class.getClassLoader().getResourceAsStream(path);
		if (stream == null) {
			throw new IllegalStateException("Missing bundled data file: " + path);
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, type);
		} catch (Exception exception) {
			LegendsRpgJeiClient.LOGGER.error("Failed to load {}", path, exception);
			throw new IllegalStateException("Could not load " + path, exception);
		}
	}

	public List<ItemDefinition> items() { return items; }
	public ItemDefinition item(String id) { return itemById.get(id); }
	public List<CraftingRecipeData> craftingRecipes() { return craftingRecipes; }
	public List<ShopRecipeData> shopRecipes() { return shopRecipes; }
	public List<ShopGroupData> shopGroups() { return shopGroups; }
	public List<DropRecipeData> dropRecipes() { return dropRecipes; }
	public List<SourceRecipeData> sourceRecipes() { return sourceRecipes; }
	public List<ForgeRecipeData> forgeRecipes() { return forgeRecipes; }
	public List<OreForgeRecipeData> oreForgeRecipes() { return oreForgeRecipes; }
	public List<TreasureRecipeData> treasureRecipes() { return treasureRecipes; }
	public List<RawMaterialsRecipeData> rawMaterialsRecipes() { return rawMaterialsRecipes; }
	public List<ResearchTierRecipeData> researchRecipes() { return researchRecipes; }
	public List<ResearchPageRecipeData> researchPages() { return researchPages; }
	public List<ItemInfoRecipeData> itemInfoRecipes() { return itemInfoRecipes; }
}
