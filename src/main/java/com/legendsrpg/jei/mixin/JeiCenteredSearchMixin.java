package com.legendsrpg.jei.mixin;

import com.legendsrpg.jei.config.LegendsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlayLayout$Layout", remap = false)
public abstract class JeiCenteredSearchMixin {
	@ModifyArg(
		method = "getSearchAndConfigAreas",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/util/ImmutableRect2i;cropRight(I)Lmezz/jei/common/util/ImmutableRect2i;"
		),
		index = 0,
		remap = false,
		require = 0
	)
	private int legendsrpg$restoreHiddenConfigSpace(int amount) {
		return LegendsConfig.get().showJeiButtons() ? amount : 0;
	}
}
