package com.legendsrpg.jei.client;

import com.legendsrpg.jei.LegendsRpgJeiClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.Map;

public final class LegendsModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return LegendsSettingsScreen::new;
	}

	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		return Map.of(
			LegendsRpgJeiClient.MOD_ID, LegendsSettingsScreen::new,
			"jei", LegendsSettingsScreen::new
		);
	}
}
