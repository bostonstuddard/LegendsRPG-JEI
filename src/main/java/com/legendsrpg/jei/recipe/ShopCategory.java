package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.client.ClientInputState;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.ShopGroupData;
import com.legendsrpg.jei.data.ShopRecipeData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ShopCategory implements IRecipeCategory<ShopRecipeData> {
	private static final int HEIGHT = 126;
	private static final int ACCENT = 0xffffc85a;
	private final ShopGroupData group;
	private final IRecipeType<ShopRecipeData> recipeType;
	private final IDrawable icon;

	public ShopCategory(IGuiHelper guiHelper, ShopGroupData group) {
		this.group = group;
		this.recipeType = LegendsRecipeTypes.shop(group.key());
		ItemStack iconStack = group.recipes().isEmpty()
			? new ItemStack(Items.EMERALD)
			: ItemStackFactory.create(group.recipes().getFirst().output().id(), 1);
		this.icon = guiHelper.createDrawableItemStack(iconStack);
	}

	@Override public IRecipeType<ShopRecipeData> getRecipeType() { return recipeType; }
	@Override public Component getTitle() { return Component.literal(group.name() + " Shop"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ShopRecipeData recipe, IFocusGroup focuses) {
		if (recipe.anyOf() && !recipe.inputs().isEmpty()) {
			IRecipeSlotBuilder slot = CompactItemStackRenderer.apply(
				builder.addInputSlot(42, 66).setStandardSlotBackground()
			);
			for (IngredientDefinition input : recipe.inputs()) slot.add(ItemStackFactory.createIngredient(input));
			slot.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, "Any one listed item may be used", ChatFormatting.GOLD);
			});
		} else {
			int index = 0;
			for (IngredientDefinition input : recipe.inputs()) {
				if (index >= 16) break;
				int x = 14 + (index % 4) * 23;
				int y = 39 + (index / 4) * 20;
				IRecipeSlotBuilder slot = CompactItemStackRenderer.apply(
					builder.addInputSlot(x, y).setStandardSlotBackground()
				).add(ItemStackFactory.createIngredient(input));
				if (input.label() != null && !input.label().isBlank()) {
					slot.addRichTooltipCallback((view, tooltip) -> {
						tooltip.clear();
						CategoryUi.heading(tooltip, input.label(), ChatFormatting.GOLD);
						CategoryUi.line(tooltip, "LegendsRPG currency", ChatFormatting.GRAY);
						if (CategoryUi.isCompactAmount(input.count())) {
							if (ClientInputState.isShiftDown()) CategoryUi.line(tooltip, "Exact amount: " + CategoryUi.exactAmount(input.count()), ChatFormatting.AQUA);
							else {
								CategoryUi.line(tooltip, "Amount: " + CategoryUi.compactAmount(input.count()), ChatFormatting.AQUA);
								CategoryUi.line(tooltip, "Hold Shift for the exact amount", ChatFormatting.DARK_GRAY);
							}
						}
					});
				}
				index++;
			}
		}

		if (recipe.researchLevel() > 0) {
			CompactItemStackRenderer.apply(
				builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 114, 98).setStandardSlotBackground()
			).add(ItemStackFactory.researchTier(recipe.researchLevel()))
				.addRichTooltipCallback((view, tooltip) -> {
					CategoryUi.blank(tooltip);
					CategoryUi.line(tooltip, "Requires Mining Research Tier " + recipe.researchLevel(), ChatFormatting.AQUA);
				});
		}

		CompactItemStackRenderer.apply(builder.addOutputSlot(145, 66).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, "Sold by " + recipe.source(), ChatFormatting.GOLD);
				if (!recipe.tradeRequirements().isEmpty()) CategoryUi.line(tooltip, "Trade has additional requirements", ChatFormatting.RED);
			});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, ShopRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, recipe.source(), recipe.anyOf() ? "Choose one payment item" : "Purchase recipe", ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(ShopRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "shop");
		CategoryUi.drawArrow(graphics, 108, 68, 0xffb8aa87);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ShopRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		String headerSubtitle = recipe.anyOf() ? "Choose one payment item" : "Purchase recipe";
		if (CategoryUi.addHeaderTooltip(tooltip, recipe.source(), headerSubtitle, mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, recipe.source(), ChatFormatting.GOLD);
		CategoryUi.line(tooltip, group.recipes().size() + " trades are indexed in this shop tab", ChatFormatting.DARK_GRAY);
		if (!recipe.shopRequirements().isEmpty()) {
			CategoryUi.blank(tooltip);
			CategoryUi.heading(tooltip, "Shop Access", ChatFormatting.AQUA);
			for (String line : recipe.shopRequirements()) CategoryUi.line(tooltip, "• " + line, ChatFormatting.GRAY);
		}
		if (!recipe.tradeRequirements().isEmpty()) {
			CategoryUi.blank(tooltip);
			CategoryUi.heading(tooltip, "Trade Requirements", ChatFormatting.RED);
			for (String line : recipe.tradeRequirements()) CategoryUi.line(tooltip, "• " + line, ChatFormatting.GRAY);
		}
		if (!recipe.unresolved().isEmpty()) {
			CategoryUi.blank(tooltip);
			CategoryUi.heading(tooltip, "Other Conditions", ChatFormatting.YELLOW);
			for (String line : recipe.unresolved()) CategoryUi.line(tooltip, "• " + line, ChatFormatting.GRAY);
		}
	}

	private static RecipeSnapshot snapshot(ShopRecipeData recipe) {
		List<String> notes = new ArrayList<>();
		if (recipe.anyOf()) notes.add("Choose any one payment item");
		if (recipe.researchLevel() > 0) notes.add("Mining Research Tier " + recipe.researchLevel());
		notes.addAll(recipe.tradeRequirements());
		return new RecipeSnapshot(
			"shop/" + recipe.sourceKey() + "/" + recipe.id(),
			recipe.source() + ": " + CategoryUi.itemName(recipe.output().id()),
			recipe.inputs(),
			recipe.output(),
			notes
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(ShopRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "shop/" + group.key() + "/" + CategoryUi.sanitize(recipe.id()));
	}
}
