package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PinnedRecipeHud {
	private static final int WIDTH = 158;
	private static final int HEADER_HEIGHT = 39;
	private static final int MATERIAL_ROW_HEIGHT = 20;
	private static final int NOTE_ROW_HEIGHT = 11;
	private static final int SCREEN_MARGIN = 8;
	private static final double MIN_AUTOMATIC_SCALE = 0.15D;
	private static RecipeSnapshot cachedRecipeReference;
	private static CachedRecipe cachedRecipe;

	private PinnedRecipeHud() {}

	public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) return;

		LegendsConfig config = LegendsConfig.get();
		RecipeSnapshot recipe = config.pinnedRecipe();
		if (recipe == null) return;

		Font font = client.font;
		CachedRecipe cached = cached(recipe);
		boolean hasMaterials = !cached.ingredients().isEmpty();
		int rowCount = hasMaterials ? cached.ingredients().size() : cached.notes().size();
		if (rowCount == 0) rowCount = 1;

		int rowHeight = hasMaterials ? MATERIAL_ROW_HEIGHT : NOTE_ROW_HEIGHT;
		int height = HEADER_HEIGHT + rowCount * rowHeight + 8;
		double selectedScale = config.pinnedHudScale();
		double heightScale = (graphics.guiHeight() - SCREEN_MARGIN * 2D) / height;
		double widthScale = (graphics.guiWidth() - SCREEN_MARGIN * 2D) / WIDTH;
		double scale = Math.min(selectedScale, Math.min(heightScale, widthScale));
		scale = Math.max(MIN_AUTOMATIC_SCALE, scale);

		int scaledWidth = (int) Math.ceil(WIDTH * scale);
		int x = config.pinnedHudOnLeft()
			? SCREEN_MARGIN
			: Math.max(SCREEN_MARGIN, graphics.guiWidth() - scaledWidth - SCREEN_MARGIN);
		int y = SCREEN_MARGIN;

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) x, (float) y);
		graphics.pose().scale((float) scale, (float) scale);

		drawFrame(graphics, height);
		HeaderText header = cached.header();
		graphics.drawString(font, fit(header.source(), WIDTH - 37, font), 8, 7, ThemePalette.current().secondary(), false);
		graphics.drawString(font, fit(header.itemName(), WIDTH - 37, font), 8, 18, ThemePalette.current().text(), true);

		if (cached.output() != null) {
			graphics.renderItem(cached.output(), WIDTH - 24, 10);
			if (cached.outputCount() > 1) {
				drawItemAmount(graphics, font, cached.outputCount(), WIDTH - 24, 10);
			}
		}

		if (hasMaterials) {
			for (int index = 0; index < cached.ingredients().size(); index++) {
				IngredientRow ingredient = cached.ingredients().get(index);
				int rowY = HEADER_HEIGHT + index * MATERIAL_ROW_HEIGHT;
				graphics.renderItem(ingredient.stack(), 8, rowY);

				String name = ingredient.name();
				String fullLine = ingredient.count() == 1
					? name
					: formattedAmount(ingredient.count()) + " × " + name;
				String visibleLine = fit(fullLine, WIDTH - 38, font);
				int textLeft = 29;
				int available = WIDTH - textLeft - 7;
				int centeredX = textLeft + Math.max(0, (available - font.width(visibleLine)) / 2);
				graphics.drawString(font, visibleLine, centeredX, rowY + 4, ThemePalette.current().text(), false);
			}
		} else if (!cached.notes().isEmpty()) {
			for (int index = 0; index < cached.notes().size(); index++) {
				String note = cached.notes().get(index);
				int rowY = HEADER_HEIGHT + 1 + index * NOTE_ROW_HEIGHT;
				graphics.drawString(font, "• " + fit(note, WIDTH - 20, font), 9, rowY, ThemePalette.current().secondary(), false);
			}
		} else {
			graphics.drawString(font, "No pinned details.", 9, HEADER_HEIGHT + 2, ThemePalette.current().muted(), false);
		}

		graphics.pose().popMatrix();
	}

	private static CachedRecipe cached(RecipeSnapshot recipe) {
		if (cachedRecipeReference == recipe && cachedRecipe != null) return cachedRecipe;
		List<IngredientRow> ingredients = new ArrayList<>();
		for (IngredientDefinition ingredient : recipe.ingredients()) {
			ingredients.add(new IngredientRow(ItemStackFactory.create(ingredient.id(), 1), displayName(ingredient), ingredient.count()));
		}
		ItemStack output = recipe.output() == null ? null : hudVisual(ItemStackFactory.create(recipe.output().id(), 1));
		cachedRecipeReference = recipe;
		cachedRecipe = new CachedRecipe(
			splitTitle(recipe.title()),
			output,
			recipe.output() == null ? 0 : recipe.output().count(),
			List.copyOf(ingredients),
			List.copyOf(recipe.notes())
		);
		return cachedRecipe;
	}

	private static ItemStack hudVisual(ItemStack stack) {
		if (stack == null) return null;
		ItemStack visual = stack.copy();
		visual.setCount(1);
		visual.remove(DataComponents.DAMAGE);
		visual.remove(DataComponents.MAX_DAMAGE);
		return visual;
	}

	private static HeaderText splitTitle(String title) {
		if (title == null || title.isBlank()) return new HeaderText("Pinned Recipe:", "Unknown Item");
		int colon = title.indexOf(':');
		if (colon > 0 && colon < title.length() - 1) {
			String source = title.substring(0, colon).trim() + ":";
			String itemName = title.substring(colon + 1).trim();
			return new HeaderText(source, itemName);
		}
		return new HeaderText("Pinned Recipe:", title.trim());
	}

	private static void drawFrame(GuiGraphics graphics, int height) {
		ThemePalette palette = ThemePalette.current();
		graphics.fill(0, 0, WIDTH, height, palette.panel());
		graphics.fill(2, 2, WIDTH - 2, HEADER_HEIGHT - 5, palette.header());
		graphics.fill(6, HEADER_HEIGHT - 2, WIDTH - 6, height - 6, palette.inner());
		graphics.fill(0, 0, WIDTH, 3, palette.stripe());
		graphics.fill(6, HEADER_HEIGHT - 4, WIDTH - 6, HEADER_HEIGHT - 3, palette.divider());
		drawOutline(graphics, 0, 0, WIDTH, height, palette.outline());
		drawOutline(graphics, 6, HEADER_HEIGHT - 2, WIDTH - 12, height - HEADER_HEIGHT - 4, palette.innerOutline());
	}

	private static void drawItemAmount(GuiGraphics graphics, Font font, int amount, int x, int y) {
		String text = formattedAmount(amount);
		int drawX = Math.max(x + 1, x + 17 - font.width(text));
		graphics.drawString(font, text, drawX, y + 10, 0xffffffff, true);
	}

	private static String formattedAmount(int amount) {
		LegendsConfig config = LegendsConfig.get();
		boolean abbreviate = config.abbreviatePinnedAmounts();
		if (ClientInputState.isPinnedAmountInvertHeld()) abbreviate = !abbreviate;
		return abbreviate ? compact(amount) : NumberFormat.getIntegerInstance(Locale.US).format(amount);
	}

	private static String displayName(IngredientDefinition ingredient) {
		if (ingredient.label() != null && !ingredient.label().isBlank()) return ingredient.label();
		ItemDefinition item = AddonData.get().item(ingredient.id());
		if (item != null) return item.name();
		String path = ingredient.id().contains(":") ? ingredient.id().substring(ingredient.id().indexOf(':') + 1) : ingredient.id();
		String name = path.replace('_', ' ');
		return name.isBlank() ? ingredient.id() : Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static String compact(int amount) {
		if (amount > 950_000) return decimal(amount / 1_000_000D) + "M";
		if (amount > 950) return decimal(amount / 1_000D) + "K";
		return NumberFormat.getIntegerInstance(Locale.US).format(amount);
	}

	private static String decimal(double value) {
		String result = value >= 10 ? String.format(Locale.US, "%.0f", value) : String.format(Locale.US, "%.1f", value);
		while (result.contains(".") && (result.endsWith("0") || result.endsWith("."))) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	private static String fit(String text, int width, Font font) {
		if (text == null) return "";
		if (font.width(text) <= width) return text;
		String suffix = "...";
		int allowed = Math.max(0, width - font.width(suffix));
		String value = text;
		while (!value.isEmpty() && font.width(value) > allowed) value = value.substring(0, value.length() - 1);
		return value.stripTrailing() + suffix;
	}

	private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) return;
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y + 1, x + 1, y + height - 1, color);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}

	private record IngredientRow(ItemStack stack, String name, int count) {}
	private record CachedRecipe(HeaderText header, ItemStack output, int outputCount, List<IngredientRow> ingredients, List<String> notes) {}
	private record HeaderText(String source, String itemName) {}
}
