package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.JeiClientConfigEditor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class JeiLayoutSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 400;
	private static final int PANEL_HEIGHT = 362;
	private final Screen parent;
	private Button centerSearch;
	private Button lookupHistory;
	private Button ingredientBackground;
	private Button bookmarkBackground;
	private Button pageNavigation;
	private Button bookmarksFront;
	private Button recipeGuiHeight;
	private Button ingredientRows;
	private Button ingredientColumns;
	private Button bookmarkRows;
	private Button bookmarkColumns;
	private Button lowMemory;
	private Component status = Component.literal("Changes are written directly to JEI's config.");
	private int statusColor = 0xffaeb8c2;

	public JeiLayoutSettingsScreen(Screen parent) {
		super(Component.literal("JEI Layout Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int column = (panelWidth - 40) / 2;
		int rightColumn = left + 24 + column;

		centerSearch = addToggle(left + 16, top + 58, column, this::centerSearchText, () -> toggle("appearance", "centerSearch", true));
		lookupHistory = addToggle(rightColumn, top + 58, column, this::lookupHistoryText, () -> toggle("lookupHistory", "enabled", false));
		ingredientBackground = addToggle(left + 16, top + 86, column, this::ingredientBackgroundText, () -> toggle("ingredientList", "drawBackground", true));
		bookmarkBackground = addToggle(rightColumn, top + 86, column, this::bookmarkBackgroundText, () -> toggle("bookmarkList", "drawBackground", false));
		pageNavigation = addToggle(left + 16, top + 114, column, this::pageNavigationText, this::cycleNavigation);
		bookmarksFront = addToggle(rightColumn, top + 114, column, this::bookmarksFrontText, () -> toggle("bookmarks", "addBookmarksToFrontEnabled", true));

		recipeGuiHeight = addToggle(left + 16, top + 146, panelWidth - 32, this::recipeGuiHeightText,
			() -> cycleInt("appearance", "recipeGuiHeight", 350, 175, 600, 25));
		ingredientRows = addToggle(left + 16, top + 174, column, this::ingredientRowsText,
			() -> cycleInt("ingredientList", "maxRows", 12, 1, 24, 1));
		ingredientColumns = addToggle(rightColumn, top + 174, column, this::ingredientColumnsText,
			() -> cycleInt("ingredientList", "maxColumns", 9, 2, 24, 1));
		bookmarkRows = addToggle(left + 16, top + 202, column, this::bookmarkRowsText,
			() -> cycleInt("bookmarkList", "maxRows", 2, 1, 16, 1));
		bookmarkColumns = addToggle(rightColumn, top + 202, column, this::bookmarkColumnsText,
			() -> cycleInt("bookmarkList", "maxColumns", 9, 2, 24, 1));
		lowMemory = addToggle(left + 16, top + 234, panelWidth - 32, this::lowMemoryText,
			() -> toggle("performance", "lowMemorySlowSearchEnabled", false));

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(left + panelWidth / 2 - 55, top + 312, 110, 20).build());
		refresh();
	}

	private Button addToggle(int x, int y, int width, Supplier<Component> label, Runnable action) {
		return this.addRenderableWidget(Button.builder(label.get(), button -> {
			action.run();
			refresh();
		}).bounds(x, y, width, 20).build());
	}

	private void toggle(String section, String key, boolean fallback) {
		boolean success = JeiClientConfigEditor.toggleBoolean(section, key, fallback);
		setStatus(success, success ? "Saved. Reopen the inventory if JEI does not refresh immediately." : "Could not save the JEI config.");
	}

	private void cycleInt(String section, String key, int fallback, int minimum, int maximum, int step) {
		int current = JeiClientConfigEditor.getInt(section, key, fallback);
		int next = current + step;
		if (next > maximum || next < minimum) next = minimum;
		boolean success = JeiClientConfigEditor.setInt(section, key, next);
		setStatus(success, success ? "Size saved. Reopen the inventory to apply the layout." : "Could not save the JEI config.");
	}

	private void cycleNavigation() {
		String current = JeiClientConfigEditor.get("ingredientList", "buttonNavigationVisibility", "AUTO_HIDE");
		String next = switch (current.toUpperCase()) {
			case "ENABLED" -> "AUTO_HIDE";
			case "AUTO_HIDE" -> "DISABLED";
			default -> "ENABLED";
		};
		boolean success = JeiClientConfigEditor.set("ingredientList", "buttonNavigationVisibility", next);
		setStatus(success, success ? "Page navigation saved." : "Could not save the JEI config.");
	}

	private void setStatus(boolean success, String message) {
		status = Component.literal(message);
		statusColor = success ? 0xff79d279 : 0xffff6b6b;
	}

	private void refresh() {
		if (centerSearch == null) return;
		centerSearch.setMessage(centerSearchText());
		lookupHistory.setMessage(lookupHistoryText());
		ingredientBackground.setMessage(ingredientBackgroundText());
		bookmarkBackground.setMessage(bookmarkBackgroundText());
		pageNavigation.setMessage(pageNavigationText());
		bookmarksFront.setMessage(bookmarksFrontText());
		recipeGuiHeight.setMessage(recipeGuiHeightText());
		ingredientRows.setMessage(ingredientRowsText());
		ingredientColumns.setMessage(ingredientColumnsText());
		bookmarkRows.setMessage(bookmarkRowsText());
		bookmarkColumns.setMessage(bookmarkColumnsText());
		lowMemory.setMessage(lowMemoryText());
	}

	private Component centerSearchText() { return onOff("Centered Search", JeiClientConfigEditor.getBoolean("appearance", "centerSearch", true)); }
	private Component lookupHistoryText() { return onOff("Lookup History", JeiClientConfigEditor.getBoolean("lookupHistory", "enabled", false)); }
	private Component ingredientBackgroundText() { return onOff("Item Background", JeiClientConfigEditor.getBoolean("ingredientList", "drawBackground", true)); }
	private Component bookmarkBackgroundText() { return onOff("Favorite Background", JeiClientConfigEditor.getBoolean("bookmarkList", "drawBackground", false)); }
	private Component bookmarksFrontText() { return onOff("Newest Favorites First", JeiClientConfigEditor.getBoolean("bookmarks", "addBookmarksToFrontEnabled", true)); }
	private Component lowMemoryText() { return onOff("Low-Memory Search (slower)", JeiClientConfigEditor.getBoolean("performance", "lowMemorySlowSearchEnabled", false)); }
	private Component recipeGuiHeightText() { return number("Recipe Window Height", JeiClientConfigEditor.getInt("appearance", "recipeGuiHeight", 350), " px"); }
	private Component ingredientRowsText() { return number("Item Rows", JeiClientConfigEditor.getInt("ingredientList", "maxRows", 12), ""); }
	private Component ingredientColumnsText() { return number("Item Columns", JeiClientConfigEditor.getInt("ingredientList", "maxColumns", 9), ""); }
	private Component bookmarkRowsText() { return number("Favorite Rows", JeiClientConfigEditor.getInt("bookmarkList", "maxRows", 2), ""); }
	private Component bookmarkColumnsText() { return number("Favorite Columns", JeiClientConfigEditor.getInt("bookmarkList", "maxColumns", 9), ""); }
	private Component pageNavigationText() {
		return Component.literal("Page Buttons: " + JeiClientConfigEditor.get("ingredientList", "buttonNavigationVisibility", "AUTO_HIDE"))
			.withStyle(ChatFormatting.AQUA);
	}

	private static Component onOff(String label, boolean value) {
		return Component.literal(label + ": " + (value ? "ON" : "OFF"))
			.withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
	}

	private static Component number(String label, int value, String suffix) {
		return Component.literal(label + ": " + value + suffix).withStyle(ChatFormatting.AQUA);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		ThemePalette palette = ThemePalette.current();
		graphics.fillGradient(0, 0, this.width, this.height, palette.screenTop(), palette.screenBottom());
		graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, palette.panel());
		graphics.renderOutline(left, top, panelWidth, PANEL_HEIGHT, palette.outline());
		graphics.fill(left, top, left + panelWidth, top + 3, palette.stripe());
		graphics.fill(left + 12, top + 138, left + panelWidth - 12, top + 139, palette.divider());
		graphics.fill(left + 12, top + 226, left + panelWidth - 12, top + 227, palette.divider());
		graphics.drawCenteredString(this.font, "JEI Layout Settings", this.width / 2, top + 13, palette.secondary());
		graphics.drawCenteredString(this.font, "Overlay visibility, backgrounds, and grid sizing.", this.width / 2, top + 29, palette.text());
		graphics.drawCenteredString(this.font, "Click a size repeatedly to cycle through its allowed range.", this.width / 2, top + 41, palette.muted());
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, status, this.width / 2, top + 270, statusColor);
		graphics.drawCenteredString(this.font, "Favorite backgrounds default to OFF.", this.width / 2, top + 286, palette.muted());
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}
}
