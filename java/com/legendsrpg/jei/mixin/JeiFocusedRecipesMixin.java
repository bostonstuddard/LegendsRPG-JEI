package com.legendsrpg.jei.mixin;

import com.legendsrpg.jei.client.FavoriteRecipeIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "mezz.jei.gui.recipes.lookups.FocusedRecipes", remap = false)
public abstract class JeiFocusedRecipesMixin {
	@Inject(method = "getRecipes", at = @At("RETURN"), cancellable = true, remap = false)
	private void legendsrpg$prioritizeFavoriteRecipes(CallbackInfoReturnable<List<?>> callback) {
		List<?> recipes = callback.getReturnValue();
		List<?> prioritized = FavoriteRecipeIndex.prioritize(recipes);
		if (prioritized != recipes) callback.setReturnValue(prioritized);
	}
}
