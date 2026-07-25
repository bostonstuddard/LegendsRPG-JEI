package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.ForgeRecipeData;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ForgeCategory implements IRecipeCategory<ForgeRecipeData> {
	private static final int HEIGHT = 120;
	private static final int ACCENT = 0xffff9b45;
	private final IDrawable icon;

	public ForgeCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.BLAST_FURNACE); }
	@Override public IRecipeType<ForgeRecipeData> getRecipeType() { return LegendsRecipeTypes.SMITHING; }
	@Override public Component getTitle() { return Component.literal("LegendsRPG Smithing"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ForgeRecipeData recipe, IFocusGroup focuses) {
		int index = 0;
		for (IngredientDefinition input : recipe.inputs()) {
			if (index >= 12) break;
			int x = 14 + (index % 4) * 23;
			int y = 39 + (index / 4) * 22;
			CompactItemStackRenderer.apply(builder.addInputSlot(x, y).setStandardSlotBackground())
				.add(ItemStackFactory.createIngredient(input));
			index++;
		}
		CompactItemStackRenderer.apply(builder.addOutputSlot(141, 61).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, "Smithing unlock: Level " + recipe.level(), ChatFormatting.GOLD);
			});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, ForgeRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, recipe.name(), "Smithing Level " + recipe.level(), ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(ForgeRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "forge");
		CategoryUi.drawArrow(graphics, 103, 63, 0xffc9a16f);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ForgeRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (CategoryUi.addHeaderTooltip(tooltip, recipe.name(), "Smithing Level " + recipe.level(), mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Smithing Recipe", ChatFormatting.GOLD);
		CategoryUi.line(tooltip, "Unlocks at Smithing Level " + recipe.level(), ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Base smelting time: " + format(recipe.smeltingTime()), ChatFormatting.GRAY);
		CategoryUi.line(tooltip, "Smithing time reduction: " + format(recipe.smithingTimeReduction()), ChatFormatting.GRAY);
	}

	private static RecipeSnapshot snapshot(ForgeRecipeData recipe) {
		return new RecipeSnapshot(
			"smithing/" + recipe.id(),
			recipe.name(),
			recipe.inputs(),
			recipe.output(),
			List.of("Smithing Level " + recipe.level(), "Base time: " + format(recipe.smeltingTime()))
		);
	}

	private static String format(double value) {
		return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(ForgeRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "smithing/" + CategoryUi.sanitize(recipe.id()));
	}
}
