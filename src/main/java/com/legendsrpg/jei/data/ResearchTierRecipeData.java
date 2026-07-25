package com.legendsrpg.jei.data;

import java.util.List;

public record ResearchTierRecipeData(
	String id,
	int tier,
	List<String> unlocks,
	List<IngredientDefinition> costs,
	List<IngredientDefinition> rawCosts,
	List<IngredientDefinition> cumulativeCosts,
	List<IngredientDefinition> cumulativeRawCosts,
	List<String> steps,
	List<String> cumulativeSteps
) {
	public ResearchTierRecipeData {
		tier = Math.max(1, tier);
		unlocks = unlocks == null ? List.of() : List.copyOf(unlocks);
		costs = costs == null ? List.of() : List.copyOf(costs);
		rawCosts = rawCosts == null ? List.of() : List.copyOf(rawCosts);
		cumulativeCosts = cumulativeCosts == null ? List.of() : List.copyOf(cumulativeCosts);
		cumulativeRawCosts = cumulativeRawCosts == null ? List.of() : List.copyOf(cumulativeRawCosts);
		steps = steps == null ? List.of() : List.copyOf(steps);
		cumulativeSteps = cumulativeSteps == null ? List.of() : List.copyOf(cumulativeSteps);
	}
}
