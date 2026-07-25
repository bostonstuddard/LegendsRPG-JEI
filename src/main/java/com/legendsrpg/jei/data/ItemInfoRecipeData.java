package com.legendsrpg.jei.data;

import java.util.List;

public record ItemInfoRecipeData(
	String id,
	IngredientDefinition output,
	List<String> methods
) {
	public ItemInfoRecipeData {
		methods = methods == null ? List.of() : List.copyOf(methods);
	}
}
