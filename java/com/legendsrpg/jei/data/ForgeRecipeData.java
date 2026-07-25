package com.legendsrpg.jei.data;

import java.util.List;

public record ForgeRecipeData(
	String id,
	int selection,
	String name,
	int level,
	double smeltingTime,
	double smithingTimeReduction,
	List<IngredientDefinition> inputs,
	IngredientDefinition output
) {
	public ForgeRecipeData {
		inputs = inputs == null ? List.of() : List.copyOf(inputs);
		level = Math.max(0, level);
	}
}
