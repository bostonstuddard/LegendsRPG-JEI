package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.OreForgeRecipeData;
import com.legendsrpg.jei.data.RecipeSnapshot;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.List;

public final class OreForgeCategory implements IRecipeCategory<OreForgeRecipeData> {
	private static final int HEIGHT = 142;
	private static final int ACCENT = 0xffff7f50;
	private static final int TIER_X = 10;
	private static final int TIER_Y = 49;
	private static final int TIER_WIDTH = 20;
	private static final int TIER_GAP = 2;
	private static int selectedTier = 1;

	public static int selectedTier() { return selectedTier; }
	public static void setSelectedTier(int tier) { selectedTier = Math.max(1, Math.min(7, tier)); }
	private final IDrawable icon;

	public OreForgeCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.SMITHING_TABLE); }
	@Override public IRecipeType<OreForgeRecipeData> getRecipeType() { return LegendsRecipeTypes.ORE_FORGE; }
	@Override public Component getTitle() { return Component.literal("LegendsRPG Ore Forge"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, OreForgeRecipeData recipe, IFocusGroup focuses) {
		var oreSlot = CompactItemStackRenderer.apply(
			builder.addInputSlot(25, 88).setStandardSlotBackground().setSlotName("forge_ore")
		);
		var coalSlot = CompactItemStackRenderer.apply(
			builder.addInputSlot(55, 88).setStandardSlotBackground().setSlotName("forge_coal")
		);
		var outputSlot = CompactItemStackRenderer.apply(
			builder.addOutputSlot(139, 88).setStandardSlotBackground().setSlotName("forge_output")
		);

		for (int tier = 1; tier <= 7; tier++) {
			int amount = recipe.amountAt(tier);
			oreSlot.add(ItemStackFactory.create(recipe.oreId(), amount));
			coalSlot.add(ItemStackFactory.create("enchanted_coal_block", recipe.coalCost()));
			outputSlot.add(ItemStackFactory.create(recipe.ingotId(), amount));
		}

		oreSlot.addRichTooltipCallback((view, tooltip) -> {
			CategoryUi.blank(tooltip);
			CategoryUi.line(tooltip, "Forge Level " + selectedTier + " batch: " + CategoryUi.exactAmount(recipe.amountAt(selectedTier)), ChatFormatting.GOLD);
		});
		coalSlot.addRichTooltipCallback((view, tooltip) -> {
			CategoryUi.blank(tooltip);
			CategoryUi.line(tooltip, "Fuel cost is unchanged by Forge Level", ChatFormatting.GRAY);
		});
		outputSlot.addRichTooltipCallback((view, tooltip) -> {
			CategoryUi.blank(tooltip);
			CategoryUi.line(tooltip, recipe.minutesAt(selectedTier) + " minutes • " + CategoryUi.exactAmount(recipe.smithingXpAt(selectedTier)) + " Smithing XP", ChatFormatting.AQUA);
		});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, OreForgeRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, CategoryUi.itemName(recipe.ingotId()) + " Forge", "Selectable Forge Level 1–7", ACCENT);

		List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots();
		applyTier(recipe, slots);
		for (int tier = 1; tier <= 7; tier++) {
			final int clickedTier = tier;
			final int x = tierX(tier);
			builder.addGuiEventListener(new IJeiGuiEventListener() {
				@Override public ScreenRectangle getArea() { return new ScreenRectangle(x, TIER_Y, TIER_WIDTH, 18); }
				@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
					if (button != 0) return false;
					selectedTier = clickedTier;
					applyTier(recipe, slots);
					return true;
				}
			});
		}
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void onDisplayedIngredientsUpdate(OreForgeRecipeData recipe, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
		applyTier(recipe, slots);
	}

	private static void applyTier(OreForgeRecipeData recipe, List<IRecipeSlotDrawable> slots) {
		for (IRecipeSlotDrawable slot : slots) {
			String name = slot.getSlotName().orElse("");
			switch (name) {
				case "forge_ore" -> slot.createDisplayOverrides().add(ItemStackFactory.create(recipe.oreId(), recipe.amountAt(selectedTier)));
				case "forge_coal" -> slot.createDisplayOverrides().add(ItemStackFactory.create("enchanted_coal_block", recipe.coalCost()));
				case "forge_output" -> slot.createDisplayOverrides().add(ItemStackFactory.create(recipe.ingotId(), recipe.amountAt(selectedTier)));
				default -> { }
			}
		}
	}

	@Override
	public void draw(OreForgeRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "forge");
		var font = Minecraft.getInstance().font;
		graphics.drawString(font, "Forge Level", 10, 39, 0xffd9b28a, false);
		for (int tier = 1; tier <= 7; tier++) {
			int x = tierX(tier);
			boolean selected = tier == selectedTier;
			boolean usable = tier >= recipe.requiredForgeLevel();
			int fill = selected ? 0xffa84b31 : (usable ? 0xff183347 : 0xff251b24);
			int outline = selected ? 0xffffb061 : (usable ? 0xff4e87a6 : 0xff744052);
			graphics.fill(x, TIER_Y, x + TIER_WIDTH, TIER_Y + 18, fill);
			graphics.renderOutline(x, TIER_Y, TIER_WIDTH, 18, outline);
			graphics.drawCenteredString(font, Integer.toString(tier), x + TIER_WIDTH / 2, TIER_Y + 5, selected ? 0xffffffff : 0xffb8cad8);
		}
		graphics.drawString(font, "Ore", 25, 76, 0xff8fa3b5, false);
		graphics.drawString(font, "Fuel", 55, 76, 0xff8fa3b5, false);
		graphics.drawString(font, "Output", 136, 76, 0xff8fa3b5, false);
		CategoryUi.drawArrow(graphics, 100, 90, 0xffc6a183);

		String details = CategoryUi.compactAmount(recipe.amountAt(selectedTier)) + "x • " + recipe.minutesAt(selectedTier) + "m • "
			+ CategoryUi.compactAmount(recipe.smithingXpAt(selectedTier)) + " XP";
		graphics.drawCenteredString(font, CategoryUi.fit(details, 164), CategoryUi.WIDTH / 2, 118, 0xff8ee6c2);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, OreForgeRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		String headerTitle = CategoryUi.itemName(recipe.ingotId()) + " Forge";
		if (CategoryUi.addHeaderTooltip(tooltip, headerTitle, "Selectable Forge Level 1–7", mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;

		String details = CategoryUi.compactAmount(recipe.amountAt(selectedTier)) + "x • " + recipe.minutesAt(selectedTier) + "m • "
			+ CategoryUi.compactAmount(recipe.smithingXpAt(selectedTier)) + " XP";
		if (CategoryUi.addTruncatedTextTooltip(tooltip, details, 6, 115, 164, 12, mouseX, mouseY)) return;
		int hoveredTier = hoveredTier(mouseX, mouseY);
		if (hoveredTier > 0) {
			CategoryUi.heading(tooltip, "Forge Level " + hoveredTier, ChatFormatting.GOLD);
			CategoryUi.line(tooltip, CategoryUi.exactAmount(recipe.amountAt(hoveredTier)) + "x " + CategoryUi.itemName(recipe.oreId()) + " → " + CategoryUi.exactAmount(recipe.amountAt(hoveredTier)) + "x " + CategoryUi.itemName(recipe.ingotId()), ChatFormatting.GRAY);
			CategoryUi.line(tooltip, recipe.coalCost() + "x Enchanted Coal Block", ChatFormatting.GRAY);
			CategoryUi.line(tooltip, recipe.minutesAt(hoveredTier) + " minutes", ChatFormatting.AQUA);
			CategoryUi.line(tooltip, CategoryUi.exactAmount(recipe.smithingXpAt(hoveredTier)) + " Smithing XP", ChatFormatting.GREEN);
			CategoryUi.blank(tooltip);
			CategoryUi.line(tooltip, upgradeText(hoveredTier), ChatFormatting.YELLOW);
			if (hoveredTier < recipe.requiredForgeLevel()) CategoryUi.line(tooltip, "This ore requires Forge Level " + recipe.requiredForgeLevel(), ChatFormatting.RED);
			return;
		}
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Smithing Ore Forge", ChatFormatting.GOLD);
		CategoryUi.line(tooltip, "Click Forge Levels 1–7 to compare batch size, time, XP, and slot benefits.", ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Ore unlock: Forge Level " + recipe.requiredForgeLevel(), ChatFormatting.YELLOW);
		CategoryUi.line(tooltip, "Selected level Forge slots: " + OreForgeRecipeData.slotsAt(selectedTier), ChatFormatting.GREEN);
		CategoryUi.line(tooltip, "+1 additional Forge slot at Mining Research Tier 11.", ChatFormatting.GRAY);
		CategoryUi.line(tooltip, upgradeText(selectedTier), ChatFormatting.GOLD);
		CategoryUi.line(tooltip, "Mining Sacks may supply the ore in game.", ChatFormatting.DARK_GRAY);
	}

	private static RecipeSnapshot snapshot(OreForgeRecipeData recipe) {
		int amount = recipe.amountAt(selectedTier);
		return new RecipeSnapshot(
			"ore_forge/" + recipe.id() + "/tier_" + selectedTier,
			CategoryUi.itemName(recipe.ingotId()) + " — Forge Level " + selectedTier,
			List.of(
				new IngredientDefinition(recipe.oreId(), amount, null),
				new IngredientDefinition("enchanted_coal_block", recipe.coalCost(), null)
			),
			new IngredientDefinition(recipe.ingotId(), amount, null),
			List.of(recipe.minutesAt(selectedTier) + " minutes", CategoryUi.exactAmount(recipe.smithingXpAt(selectedTier)) + " Smithing XP")
		);
	}

	private static int tierX(int tier) { return TIER_X + (tier - 1) * (TIER_WIDTH + TIER_GAP); }
	private static int hoveredTier(double mouseX, double mouseY) {
		if (mouseY < TIER_Y || mouseY >= TIER_Y + 18) return 0;
		for (int tier = 1; tier <= 7; tier++) {
			int x = tierX(tier);
			if (mouseX >= x && mouseX < x + TIER_WIDTH) return tier;
		}
		return 0;
	}

	private static String upgradeText(int tier) {
		return switch (tier) {
			case 1 -> "Access to the Forge and Forge upgrades";
			case 2 -> "Nickel/Lapis, -1 minute, +1 Forge slot";
			case 3 -> "Mithril and +64 batch size";
			case 4 -> "-1 minute, +64 batch size, +1 Forge slot";
			case 5 -> "Raw Diamond and +64 batch size";
			case 6 -> "+128 batch size and +1 Forge slot";
			case 7 -> "Infernal Ore and +128 batch size";
			default -> "";
		};
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(OreForgeRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "ore_forge/" + CategoryUi.sanitize(recipe.id()));
	}
}
