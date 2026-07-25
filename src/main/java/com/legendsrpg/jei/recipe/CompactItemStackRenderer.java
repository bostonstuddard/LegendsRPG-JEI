package com.legendsrpg.jei.recipe;

import com.legendsrpg.jei.client.ClientInputState;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public final class CompactItemStackRenderer implements IIngredientRenderer<ItemStack> {
	private static CompactItemStackRenderer instance;
	private final IIngredientRenderer<ItemStack> delegate;

	private CompactItemStackRenderer(IIngredientRenderer<ItemStack> delegate) {
		this.delegate = delegate;
	}

	public static void install(IIngredientRenderer<ItemStack> delegate) {
		instance = new CompactItemStackRenderer(delegate);
	}

	public static IRecipeSlotBuilder apply(IRecipeSlotBuilder slot) {
		if (instance != null) {
			slot.setCustomRenderer(VanillaTypes.ITEM_STACK, instance);
		}
		return slot;
	}

	@Override
	public void render(GuiGraphics graphics, ItemStack stack) {
		int count = stack.getCount();
		if (!CategoryUi.isCompactAmount(count)) {
			delegate.render(graphics, stack);
			return;
		}

		ItemStack visual = stack.copy();
		visual.setCount(1);
		delegate.render(graphics, visual);

		String amount = CategoryUi.compactAmount(count);
		Font font = delegate.getFontRenderer(Minecraft.getInstance(), visual);
		int x = Math.max(0, 17 - font.width(amount));
		graphics.drawString(font, amount, x, 9, 0xffffffff, true);
	}

	@Override
	public List<Component> getTooltip(ItemStack stack, TooltipFlag tooltipFlag) {
		List<Component> tooltip = new ArrayList<>(delegate.getTooltip(stack, tooltipFlag));
		int count = stack.getCount();
		if (CategoryUi.isCompactAmount(count)) {
			tooltip.add(Component.literal(" "));
			if (ClientInputState.isShiftDown()) {
				tooltip.add(Component.literal("Exact amount: " + CategoryUi.exactAmount(count))
					.withStyle(ChatFormatting.AQUA));
			} else {
				tooltip.add(Component.literal("Amount: " + CategoryUi.compactAmount(count))
					.withStyle(ChatFormatting.AQUA));
				tooltip.add(Component.literal("Hold Shift for the exact amount")
					.withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		return tooltip;
	}

	@Override
	public Font getFontRenderer(Minecraft minecraft, ItemStack stack) {
		return delegate.getFontRenderer(minecraft, stack);
	}

	@Override
	public int getWidth() {
		return delegate.getWidth();
	}

	@Override
	public int getHeight() {
		return delegate.getHeight();
	}
}
