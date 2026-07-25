package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;
import net.minecraft.client.gui.GuiGraphics;

public final class PinnedRecipeScreenButton {
	public static final int SIZE = 20;
	private static final int GAP = 2;
	private static final int BOTTOM_MARGIN = 26;
	public static final String TREE_TOOLTIP = "Open favorite recipe tree";
	public static final String CLEAR_TOOLTIP = "Clear pinned recipe";

	private PinnedRecipeScreenButton() {}

	public static void register() {}

	public static int treeX() {
		return LegendsConfig.get().showJeiButtons() ? 50 : 6;
	}

	public static int clearX() {
		return treeX() + SIZE + GAP;
	}

	public static int buttonY(int screenHeight) {
		return screenHeight - BOTTOM_MARGIN;
	}

	public static void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		int y = buttonY(screenHeight);
		int treeX = treeX();
		int clearX = clearX();
		boolean hasPinned = LegendsConfig.get().pinnedRecipe() != null;
		drawTreeButton(graphics, mouseX, mouseY, treeX, y);
		drawClearButton(graphics, mouseX, mouseY, clearX, y, hasPinned);
	}

	private static void drawTreeButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
		boolean hovered = inside(mouseX, mouseY, x, y, SIZE, SIZE);
		JeiButtonStyle.draw(graphics, true, hovered, false, x, y, SIZE, SIZE);
		int color = ThemePalette.current().text();
		graphics.fill(x + 8, y + 4, x + 12, y + 8, color);
		graphics.fill(x + 4, y + 12, x + 8, y + 16, color);
		graphics.fill(x + 12, y + 12, x + 16, y + 16, color);
		graphics.fill(x + 9, y + 8, x + 11, y + 12, color);
		graphics.fill(x + 6, y + 10, x + 14, y + 12, color);
	}

	private static void drawClearButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean active) {
		boolean hovered = active && inside(mouseX, mouseY, x, y, SIZE, SIZE);
		JeiButtonStyle.draw(graphics, active, hovered, false, x, y, SIZE, SIZE);
		int color = active ? ThemePalette.current().text() : ThemePalette.current().muted();
		for (int offset = 0; offset < 8; offset++) {
			graphics.fill(x + 6 + offset, y + 6 + offset, x + 8 + offset, y + 8 + offset, color);
			graphics.fill(x + 12 - offset, y + 6 + offset, x + 14 - offset, y + 8 + offset, color);
		}
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
