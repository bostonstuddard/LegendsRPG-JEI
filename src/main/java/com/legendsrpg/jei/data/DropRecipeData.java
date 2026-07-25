package com.legendsrpg.jei.data;

public record DropRecipeData(
	String id,
	String mobId,
	String mobName,
	String location,
	String coords,
	String description,
	ItemDefinition icon,
	double chance,
	int amount,
	IngredientDefinition output
) {}
