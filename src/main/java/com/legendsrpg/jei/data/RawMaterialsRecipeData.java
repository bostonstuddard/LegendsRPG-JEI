package com.legendsrpg.jei.data;

import java.util.List;

public record RawMaterialsRecipeData(
	String id,
	String source,
	List<IngredientDefinition> directInputs,
	List<IngredientDefinition> rawInputs,
	List<String> steps,
	IngredientDefinition output
) {
	public RawMaterialsRecipeData {
		directInputs = directInputs == null ? List.of() : List.copyOf(directInputs);
		rawInputs = rawInputs == null ? List.of() : List.copyOf(rawInputs);
		steps = steps == null ? List.of() : List.copyOf(steps);
	}
}
