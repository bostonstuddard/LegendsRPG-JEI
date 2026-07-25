package com.legendsrpg.jei;

import com.legendsrpg.jei.client.ClientInputState;
import com.legendsrpg.jei.client.PinnedRecipeHud;
import com.legendsrpg.jei.client.PinnedRecipeScreenButton;
import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.update.UpdateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LegendsRpgJeiClient implements ClientModInitializer {
	public static final String MOD_ID = "legendsrpg_jei";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		AddonData data = AddonData.get();
		LOGGER.info(
			"Loaded LegendsRPG JEI data: {} items, {} crafts, {} shops, {} smithing recipes, {} Ore Forge recipes, {} bestiary drops, {} acquisition sources, {} item pages",
			data.items().size(),
			data.craftingRecipes().size(),
			data.shopRecipes().size(),
			data.forgeRecipes().size(),
			data.oreForgeRecipes().size(),
			data.dropRecipes().size(),
			data.sourceRecipes().size(),
			data.itemInfoRecipes().size()
		);

		UpdateManager.init();
		ClientInputState.register();
		HudRenderCallback.EVENT.register(PinnedRecipeHud::render);
		PinnedRecipeScreenButton.register();
		ClientTickEvents.END_CLIENT_TICK.register(ClientInputState::handleEndTick);
	}
}
