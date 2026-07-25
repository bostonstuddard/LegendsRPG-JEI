package com.legendsrpg.jei.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ComponentParser {
	private ComponentParser() {}

	public static Component parse(String json, String fallback) {
		if (json == null || json.isBlank()) {
			return Component.literal(fallback == null ? "" : fallback);
		}
		try {
			return parseElement(JsonParser.parseString(json));
		} catch (RuntimeException exception) {
			return Component.literal(fallback == null ? json : fallback);
		}
	}

	private static MutableComponent parseElement(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return Component.empty();
		}
		if (element.isJsonPrimitive()) {
			return Component.literal(element.getAsString());
		}
		if (element.isJsonArray()) {
			MutableComponent result = Component.empty();
			for (JsonElement child : element.getAsJsonArray()) {
				result.append(parseElement(child));
			}
			return result;
		}

		JsonObject object = element.getAsJsonObject();
		MutableComponent component = Component.literal(object.has("text") ? object.get("text").getAsString() : "");
		component.setStyle(parseStyle(object));
		JsonArray extra = object.has("extra") && object.get("extra").isJsonArray() ? object.getAsJsonArray("extra") : null;
		if (extra != null) {
			for (JsonElement child : extra) {
				component.append(parseElement(child));
			}
		}
		return component;
	}

	private static Style parseStyle(JsonObject object) {
		Style style = Style.EMPTY;
		if (object.has("color") && object.get("color").isJsonPrimitive()) {
			String colorName = object.get("color").getAsString();
			if (colorName.startsWith("#") && colorName.length() == 7) {
				try {
					style = style.withColor(TextColor.fromRgb(Integer.parseInt(colorName.substring(1), 16)));
				} catch (NumberFormatException ignored) {
					// Keep default color.
				}
			} else {
				ChatFormatting color = ChatFormatting.getByName(colorName);
				if (color != null && color.isColor()) {
					style = style.withColor(color);
				}
			}
		}
		if (object.has("bold")) {
			style = style.withBold(object.get("bold").getAsBoolean());
		}
		if (object.has("italic")) {
			style = style.withItalic(object.get("italic").getAsBoolean());
		}
		if (object.has("underlined")) {
			style = style.withUnderlined(object.get("underlined").getAsBoolean());
		}
		if (object.has("strikethrough")) {
			style = style.withStrikethrough(object.get("strikethrough").getAsBoolean());
		}
		if (object.has("obfuscated")) {
			style = style.withObfuscated(object.get("obfuscated").getAsBoolean());
		}
		return style;
	}
}
