package com.legendsrpg.jei.mixin;

import com.legendsrpg.jei.client.JeiButtonStyle;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.gui.elements.ButtonSprites", remap = false)
public abstract class JeiButtonSpritesMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
	private void legendsrpg$drawThemedButton(
		GuiGraphics graphics,
		boolean active,
		boolean highlighted,
		boolean pressed,
		int x,
		int y,
		int width,
		int height,
		int alpha,
		CallbackInfo callback
	) {
		JeiButtonStyle.draw(graphics, active, highlighted, pressed, x, y, width, height);
		callback.cancel();
	}
}
