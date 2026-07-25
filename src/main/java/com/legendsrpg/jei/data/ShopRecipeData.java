package com.legendsrpg.jei.data;

import java.util.List;

public record ShopRecipeData(
	String id,
	String sourceKey,
	String source,
	String costText,
	List<IngredientDefinition> inputs,
	boolean anyOf,
	List<String> unresolved,
	List<String> shopRequirements,
	List<String> tradeRequirements,
	int researchLevel,
	IngredientDefinition output
) {
	public ShopRecipeData {
		inputs = inputs == null ? List.of() : List.copyOf(inputs);
		unresolved = unresolved == null ? List.of() : List.copyOf(unresolved);
		shopRequirements = shopRequirements == null ? List.of() : List.copyOf(shopRequirements);
		tradeRequirements = tradeRequirements == null ? List.of() : List.copyOf(tradeRequirements);
		researchLevel = Math.max(0, researchLevel);
	}
}
