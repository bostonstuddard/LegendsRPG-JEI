package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.BundledConfigDefaults;
import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.update.UpdateManager;
import com.legendsrpg.jei.update.UpdaterConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class LegendsSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 390;
	private static final int PANEL_HEIGHT = 366;
	private final Screen parent;
	private Button viewerButton;
	private Button rawButton;
	private Button jeiButtonsButton;
	private Button defaultPageButton;
	private Button sideButton;
	private Button abbreviateButton;
	private Button themeButton;
	private Button clearPinnedButton;
	private Button resetFavoritesButton;
	private Button updateChannelButton;
	private ScaleSlider scaleSlider;
	private Component statusMessage = Component.empty();
	private int statusColor = 0xff8fa3b5;

	public LegendsSettingsScreen(Screen parent) {
		super(Component.literal("LegendsRPG + JEI Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int columnWidth = (panelWidth - 40) / 2;

		viewerButton = this.addRenderableWidget(Button.builder(viewerText(), button -> {
			JeiRuntimeController.toggle();
			refreshButtons();
		}).bounds(left + 16, top + 58, columnWidth, 20).build());

		rawButton = this.addRenderableWidget(Button.builder(rawText(), button -> {
			JeiRuntimeController.toggleRawRecipes();
			refreshButtons();
		}).bounds(left + 24 + columnWidth, top + 58, columnWidth, 20).build());

		jeiButtonsButton = this.addRenderableWidget(Button.builder(jeiButtonsText(), button -> {
			LegendsConfig.get().toggleShowJeiButtons();
			refreshButtons();
			statusMessage = Component.literal("JEI overlay buttons updated.");
			statusColor = 0xff79d279;
		}).bounds(left + 16, top + 86, columnWidth, 20).build());

		defaultPageButton = this.addRenderableWidget(Button.builder(defaultPageText(), button -> {
			LegendsConfig.get().cycleDefaultRecipePage();
			refreshButtons();
		}).bounds(left + 24 + columnWidth, top + 86, columnWidth, 20).build());

		sideButton = this.addRenderableWidget(Button.builder(sideText(), button -> {
			LegendsConfig.get().togglePinnedHudSide();
			refreshButtons();
		}).bounds(left + 16, top + 114, columnWidth, 20).build());

		abbreviateButton = this.addRenderableWidget(Button.builder(abbreviateText(), button -> {
			LegendsConfig.get().toggleAbbreviatePinnedAmounts();
			refreshButtons();
		}).bounds(left + 24 + columnWidth, top + 114, columnWidth, 20).build());

		themeButton = this.addRenderableWidget(Button.builder(themeText(), button -> {
			LegendsConfig.get().cycleTheme();
			refreshButtons();
			statusMessage = Component.literal("Theme applied to JEI and the recipe viewer.");
			statusColor = 0xffffcf63;
		}).bounds(left + 16, top + 142, columnWidth, 20).build());

		this.addRenderableWidget(Button.builder(
			Component.literal("JEI Layout...").withStyle(ChatFormatting.AQUA),
			button -> this.minecraft.setScreen(new JeiLayoutSettingsScreen(this))
		).bounds(left + 24 + columnWidth, top + 142, columnWidth, 20).build());

		scaleSlider = this.addRenderableWidget(new ScaleSlider(left + 16, top + 174, panelWidth - 32, 20));

		this.addRenderableWidget(Button.builder(
			Component.literal("Recipe Tree").withStyle(ChatFormatting.AQUA),
			button -> RecipeActionManager.openFavoriteTree(null)
		).bounds(left + 16, top + 206, columnWidth, 20).build());

		clearPinnedButton = this.addRenderableWidget(Button.builder(pinnedText(), button -> {
			RecipeActionManager.clearPinned();
			refreshButtons();
			statusMessage = Component.literal("Pinned recipe cleared.");
			statusColor = 0xff79d279;
		}).bounds(left + 24 + columnWidth, top + 206, columnWidth, 20).build());

		resetFavoritesButton = this.addRenderableWidget(Button.builder(favoritesText(), button -> confirmResetFavorites())
			.bounds(left + 16, top + 234, columnWidth, 20).build());

		this.addRenderableWidget(Button.builder(
			Component.literal("Restore Defaults...").withStyle(ChatFormatting.YELLOW),
			button -> confirmRestoreDefaults()
		).bounds(left + 24 + columnWidth, top + 234, columnWidth, 20).build());

		updateChannelButton = this.addRenderableWidget(Button.builder(updateChannelText(), button -> {
			UpdaterConfig.get().toggleChannel();
			UpdateManager.checkForUpdates();
			refreshButtons();
			statusMessage = Component.literal("Updater channel changed to " + UpdaterConfig.get().channelDisplayName() + ".");
			statusColor = 0xff79d279;
		}).bounds(left + 16, top + 266, panelWidth - 32, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(left + panelWidth / 2 - 55, top + 314, 110, 20).build());

		refreshButtons();
	}

	@Override
	public void tick() {
		super.tick();
		refreshActionStates();
	}

	private void refreshButtons() {
		viewerButton.setMessage(viewerText());
		rawButton.setMessage(rawText());
		jeiButtonsButton.setMessage(jeiButtonsText());
		defaultPageButton.setMessage(defaultPageText());
		sideButton.setMessage(sideText());
		abbreviateButton.setMessage(abbreviateText());
		themeButton.setMessage(themeText());
		if (updateChannelButton != null) updateChannelButton.setMessage(updateChannelText());
		refreshActionStates();
		if (scaleSlider != null) scaleSlider.syncFromConfig();
	}

	private void refreshActionStates() {
		if (clearPinnedButton != null) {
			clearPinnedButton.setMessage(pinnedText());
			clearPinnedButton.active = LegendsConfig.get().pinnedRecipe() != null;
		}
		if (resetFavoritesButton != null) {
			resetFavoritesButton.setMessage(favoritesText());
			resetFavoritesButton.active = LegendsConfig.get().favoriteCount() > 0;
		}
	}

	private void confirmRestoreDefaults() {
		if (this.minecraft == null) return;
		this.minecraft.setScreen(new LegendsConfirmScreen(
			this,
			Component.literal("Restore LegendsRPG + JEI Defaults?"),
			Component.literal("This replaces your viewer and JEI layout settings."),
			this::restoreDefaults
		));
	}

	private void confirmResetFavorites() {
		if (this.minecraft == null) return;
		this.minecraft.setScreen(new LegendsConfirmScreen(
			this,
			Component.literal("Reset All Favorite Recipes?"),
			Component.literal("This removes all " + LegendsConfig.get().favoriteCount() + " saved favorites."),
			() -> {
				LegendsConfig.get().clearFavorites();
				statusMessage = Component.literal("All recipe favorites cleared.");
				statusColor = 0xff79d279;
				refreshActionStates();
			}
		));
	}

	private void restoreDefaults() {
		BundledConfigDefaults.RestoreResult result = BundledConfigDefaults.restoreAll();
		LegendsConfig.reload();
		JeiRuntimeController.apply();
		if (result.successful()) {
			statusMessage = Component.literal("Defaults restored — restart to fully reload JEI.");
			statusColor = 0xff79d279;
		} else {
			statusMessage = Component.literal("Restore failed; check the game log.");
			statusColor = 0xffff6b6b;
		}
		refreshButtons();
	}

	private Component viewerText() {
		return LegendsConfig.get().enabled()
			? Component.literal("Viewer: ON").withStyle(ChatFormatting.GREEN)
			: Component.literal("Viewer: OFF").withStyle(ChatFormatting.RED);
	}

	private Component rawText() {
		return LegendsConfig.get().rawRecipesEnabled()
			? Component.literal("Raw Pages: ON").withStyle(ChatFormatting.GREEN)
			: Component.literal("Raw Pages: OFF").withStyle(ChatFormatting.RED);
	}

	private Component jeiButtonsText() {
		return LegendsConfig.get().showJeiButtons()
			? Component.literal("JEI Overlay Buttons: ON").withStyle(ChatFormatting.GREEN)
			: Component.literal("JEI Overlay Buttons: OFF").withStyle(ChatFormatting.RED);
	}

	private Component defaultPageText() {
		return Component.literal("Default Page: " + LegendsConfig.get().defaultRecipePageDisplayName()).withStyle(ChatFormatting.AQUA);
	}

	private Component sideText() {
		return LegendsConfig.get().pinnedHudOnLeft()
			? Component.literal("Pinned HUD: LEFT").withStyle(ChatFormatting.AQUA)
			: Component.literal("Pinned HUD: RIGHT").withStyle(ChatFormatting.AQUA);
	}

	private Component abbreviateText() {
		return LegendsConfig.get().abbreviatePinnedAmounts()
			? Component.literal("Pinned Amounts: K/M").withStyle(ChatFormatting.LIGHT_PURPLE)
			: Component.literal("Pinned Amounts: Exact").withStyle(ChatFormatting.LIGHT_PURPLE);
	}

	private Component themeText() {
		return Component.literal("Theme: " + LegendsConfig.get().themeDisplayName()).withStyle(ChatFormatting.GOLD);
	}

	private Component updateChannelText() {
		return Component.literal("Update Channel: " + UpdaterConfig.get().channelDisplayName()).withStyle(
			UpdaterConfig.CHANNEL_DEV.equals(UpdaterConfig.get().channel()) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN
		);
	}

	private Component pinnedText() {
		return LegendsConfig.get().pinnedRecipe() == null
			? Component.literal("Pinned Recipe: None").withStyle(ChatFormatting.GRAY)
			: Component.literal("Clear Pinned Recipe").withStyle(ChatFormatting.AQUA);
	}

	private Component favoritesText() {
		int count = LegendsConfig.get().favoriteCount();
		return count == 0
			? Component.literal("Favorites: None").withStyle(ChatFormatting.GRAY)
			: Component.literal("Reset Favorites (" + count + ")").withStyle(ChatFormatting.RED);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int right = left + panelWidth;
		int bottom = top + PANEL_HEIGHT;
		ThemePalette palette = ThemePalette.current();

		graphics.fillGradient(0, 0, this.width, this.height, palette.screenTop(), palette.screenBottom());
		graphics.fill(left, top, right, bottom, palette.panel());
		graphics.renderOutline(left, top, panelWidth, PANEL_HEIGHT, palette.outline());
		graphics.fill(left, top, right, top + 3, palette.stripe());
		graphics.fill(left + 12, top + 48, right - 12, top + 49, palette.divider());
		graphics.fill(left + 12, top + 166, right - 12, top + 167, palette.divider());
		graphics.fill(left + 12, top + 198, right - 12, top + 199, palette.divider());
		graphics.fill(left + 12, top + 258, right - 12, top + 259, palette.divider());
		graphics.fill(left + 12, top + 294, right - 12, top + 295, palette.divider());

		super.render(graphics, mouseX, mouseY, delta);

		graphics.drawCenteredString(this.font, Component.literal("LegendsRPG + JEI").withStyle(ChatFormatting.GOLD), this.width / 2, top + 12, palette.secondary());
		graphics.drawCenteredString(this.font, Component.literal("Fast, focused recipe-viewer settings"), this.width / 2, top + 27, palette.text());
		graphics.drawCenteredString(this.font, Component.literal("Open with K, Mod Menu, or JEI's config button."), this.width / 2, top + 38, palette.muted());

		if (!statusMessage.getString().isBlank()) {
			graphics.drawCenteredString(this.font, statusMessage, this.width / 2, top + 298, statusColor);
		} else {
			graphics.drawCenteredString(this.font, Component.literal("Favorites saved: " + LegendsConfig.get().favoriteCount()), this.width / 2, top + 298, palette.muted());
		}

		graphics.drawCenteredString(
			this.font,
			Component.literal("Hold " + ClientInputState.exactPinnedAmountsKeyName() + " to invert pinned amount formatting."),
			this.width / 2,
			bottom - 10,
			palette.secondary()
		);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}

	private final class ScaleSlider extends AbstractSliderButton {
		private ScaleSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), sliderValueFor(LegendsConfig.get().pinnedHudScale()));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(String.format(Locale.US, "HUD Scale: %.1fx", configValueFor(this.value))));
		}

		@Override
		protected void applyValue() {
			LegendsConfig.get().setPinnedHudScale(configValueFor(this.value));
			updateMessage();
		}

		private static double configValueFor(double slider) {
			return 0.1D + (slider * 1.9D);
		}

		private static double sliderValueFor(double config) {
			return (Math.max(0.1D, Math.min(2.0D, config)) - 0.1D) / 1.9D;
		}

		private void syncFromConfig() {
			this.value = sliderValueFor(LegendsConfig.get().pinnedHudScale());
			updateMessage();
		}
	}
}
