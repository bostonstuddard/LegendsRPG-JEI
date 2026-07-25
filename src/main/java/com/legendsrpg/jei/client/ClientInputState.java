package com.legendsrpg.jei.client;

import com.legendsrpg.jei.LegendsRpgJeiClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ClientInputState {
	private static KeyMapping settingsKey;
	private static KeyMapping exactPinnedAmountsKey;

	private ClientInputState() {}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(LegendsRpgJeiClient.MOD_ID, "settings"));
		settingsKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.legendsrpg_jei.settings", GLFW.GLFW_KEY_K, category)
		);
		exactPinnedAmountsKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.legendsrpg_jei.toggle_exact_pinned", GLFW.GLFW_KEY_LEFT_ALT, category)
		);
	}

	public static void handleEndTick(net.minecraft.client.Minecraft client) {
		while (settingsKey != null && settingsKey.consumeClick()) {
			client.setScreen(new LegendsSettingsScreen(client.screen));
		}
	}

	public static boolean isShiftDown() {
		return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
	}

	public static boolean isPinnedAmountInvertHeld() {
		return exactPinnedAmountsKey != null && exactPinnedAmountsKey.isDown();
	}

	public static String exactPinnedAmountsKeyName() {
		return exactPinnedAmountsKey != null ? exactPinnedAmountsKey.getTranslatedKeyMessage().getString() : "Unbound";
	}
}
