package com.legendsrpg.jei.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LegendsConfirmScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 136;
	private static final int BUTTON_WIDTH = 104;
	private static final int BUTTON_GAP = 10;

	private final Screen parent;
	private final Component message;
	private final Runnable onConfirm;

	public LegendsConfirmScreen(Screen parent, Component title, Component message, Runnable onConfirm) {
		super(title);
		this.parent = parent;
		this.message = message;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init() {
		int panelHeight = Math.min(PANEL_HEIGHT, this.height - 24);
		int top = (this.height - panelHeight) / 2;
		int groupWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
		int left = (this.width - groupWidth) / 2;
		int y = top + panelHeight - 31;
		this.addRenderableWidget(Button.builder(Component.literal("Confirm"), button -> confirm())
			.bounds(left, y, BUTTON_WIDTH, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
			.bounds(left + BUTTON_WIDTH + BUTTON_GAP, y, BUTTON_WIDTH, 20).build());
	}

	private void confirm() {
		onConfirm.run();
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		ThemePalette palette = ThemePalette.current();
		graphics.fillGradient(0, 0, this.width, this.height, palette.screenTop(), palette.screenBottom());
		int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		int panelHeight = Math.min(PANEL_HEIGHT, this.height - 24);
		int left = (this.width - panelWidth) / 2;
		int top = (this.height - panelHeight) / 2;
		graphics.fill(left, top, left + panelWidth, top + panelHeight, palette.panel());
		graphics.renderOutline(left, top, panelWidth, panelHeight, palette.outline());
		graphics.fill(left, top, left + panelWidth, top + 3, palette.stripe());
		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 18, palette.secondary());
		graphics.drawCenteredString(this.font, message, this.width / 2, top + 53, palette.text());
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}
}
