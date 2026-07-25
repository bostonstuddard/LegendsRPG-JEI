package com.legendsrpg.jei.data;

import java.util.List;

public record RecipeSnapshot(
	String key,
	String title,
	List<IngredientDefinition> ingredients,
	IngredientDefinition output,
	List<String> notes
) {
	public RecipeSnapshot {
		key = key == null ? "" : key;
		title = title == null || title.isBlank() ? "Pinned Recipe" : title;
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
		notes = notes == null ? List.of() : List.copyOf(notes);
	}
}
