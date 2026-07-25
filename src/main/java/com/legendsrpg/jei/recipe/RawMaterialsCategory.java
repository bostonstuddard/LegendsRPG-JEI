package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RawMaterialsRecipeData;
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

public final class RawMaterialsCategory implements IRecipeCategory<RawMaterialsRecipeData> {
	private static final int HEIGHT = 151;
	private static final int ACCENT = 0xff75c5ff;
	private final IDrawable icon;

	public RawMaterialsCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.DEEPSLATE_DIAMOND_ORE); }
	@Override public IRecipeType<RawMaterialsRecipeData> getRecipeType() { return LegendsRecipeTypes.RAW_MATERIALS; }
	@Override public Component getTitle() { return Component.literal("Raw Resource Cost"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RawMaterialsRecipeData recipe, IFocusGroup focuses) {
		int index = 0;
		for (IngredientDefinition input : recipe.rawInputs()) {
			if (index >= 25) break;
			int x = 14 + (index % 5) * 23;
			int y = 40 + (index / 5) * 22;
			CompactItemStackRenderer.apply(builder.addInputSlot(x, y).setStandardSlotBackground())
				.add(ItemStackFactory.createIngredient(input));
			index++;
		}
		CompactItemStackRenderer.apply(builder.addOutputSlot(145, 76).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, "Fully expanded raw resource total", ChatFormatting.AQUA);
			});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, RawMaterialsRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, "Raw Resource Breakdown", recipe.source(), ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(RawMaterialsRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "raw");
		CategoryUi.drawArrow(graphics, 116, 78, 0xff8baec6);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, RawMaterialsRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (CategoryUi.addHeaderTooltip(tooltip, "Raw Resource Breakdown", recipe.source(), mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Raw Resource Breakdown", ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Calculated from: " + recipe.source(), ChatFormatting.GRAY);
		CategoryUi.line(tooltip, recipe.rawInputs().size() + " raw material types are shown directly on this page.", ChatFormatting.GREEN);
		if (!recipe.directInputs().isEmpty()) {
			CategoryUi.blank(tooltip);
			CategoryUi.heading(tooltip, "Original Cost", ChatFormatting.GOLD);
			for (IngredientDefinition ingredient : recipe.directInputs()) {
				CategoryUi.ingredientLine(tooltip, CategoryUi.ingredientName(ingredient), ingredient.count(), ChatFormatting.GRAY);
			}
		}
	}

	private static RecipeSnapshot snapshot(RawMaterialsRecipeData recipe) {
		return new RecipeSnapshot(
			"raw/" + recipe.id(),
			"Raw Cost: " + CategoryUi.itemName(recipe.output().id()),
			recipe.rawInputs(),
			recipe.output(),
			List.of("Calculated from " + recipe.source())
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(RawMaterialsRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "raw/" + CategoryUi.sanitize(recipe.id()));
	}
}
