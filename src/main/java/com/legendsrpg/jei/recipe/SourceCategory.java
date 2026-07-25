package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.SourceRecipeData;
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

public final class SourceCategory implements IRecipeCategory<SourceRecipeData> {
	private static final int HEIGHT = 102;
	private final IDrawable icon;

	public SourceCategory(IGuiHelper guiHelper) {
		this.icon = guiHelper.createDrawableItemLike(Items.COMPASS);
	}

	@Override
	public IRecipeType<SourceRecipeData> getRecipeType() {
		return LegendsRecipeTypes.SOURCE;
	}

	@Override
	public Component getTitle() {
		return Component.literal("LegendsRPG Sources");
	}

	@Override
	public int getWidth() {
		return CategoryUi.WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SourceRecipeData recipe, IFocusGroup focuses) {
		CompactItemStackRenderer.apply(builder.addInputSlot(28, 50).setStandardSlotBackground())
			.add(ItemStackFactory.guideIcon(recipe.icon(), recipe.source()))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, recipe.detail(), ChatFormatting.GRAY);
				if (recipe.chanceText() != null && !recipe.chanceText().isBlank()) {
					CategoryUi.line(tooltip, recipe.chanceText(), ChatFormatting.GOLD);
				}
			});

		CompactItemStackRenderer.apply(builder.addOutputSlot(137, 50).setStandardSlotBackground())
			.add(ItemStackFactory.create(recipe.output().id(), recipe.output().count()))
			.addRichTooltipCallback((view, tooltip) -> {
				CategoryUi.blank(tooltip);
				CategoryUi.line(tooltip, recipe.type() + ": " + recipe.source(), headingColor(recipe.type()));
			});
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, SourceRecipeData recipe, IFocusGroup focuses) {
		CategoryUi.addHeader(builder, recipe.source(), recipe.type(), accent(recipe.type()));
		CategoryUi.addActionListeners(builder, () -> snapshot(recipe));
	}

	@Override
	public void draw(SourceRecipeData recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		int accent = accent(recipe.type());
		CategoryUi.drawPanel(graphics, HEIGHT, accent, theme(recipe.type()));
		CategoryUi.drawArrow(graphics, 78, 51, 0xff8fa9bb);

		var font = Minecraft.getInstance().font;
		graphics.drawCenteredString(font, "Source", 37, 38, 0xff91a8b8);
		graphics.drawCenteredString(font, "Result", 146, 38, 0xff91a8b8);
		graphics.drawString(font, CategoryUi.fit(recipe.detail(), 154), 10, 79, 0xffa9bac6, false);
		CategoryUi.drawActionButtons(graphics, snapshot(recipe), mouseX, mouseY, accent);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, SourceRecipeData recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		if (CategoryUi.addHeaderTooltip(tooltip, recipe.source(), recipe.type(), mouseX, mouseY)) return;
		RecipeSnapshot snapshot = snapshot(recipe);
		if (CategoryUi.addActionTooltip(tooltip, snapshot, mouseX, mouseY)) return;
		if (CategoryUi.addTruncatedTextTooltip(tooltip, recipe.detail(), 10, 76, 154, 13, mouseX, mouseY)) return;
		if (!CategoryUi.isInfoHovered(mouseX, mouseY)) return;

		CategoryUi.heading(tooltip, recipe.type(), headingColor(recipe.type()));
		CategoryUi.line(tooltip, "Source: " + recipe.source(), ChatFormatting.AQUA);
		CategoryUi.line(tooltip, recipe.detail(), ChatFormatting.GRAY);
		if (recipe.chanceText() != null && !recipe.chanceText().isBlank()) {
			CategoryUi.line(tooltip, "Chance: " + recipe.chanceText(), ChatFormatting.GOLD);
		}
		CategoryUi.line(tooltip, "Amount: " + recipe.amount(), ChatFormatting.YELLOW);
		for (String note : recipe.notes()) {
			CategoryUi.line(tooltip, "• " + note, ChatFormatting.DARK_GRAY);
		}
	}

	private static RecipeSnapshot snapshot(SourceRecipeData recipe) {
		List<String> notes = new ArrayList<>();
		notes.add(recipe.type() + ": " + recipe.source());
		if (recipe.detail() != null && !recipe.detail().isBlank()) notes.add(recipe.detail());
		if (recipe.chanceText() != null && !recipe.chanceText().isBlank()) notes.add("Chance: " + recipe.chanceText());
		notes.addAll(recipe.notes());
		return new RecipeSnapshot(
			"source/" + CategoryUi.sanitize(recipe.id()),
			recipe.source(),
			List.of(),
			new IngredientDefinition(recipe.output().id(), recipe.output().count(), recipe.output().label()),
			notes
		);
	}

	@Override
	public boolean needsRecipeBorder() {
		return false;
	}

	@Override
	public Identifier getIdentifier(SourceRecipeData recipe) {
		return Identifier.fromNamespaceAndPath("legendsrpg_jei", "source/" + CategoryUi.sanitize(recipe.id()));
	}

	private static int accent(String type) {
		return switch (type) {
			case "Mining" -> 0xff75c5ff;
			case "Fishing" -> 0xff4cc9f0;
			case "Foraging" -> 0xff66d98c;
			case "Gathering" -> 0xff8ee08e;
			case "Apiary" -> 0xffffc84a;
			case "Smithing" -> 0xffff9f43;
			case "Quest Reward" -> 0xffffd166;
			case "Boss Drop" -> 0xffff5d73;
			case "Container Reward" -> 0xffb388ff;
			case "Dungeon Reward" -> 0xffc77dff;
			case "Gift Reward" -> 0xffff9ed2;
			case "Event Reward" -> 0xffff7ab6;
			case "Package Reward" -> 0xff75e6da;
			case "Daily Reward" -> 0xff63d8ff;
			case "Secret Discovery" -> 0xffa88cff;
			case "Coming Soon" -> 0xff7f8c9a;
			case "Disabled Recipe" -> 0xffe75b5b;
			default -> 0xff7bc7ff;
		};
	}

	private static String theme(String type) {
		return switch (type) {
			case "Mining" -> "mining";
			case "Fishing" -> "fishing";
			case "Foraging", "Gathering", "Apiary" -> "foraging";
			case "Boss Drop", "Dungeon Reward" -> "boss";
			case "Smithing" -> "forge";
			default -> "guide";
		};
	}

	private static ChatFormatting headingColor(String type) {
		return switch (type) {
			case "Foraging", "Gathering" -> ChatFormatting.GREEN;
			case "Apiary", "Quest Reward", "Daily Reward" -> ChatFormatting.GOLD;
			case "Boss Drop", "Disabled Recipe" -> ChatFormatting.RED;
			case "Container Reward", "Dungeon Reward", "Secret Discovery" -> ChatFormatting.LIGHT_PURPLE;
			case "Event Reward", "Gift Reward" -> ChatFormatting.LIGHT_PURPLE;
			case "Smithing" -> ChatFormatting.YELLOW;
			case "Coming Soon" -> ChatFormatting.DARK_GRAY;
			default -> ChatFormatting.AQUA;
		};
	}
}
