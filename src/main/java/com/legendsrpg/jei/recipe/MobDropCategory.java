package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.DropRecipeData;
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

import java.util.ArrayList;
import java.util.List;

public final class MobDropCategory implements IRecipeCategory<DropRecipeData> {
	private static final int HEIGHT = 96;
	private static final int ACCENT = 0xffff667f;
	private final IDrawable icon;

	public MobDropCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.ZOMBIE_HEAD); }
	@Override public IRecipeType<DropRecipeData> getRecipeType() { return LegendsRecipeTypes.MOB_DROP; }
	@Override public Component getTitle() { return Component.literal("LegendsRPG Mob Drops"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, DropRecipeData recipe, IFocusGroup focuses) {
		CompactItemStackRenderer.apply(builder.addInputSlot(28, 49).setStandardSlotBackground())
			.add(ItemStackFactory.createVisual(recipe.icon(), 1));
		CompactItemStackRenderer.apply(builder.addOutputSlot(139, 49).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, DropRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, recipe.mobName(), "Mob drop • " + formatChance(recipe.chance()) + "%", ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(DropRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "boss");
		CategoryUi.drawArrow(graphics, 91, 51, 0xffbd8790);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, DropRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		String headerSubtitle = "Mob drop • " + formatChance(recipe.chance()) + "%";
		if (CategoryUi.addHeaderTooltip(tooltip, recipe.mobName(), headerSubtitle, mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, recipe.mobName(), ChatFormatting.RED);
		CategoryUi.line(tooltip, "Area: " + recipe.location(), ChatFormatting.GRAY);
		if (recipe.coords() != null && !recipe.coords().isBlank()) CategoryUi.line(tooltip, recipe.coords(), ChatFormatting.DARK_GRAY);
		CategoryUi.line(tooltip, "Drop chance: " + formatChance(recipe.chance()) + "%", ChatFormatting.GOLD);
		CategoryUi.line(tooltip, "Amount: " + recipe.amount(), ChatFormatting.YELLOW);
	}

	private static RecipeSnapshot snapshot(DropRecipeData recipe) {
		List<String> notes = new ArrayList<>();
		notes.add("Dropped by " + recipe.mobName());
		notes.add(formatChance(recipe.chance()) + "% chance");
		if (recipe.location() != null && !recipe.location().isBlank()) notes.add(recipe.location());
		return new RecipeSnapshot(
			"mob_drop/" + recipe.id(),
			recipe.mobName() + " Drop",
			List.of(new IngredientDefinition(recipe.icon().id(), 1, recipe.mobName())),
			recipe.output(),
			notes
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(DropRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "mob_drop/" + CategoryUi.sanitize(recipe.id()));
	}
	private static String formatChance(double chance) { return chance == Math.rint(chance) ? Long.toString(Math.round(chance)) : Double.toString(chance); }
}
