package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.client.RecipeActionManager;
import com.legendsrpg.jei.client.ThemePalette;
import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.RecipeSnapshot;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class CategoryUi {
	static final int WIDTH = 176;
	static final int HEADER_HEIGHT = 31;
	static final int HEADER_TEXT_WIDTH = 102;
	static final int TITLE_X = 8;
	static final int TITLE_Y = 7;
	static final int SUBTITLE_Y = 18;
	static final int FAVORITE_X = 114;
	static final int PIN_X = 135;
	static final int INFO_X = 156;
	static final int ACTION_Y = 7;
	static final int ACTION_SIZE = 16;

	private CategoryUi() {}

	static void drawPanel(GuiGraphics graphics, int height, int accent) {
		drawPanel(graphics, height, accent, "default");
	}

	static void drawPanel(GuiGraphics graphics, int height, int accent, String theme) {
		ThemePalette palette = ThemePalette.current();
		graphics.fill(0, 0, WIDTH, height, palette.panel());
		graphics.fill(2, 2, WIDTH - 2, HEADER_HEIGHT, palette.header());
		graphics.fill(6, HEADER_HEIGHT + 4, WIDTH - 6, height - 6, palette.inner());
		graphics.fill(0, 0, WIDTH, 3, accent);
		graphics.fill(2, HEADER_HEIGHT, WIDTH - 2, HEADER_HEIGHT + 1, palette.divider());
		drawOutline(graphics, 0, 0, WIDTH, height, palette.outline());
		drawOutline(graphics, 6, HEADER_HEIGHT + 4, WIDTH - 12, height - HEADER_HEIGHT - 10, palette.innerOutline());
	}

	static void addHeader(IRecipeExtrasBuilder builder, String title, String subtitle, int accent) {
		builder.addText(Component.literal(fit(title, HEADER_TEXT_WIDTH)), HEADER_TEXT_WIDTH, 11)
			.setPosition(TITLE_X, TITLE_Y)
			.setColor(accent)
			.setShadow(true);
		if (subtitle != null && !subtitle.isBlank()) {
			builder.addText(Component.literal(fit(subtitle, HEADER_TEXT_WIDTH)), HEADER_TEXT_WIDTH, 10)
				.setPosition(TITLE_X, SUBTITLE_Y)
				.setColor(ThemePalette.current().secondary());
		}
	}

	static boolean addHeaderTooltip(ITooltipBuilder tooltip, String title, String subtitle, double mouseX, double mouseY) {
		var font = Minecraft.getInstance().font;
		if (font.width(title) > HEADER_TEXT_WIDTH && inside(mouseX, mouseY, TITLE_X, TITLE_Y - 1, HEADER_TEXT_WIDTH, 11)) {
			heading(tooltip, title, ChatFormatting.WHITE);
			return true;
		}
		if (subtitle != null && font.width(subtitle) > HEADER_TEXT_WIDTH && inside(mouseX, mouseY, TITLE_X, SUBTITLE_Y - 1, HEADER_TEXT_WIDTH, 11)) {
			line(tooltip, subtitle, ChatFormatting.GRAY);
			return true;
		}
		return false;
	}

	static boolean addTruncatedTextTooltip(ITooltipBuilder tooltip, String text, int x, int y, int width, int height, double mouseX, double mouseY) {
		if (text == null || text.isBlank()) return false;
		if (Minecraft.getInstance().font.width(text) <= width) return false;
		if (!inside(mouseX, mouseY, x, y, width, height)) return false;
		line(tooltip, text, ChatFormatting.GRAY);
		return true;
	}

	static boolean addTextBlockTooltip(ITooltipBuilder tooltip, List<String> lines, int x, int y, int width, int height, double mouseX, double mouseY) {
		if (lines == null || lines.isEmpty() || !inside(mouseX, mouseY, x, y, width, height)) return false;
		heading(tooltip, "Full Details", ChatFormatting.AQUA);
		for (String line : lines) line(tooltip, line, ChatFormatting.GRAY);
		return true;
	}

	static void addActionListeners(IRecipeExtrasBuilder builder, Supplier<RecipeSnapshot> snapshotSupplier) {
		builder.addGuiEventListener(favoriteButton(FAVORITE_X, snapshotSupplier));
		builder.addGuiEventListener(button(PIN_X, () -> RecipeActionManager.togglePinned(snapshotSupplier.get())));
	}

	private static IJeiGuiEventListener favoriteButton(int x, Supplier<RecipeSnapshot> snapshotSupplier) {
		return new IJeiGuiEventListener() {
			@Override
			public ScreenRectangle getArea() {
				return new ScreenRectangle(x, ACTION_Y, ACTION_SIZE, ACTION_SIZE);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					RecipeActionManager.toggleFavorite(snapshotSupplier.get());
					return true;
				}
				if (button == 1) {
					RecipeActionManager.openFavoriteTree(snapshotSupplier.get());
					return true;
				}
				return false;
			}
		};
	}

	private static IJeiGuiEventListener button(int x, Runnable action) {
		return new IJeiGuiEventListener() {
			@Override
			public ScreenRectangle getArea() {
				return new ScreenRectangle(x, ACTION_Y, ACTION_SIZE, ACTION_SIZE);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button != 0) return false;
				action.run();
				return true;
			}
		};
	}

	static void drawActionButtons(GuiGraphics graphics, RecipeSnapshot snapshot, double mouseX, double mouseY, int accent) {
		boolean favorite = RecipeActionManager.isFavorite(snapshot.key());
		boolean pinned = RecipeActionManager.isPinned(snapshot.key());
		drawActionButton(graphics, FAVORITE_X, mouseX, mouseY, favorite, accent);
		drawActionButton(graphics, PIN_X, mouseX, mouseY, pinned, accent);
		drawActionButton(graphics, INFO_X, mouseX, mouseY, false, accent);
		drawStar(graphics, FAVORITE_X, favorite ? 0xffffd66b : ThemePalette.current().text());
		drawPin(graphics, PIN_X, pinned ? 0xffffd66b : ThemePalette.current().text());
		drawInfo(graphics, INFO_X, ThemePalette.current().text());
	}

	private static void drawActionButton(GuiGraphics graphics, int x, double mouseX, double mouseY, boolean active, int accent) {
		ThemePalette palette = ThemePalette.current();
		boolean hovered = isHovered(x, mouseX, mouseY);
		int fill = active ? palette.divider() : (hovered ? palette.slotOutline() : palette.slot());
		int outline = active || hovered ? accent : palette.slotOutline();
		graphics.fill(x, ACTION_Y, x + ACTION_SIZE, ACTION_Y + ACTION_SIZE, fill);
		drawOutline(graphics, x, ACTION_Y, ACTION_SIZE, ACTION_SIZE, outline);
	}

	private static void drawStar(GuiGraphics graphics, int x, int color) {
		int centerX = x + ACTION_SIZE / 2;
		int centerY = ACTION_Y + ACTION_SIZE / 2;
		graphics.fill(centerX - 1, ACTION_Y + 3, centerX + 1, ACTION_Y + 13, color);
		graphics.fill(x + 3, centerY - 1, x + 13, centerY + 1, color);
		graphics.fill(x + 5, ACTION_Y + 5, x + 11, ACTION_Y + 11, color);
		graphics.fill(x + 4, ACTION_Y + 4, x + 6, ACTION_Y + 6, color);
		graphics.fill(x + 10, ACTION_Y + 4, x + 12, ACTION_Y + 6, color);
		graphics.fill(x + 4, ACTION_Y + 10, x + 6, ACTION_Y + 12, color);
		graphics.fill(x + 10, ACTION_Y + 10, x + 12, ACTION_Y + 12, color);
	}

	private static void drawPin(GuiGraphics graphics, int x, int color) {
		graphics.fill(x + 5, ACTION_Y + 3, x + 10, ACTION_Y + 5, color);
		graphics.fill(x + 6, ACTION_Y + 5, x + 9, ACTION_Y + 9, color);
		graphics.fill(x + 4, ACTION_Y + 8, x + 11, ACTION_Y + 10, color);
		graphics.fill(x + 7, ACTION_Y + 10, x + 8, ACTION_Y + 13, color);
	}

	private static void drawInfo(GuiGraphics graphics, int x, int color) {
		graphics.fill(x + 7, ACTION_Y + 3, x + 10, ACTION_Y + 6, color);
		graphics.fill(x + 7, ACTION_Y + 8, x + 10, ACTION_Y + 13, color);
		graphics.fill(x + 6, ACTION_Y + 12, x + 11, ACTION_Y + 14, color);
	}

	static boolean addActionTooltip(ITooltipBuilder tooltip, RecipeSnapshot snapshot, double mouseX, double mouseY) {
		if (isHovered(FAVORITE_X, mouseX, mouseY)) {
			heading(tooltip, RecipeActionManager.isFavorite(snapshot.key()) ? "Remove Favorite" : "Favorite Recipe", ChatFormatting.GOLD);
			line(tooltip, "Left-click to favorite. Right-click to open the favorite recipe tree.", ChatFormatting.GRAY);
			return true;
		}
		if (isHovered(PIN_X, mouseX, mouseY)) {
			heading(tooltip, RecipeActionManager.isPinned(snapshot.key()) ? "Unpin Recipe" : "Pin Recipe", ChatFormatting.AQUA);
			line(tooltip, "Keeps the required materials visible after closing the inventory.", ChatFormatting.GRAY);
			return true;
		}
		return false;
	}

	static void drawArrow(GuiGraphics graphics, int x, int y, int color) {
		graphics.fill(x, y + 5, x + 14, y + 9, color);
		graphics.fill(x + 12, y + 2, x + 16, y + 12, color);
		graphics.fill(x + 16, y + 4, x + 22, y + 10, color);
	}

	private static boolean isHovered(int x, double mouseX, double mouseY) {
		return inside(mouseX, mouseY, x, ACTION_Y, ACTION_SIZE, ACTION_SIZE);
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	static boolean isInfoHovered(double mouseX, double mouseY) { return isHovered(INFO_X, mouseX, mouseY); }
	static void heading(ITooltipBuilder tooltip, String text, ChatFormatting color) { tooltip.add(Component.literal(text).withStyle(color, ChatFormatting.BOLD)); }

	static void line(ITooltipBuilder tooltip, String text, ChatFormatting color) {
		if (text == null || text.isBlank()) return;
		for (String wrapped : wrap(text, 38)) tooltip.add(Component.literal(wrapped).withStyle(color));
	}

	static void ingredientLine(ITooltipBuilder tooltip, String name, int count, ChatFormatting color) { line(tooltip, "• " + count + "x " + name, color); }
	static void blank(ITooltipBuilder tooltip) { tooltip.add(Component.literal(" ")); }

	static List<String> wrap(String text, int limit) {
		List<String> result = new ArrayList<>();
		if (text == null) return result;
		for (String explicitLine : text.replace("%nl%", "\n").replace("/n", "\n").split("\\R", -1)) {
			String trimmed = explicitLine.trim();
			if (trimmed.isEmpty()) {
				result.add(" ");
				continue;
			}
			StringBuilder current = new StringBuilder();
			for (String word : trimmed.split("\\s+")) {
				if (current.isEmpty()) current.append(word);
				else if (current.length() + 1 + word.length() <= limit) current.append(' ').append(word);
				else {
					result.add(current.toString());
					current.setLength(0);
					current.append(word);
				}
			}
			if (!current.isEmpty()) result.add(current.toString());
		}
		return result;
	}

	static boolean isCompactAmount(int amount) { return amount > 950; }
	static String compactAmount(int amount) {
		if (amount > 950_000) return compactDecimal(Math.max(1D, amount / 1_000_000D)) + "M";
		if (amount > 950) return compactDecimal(amount / 1_000D) + "K";
		return Integer.toString(amount);
	}

	static String exactAmount(int amount) { return NumberFormat.getIntegerInstance(Locale.US).format(amount); }

	static String fit(String text, int maxWidth) {
		if (text == null || text.isBlank()) return "";
		var font = Minecraft.getInstance().font;
		if (font.width(text) <= maxWidth) return text;
		String ellipsis = "...";
		int allowed = Math.max(0, maxWidth - font.width(ellipsis));
		String value = text;
		while (!value.isEmpty() && font.width(value) > allowed) value = value.substring(0, value.length() - 1);
		return value.stripTrailing() + ellipsis;
	}

	private static String compactDecimal(double value) {
		String format = value >= 10D ? String.format(Locale.US, "%.0f", value) : String.format(Locale.US, "%.1f", value);
		while (format.contains(".") && (format.endsWith("0") || format.endsWith("."))) format = format.substring(0, format.length() - 1);
		return format;
	}

	static String ingredientName(IngredientDefinition ingredient) {
		if (ingredient.label() != null && !ingredient.label().isBlank()) return ingredient.label();
		return itemName(ingredient.id());
	}

	static String itemName(String id) {
		ItemDefinition definition = AddonData.get().item(id);
		if (definition != null) return definition.name();
		String path = id != null && id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		if (path == null || path.isBlank()) return "Unknown Item";
		String value = path.replace('_', ' ');
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	static String sanitize(String id) { return id.toLowerCase().replaceAll("[^a-z0-9/._-]", "_"); }

	private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) return;
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y + 1, x + 1, y + height - 1, color);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}
}
