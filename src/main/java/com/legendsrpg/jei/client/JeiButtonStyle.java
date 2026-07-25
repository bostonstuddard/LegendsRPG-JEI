package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;
import net.minecraft.client.gui.GuiGraphics;

public final class JeiButtonStyle {
	private JeiButtonStyle() {}

	public static void draw(GuiGraphics graphics, boolean active, boolean hovered, boolean pressed, int x, int y, int width, int height) {
		ThemePalette palette = ThemePalette.current();
		if (LegendsConfig.THEME_MINECRAFT.equals(LegendsConfig.get().theme())) {
			drawMinecraft(graphics, active, hovered, pressed, x, y, width, height, palette);
			return;
		}
		int fill;
		int outline;
		if (!active) {
			fill = palette.inner();
			outline = palette.innerOutline();
		} else if (pressed) {
			fill = palette.divider();
			outline = palette.stripe();
		} else if (hovered) {
			fill = palette.header();
			outline = palette.stripe();
		} else {
			fill = palette.slot();
			outline = palette.outline();
		}

		graphics.fill(x, y, x + width, y + height, fill);
		drawOutline(graphics, x, y, width, height, outline);
		if (height >= 6 && width >= 6) {
			graphics.fill(x + 2, y + 2, x + width - 2, y + 3, palette.divider());
		}
	}

	private static void drawMinecraft(
		GuiGraphics graphics,
		boolean active,
		boolean hovered,
		boolean pressed,
		int x,
		int y,
		int width,
		int height,
		ThemePalette palette
	) {
		int outer = active ? 0xff000000 : 0xff303030;
		int face = !active ? 0xff555555 : pressed ? 0xff6b6b6b : hovered ? 0xff777777 : 0xff666666;
		graphics.fill(x, y, x + width, y + height, outer);
		if (width <= 2 || height <= 2) return;
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, face);
		if (width <= 4 || height <= 4) return;
		graphics.fill(x + 2, y + 2, x + width - 2, y + 3, active ? 0xffaaaaaa : 0xff777777);
		graphics.fill(x + 2, y + 3, x + 3, y + height - 2, active ? 0xff999999 : 0xff666666);
		graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xff333333);
		graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 2, 0xff333333);
		if (hovered && active && !pressed) drawOutline(graphics, x, y, width, height, palette.secondary());
	}

	public static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) return;
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y + 1, x + 1, y + height - 1, color);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}
}
