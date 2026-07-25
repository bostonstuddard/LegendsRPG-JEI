package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;

public record ThemePalette(
	int screenTop,
	int screenBottom,
	int panel,
	int header,
	int inner,
	int outline,
	int innerOutline,
	int divider,
	int stripe,
	int text,
	int secondary,
	int muted,
	int slot,
	int slotOutline,
	int search,
	int scrollbar,
	int scrollbarMarker
) {
	private static final ThemePalette CRIMSON = new ThemePalette(
		0xe6090207, 0xf0020002, 0xf2220613, 0xff310817, 0xff140910,
		0xff9f335a, 0xff5a2740, 0xff4c1930, 0xffd45582,
		0xfff5e7ee, 0xffd7a9bc, 0xffbd879d,
		0xff24111a, 0xff81425c, 0xff0c0509, 0xff2d101d, 0xffbd4f75
	);

	private static final ThemePalette SLATE = new ThemePalette(
		0xe6070c13, 0xf0020509, 0xf20d1722, 0xff111a24, 0xff0d1720,
		0xff31506a, 0xff1d3140, 0xff2d4050, 0xff55c9ff,
		0xffd7e3ec, 0xff9db6ca, 0xff8197a8,
		0xff172738, 0xff3a5367, 0xff05090d, 0xff172430, 0xff4c7c9b
	);

	private static final ThemePalette DARK = new ThemePalette(
		0xf0060606, 0xf0000000, 0xf2111111, 0xff1a1a1a, 0xff0b0b0b,
		0xff555555, 0xff303030, 0xff3a3a3a, 0xffa8a8a8,
		0xffededed, 0xffbdbdbd, 0xff8f8f8f,
		0xff181818, 0xff505050, 0xff050505, 0xff222222, 0xff8a8a8a
	);

	private static final ThemePalette MINECRAFT = new ThemePalette(
		0xe6181818, 0xf0050505, 0xf23b3b3b, 0xff555555, 0xff242424,
		0xffbdbdbd, 0xff666666, 0xff777777, 0xffffffff,
		0xffffffff, 0xffffd75e, 0xffb7b7b7,
		0xff2b2b2b, 0xff8b8b8b, 0xff111111, 0xff4a4a4a, 0xffc6c6c6
	);

	public static ThemePalette current() {
		return switch (LegendsConfig.get().theme()) {
			case LegendsConfig.THEME_SLATE -> SLATE;
			case LegendsConfig.THEME_DARK -> DARK;
			case LegendsConfig.THEME_MINECRAFT -> MINECRAFT;
			default -> CRIMSON;
		};
	}
}
