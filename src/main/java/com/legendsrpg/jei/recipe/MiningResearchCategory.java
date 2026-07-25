package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.client.ThemePalette;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.ResearchPageRecipeData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class MiningResearchCategory implements IRecipeCategory<ResearchPageRecipeData> {
	private static final int HEIGHT = 132;
	private static final int ACCENT = 0xff4ee7e7;
	private static final int MAX_COLUMNS = 6;
	private static final int SLOT_SIZE = 18;
	private static final int SLOT_GAP = 5;
	private final IDrawable icon;

	public MiningResearchCategory(IGuiHelper guiHelper) { this.icon = guiHelper.createDrawableItemLike(Items.KNOWLEDGE_BOOK); }
	@Override public IRecipeType<ResearchPageRecipeData> getRecipeType() { return LegendsRecipeTypes.MINING_RESEARCH; }
	@Override public Component getTitle() { return Component.literal("Mining Research Tree"); }
	@Override public int getWidth() { return CategoryUi.WIDTH; }
	@Override public int getHeight() { return HEIGHT; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ResearchPageRecipeData recipe, IFocusGroup focuses) {
		// Keep an output focus attached to every page so JEI can index and open
		// the Mining Research category without drawing helper tier items in the UI.
		builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
			.add(ItemStackFactory.researchTier(recipe.tier()));

		int total = Math.min(18, recipe.ingredients().size());
		for (int index = 0; index < total; index++) {
			int row = index / MAX_COLUMNS;
			int rowStartIndex = row * MAX_COLUMNS;
			int rowCount = Math.min(MAX_COLUMNS, total - rowStartIndex);
			int rowWidth = rowCount * SLOT_SIZE + Math.max(0, rowCount - 1) * SLOT_GAP;
			int startX = (CategoryUi.WIDTH - rowWidth) / 2;
			int column = index - rowStartIndex;
			int x = startX + column * (SLOT_SIZE + SLOT_GAP);
			int y = 49 + row * 23;
			IngredientDefinition ingredient = recipe.ingredients().get(index);
			CompactItemStackRenderer.apply(builder.addInputSlot(x, y).setStandardSlotBackground())
				.add(ItemStackFactory.createIngredient(ingredient));
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, ResearchPageRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, "Research Tier " + recipe.tier(), recipe.pageTitle(), ACCENT);
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(ResearchPageRecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		CategoryUi.drawPanel(graphics, HEIGHT, ACCENT, "research");
		var font = Minecraft.getInstance().font;
		ThemePalette palette = ThemePalette.current();
		if (!recipe.ingredients().isEmpty()) {
			graphics.drawCenteredString(font, "Required Materials", CategoryUi.WIDTH / 2, 38, 0xffffd166);
			int shown = Math.min(18, recipe.ingredients().size());
			String footer = shown + " material" + (shown == 1 ? "" : "s") + " • hover items for details";
			graphics.drawCenteredString(font, CategoryUi.fit(footer, 160), CategoryUi.WIDTH / 2, HEIGHT - 13, palette.muted());
		} else {
			List<String> wrapped = wrappedDetails(recipe.lines());
			int maxRows = 8;
			int shown = Math.min(maxRows, wrapped.size());
			for (int index = 0; index < shown; index++) {
				graphics.drawString(font, "• " + CategoryUi.fit(wrapped.get(index), 154), 10, 42 + index * 10, palette.secondary(), false);
			}
			if (wrapped.isEmpty()) {
				graphics.drawCenteredString(font, "No additional details for this view.", CategoryUi.WIDTH / 2, 63, palette.muted());
			} else if (wrapped.size() > maxRows) {
				graphics.drawString(font, "+" + (wrapped.size() - maxRows) + " more — hover for full details", 10, HEIGHT - 13, palette.muted(), false);
			}
		}
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, ACCENT);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ResearchPageRecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		String title = "Research Tier " + recipe.tier();
		if (CategoryUi.addHeaderTooltip(tooltip, title, recipe.pageTitle(), mouseX, mouseY)) return;
		if (CategoryUi.addActionTooltip(tooltip, snapshot(recipe), mouseX, mouseY)) return;
		if (recipe.ingredients().isEmpty() && CategoryUi.addTextBlockTooltip(tooltip, recipe.lines(), 8, 38, 160, HEIGHT - 45, mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;
		CategoryUi.heading(tooltip, "Mining Research Tier " + recipe.tier(), ChatFormatting.AQUA);
		CategoryUi.line(tooltip, "View: " + recipe.pageTitle(), ChatFormatting.GREEN);
		if (!recipe.lines().isEmpty()) {
			CategoryUi.blank(tooltip);
			for (String line : recipe.lines()) CategoryUi.line(tooltip, line, ChatFormatting.GRAY);
		}
	}

	private static List<String> wrappedDetails(List<String> lines) {
		List<String> wrapped = new ArrayList<>();
		for (String line : lines) wrapped.addAll(CategoryUi.wrap(line, 31));
		return wrapped;
	}

	private static RecipeSnapshot snapshot(ResearchPageRecipeData recipe) {
		List<String> notes = new ArrayList<>(recipe.lines());
		notes.add("View: " + recipe.pageTitle());
		return new RecipeSnapshot(
			"research/tier_" + recipe.tier() + "/" + recipe.pageId(),
			"Mining Research Tier " + recipe.tier() + " — " + recipe.pageTitle(),
			recipe.ingredients(),
			new IngredientDefinition("minecraft:knowledge_book", 1, "Mining Research Tier " + recipe.tier()),
			notes
		);
	}

	@Override public boolean needsRecipeBorder() { return false; }
	@Override public Identifier getIdentifier(ResearchPageRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "research/tier_" + recipe.tier() + "/" + CategoryUi.sanitize(recipe.pageId()));
	}
}
