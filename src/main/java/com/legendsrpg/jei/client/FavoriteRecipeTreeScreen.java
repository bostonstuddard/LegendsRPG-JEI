package com.legendsrpg.jei.client;

import com.legendsrpg.jei.data.AddonData;
import com.legendsrpg.jei.data.ItemDefinition;
import com.legendsrpg.jei.data.ItemStackFactory;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.RecipeSnapshotCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FavoriteRecipeTreeScreen extends Screen {
	private static final int SCREEN_MARGIN = 8;
	private static final int HEADER_HEIGHT = 72;
	private static final int SUMMARY_HEIGHT = 96;
	private static final int FOOTER_HEIGHT = 28;
	private static final int NODE_WIDTH = 66;
	private static final int NODE_HEIGHT = 34;
	private static final int LEAF_WIDTH = 24;
	private static final int LEAF_HEIGHT = 21;
	private static final int HORIZONTAL_GAP = 17;
	private static final int LEVEL_GAP = 66;
	private static final float MIN_ZOOM = 0.35F;
	private static final float MAX_ZOOM = 1.40F;

	private final Screen parent;
	private final Map<String, String> selectedRecipes = new LinkedHashMap<>();
	private final Map<String, ItemStack> itemStackCache = new LinkedHashMap<>();
	private final Map<String, ItemStack> recipeIconCache = new LinkedHashMap<>();
	private List<RecipeSnapshot> roots = List.of();
	private int rootIndex;
	private long targetAmount = 1L;
	private RecipeTreeCalculator.Result result = RecipeTreeCalculator.Result.empty();
	private TreeLayout layout = TreeLayout.empty();
	private float zoom = 1.0F;
	private float panX;
	private float panY;

	private int panelLeft;
	private int panelTop;
	private int panelRight;
	private int panelBottom;
	private int treeLeft;
	private int treeTop;
	private int treeRight;
	private int treeBottom;
	private int summaryTop;

	private Button selectRootButton;
	private Button decreaseTarget;
	private Button increaseTarget;
	private Button zoomOut;
	private Button zoomIn;
	private Button fitTree;
	private HoverInfo hoverInfo;
	private String hoveredItemId;

	public FavoriteRecipeTreeScreen(Screen parent, RecipeSnapshot preferredRoot) {
		super(Component.literal("Recipe Tree"));
		this.parent = parent;
		selectedRecipes.putAll(com.legendsrpg.jei.config.LegendsConfig.get().treeRecipeSelections());
		RecipeSnapshot remembered = preferredRoot;
		if (remembered == null) {
			remembered = RecipeSnapshotCatalog.get().byKey(com.legendsrpg.jei.config.LegendsConfig.get().lastTreeRootKey());
		}
		refreshRoots(remembered);
		RecipeSnapshot root = currentRoot();
		if (preferredRoot == null && root != null && root.key().equals(com.legendsrpg.jei.config.LegendsConfig.get().lastTreeRootKey())) {
			targetAmount = com.legendsrpg.jei.config.LegendsConfig.get().lastTreeTargetAmount();
		} else {
			targetAmount = defaultTarget(root);
		}
	}

	@Override
	protected void init() {
		super.init();
		updateGeometry();
		refreshRoots(currentRoot());

		selectRootButton = this.addRenderableWidget(Button.builder(Component.literal("Select Recipe"), button -> openRootPicker())
			.bounds(panelLeft + 10, panelTop + 44, 104, 20).build());

		int center = (panelLeft + panelRight) / 2;
		decreaseTarget = this.addRenderableWidget(Button.builder(Component.literal("−"), button -> changeTarget(-targetStep()))
			.bounds(center - 72, panelTop + 44, 20, 20).build());
		increaseTarget = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> changeTarget(targetStep()))
			.bounds(center + 52, panelTop + 44, 20, 20).build());

		zoomOut = this.addRenderableWidget(Button.builder(Component.literal("−"), button -> zoomBy(0.86F))
			.bounds(panelRight - 116, panelTop + 44, 20, 20).build());
		zoomIn = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> zoomBy(1.16F))
			.bounds(panelRight - 92, panelTop + 44, 20, 20).build());
		fitTree = this.addRenderableWidget(Button.builder(Component.literal("Fit"), button -> resetView())
			.bounds(panelRight - 68, panelTop + 44, 58, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(panelRight - 66, panelBottom - 24, 56, 20).build());

		rebuild(true);
		refreshButtons();
	}

	private void updateGeometry() {
		panelLeft = SCREEN_MARGIN;
		panelTop = SCREEN_MARGIN;
		panelRight = Math.max(panelLeft + 200, this.width - SCREEN_MARGIN);
		panelBottom = Math.max(panelTop + 160, this.height - SCREEN_MARGIN);
		summaryTop = panelBottom - FOOTER_HEIGHT - SUMMARY_HEIGHT;
		treeLeft = panelLeft + 8;
		treeTop = panelTop + HEADER_HEIGHT;
		treeRight = panelRight - 8;
		treeBottom = summaryTop - 6;
	}

	private static long defaultTarget(RecipeSnapshot root) {
		if (root == null || root.output() == null) return 1L;
		return root.key().startsWith("source/") ? Math.max(1L, root.output().count()) : 1L;
	}

	private void persistTreeState() {
		RecipeSnapshot root = currentRoot();
		com.legendsrpg.jei.config.LegendsConfig.get().saveTreeState(root == null ? "" : root.key(), targetAmount, selectedRecipes);
	}

	private long targetStep() {
		return Minecraft.getInstance().hasShiftDown() ? 10L : 1L;
	}

	private void refreshRoots(RecipeSnapshot preferredRoot) {
		roots = RecipeTreeCalculator.favoriteRoots();
		if (roots.isEmpty()) {
			rootIndex = 0;
			result = RecipeTreeCalculator.Result.empty();
			layout = TreeLayout.empty();
			return;
		}

		if (preferredRoot != null) {
			for (int index = 0; index < roots.size(); index++) {
				if (roots.get(index).key().equals(preferredRoot.key())) {
					rootIndex = index;
					break;
				}
			}
		}
		rootIndex = Math.max(0, Math.min(rootIndex, roots.size() - 1));
	}

	private RecipeSnapshot currentRoot() {
		return roots.isEmpty() ? null : roots.get(Math.max(0, Math.min(rootIndex, roots.size() - 1)));
	}

	private void openRootPicker() {
		if (this.minecraft != null) this.minecraft.setScreen(new FavoriteRecipePickerScreen(this, roots, currentRoot()));
	}

	void selectRoot(RecipeSnapshot root) {
		if (root == null) return;
		for (int index = 0; index < roots.size(); index++) {
			if (!roots.get(index).key().equals(root.key())) continue;
			rootIndex = index;
			targetAmount = defaultTarget(root);
			selectedRecipes.clear();
			persistTreeState();
			rebuild(true);
			refreshButtons();
			return;
		}
	}

	private void changeTarget(long amount) {
		long next;
		if (amount > 0L && targetAmount > Long.MAX_VALUE - amount) next = Long.MAX_VALUE;
		else next = Math.max(1L, targetAmount + amount);
		if (next == targetAmount) return;
		targetAmount = next;
		persistTreeState();
		rebuild(false);
	}

	private void rebuild(boolean resetView) {
		RecipeSnapshot root = currentRoot();
		result = RecipeTreeCalculator.calculate(root, targetAmount, selectedRecipes);
		layout = TreeLayout.build(result.root());
		if (resetView) resetView();
		else clampPan();
	}

	private void resetView() {
		if (layout.nodes().isEmpty()) {
			zoom = 1.0F;
			panX = 0F;
			panY = 0F;
			return;
		}

		float viewWidth = Math.max(1F, treeRight - treeLeft - 20F);
		float viewHeight = Math.max(1F, treeBottom - treeTop - 20F);
		float widthFit = viewWidth / Math.max(1F, layout.width());
		float heightFit = viewHeight / Math.max(1F, layout.height());
		zoom = clampZoom(Math.min(1.05F, Math.min(widthFit, heightFit)));
		panX = ((treeRight - treeLeft) - layout.width() * zoom) / 2F;
		panY = 10F;
		clampPan();
	}

	private void zoomBy(float factor) {
		if (layout.nodes().isEmpty()) return;
		float oldZoom = zoom;
		zoom = clampZoom(zoom * factor);
		if (Math.abs(oldZoom - zoom) < 0.001F) return;

		float viewCenterX = (treeRight - treeLeft) / 2F;
		float viewCenterY = (treeBottom - treeTop) / 2F;
		float canvasX = (viewCenterX - panX) / oldZoom;
		float canvasY = (viewCenterY - panY) / oldZoom;
		panX = viewCenterX - canvasX * zoom;
		panY = viewCenterY - canvasY * zoom;
		clampPan();
	}

	private float clampZoom(float value) {
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
	}

	private void clampPan() {
		float viewWidth = Math.max(1F, treeRight - treeLeft);
		float viewHeight = Math.max(1F, treeBottom - treeTop);
		float contentWidth = layout.width() * zoom;
		float contentHeight = layout.height() * zoom;

		if (contentWidth <= viewWidth - 16F) {
			panX = (viewWidth - contentWidth) / 2F;
		} else {
			float minimum = viewWidth - contentWidth - 10F;
			panX = Math.max(minimum, Math.min(10F, panX));
		}

		if (contentHeight <= viewHeight - 16F) {
			panY = 10F;
		} else {
			float minimum = viewHeight - contentHeight - 10F;
			panY = Math.max(minimum, Math.min(10F, panY));
		}
	}

	private void refreshButtons() {
		if (selectRootButton == null) return;
		boolean hasRoots = !roots.isEmpty();
		selectRootButton.active = hasRoots;
		decreaseTarget.active = hasRoots && targetAmount > 1L;
		increaseTarget.active = hasRoots && targetAmount < Long.MAX_VALUE;
		zoomOut.active = hasRoots && zoom > MIN_ZOOM + 0.001F;
		zoomIn.active = hasRoots && zoom < MAX_ZOOM - 0.001F;
		fitTree.active = hasRoots;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (super.mouseClicked(event, doubled)) return true;

		LayoutNode clicked = insideTree(event.x(), event.y()) ? nodeAt(event.x(), event.y()) : null;
		if (clicked != null && event.button() == 0 && Minecraft.getInstance().hasShiftDown()) {
			cycleRecipe(clicked.node());
			return true;
		}

		String itemId = clicked == null ? hoveredItemId : clicked.node().itemId();
		if (itemId == null) return false;
		if (event.button() == 0) return JeiRuntimeController.showRecipes(treeStack(itemId));
		if (event.button() == 1) return JeiRuntimeController.showUses(treeStack(itemId));
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (hoveredItemId != null) {
			ItemStack stack = treeStack(hoveredItemId);
			if (JeiRuntimeController.matchesShowRecipe(event)) return JeiRuntimeController.showRecipes(stack);
			if (JeiRuntimeController.matchesShowUses(event)) return JeiRuntimeController.showUses(stack);
		}
		return super.keyPressed(event);
	}

	private LayoutNode nodeAt(double mouseX, double mouseY) {
		float canvasX = (float) ((mouseX - treeLeft - panX) / zoom);
		float canvasY = (float) ((mouseY - treeTop - panY) / zoom);
		for (LayoutNode node : layout.nodes()) {
			if (node.contains(canvasX, canvasY)) return node;
		}
		return null;
	}

	private void cycleRecipe(RecipeTreeCalculator.TreeNode node) {
		List<RecipeSnapshot> choices = RecipeTreeCalculator.favoriteRecipesForOutput(node.itemId());
		if (choices.size() < 2) return;

		String currentKey = node.recipeKey();
		int currentIndex = -1;
		for (int index = 0; index < choices.size(); index++) {
			if (choices.get(index).key().equals(currentKey)) {
				currentIndex = index;
				break;
			}
		}
		RecipeSnapshot next = choices.get(Math.floorMod(currentIndex + 1, choices.size()));

		if (node == result.root()) {
			for (int index = 0; index < roots.size(); index++) {
				if (roots.get(index).key().equals(next.key())) {
					rootIndex = index;
					selectedRecipes.clear();
					persistTreeState();
					rebuild(true);
					refreshButtons();
					return;
				}
			}
		}

		selectedRecipes.put(node.itemId(), next.key());
		persistTreeState();
		rebuild(false);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (insideTree(mouseX, mouseY) && !layout.nodes().isEmpty()) {
			if (Minecraft.getInstance().hasControlDown()) {
				zoomBy(verticalAmount > 0D ? 1.12F : 0.89F);
			} else if (Minecraft.getInstance().hasShiftDown()) {
				panX += (float) (verticalAmount * 30D);
				clampPan();
			} else {
				panY += (float) (verticalAmount * 30D);
				clampPan();
			}
			refreshButtons();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private boolean insideTree(double mouseX, double mouseY) {
		return mouseX >= treeLeft && mouseX < treeRight && mouseY >= treeTop && mouseY < treeBottom;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		updateGeometry();
		ThemePalette palette = ThemePalette.current();
		hoverInfo = null;
		hoveredItemId = null;

		graphics.fillGradient(0, 0, this.width, this.height, palette.screenTop(), palette.screenBottom());
		graphics.fill(panelLeft, panelTop, panelRight, panelBottom, palette.panel());
		graphics.renderOutline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, palette.outline());
		graphics.fill(panelLeft, panelTop, panelRight, panelTop + 3, palette.stripe());

		graphics.fill(treeLeft, treeTop, treeRight, treeBottom, palette.inner());
		graphics.renderOutline(treeLeft, treeTop, treeRight - treeLeft, treeBottom - treeTop, palette.innerOutline());

		int totalWidth = panelRight - panelLeft - 24;
		int baseWidth = Math.max(180, (int) (totalWidth * 0.68F));
		int baseLeft = panelLeft + 8;
		int leftoverLeft = baseLeft + baseWidth + 6;
		int summaryBottom = panelBottom - FOOTER_HEIGHT - 2;
		drawSummaryPanel(graphics, baseLeft, summaryTop, baseWidth, summaryBottom - summaryTop, "Total Base Cost", result.rawTotals(), mouseX, mouseY);
		drawSummaryPanel(graphics, leftoverLeft, summaryTop, panelRight - 8 - leftoverLeft, summaryBottom - summaryTop, "Leftovers", result.leftovers(), mouseX, mouseY);

		if (!roots.isEmpty() && !layout.nodes().isEmpty()) {
			drawTree(graphics, mouseX, mouseY);
		}

		super.render(graphics, mouseX, mouseY, delta);

		graphics.drawCenteredString(this.font, Component.literal("Recipe Tree").withStyle(ChatFormatting.GOLD), this.width / 2, panelTop + 9, 0xffffcf63);
		RecipeSnapshot root = currentRoot();
		if (root == null) {
			graphics.drawCenteredString(this.font, "Favorite a recipe with the star button first.", this.width / 2, (treeTop + treeBottom) / 2, palette.muted());
		} else {
			String title = fit(root.title(), Math.max(120, panelRight - panelLeft - 40));
			graphics.drawCenteredString(this.font, title, this.width / 2, panelTop + 22, palette.text());
			graphics.drawCenteredString(this.font, "Target: " + format(targetAmount), this.width / 2, panelTop + 50, palette.secondary());
			graphics.drawString(this.font, Math.round(zoom * 100F) + "%", panelRight - 154, panelTop + 50, palette.muted(), false);
		}

		String controls = "Wheel: scroll  •  Shift+wheel: sideways  •  Ctrl+wheel: zoom  •  Shift +/-: 10";
		graphics.drawString(this.font, fit(controls, panelRight - panelLeft - 90), panelLeft + 10, panelBottom - 19, palette.muted(), false);
		if (result.truncated()) {
			graphics.drawCenteredString(this.font, "A cycle or tree safety limit was shortened.", this.width / 2, panelBottom - 19, 0xffff9f43);
		}

		if (hoverInfo != null) drawHoverInfo(graphics, hoverInfo, mouseX, mouseY);
		refreshButtons();
	}

	private void drawTree(GuiGraphics graphics, int mouseX, int mouseY) {
		ThemePalette palette = ThemePalette.current();
		graphics.enableScissor(treeLeft + 1, treeTop + 1, treeRight - 1, treeBottom - 1);
		graphics.pose().pushMatrix();
		graphics.pose().translate(treeLeft + panX, treeTop + panY);
		graphics.pose().scale(zoom, zoom);

		drawConnectors(graphics, result.root(), palette);
		for (LayoutNode layoutNode : layout.nodes()) {
			drawNode(graphics, layoutNode, layoutNode.node() == result.root(), palette);
		}

		graphics.pose().popMatrix();
		graphics.disableScissor();

		if (insideTree(mouseX, mouseY)) {
			float canvasX = (mouseX - treeLeft - panX) / zoom;
			float canvasY = (mouseY - treeTop - panY) / zoom;
			for (LayoutNode node : layout.nodes()) {
				if (node.contains(canvasX, canvasY)) {
					hoveredItemId = node.node().itemId();
					hoverInfo = HoverInfo.forNode(node.node());
					break;
				}
			}
		}
	}

	private void drawConnectors(GuiGraphics graphics, RecipeTreeCalculator.TreeNode parent, ThemePalette palette) {
		if (parent == null || parent.children().isEmpty()) return;
		LayoutNode parentLayout = layout.byNode().get(parent);
		if (parentLayout == null) return;

		int parentCenterX = Math.round(parentLayout.centerX());
		int parentBottom = Math.round(parentLayout.y() + parentLayout.height());
		int childTop = Integer.MAX_VALUE;
		int minimumChildX = Integer.MAX_VALUE;
		int maximumChildX = Integer.MIN_VALUE;
		for (RecipeTreeCalculator.TreeNode child : parent.children()) {
			LayoutNode childLayout = layout.byNode().get(child);
			if (childLayout == null) continue;
			childTop = Math.min(childTop, Math.round(childLayout.y()));
			minimumChildX = Math.min(minimumChildX, Math.round(childLayout.centerX()));
			maximumChildX = Math.max(maximumChildX, Math.round(childLayout.centerX()));
		}
		if (childTop == Integer.MAX_VALUE) return;

		int middleY = parentBottom + Math.max(8, (childTop - parentBottom) / 2);
		int lineColor = palette.secondary();
		graphics.fill(parentCenterX, parentBottom, parentCenterX + 1, middleY + 1, lineColor);
		graphics.fill(minimumChildX, middleY, maximumChildX + 1, middleY + 1, lineColor);
		for (RecipeTreeCalculator.TreeNode child : parent.children()) {
			LayoutNode childLayout = layout.byNode().get(child);
			if (childLayout == null) continue;
			int childCenterX = Math.round(childLayout.centerX());
			graphics.fill(childCenterX, middleY, childCenterX + 1, Math.round(childLayout.y()) + 1, lineColor);
			drawConnectors(graphics, child, palette);
		}
	}

	private void drawNode(GuiGraphics graphics, LayoutNode layoutNode, boolean root, ThemePalette palette) {
		RecipeTreeCalculator.TreeNode node = layoutNode.node();
		int x = Math.round(layoutNode.x());
		int y = Math.round(layoutNode.y());

		if (node.recipeKey() != null) {
			int fill = root ? palette.header() : palette.slot();
			int outline = node.cycle() ? 0xffff6b6b : (root ? 0xffffcf63 : palette.slotOutline());
			graphics.fill(x, y, x + NODE_WIDTH, y + NODE_HEIGHT, fill);
			graphics.renderOutline(x, y, NODE_WIDTH, NODE_HEIGHT, outline);
			graphics.fill(x + 31, y + 4, x + 32, y + NODE_HEIGHT - 4, palette.divider());

			ItemStack recipeIcon = recipeIconCache.computeIfAbsent(node.recipeKey(), key -> ItemStackFactory.create(recipeIcon(key), 1));
			graphics.renderItem(recipeIcon, x + 8, y + 9);
			graphics.renderItem(treeStack(node.itemId()), x + 42, y + 9);
			if (node.required() > 1L) drawAmount(graphics, node.required(), x + NODE_WIDTH + 3, y + 12, palette.text());
			if (RecipeTreeCalculator.favoriteRecipesForOutput(node.itemId()).size() > 1) {
				graphics.drawString(this.font, "◆", x + NODE_WIDTH - 7, y + 1, 0xffffcf63, false);
			}
		} else {
			int itemX = x + (LEAF_WIDTH - 16) / 2;
			graphics.renderItem(treeStack(node.itemId()), itemX, y + 1);
			if (node.required() > 1L) drawAmount(graphics, node.required(), itemX + 16, y + 9, palette.text());
			if (node.cycle()) graphics.drawString(this.font, "!", itemX + 13, y, 0xffff6b6b, true);
		}
	}

	private ItemStack treeStack(String itemId) {
		String safeId = itemId == null ? "minecraft:barrier" : itemId;
		return itemStackCache.computeIfAbsent(safeId, this::createTreeStack);
	}

	private ItemStack createTreeStack(String itemId) {
		if (itemId.startsWith("mob_icon_")) {
			ItemDefinition definition = AddonData.get().item(itemId);
			String visualItem = definition == null ? "" : definition.item();
			if (visualItem == null || visualItem.isBlank() || visualItem.equals("minecraft:barrier") || visualItem.equals("barrier")) {
				visualItem = "minecraft:zombie_head";
			}
			return ItemStackFactory.vanilla(visualItem, 1);
		}
		return ItemStackFactory.create(itemId, 1);
	}

	private void drawAmount(GuiGraphics graphics, long amount, int x, int y, int color) {
		graphics.drawString(this.font, formatCompact(amount), x, y, color, true);
	}

	private String recipeIcon(String recipeKey) {
		if (recipeKey == null) return "minecraft:crafting_table";
		if (recipeKey.startsWith("raw/")) return "minecraft:compass";
		if (recipeKey.startsWith("smithing/")) return "minecraft:smithing_table";
		if (recipeKey.startsWith("ore_forge/")) return "minecraft:blast_furnace";
		if (recipeKey.startsWith("shop/")) return "minecraft:emerald";
		if (recipeKey.startsWith("mining_treasure/")) return "minecraft:chest";
		if (recipeKey.startsWith("mob_drop/")) return "minecraft:bone";
		if (recipeKey.startsWith("source/")) return "minecraft:compass";
		if (recipeKey.startsWith("research/")) return "minecraft:knowledge_book";
		if (recipeKey.startsWith("item/")) return "minecraft:book";
		return "minecraft:crafting_table";
	}

	private void drawSummaryPanel(
		GuiGraphics graphics,
		int x,
		int y,
		int width,
		int height,
		String title,
		Map<String, Long> values,
		int mouseX,
		int mouseY
	) {
		ThemePalette palette = ThemePalette.current();
		graphics.fill(x, y, x + width, y + height, palette.inner());
		graphics.renderOutline(x, y, width, height, palette.innerOutline());
		graphics.drawCenteredString(this.font, title, x + width / 2, y + 5, 0xffffcf63);

		if (values.isEmpty()) {
			graphics.drawCenteredString(this.font, "None", x + width / 2, y + height / 2, palette.muted());
			return;
		}

		List<Map.Entry<String, Long>> entries = new ArrayList<>(values.entrySet());
		entries.sort(Comparator.comparing(entry -> RecipeSnapshotCatalog.itemName(entry.getKey()), String.CASE_INSENSITIVE_ORDER));
		int availableWidth = Math.max(20, width - 12);
		int rows = 4;
		int preferredColumns = Math.max(1, (entries.size() + rows - 1) / rows);
		int cellWidth = Math.max(17, Math.min(28, availableWidth / preferredColumns));
		int columns = Math.max(1, availableWidth / cellWidth);
		rows = Math.max(1, Math.min(4, (entries.size() + columns - 1) / columns));
		int startX = x + (width - Math.min(entries.size(), columns) * cellWidth) / 2;
		int startY = y + 18;

		for (int index = 0; index < entries.size(); index++) {
			int row = index / columns;
			int column = index % columns;
			if (row >= rows) break;
			Map.Entry<String, Long> entry = entries.get(index);
			int itemX = startX + column * cellWidth + Math.max(0, (cellWidth - 16) / 2);
			int itemY = startY + row * 19;
			graphics.renderItem(treeStack(entry.getKey()), itemX, itemY);
			if (entry.getValue() > 1L) {
				String amount = formatCompact(entry.getValue());
				graphics.drawString(this.font, amount, itemX + 17 - this.font.width(amount), itemY + 10, 0xffffffff, true);
			}
			if (mouseX >= itemX && mouseX < itemX + 18 && mouseY >= itemY && mouseY < itemY + 18) {
				hoveredItemId = entry.getKey();
				hoverInfo = HoverInfo.forSummary(entry.getKey(), entry.getValue(), title);
			}
		}
	}

	private void drawHoverInfo(GuiGraphics graphics, HoverInfo info, int mouseX, int mouseY) {
		ThemePalette palette = ThemePalette.current();
		int width = 0;
		for (String line : info.lines()) width = Math.max(width, this.font.width(line));
		width += 12;
		int height = info.lines().size() * 10 + 8;
		int x = Math.min(this.width - width - 4, mouseX + 10);
		int y = Math.min(this.height - height - 4, mouseY + 10);
		x = Math.max(4, x);
		y = Math.max(4, y);
		graphics.fill(x, y, x + width, y + height, palette.panel());
		graphics.renderOutline(x, y, width, height, palette.outline());
		for (int index = 0; index < info.lines().size(); index++) {
			graphics.drawString(this.font, info.lines().get(index), x + 6, y + 5 + index * 10, index == 0 ? 0xffffcf63 : palette.text(), false);
		}
	}

	private String fit(String value, int width) {
		if (value == null) return "";
		if (font.width(value) <= width) return value;
		return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
	}

	private static String format(long value) {
		if (value == Long.MAX_VALUE) return "∞";
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}

	private static String formatCompact(long value) {
		if (value == Long.MAX_VALUE) return "∞";
		if (value > 950_000_000L) return decimal(value / 1_000_000_000D) + "B";
		if (value > 950_000L) return decimal(value / 1_000_000D) + "M";
		if (value > 950L) return decimal(value / 1_000D) + "K";
		return Long.toString(value);
	}

	private static String decimal(double value) {
		String result = value >= 10D ? String.format(Locale.US, "%.0f", value) : String.format(Locale.US, "%.1f", value);
		while (result.contains(".") && (result.endsWith("0") || result.endsWith("."))) result = result.substring(0, result.length() - 1);
		return result;
	}

	@Override
	public void onClose() {
		persistTreeState();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	private record HoverInfo(List<String> lines) {
		private static HoverInfo forNode(RecipeTreeCalculator.TreeNode node) {
			List<String> lines = new ArrayList<>();
			lines.add(node.name());
			lines.add("Required: " + format(node.required()));
			if (node.recipeKey() != null) {
				lines.add(node.recipeTitle());
				lines.add("Crafts: " + format(node.batches()) + "  •  Produces: " + format(node.produced()));
				if (node.leftover() > 0L) lines.add("Leftover: " + format(node.leftover()));
			} else {
				lines.add("Base material — no favorited production recipe.");
			}
			lines.add("Left click / " + JeiRuntimeController.showRecipeKeyName() + ": show recipes");
			lines.add("Right click / " + JeiRuntimeController.showUsesKeyName() + ": show uses");
			int alternatives = RecipeTreeCalculator.favoriteRecipesForOutput(node.itemId()).size();
			if (alternatives > 1) lines.add("Shift-click to cycle " + alternatives + " favorited recipes.");
			if (node.cycle()) lines.add("Recipe loop stopped here.");
			return new HoverInfo(List.copyOf(lines));
		}

		private static HoverInfo forSummary(String itemId, long amount, String section) {
			return new HoverInfo(List.of(
				RecipeSnapshotCatalog.itemName(itemId),
				section + ": " + format(amount),
				"Left click / " + JeiRuntimeController.showRecipeKeyName() + ": show recipes",
				"Right click / " + JeiRuntimeController.showUsesKeyName() + ": show uses"
			));
		}
	}

	private record LayoutNode(RecipeTreeCalculator.TreeNode node, float x, float y, float width, float height) {
		private float centerX() { return x + width / 2F; }
		private boolean contains(float mouseX, float mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private record TreeLayout(List<LayoutNode> nodes, IdentityHashMap<RecipeTreeCalculator.TreeNode, LayoutNode> byNode, float width, float height) {
		private static TreeLayout empty() {
			return new TreeLayout(List.of(), new IdentityHashMap<>(), 1F, 1F);
		}

		private static TreeLayout build(RecipeTreeCalculator.TreeNode root) {
			if (root == null) return empty();
			IdentityHashMap<RecipeTreeCalculator.TreeNode, Float> widths = new IdentityHashMap<>();
			measure(root, widths);
			List<LayoutNode> nodes = new ArrayList<>();
			IdentityHashMap<RecipeTreeCalculator.TreeNode, LayoutNode> byNode = new IdentityHashMap<>();
			place(root, 0F, 0, widths, nodes, byNode);
			float totalWidth = widths.getOrDefault(root, (float) NODE_WIDTH);
			float maximumBottom = 1F;
			for (LayoutNode node : nodes) maximumBottom = Math.max(maximumBottom, node.y() + node.height());
			return new TreeLayout(List.copyOf(nodes), byNode, Math.max(1F, totalWidth), maximumBottom + 8F);
		}

		private static float measure(RecipeTreeCalculator.TreeNode node, IdentityHashMap<RecipeTreeCalculator.TreeNode, Float> widths) {
			float ownWidth = node.recipeKey() != null ? NODE_WIDTH : LEAF_WIDTH;
			if (node.children().isEmpty()) {
				widths.put(node, ownWidth);
				return ownWidth;
			}

			float childrenWidth = 0F;
			for (int index = 0; index < node.children().size(); index++) {
				if (index > 0) childrenWidth += HORIZONTAL_GAP;
				childrenWidth += measure(node.children().get(index), widths);
			}
			float result = Math.max(ownWidth, childrenWidth);
			widths.put(node, result);
			return result;
		}

		private static void place(
			RecipeTreeCalculator.TreeNode node,
			float left,
			int depth,
			IdentityHashMap<RecipeTreeCalculator.TreeNode, Float> widths,
			List<LayoutNode> nodes,
			IdentityHashMap<RecipeTreeCalculator.TreeNode, LayoutNode> byNode
		) {
			float subtreeWidth = widths.getOrDefault(node, (float) NODE_WIDTH);
			float nodeWidth = node.recipeKey() != null ? NODE_WIDTH : LEAF_WIDTH;
			float nodeHeight = node.recipeKey() != null ? NODE_HEIGHT : LEAF_HEIGHT;
			float nodeX = left + (subtreeWidth - nodeWidth) / 2F;
			float nodeY = depth * LEVEL_GAP;
			LayoutNode layoutNode = new LayoutNode(node, nodeX, nodeY, nodeWidth, nodeHeight);
			nodes.add(layoutNode);
			byNode.put(node, layoutNode);

			if (node.children().isEmpty()) return;
			float childrenWidth = 0F;
			for (int index = 0; index < node.children().size(); index++) {
				if (index > 0) childrenWidth += HORIZONTAL_GAP;
				childrenWidth += widths.getOrDefault(node.children().get(index), (float) LEAF_WIDTH);
			}
			float childLeft = left + (subtreeWidth - childrenWidth) / 2F;
			for (RecipeTreeCalculator.TreeNode child : node.children()) {
				place(child, childLeft, depth + 1, widths, nodes, byNode);
				childLeft += widths.getOrDefault(child, (float) LEAF_WIDTH) + HORIZONTAL_GAP;
			}
		}
	}
}
