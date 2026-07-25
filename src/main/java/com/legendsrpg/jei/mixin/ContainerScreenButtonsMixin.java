package com.legendsrpg.jei.mixin;

import com.legendsrpg.jei.client.PinnedRecipeScreenButton;
import com.legendsrpg.jei.client.RecipeActionManager;
import com.legendsrpg.jei.config.LegendsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenButtonsMixin extends Screen {
	@Unique private Button legendsrpg$treeButton;
	@Unique private Button legendsrpg$clearButton;

	protected ContainerScreenButtonsMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void legendsrpg$addRecipeButtons(CallbackInfo callback) {
		legendsrpg$treeButton = this.addRenderableWidget(Button.builder(Component.empty(), button ->
			RecipeActionManager.openFavoriteTree(null)
		).tooltip(Tooltip.create(Component.literal(PinnedRecipeScreenButton.TREE_TOOLTIP)))
			.bounds(PinnedRecipeScreenButton.treeX(), PinnedRecipeScreenButton.buttonY(this.height), PinnedRecipeScreenButton.SIZE, PinnedRecipeScreenButton.SIZE)
			.build());

		legendsrpg$clearButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			if (LegendsConfig.get().pinnedRecipe() != null) RecipeActionManager.clearPinned();
		}).tooltip(Tooltip.create(Component.literal(PinnedRecipeScreenButton.CLEAR_TOOLTIP)))
			.bounds(PinnedRecipeScreenButton.clearX(), PinnedRecipeScreenButton.buttonY(this.height), PinnedRecipeScreenButton.SIZE, PinnedRecipeScreenButton.SIZE)
			.build());
		legendsrpg$syncRecipeButtons();
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void legendsrpg$renderRecipeButtons(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo callback) {
		legendsrpg$syncRecipeButtons();
		PinnedRecipeScreenButton.render(graphics, mouseX, mouseY, this.width, this.height);
	}

	@Unique
	private void legendsrpg$syncRecipeButtons() {
		if (legendsrpg$treeButton == null || legendsrpg$clearButton == null) return;
		int y = PinnedRecipeScreenButton.buttonY(this.height);
		legendsrpg$treeButton.setX(PinnedRecipeScreenButton.treeX());
		legendsrpg$treeButton.setY(y);
		legendsrpg$treeButton.active = true;
		legendsrpg$clearButton.setX(PinnedRecipeScreenButton.clearX());
		legendsrpg$clearButton.setY(y);
		legendsrpg$clearButton.active = LegendsConfig.get().pinnedRecipe() != null;
	}
}
