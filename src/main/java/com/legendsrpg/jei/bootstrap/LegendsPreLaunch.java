package com.legendsrpg.jei.bootstrap;

import com.legendsrpg.jei.config.BundledConfigDefaults;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Writes the bundled LegendsRPG JEI defaults before normal mod entrypoints run.
 * Existing player configuration is preserved except for one-time versioned migrations.
 */
public final class LegendsPreLaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		BundledConfigDefaults.installMissingDefaults();
	}
}
