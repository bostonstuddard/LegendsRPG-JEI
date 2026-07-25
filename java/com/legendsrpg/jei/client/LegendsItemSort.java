package com.legendsrpg.jei.client;

import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LegendsItemSort {
	private static final SortKey NOT_CUSTOM = new SortKey(999, "~", 999, "~", 999, "~");
	private static final Map<Object, SortKey> ELEMENT_KEYS = new ConcurrentHashMap<>();
	private static final Map<String, SortKey> ITEM_KEYS = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Method> TYPED_INGREDIENT_METHODS = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Method> INGREDIENT_METHODS = new ConcurrentHashMap<>();
	private static final Map<String, Integer> TYPE_ORDER = Map.ofEntries(
		Map.entry("armor", 0),
		Map.entry("weapon", 1),
		Map.entry("tool", 2),
		Map.entry("artifact", 3),
		Map.entry("consumable", 4),
		Map.entry("bait", 5),
		Map.entry("dye", 6),
		Map.entry("infusion", 7),
		Map.entry("scroll", 8),
		Map.entry("book", 9),
		Map.entry("living wood", 10),
		Map.entry("apiary", 11),
		Map.entry("cube", 12),
		Map.entry("phone", 13),
		Map.entry("alcohol", 14),
		Map.entry("weird food", 15),
		Map.entry("item", 90),
		Map.entry("", 98)
	);
	private static final Map<String, Integer> RARITY_ORDER = Map.ofEntries(
		Map.entry("common", 0),
		Map.entry("uncommon", 1),
		Map.entry("rare", 2),
		Map.entry("epic", 3),
		Map.entry("legendary", 4),
		Map.entry("mythical", 5),
		Map.entry("enchanted", 6),
		Map.entry("ancient", 7),
		Map.entry("unique", 8),
		Map.entry("very unique", 9),
		Map.entry("special", 10),
		Map.entry("expert", 11),
		Map.entry("legend", 12),
		Map.entry("enchanting", 13)
	);

	private LegendsItemSort() {}

	public static int compareElements(Object left, Object right, Comparator<Object> fallback) {
		SortKey leftKey = keyFromElement(left);
		SortKey rightKey = keyFromElement(right);
		if (leftKey == NOT_CUSTOM && rightKey == NOT_CUSTOM) return fallback.compare(left, right);
		if (leftKey == NOT_CUSTOM) return 1;
		if (rightKey == NOT_CUSTOM) return -1;
		int compared = leftKey.compareTo(rightKey);
		return compared != 0 ? compared : fallback.compare(left, right);
	}

	private static SortKey keyFromElement(Object element) {
		if (element == null) return NOT_CUSTOM;
		return ELEMENT_KEYS.computeIfAbsent(element, LegendsItemSort::createElementKey);
	}

	private static SortKey createElementKey(Object element) {
		try {
			Method typedMethod = TYPED_INGREDIENT_METHODS.computeIfAbsent(element.getClass(), type -> findMethod(type, "getTypedIngredient"));
			Object typed = typedMethod.invoke(element);
			if (typed == null) return NOT_CUSTOM;
			Method ingredientMethod = INGREDIENT_METHODS.computeIfAbsent(typed.getClass(), type -> findMethod(type, "getIngredient"));
			Object ingredient = ingredientMethod.invoke(typed);
			if (!(ingredient instanceof ItemStack stack)) return NOT_CUSTOM;
			String id = ItemStackFactory.customItemId(stack);
			if (id == null || id.isBlank()) return NOT_CUSTOM;
			return ITEM_KEYS.computeIfAbsent(id, LegendsItemSort::createKey);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return NOT_CUSTOM;
		}
	}

	private static Method findMethod(Class<?> type, String name) {
		try {
			Method method = type.getMethod(name);
			method.setAccessible(true);
			return method;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static SortKey createKey(String id) {
		ItemDefinition definition = AddonData.get().item(id);
		if (definition == null) return NOT_CUSTOM;
		String type = normalize(definition.itemType());
		int typeRank = TYPE_ORDER.getOrDefault(type, 50);
		String rarity = normalize(definition.rarity());
		int rarityRank = RARITY_ORDER.getOrDefault(rarity, 50);
		String name = normalize(definition.name());
		ArmorName armor = type.equals("armor") ? armorName(name) : new ArmorName(name, 0);
		return new SortKey(typeRank, type, rarityRank, armor.setName(), armor.pieceRank(), name);
	}

	private static ArmorName armorName(String name) {
		String[][] pieces = {
			{"helmet", "helm", "hood", "crown", "mask", "cap", "bandana", "hat"},
			{"chestplate", "tunic", "robe", "coat", "overcoat", "breastmail", "top"},
			{"leggings", "pants", "greaves", "legs"},
			{"boots", "shoes", "sabaton", "sabatons"}
		};
		for (int rank = 0; rank < pieces.length; rank++) {
			for (String piece : pieces[rank]) {
				String suffix = " " + piece;
				if (name.endsWith(suffix)) return new ArmorName(name.substring(0, name.length() - suffix.length()), rank);
			}
		}
		return new ArmorName(name, 4);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}

	private record ArmorName(String setName, int pieceRank) {}

	private record SortKey(int itemTypeRank, String itemType, int rarity, String armorSet, int armorPiece, String name) implements Comparable<SortKey> {
		@Override
		public int compareTo(SortKey other) {
			int result = Integer.compare(itemTypeRank, other.itemTypeRank);
			if (result != 0) return result;
			result = itemType.compareTo(other.itemType);
			if (result != 0) return result;
			result = Integer.compare(rarity, other.rarity);
			if (result != 0) return result;
			result = armorSet.compareTo(other.armorSet);
			if (result != 0) return result;
			result = Integer.compare(armorPiece, other.armorPiece);
			if (result != 0) return result;
			return name.compareTo(other.name);
		}
	}
}
