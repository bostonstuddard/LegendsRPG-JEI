package com.legendsrpg.jei.data;

import java.util.List;

public record SourceRecipeData(
	String id,
	String type,
	String source,
	String detail,
	String chanceText,
	int amount,
	String icon,
	List<String> notes,
	IngredientDefinition output
) {
	public SourceRecipeData {
		notes = notes == null ? List.of() : List.copyOf(notes);
		amount = Math.max(1, amount);
	}
}
