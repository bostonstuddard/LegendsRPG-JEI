package com.legendsrpg.jei.data;

import java.util.List;

public record ItemDefinition(
	String id,
	String item,
	String name,
	String nameJson,
	List<String> loreJson,
	Double modelData,
	String itemModel,
	String tooltipStyle,
	String rarity,
	String itemType,
	String requirement,
	int researchRequirement,
	String gameModeRequirement,
	boolean soulbound,
	List<String> info,
	String headUuid,
	String headTexture,
	Integer dyeColor,
	String trimMaterial,
	String trimPattern,
	String potionType,
	boolean glint,
	int maxStackSize
) {
	public ItemDefinition {
		loreJson = loreJson == null ? List.of() : List.copyOf(loreJson);
		info = info == null ? List.of() : List.copyOf(info);
		maxStackSize = Math.max(1, Math.min(99, maxStackSize));
	}
}
