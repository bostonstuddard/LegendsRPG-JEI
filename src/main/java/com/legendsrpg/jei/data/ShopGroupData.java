package com.legendsrpg.jei.data;

import java.util.List;

public record ShopGroupData(
	String key,
	String name,
	List<ShopRecipeData> recipes
) {
	public ShopGroupData {
		recipes = recipes == null ? List.of() : List.copyOf(recipes);
	}
}
