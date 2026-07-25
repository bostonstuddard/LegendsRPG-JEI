package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.TreasureRecipeData;
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

public final class MiningTreasureCategory implements IRecipeCategory<TreasureRecipeData> {
	private static final int HEIGHT = 104;
	private static final int ACCENT = 0xffffc53d;
	private final IDrawable icon;

	public MiningTreasureCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.TRAPPED_CHEST); }
	@Override public IRecipeType<TreasureRecipeData> getRecipeType() { return LegendsRecipeTypes.MINING_TREASURE; }
	@Override public Component getTitle() { return Component.literal("Mining Treasure Loot"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, TreasureRecipeData recipe, IFocusGroup focuses) {
		CompactItemStackRenderer.apply(builder.addInputSlot(28, 53).setStandardSlotBackground())
			.add(ItemStackFactory.guideIcon("minecraft:trapped_chest", "Mining Treasure Chest"));
		CompactItemStackRenderer.apply(builder.addOutputSlot(139, 53).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, TreasureRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, "Mining Treasure Chest", recipe.dropChance(), ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(TreasureRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "treasure");
		CategoryUi.drawArrow(graphics, 91, 55, 0xffb8a875);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, TreasureRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (CategoryUi.addHeaderTooltip(tooltip, "Mining Treasure Chest", recipe.dropChance(), mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Mining Treasure Loot", ChatFormatting.GOLD);
		CategoryUi.line(tooltip, recipe.spawnText(), ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Regions: " + recipe.area(), ChatFormatting.GRAY);
		CategoryUi.line(tooltip, "Loot chance: " + recipe.dropChance(), ChatFormatting.YELLOW);
		if (recipe.pity() > 0) CategoryUi.line(tooltip, "Guaranteed at " + recipe.pity() + " RNG Meter progress", ChatFormatting.LIGHT_PURPLE);
	}

	private static RecipeSnapshot snapshot(TreasureRecipeData recipe) {
		return new RecipeSnapshot(
			"mining_treasure/" + recipe.id(),
			"Mining Treasure: " + CategoryUi.itemName(recipe.output().id()),
			List.of(new IngredientDefinition("minecraft:trapped_chest", 1, "Mining Treasure Chest")),
			recipe.output(),
			List.of(recipe.dropChance(), recipe.area())
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(TreasureRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "mining_treasure/" + CategoryUi.sanitize(recipe.id()));
	}
}
