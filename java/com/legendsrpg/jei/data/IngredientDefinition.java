package com.legendsrpg.jei.data;

public record IngredientDefinition(String id, int count, String label) {
	public IngredientDefinition {
		count = Math.max(1, count);
	}
}
