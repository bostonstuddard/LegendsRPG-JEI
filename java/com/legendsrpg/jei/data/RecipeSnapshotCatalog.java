package com.legendsrpg.jei.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeSnapshotCatalog {
	private static final RecipeSnapshotCatalog INSTANCE = new RecipeSnapshotCatalog();

	private final Map<String, RecipeSnapshot> byKey;
	private final Map<String, List<RecipeSnapshot>> byOutput;

	private RecipeSnapshotCatalog() {
		Map<String, RecipeSnapshot> snapshots = new LinkedHashMap<>();
		AddonData data = AddonData.get();

		for (CraftingRecipeData recipe : data.craftingRecipes()) {
			add(snapshots, new RecipeSnapshot(
				"crafting/" + recipe.id(), itemName(recipe.output().id()), recipe.inputs(), recipe.output(),
				List.of(recipe.shapeless() ? "Shapeless crafting recipe" : "Shaped crafting recipe")
			));
		}
		for (RawMaterialsRecipeData recipe : data.rawMaterialsRecipes()) {
			add(snapshots, new RecipeSnapshot(
				"raw/" + recipe.id(), "Raw Cost: " + itemName(recipe.output().id()), recipe.rawInputs(), recipe.output(),
				List.of("Calculated from " + recipe.source())
			));
		}
		for (ForgeRecipeData recipe : data.forgeRecipes()) {
			add(snapshots, new RecipeSnapshot(
				"smithing/" + recipe.id(), recipe.name(), recipe.inputs(), recipe.output(),
				List.of("Smithing Level " + recipe.level(), "Base time: " + format(recipe.smeltingTime()))
			));
		}
		for (OreForgeRecipeData recipe : data.oreForgeRecipes()) {
			for (int tier = 1; tier <= 7; tier++) {
				int amount = recipe.amountAt(tier);
				add(snapshots, new RecipeSnapshot(
					"ore_forge/" + recipe.id() + "/tier_" + tier,
					itemName(recipe.ingotId()) + " — Forge Level " + tier,
					List.of(
						new IngredientDefinition(recipe.oreId(), amount, null),
						new IngredientDefinition("enchanted_coal_block", recipe.coalCost(), null)
					),
					new IngredientDefinition(recipe.ingotId(), amount, null),
					List.of(recipe.minutesAt(tier) + " minutes", exact(recipe.smithingXpAt(tier)) + " Smithing XP")
				));
			}
		}
		for (ShopRecipeData recipe : data.shopRecipes()) {
			List<String> notes = new ArrayList<>();
			if (recipe.anyOf()) notes.add("Choose any one payment item");
			if (recipe.researchLevel() > 0) notes.add("Mining Research Tier " + recipe.researchLevel());
			notes.addAll(recipe.tradeRequirements());
			add(snapshots, new RecipeSnapshot(
				"shop/" + recipe.sourceKey() + "/" + recipe.id(),
				recipe.source() + ": " + itemName(recipe.output().id()),
				recipe.inputs(), recipe.output(), notes
			));
		}
		for (TreasureRecipeData recipe : data.treasureRecipes()) {
			add(snapshots, new RecipeSnapshot(
				"mining_treasure/" + recipe.id(), "Mining Treasure: " + itemName(recipe.output().id()),
				List.of(new IngredientDefinition("minecraft:trapped_chest", 1, "Mining Treasure Chest")),
				recipe.output(), List.of(recipe.dropChance(), recipe.area())
			));
		}
		for (DropRecipeData recipe : data.dropRecipes()) {
			List<String> notes = new ArrayList<>();
			notes.add("Dropped by " + recipe.mobName());
			notes.add(formatChance(recipe.chance()) + "% chance");
			if (recipe.location() != null && !recipe.location().isBlank()) notes.add(recipe.location());
			add(snapshots, new RecipeSnapshot(
				"mob_drop/" + recipe.id(), recipe.mobName() + " Drop",
				List.of(new IngredientDefinition(recipe.icon().id(), 1, recipe.mobName())), recipe.output(), notes
			));
		}
		for (SourceRecipeData recipe : data.sourceRecipes()) {
			List<String> notes = new ArrayList<>();
			notes.add(recipe.type() + ": " + recipe.source());
			if (recipe.detail() != null && !recipe.detail().isBlank()) notes.add(recipe.detail());
			if (recipe.chanceText() != null && !recipe.chanceText().isBlank()) notes.add("Chance: " + recipe.chanceText());
			notes.addAll(recipe.notes());
			add(snapshots, new RecipeSnapshot(
				"source/" + sanitize(recipe.id()), recipe.source(), List.of(),
				new IngredientDefinition(recipe.output().id(), recipe.output().count(), recipe.output().label()), notes
			));
		}
		for (ItemInfoRecipeData recipe : data.itemInfoRecipes()) {
			if (recipe.methods().isEmpty()) continue;
			ItemDefinition definition = data.item(recipe.id());
			List<IngredientDefinition> requirements = new ArrayList<>();
			if (definition != null && definition.researchRequirement() > 0) {
				requirements.add(new IngredientDefinition("minecraft:knowledge_book", 1, "Mining Research Tier " + definition.researchRequirement()));
			}
			List<String> notes = new ArrayList<>();
			if (definition != null) notes.addAll(definition.info());
			notes.addAll(recipe.methods());
			add(snapshots, new RecipeSnapshot(
				"item/" + sanitize(recipe.id()), definition == null ? itemName(recipe.id()) : definition.name(),
				requirements, new IngredientDefinition(recipe.output().id(), 1, recipe.output().label()), notes
			));
		}
		for (ResearchPageRecipeData recipe : data.researchPages()) {
			List<String> notes = new ArrayList<>(recipe.lines());
			notes.add("View: " + recipe.pageTitle());
			add(snapshots, new RecipeSnapshot(
				"research/tier_" + recipe.tier() + "/" + recipe.pageId(),
				"Mining Research Tier " + recipe.tier() + " — " + recipe.pageTitle(),
				recipe.ingredients(), new IngredientDefinition("minecraft:knowledge_book", 1, "Mining Research Tier " + recipe.tier()), notes
			));
		}

		this.byKey = Map.copyOf(snapshots);
		Map<String, List<RecipeSnapshot>> outputMap = new LinkedHashMap<>();
		for (RecipeSnapshot snapshot : snapshots.values()) {
			if (snapshot.output() == null || snapshot.output().id() == null) continue;
			outputMap.computeIfAbsent(snapshot.output().id(), ignored -> new ArrayList<>()).add(snapshot);
		}
		Map<String, List<RecipeSnapshot>> immutable = new LinkedHashMap<>();
		outputMap.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
		this.byOutput = Map.copyOf(immutable);
	}

	public static RecipeSnapshotCatalog get() { return INSTANCE; }
	public RecipeSnapshot byKey(String key) { return byKey.get(key); }
	public List<RecipeSnapshot> byOutput(String itemId) { return byOutput.getOrDefault(itemId, List.of()); }
	public List<RecipeSnapshot> all() { return List.copyOf(byKey.values()); }

	private static void add(Map<String, RecipeSnapshot> target, RecipeSnapshot snapshot) {
		target.put(snapshot.key(), snapshot);
	}

	public static String itemName(String id) {
		ItemDefinition definition = AddonData.get().item(id);
		if (definition != null) return definition.name();
		String path = id != null && id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		if (path == null || path.isBlank()) return "Unknown Item";
		String value = path.replace('_', ' ');
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static String sanitize(String id) {
		return id.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
	}

	private static String exact(int value) {
		return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(value);
	}

	private static String format(double value) {
		return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
	}

	private static String formatChance(double chance) {
		return chance == Math.rint(chance) ? Long.toString(Math.round(chance)) : Double.toString(chance);
	}
}
