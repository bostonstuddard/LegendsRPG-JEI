package com.legendsrpg.jei.client;

import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FavoriteRecipePickerScreen extends Screen {
	private static final int MAX_ROWS = 10;
	private static final int PANEL_WIDTH = 390;
	private static final int ROW_HEIGHT = 24;

	private final FavoriteRecipeTreeScreen parent;
	private final List<RecipeSnapshot> allRoots;
	private final RecipeSnapshot selected;
	private final List<Button> rowButtons = new ArrayList<>();
	private List<RecipeSnapshot> filtered = List.of();
	private EditBox search;
	private int firstRow;
	private int panelLeft;
	private int panelTop;
	private int panelRight;
	private int panelBottom;

	public FavoriteRecipePickerScreen(FavoriteRecipeTreeScreen parent, List<RecipeSnapshot> roots, RecipeSnapshot selected) {
		super(Component.literal("Select Favorite Recipe"));
		this.parent = parent;
		this.allRoots = roots == null ? List.of() : List.copyOf(roots);
		this.selected = selected;
	}

	@Override
	protected void init() {
		super.init();
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int panelHeight = Math.min(this.height - 24, 66 + MAX_ROWS * ROW_HEIGHT + 34);
		panelLeft = (this.width - panelWidth) / 2;
		panelTop = (this.height - panelHeight) / 2;
		panelRight = panelLeft + panelWidth;
		panelBottom = panelTop + panelHeight;

		search = this.addRenderableWidget(new EditBox(
			this.font,
			panelLeft + 16,
			panelTop + 34,
			panelWidth - 32,
			20,
			Component.literal("Search favorite recipes")
		));
		search.setHint(Component.literal("Search item, shop, recipe, or source..."));
		search.setResponder(value -> {
			firstRow = 0;
			filter();
		});

		for (int index = 0; index < MAX_ROWS; index++) {
			final int row = index;
			Button button = this.addRenderableWidget(Button.builder(Component.empty(), ignored -> choose(row))
				.bounds(panelLeft + 16, panelTop + 62 + index * ROW_HEIGHT, panelWidth - 32, 20)
				.build());
			rowButtons.add(button);
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, ignored -> onClose())
			.bounds(panelLeft + panelWidth / 2 - 55, panelBottom - 26, 110, 20)
			.build());

		filter();
		this.setInitialFocus(search);
	}

	private void filter() {
		String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		List<RecipeSnapshot> result = new ArrayList<>();
		for (RecipeSnapshot root : allRoots) {
			String searchable = (root.title() + " " + root.key() + " " + (root.output() == null ? "" : root.output().id())).toLowerCase(Locale.ROOT);
			if (query.isBlank() || searchable.contains(query)) result.add(root);
		}
		filtered = List.copyOf(result);
		firstRow = Math.max(0, Math.min(firstRow, Math.max(0, filtered.size() - MAX_ROWS)));
		refreshRows();
	}

	private void refreshRows() {
		if (rowButtons.isEmpty()) return;
		for (int row = 0; row < rowButtons.size(); row++) {
			int index = firstRow + row;
			Button button = rowButtons.get(row);
			boolean visible = index < filtered.size();
			button.visible = visible;
			button.active = visible;
			if (!visible) continue;
			RecipeSnapshot recipe = filtered.get(index);
			String marker = selected != null && selected.key().equals(recipe.key()) ? "▶ " : "";
			button.setMessage(Component.literal(marker + fit(recipe.title(), button.getWidth() - 34)));
		}
	}

	private void choose(int row) {
		int index = firstRow + row;
		if (index < 0 || index >= filtered.size() || this.minecraft == null) return;
		parent.selectRoot(filtered.get(index));
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (mouseX >= panelLeft && mouseX < panelRight && mouseY >= panelTop + 58 && mouseY < panelBottom - 30) {
			int maximum = Math.max(0, filtered.size() - MAX_ROWS);
			int next = Math.max(0, Math.min(maximum, firstRow - (int) Math.signum(verticalAmount)));
			if (next != firstRow) {
				firstRow = next;
				refreshRows();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ThemePalette palette = ThemePalette.current();
		graphics.fillGradient(0, 0, this.width, this.height, palette.screenTop(), palette.screenBottom());
		graphics.fill(panelLeft, panelTop, panelRight, panelBottom, palette.panel());
		graphics.renderOutline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, palette.outline());
		graphics.fill(panelLeft, panelTop, panelRight, panelTop + 3, palette.stripe());
		graphics.drawCenteredString(this.font, "Select Favorite Recipe", this.width / 2, panelTop + 11, 0xffffcf63);
		graphics.drawCenteredString(this.font, filtered.size() + " matching favorites", this.width / 2, panelTop + 22, palette.muted());

		super.render(graphics, mouseX, mouseY, delta);

		for (int row = 0; row < rowButtons.size(); row++) {
			int index = firstRow + row;
			if (index >= filtered.size()) break;
			RecipeSnapshot recipe = filtered.get(index);
			if (recipe.output() == null) continue;
			int itemX = panelLeft + 20;
			int itemY = panelTop + 64 + row * ROW_HEIGHT;
			graphics.renderItem(ItemStackFactory.create(recipe.output().id(), 1), itemX, itemY);
		}

		if (filtered.isEmpty()) {
			graphics.drawCenteredString(this.font, "No favorite recipes match that search.", this.width / 2, (panelTop + panelBottom) / 2, palette.muted());
		} else if (filtered.size() > MAX_ROWS) {
			graphics.drawString(this.font, "Mouse wheel to scroll", panelRight - 116, panelBottom - 20, palette.muted(), false);
		}
	}

	private String fit(String value, int width) {
		if (value == null) return "";
		if (this.font.width(value) <= width) return value;
		return this.font.plainSubstrByWidth(value, Math.max(0, width - this.font.width("..."))) + "...";
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}
}
