package com.legendsrpg.jei.data;

import java.util.ArrayList;
import java.util.List;

public record ResearchPageRecipeData(
	String id,
	int tier,
	String pageId,
	String pageTitle,
	List<IngredientDefinition> ingredients,
	List<String> lines
) {
	public ResearchPageRecipeData {
		tier = Math.max(1, tier);
		pageId = pageId == null || pageId.isBlank() ? "overview" : pageId;
		pageTitle = pageTitle == null || pageTitle.isBlank() ? "Overview" : pageTitle;
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
		lines = lines == null ? List.of() : List.copyOf(lines);
	}

	public static List<ResearchPageRecipeData> expand(List<ResearchTierRecipeData> tiers) {
		List<ResearchPageRecipeData> pages = new ArrayList<>();
		for (ResearchTierRecipeData tier : tiers) {
			add(pages, tier, "unlocks", "Unlocks", List.of(), tier.unlocks());
			add(pages, tier, "tier_cost", "Tier Cost", tier.costs(), List.of("Materials paid directly for this tier."));
			add(pages, tier, "raw_cost", "Raw Cost", tier.rawCosts(), tier.steps());
			add(pages, tier, "cumulative", "Cumulative Cost", tier.cumulativeCosts(), List.of("Total direct materials through Tier " + tier.tier() + "."));
			add(pages, tier, "cumulative_raw", "Cumulative Raw Cost", tier.cumulativeRawCosts(), tier.cumulativeSteps());
		}
		return List.copyOf(pages);
	}

	private static void add(
		List<ResearchPageRecipeData> pages,
		ResearchTierRecipeData tier,
		String pageId,
		String title,
		List<IngredientDefinition> ingredients,
		List<String> lines
	) {
		if ((ingredients == null || ingredients.isEmpty()) && (lines == null || lines.isEmpty())) {
			return;
		}
		pages.add(new ResearchPageRecipeData(
			tier.id() + "/" + pageId,
			tier.tier(),
			pageId,
			title,
			ingredients,
			lines
		));
	}
}
