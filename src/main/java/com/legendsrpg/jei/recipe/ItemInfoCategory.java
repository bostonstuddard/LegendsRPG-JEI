package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.client.ThemePalette;
import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.ItemInfoRecipeData;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ItemInfoCategory implements IRecipeCategory<ItemInfoRecipeData> {
	private static final int HEIGHT = 104;
	private static final int ACCENT = 0xff55c9ff;
	private final IDrawable icon;

	public ItemInfoCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.BOOK); }
	@Override public IRecipeType<ItemInfoRecipeData> getRecipeType() { return LegendsRecipeTypes.ITEM_INFO; }
	@Override public Component getTitle() { return Component.literal("LegendsRPG Item Guide"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ItemInfoRecipeData recipe, IFocusGroup focuses) {
		ItemDefinition definition = AddonData.get().item(recipe.id());
		boolean hasRequirement = definition != null && definition.researchRequirement() > 0;
		if (hasRequirement) {
			CompactItemStackRenderer.apply(builder.addInputSlot(48, 52).setStandardSlotBackground())
				.add(ItemStackFactory.researchTier(definition.researchRequirement()))
				.addRichTooltipCallback((view, tooltip) -> {
					CategoryUi.blank(tooltip);
					CategoryUi.line(tooltip, "Requires Mining Research Tier " + definition.researchRequirement(), ChatFormatting.AQUA);
				});
		}
		int outputX = hasRequirement ? 110 : 79;
		CompactItemStackRenderer.apply(builder.addOutputSlot(outputX, 52).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), 1))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, "Custom Item ID: " + recipe.id(), ChatFormatting.DARK_AQUA);
			});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, ItemInfoRecipeData recipe, IFocusGroup focuses) {
		ItemDefinition definition = AddonData.get().item(recipe.id());
		String title = definition == null ? CategoryUi.itemName(recipe.id()) : definition.name();
		CategoryUi.addHeader(builder, title, "Item Information", ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(ItemInfoRecipeData recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "guide");
		ItemDefinition definition = AddonData.get().item(recipe.id());
		boolean hasRequirement = definition != null && definition.researchRequirement() > 0;
		var font = Minecraft.getInstance().font;
		ThemePalette palette = ThemePalette.current();
		if (hasRequirement) {
			graphics.drawCenteredString(font, "Research", 57, 39, palette.secondary());
			CategoryUi.drawArrow(graphics, 78, 53, palette.muted());
			graphics.drawCenteredString(font, "Item", 119, 39, palette.secondary());
		} else {
			graphics.drawCenteredString(font, "Item", CategoryUi.WIDTH / 2, 39, palette.secondary());
		}
		String methodText = recipe.methods().isEmpty()
			? "No automatic acquisition source found"
			: recipe.methods().size() + " known acquisition method" + (recipe.methods().size() == 1 ? "" : "s");
		graphics.drawCenteredString(font, CategoryUi.fit(methodText, 154), CategoryUi.WIDTH / 2, 82, palette.secondary());
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ItemInfoRecipeData recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		ItemDefinition definition = AddonData.get().item(recipe.id());
		String title = definition == null ? CategoryUi.itemName(recipe.id()) : definition.name();
		if (CategoryUi.addHeaderTooltip(tooltip, title, "Item Information", mouseX, mouseY)) return;
		String methodText = recipe.methods().isEmpty()
			? "No automatic acquisition source found"
			: recipe.methods().size() + " known acquisition method" + (recipe.methods().size() == 1 ? "" : "s");
		if (CategoryUi.addTruncatedTextTooltip(tooltip, methodText, 11, 79, 154, 13, mouseX, mouseY)) return;
		RecipeSnapshot snapshot = snapshot(recipe);
		if (CategoryUi.addActionTooltip(tooltip, snapshot, mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, title, ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "Custom Item ID: " + recipe.id(), ChatFormatting.DARK_AQUA);
		if (definition != null) for (String line : definition.info()) CategoryUi.line(tooltip, line, ChatFormatting.GRAY);
		CategoryUi.blank(tooltip);
		CategoryUi.heading(tooltip, "Known Acquisition Methods", ChatFormatting.GOLD);
		if (recipe.methods().isEmpty()) CategoryUi.line(tooltip, "No automatic source was found in the current server scripts.", ChatFormatting.DARK_GRAY);
		else for (String method : recipe.methods()) CategoryUi.line(tooltip, "• " + method, ChatFormatting.GRAY);
	}

	private static RecipeSnapshot snapshot(ItemInfoRecipeData recipe) {
		ItemDefinition definition = AddonData.get().item(recipe.id());
		List<IngredientDefinition> requirements = new ArrayList<>();
		if (definition != null && definition.researchRequirement() > 0) {
			requirements.add(new IngredientDefinition("minecraft:knowledge_book", 1, "Mining Research Tier " + definition.researchRequirement()));
		}
		List<String> notes = new ArrayList<>();
		if (definition != null) notes.addAll(definition.info());
		notes.addAll(recipe.methods());
		return new RecipeSnapshot(
			"item/" + CategoryUi.sanitize(recipe.id()),
			definition == null ? CategoryUi.itemName(recipe.id()) : definition.name(),
			requirements,
			new IngredientDefinition(recipe.output().id(), 1, recipe.output().label()),
			notes
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(ItemInfoRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "item/" + CategoryUi.sanitize(recipe.id()));
	}
}
