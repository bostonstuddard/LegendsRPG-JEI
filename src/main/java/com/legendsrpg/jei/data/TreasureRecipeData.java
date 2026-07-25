package com.legendsrpg.jei.data;

import java.util.List;

public record TreasureRecipeData(
	String id,
	String chestName,
	String area,
	String spawnText,
	String dropChance,
	int pity,
	List<String> notes,
	IngredientDefinition output
) {
	public TreasureRecipeData {
		notes = notes == null ? List.of() : List.copyOf(notes);
		pity = Math.max(0, pity);
	}
}
