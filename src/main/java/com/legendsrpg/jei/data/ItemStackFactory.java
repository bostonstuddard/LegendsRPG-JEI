package com.legendsrpg.jei.data;

import com.google.common.collect.ImmutableMultimap;
import com.legendsrpg.jei.LegendsRpgJeiClient;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemStackFactory {
	private static final String SERVER_CUSTOM_ID_KEY = "custom_item_id";
	private static final String INTERNAL_CUSTOM_ID_KEY = "legendsrpg_item_id";
	private static final Map<String, String> FALLBACK_IDS = createFallbackIds();
	private static final Map<String, ItemStack> SERVER_TEMPLATES = new ConcurrentHashMap<>();
	private static final Map<String, ItemStack> VISUAL_TEMPLATES = new ConcurrentHashMap<>();

	private ItemStackFactory() {}

	public static ItemStack create(String id, int count) {
		ItemDefinition definition = AddonData.get().item(id);
		if (definition == null) return vanilla(id, count);
		if (!cacheable(definition)) return create(definition, count, true);
		ItemStack template = SERVER_TEMPLATES.computeIfAbsent(definition.id(), ignored -> create(definition, 1, true));
		ItemStack stack = template.copy();
		stack.setCount(Math.max(1, count));
		return stack;
	}

	public static ItemStack createVisual(ItemDefinition definition, int count) {
		if (!cacheable(definition)) {
			ItemStack stack = create(definition, count, false);
			markInternal(stack, definition.id());
			return stack;
		}
		ItemStack template = VISUAL_TEMPLATES.computeIfAbsent(definition.id(), ignored -> {
			ItemStack created = create(definition, 1, false);
			markInternal(created, definition.id());
			return created;
		});
		ItemStack stack = template.copy();
		stack.setCount(Math.max(1, count));
		return stack;
	}

	public static ItemStack createIngredient(IngredientDefinition ingredient) {
		ItemStack stack = create(ingredient.id(), ingredient.count());
		if (ingredient.label() == null || ingredient.label().isBlank()) {
			return stack;
		}

		stack.set(DataComponents.CUSTOM_NAME, Component.literal(ingredient.label()).withStyle(ChatFormatting.GOLD));
		stack.set(
			DataComponents.LORE,
			new ItemLore(List.of(Component.literal("LegendsRPG shop currency").withStyle(ChatFormatting.GRAY)))
		);
		markInternal(stack, "currency_" + ingredient.label().toLowerCase().replaceAll("[^a-z0-9]+", "_"));
		applyTooltipHiding(stack);
		return stack;
	}

	public static ItemStack guideIcon(String itemId, String name) {
		// Some sources are themselves custom LegendsRPG items, such as treasure
		// bags and dungeon keys. Use the real stack so the resource-pack model,
		// tooltip style, player-head texture, and custom_item_id all stay intact.
		ItemDefinition definition = AddonData.get().item(itemId);
		if (definition != null) {
			return create(definition.id(), 1);
		}

		ItemStack stack = vanilla(itemId, 1);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(ChatFormatting.AQUA));
		stack.set(
			DataComponents.LORE,
			new ItemLore(List.of(Component.literal("LegendsRPG acquisition source").withStyle(ChatFormatting.GRAY)))
		);
		markInternal(stack, "guide_" + name.toLowerCase().replaceAll("[^a-z0-9]+", "_"));
		applyTooltipHiding(stack);
		return stack;
	}

	private static boolean cacheable(ItemDefinition definition) {
		return (definition.trimMaterial() == null || definition.trimMaterial().isBlank())
			&& (definition.trimPattern() == null || definition.trimPattern().isBlank())
			&& (definition.potionType() == null || definition.potionType().isBlank());
	}

	private static ItemStack create(ItemDefinition definition, int count, boolean markAsServerItem) {
		Item item = resolve(definition.item());
		ItemStack stack = new ItemStack(item, Math.max(1, count));
		stack.set(DataComponents.CUSTOM_NAME, ComponentParser.parse(definition.nameJson(), definition.name()));

		if (definition.modelData() != null) {
			stack.set(
				DataComponents.CUSTOM_MODEL_DATA,
				new CustomModelData(
					List.of(definition.modelData().floatValue()),
					List.of(),
					List.of(),
					List.of()
				)
			);
		}

		if (definition.itemModel() != null && !definition.itemModel().isBlank()) {
			try {
				stack.set(DataComponents.ITEM_MODEL, Identifier.parse(definition.itemModel()));
			} catch (RuntimeException exception) {
				LegendsRpgJeiClient.LOGGER.debug("Invalid item model {} for {}", definition.itemModel(), definition.id());
			}
		}

		if (definition.tooltipStyle() != null && !definition.tooltipStyle().isBlank()) {
			try {
				stack.set(DataComponents.TOOLTIP_STYLE, Identifier.parse(definition.tooltipStyle()));
			} catch (RuntimeException exception) {
				LegendsRpgJeiClient.LOGGER.debug("Invalid tooltip style {} for {}", definition.tooltipStyle(), definition.id());
			}
		}

		if (definition.dyeColor() != null) {
			stack.set(DataComponents.DYED_COLOR, new DyedItemColor(definition.dyeColor()));
		}
		applyRegistryVisuals(stack, definition);

		List<Component> lore = new ArrayList<>();
		for (String line : definition.loreJson()) {
			Component parsed = ComponentParser.parse(line, "");
			lore.add(parsed.getString().isEmpty() ? Component.literal(" ") : parsed);
		}
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(lore));
		}

		if (definition.glint()) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}

		applyHeadProfile(stack, definition);
		applyTooltipHiding(stack);

		if (markAsServerItem) {
			CompoundTag customTag = new CompoundTag();
			customTag.putString(SERVER_CUSTOM_ID_KEY, definition.id());
			customTag.putString(INTERNAL_CUSTOM_ID_KEY, definition.id());
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
		}
		return stack;
	}

	public static ItemStack researchTier(int tier) {
		int safeTier = Math.max(1, Math.min(16, tier));
		ItemStack stack = new ItemStack(Items.KNOWLEDGE_BOOK);
		stack.set(
			DataComponents.CUSTOM_NAME,
			Component.literal("Mining Research Tier " + safeTier).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
		);
		stack.set(
			DataComponents.LORE,
			new ItemLore(List.of(
				Component.literal("Open this tier to view its research pages.").withStyle(ChatFormatting.GRAY),
				Component.literal("Use JEI's page arrows to switch cost views.").withStyle(ChatFormatting.DARK_GRAY)
			))
		);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		markInternal(stack, "mining_research_tier_" + safeTier);
		applyTooltipHiding(stack);
		return stack;
	}

	public static ItemStack vanilla(String id, int count) {
		String identifier = id.contains(":") ? id : "minecraft:" + id;
		return new ItemStack(resolve(identifier), Math.max(1, count));
	}

	public static Item baseItem(ItemDefinition definition) {
		return resolve(definition.item());
	}

	public static String customItemId(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null) {
			CompoundTag tag = data.copyTag();
			String serverId = findString(tag, SERVER_CUSTOM_ID_KEY, 0);
			if (serverId != null && !serverId.isBlank()) {
				return serverId;
			}
			String internalId = findString(tag, INTERNAL_CUSTOM_ID_KEY, 0);
			if (internalId != null && !internalId.isBlank()) {
				return internalId;
			}
		}

		// Some proxy/server combinations strip or relocate custom_data before JEI
		// creates its focus. Base item + exact custom name is a safe fallback for
		// the server's uniquely-named custom items.
		return FALLBACK_IDS.get(fallbackKey(stack));
	}

	public static boolean isLegendsItem(ItemStack stack) {
		return customItemId(stack) != null;
	}

	private static String findString(CompoundTag tag, String key, int depth) {
		if (depth > 4) {
			return null;
		}
		for (String childKey : tag.keySet()) {
			if (childKey.equalsIgnoreCase(key)) {
				String direct = tag.getStringOr(childKey, "");
				if (!direct.isBlank()) {
					return direct;
				}
			}
		}
		for (String childKey : tag.keySet()) {
			Tag child = tag.get(childKey);
			if (child instanceof CompoundTag childCompound) {
				String nested = findString(childCompound, key, depth + 1);
				if (nested != null && !nested.isBlank()) {
					return nested;
				}
			}
		}
		return null;
	}

	private static Map<String, String> createFallbackIds() {
		Map<String, String> values = new HashMap<>();
		Map<String, Boolean> duplicates = new HashMap<>();
		for (ItemDefinition definition : AddonData.get().items()) {
			String key = fallbackKey(definition.item(), definition.name());
			String previous = values.putIfAbsent(key, definition.id());
			if (previous != null && !previous.equals(definition.id())) {
				duplicates.put(key, true);
			}
		}
		for (String key : duplicates.keySet()) {
			values.remove(key);
		}
		return Map.copyOf(values);
	}

	private static String fallbackKey(ItemStack stack) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return fallbackKey(itemId == null ? "minecraft:air" : itemId.toString(), stack.getHoverName().getString());
	}

	private static String fallbackKey(String itemId, String name) {
		String normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
		return itemId.toLowerCase(Locale.ROOT) + "|" + normalizedName;
	}

	private static void markInternal(ItemStack stack, String id) {
		CompoundTag tag = new CompoundTag();
		tag.putString(INTERNAL_CUSTOM_ID_KEY, id);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	private static void applyRegistryVisuals(ItemStack stack, ItemDefinition definition) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		DynamicOps<Tag> ops = client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		if (definition.trimMaterial() != null && !definition.trimMaterial().isBlank()
			&& definition.trimPattern() != null && !definition.trimPattern().isBlank()) {
			CompoundTag trimTag = new CompoundTag();
			trimTag.putString("material", definition.trimMaterial());
			trimTag.putString("pattern", definition.trimPattern());
			ArmorTrim.CODEC.parse(ops, trimTag).result().ifPresent(trim -> stack.set(DataComponents.TRIM, trim));
		}

		if (definition.potionType() != null && !definition.potionType().isBlank()) {
			PotionContents.CODEC.parse(ops, StringTag.valueOf(definition.potionType()))
				.result()
				.ifPresent(contents -> stack.set(DataComponents.POTION_CONTENTS, contents));
		}
	}

	private static void applyHeadProfile(ItemStack stack, ItemDefinition definition) {
		if (definition.headTexture() == null || definition.headTexture().isBlank()) {
			return;
		}
		try {
			UUID uuid = definition.headUuid() == null || definition.headUuid().isBlank()
				? UUID.nameUUIDFromBytes(definition.id().getBytes(StandardCharsets.UTF_8))
				: UUID.fromString(definition.headUuid());
			Property texture = new Property("textures", definition.headTexture());
			PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", texture));
			String profileName = "LRPG" + Integer.toUnsignedString(definition.id().hashCode(), 36);
			profileName = profileName.substring(0, Math.min(16, profileName.length()));
			GameProfile profile = new GameProfile(uuid, profileName, properties);
			stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
		} catch (RuntimeException exception) {
			LegendsRpgJeiClient.LOGGER.debug("Could not create head profile for {}", definition.id(), exception);
		}
	}

	private static void applyTooltipHiding(ItemStack stack) {
		TooltipDisplay display = TooltipDisplay.DEFAULT
			.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
			.withHidden(DataComponents.ENCHANTMENTS, true)
			.withHidden(DataComponents.UNBREAKABLE, true)
			.withHidden(DataComponents.TOOL, true)
			.withHidden(DataComponents.DYED_COLOR, true)
			.withHidden(DataComponents.TRIM, true)
			.withHidden(DataComponents.POTION_CONTENTS, true);
		stack.set(DataComponents.TOOLTIP_DISPLAY, display);
	}

	private static Item resolve(String id) {
		try {
			Identifier identifier = Identifier.parse(id);
			return BuiltInRegistries.ITEM.get(identifier)
				.map(reference -> reference.value())
				.orElse(Items.BARRIER);
		} catch (RuntimeException exception) {
			return Items.BARRIER;
		}
	}
}
