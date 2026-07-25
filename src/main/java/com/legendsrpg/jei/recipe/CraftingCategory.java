package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.CraftingRecipeData;
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

public final class CraftingCategory implements IRecipeCategory<CraftingRecipeData> {
	private static final int HEIGHT = 112;
	private static final int ACCENT = 0xff55c9ff;
	private final IDrawable icon;

	public CraftingCategory(IGuiHelper guiHelper) {
		this.icon = guiHelper.createDrawableItemLike(Items.CRAFTING_TABLE);
	}

	@Override public IRecipeType<CraftingRecipeData> getRecipeType() { return LegendsRecipeTypes.CRAFTING; }
	@Override public Component getTitle() { return Component.literal("LegendsRPG Crafting"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CraftingRecipeData recipe, IFocusGroup focuses) {
		int index = 0;
		for (IngredientDefinition input : recipe.inputs()) {
			if (index >= 9) break;
			int x = 14 + (index % 3) * 23;
			int y = 37 + (index / 3) * 22;
			CompactItemStackRenderer.apply(builder.addInputSlot(x, y).setStandardSlotBackground())
				.add(ItemStackFactory.createIngredient(input));
			index++;
		}
		CompactItemStackRenderer.apply(builder.addOutputSlot(139, 58).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()));
		if (recipe.shapeless()) builder.setShapeless(92, 38);
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, CraftingRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, CategoryUi.itemName(recipe.output().id()), recipe.shapeless() ? "Shapeless crafting" : "Shaped crafting", ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(CraftingRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "crafting");
		CategoryUi.drawArrow(graphics, 98, 60, 0xff91a9ba);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, CraftingRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		String headerTitle = CategoryUi.itemName(recipe.output().id());
		String headerSubtitle = recipe.shapeless() ? "Shapeless crafting" : "Shaped crafting";
		if (CategoryUi.addHeaderTooltip(tooltip, headerTitle, headerSubtitle, mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Crafting Information", ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Recipe group: " + prettyGroup(recipe.group()), ChatFormatting.GRAY);
		CategoryUi.line(tooltip, recipe.shapeless() ? "Shapeless recipe" : "Shaped recipe", ChatFormatting.GOLD);
	}

	private static RecipeSnapshot snapshot(CraftingRecipeData recipe) {
		return new RecipeSnapshot(
			"crafting/" + recipe.id(),
			CategoryUi.itemName(recipe.output().id()),
			recipe.inputs(),
			recipe.output(),
			List.of(recipe.shapeless() ? "Shapeless crafting recipe" : "Shaped crafting recipe")
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(CraftingRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "crafting/" + CategoryUi.sanitize(recipe.id()));
	}

	private static String prettyGroup(String group) {
		if (group == null || group.isBlank()) return "Custom Recipe";
		String result = group.replace('_', ' ');
		return Character.toUpperCase(result.charAt(0)) + result.substring(1);
	}
}
