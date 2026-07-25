package com.legendsrpg.jei.data;

import java.util.List;

public record CraftingRecipeData(
	String id,
	String group,
	boolean shapeless,
	List<IngredientDefinition> inputs,
	IngredientDefinition output
) {
	public CraftingRecipeData {
		inputs = inputs == null ? List.of() : List.copyOf(inputs);
	}
}
