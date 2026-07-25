package com.legendsrpg.jei.mixin.jei;

import com.legendsrpg.jei.client.LegendsSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.gui.overlay.ConfigButtonController", remap = false)
public abstract class JeiConfigButtonControllerMixin {
	@Inject(method = "openSettings", at = @At("HEAD"), cancellable = true, remap = false)
	private static void legendsrpg$openImprovedSettings(CallbackInfo callback) {
		Minecraft minecraft = Minecraft.getInstance();
		Screen parent = minecraft.screen;
		if (parent instanceof AbstractContainerScreen<?> && minecraft.player != null) {
			minecraft.player.closeContainer();
			parent = null;
		}
		Screen finalParent = parent;
		minecraft.execute(() -> minecraft.setScreen(new LegendsSettingsScreen(finalParent)));
		callback.cancel();
	}
}
