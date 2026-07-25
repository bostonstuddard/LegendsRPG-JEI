package com.legendsrpg.jei.data;

public record OreForgeRecipeData(
	String id,
	int index,
	String oreId,
	String ingotId,
	int requiredForgeLevel,
	double multiplier,
	int baseAmount,
	int baseTime,
	int coalCost
) {
	public OreForgeRecipeData {
		index = Math.max(1, index);
		requiredForgeLevel = Math.max(1, Math.min(7, requiredForgeLevel));
		baseAmount = Math.max(1, baseAmount);
		baseTime = Math.max(1, baseTime);
		coalCost = Math.max(1, coalCost);
	}

	public int amountAt(int forgeLevel) {
		int level = clampLevel(forgeLevel);
		int amount = baseAmount;
		if (level >= 3) amount += 64;
		if (level >= 4) amount += 64;
		if (level >= 5) amount += 64;
		if (level >= 6) amount += 128;
		if (level >= 7) amount += 128;
		return amount;
	}

	public int minutesAt(int forgeLevel) {
		int level = clampLevel(forgeLevel);
		int minutes = baseTime;
		if (level >= 2) minutes--;
		if (level >= 4) minutes--;
		return Math.max(1, minutes);
	}

	public int smithingXpAt(int forgeLevel) {
		long value = (long) minutesAt(forgeLevel)
			* amountAt(forgeLevel)
			* (long) Math.ceil(multiplier * 1.25D);
		return (int) Math.min(Integer.MAX_VALUE, value);
	}

	public static int slotsAt(int forgeLevel) {
		int level = clampLevel(forgeLevel);
		int slots = 1;
		if (level >= 2) slots++;
		if (level >= 4) slots++;
		if (level >= 6) slots++;
		return slots;
	}

	private static int clampLevel(int forgeLevel) {
		return Math.max(1, Math.min(7, forgeLevel));
	}
}
