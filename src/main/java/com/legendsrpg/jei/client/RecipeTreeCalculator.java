package com.legendsrpg.jei.client;

import com.legendsrpg.jei.config.LegendsConfig;
import com.legendsrpg.jei.data.IngredientDefinition;
import com.legendsrpg.jei.data.RecipeSnapshot;
import com.legendsrpg.jei.data.RecipeSnapshotCatalog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeTreeCalculator {
	private static final int MAX_DEPTH = 16;
	private static final int MAX_ITEMS = 600;
	private static final int MAX_QUEUE_PASSES = 10_000;
	private static final Object FAVORITE_INDEX_LOCK = new Object();
	private static volatile List<String> indexedFavoriteKeys = List.of();
	private static volatile List<RecipeSnapshot> indexedRoots = List.of();
	private static volatile Map<String, List<RecipeSnapshot>> indexedByOutput = Map.of();

	private RecipeTreeCalculator() {}

	public static List<RecipeSnapshot> favoriteRoots() {
		ensureFavoriteIndex();
		return indexedRoots;
	}

	public static List<RecipeSnapshot> favoriteRecipesForOutput(String outputId) {
		if (outputId == null || outputId.isBlank()) return List.of();
		ensureFavoriteIndex();
		return indexedByOutput.getOrDefault(outputId, List.of());
	}

	private static void ensureFavoriteIndex() {
		List<String> favorites = LegendsConfig.get().favoriteRecipeKeys();
		if (favorites == indexedFavoriteKeys) return;
		synchronized (FAVORITE_INDEX_LOCK) {
			if (favorites == indexedFavoriteKeys) return;
			List<RecipeSnapshot> roots = new ArrayList<>();
			Set<String> seen = new LinkedHashSet<>();
			for (String key : favorites) {
				RecipeSnapshot snapshot = RecipeSnapshotCatalog.get().byKey(key);
				if (snapshot == null || snapshot.output() == null || !seen.add(snapshot.key())) continue;
				roots.add(snapshot);
			}

			Map<String, List<RecipeSnapshot>> byOutput = new LinkedHashMap<>();
			for (int index = favorites.size() - 1; index >= 0; index--) {
				RecipeSnapshot snapshot = RecipeSnapshotCatalog.get().byKey(favorites.get(index));
				if (snapshot == null || snapshot.output() == null) continue;
				byOutput.computeIfAbsent(snapshot.output().id(), ignored -> new ArrayList<>()).add(snapshot);
			}
			Map<String, List<RecipeSnapshot>> immutableByOutput = new LinkedHashMap<>();
			byOutput.forEach((itemId, recipes) -> immutableByOutput.put(itemId, List.copyOf(recipes)));

			indexedRoots = List.copyOf(roots);
			indexedByOutput = Map.copyOf(immutableByOutput);
			indexedFavoriteKeys = favorites;
		}
	}

	public static Result calculate(RecipeSnapshot root) {
		return calculate(root, 1L, Map.of());
	}

	public static Result calculate(RecipeSnapshot root, long targetAmount, Map<String, String> selectedRecipeKeys) {
		if (root == null || root.output() == null) return Result.empty();

		Planner planner = new Planner(root, Math.max(1L, targetAmount), selectedRecipeKeys == null ? Map.of() : selectedRecipeKeys);
		planner.plan();
		TreeNode tree = planner.buildDisplayTree();
		return new Result(
			tree,
			Map.copyOf(planner.rawTotals),
			Map.copyOf(planner.leftovers),
			planner.truncated,
			planner.entries.size(),
			planner.maxDepth
		);
	}

	private static final class Planner {
		private final RecipeSnapshot root;
		private final long targetAmount;
		private final Map<String, String> selectedRecipeKeys;
		private final Map<String, Entry> entries = new LinkedHashMap<>();
		private final Map<String, Long> rawTotals = new LinkedHashMap<>();
		private final Map<String, Long> leftovers = new LinkedHashMap<>();
		private final Set<String> cyclicItems = new LinkedHashSet<>();
		private boolean truncated;
		private int maxDepth;

		private Planner(RecipeSnapshot root, long targetAmount, Map<String, String> selectedRecipeKeys) {
			this.root = root;
			this.targetAmount = targetAmount;
			this.selectedRecipeKeys = Map.copyOf(selectedRecipeKeys);
		}

		private void plan() {
			detectCycles(root.output().id(), new ArrayList<>(), new LinkedHashSet<>(), root);

			Deque<String> queue = new ArrayDeque<>();
			Entry rootEntry = entry(root.output().id());
			rootEntry.forcedRecipe = root;
			rootEntry.required = targetAmount;
			rootEntry.depth = 0;
			queue.add(root.output().id());

			int passes = 0;
			while (!queue.isEmpty() && passes++ < MAX_QUEUE_PASSES) {
				String itemId = queue.removeFirst();
				Entry current = entry(itemId);
				RecipeSnapshot recipe = recipeFor(current);
				current.recipe = recipe;

				if (recipe == null || recipe.ingredients().isEmpty() || cyclicItems.contains(itemId) || current.depth >= MAX_DEPTH) {
					current.expandable = false;
					if (cyclicItems.contains(itemId) || current.depth >= MAX_DEPTH) truncated = true;
					continue;
				}

				current.expandable = true;
				long outputPerBatch = Math.max(1L, recipe.output() == null ? 1L : recipe.output().count());
				long desiredBatches = ceilDiv(current.required, outputPerBatch);
				if (desiredBatches <= current.batches) continue;

				long deltaBatches = desiredBatches - current.batches;
				current.batches = desiredBatches;
				current.produced = safeMultiply(desiredBatches, outputPerBatch);

				Map<String, IngredientDefinition> merged = mergeIngredients(recipe.ingredients());
				for (IngredientDefinition ingredient : merged.values()) {
					long addedDemand = safeMultiply(Math.max(1L, ingredient.count()), deltaBatches);
					Entry child = entry(ingredient.id());
					child.required = safeAdd(child.required, addedDemand);
					int childDepth = Math.min(MAX_DEPTH, current.depth + 1);
					if (child.depth == Integer.MAX_VALUE || childDepth < child.depth) child.depth = childDepth;
					maxDepth = Math.max(maxDepth, childDepth);
					queue.addLast(ingredient.id());
					if (entries.size() >= MAX_ITEMS) {
						truncated = true;
						queue.clear();
						break;
					}
				}
			}

			if (passes >= MAX_QUEUE_PASSES) truncated = true;

			for (Entry value : entries.values()) {
				RecipeSnapshot recipe = recipeFor(value);
				boolean base = recipe == null || recipe.ingredients().isEmpty() || cyclicItems.contains(value.itemId) || value.depth >= MAX_DEPTH || !value.expandable;
				if (base) {
					rawTotals.merge(value.itemId, value.required, RecipeTreeCalculator::safeAdd);
					value.produced = value.required;
					value.batches = 1L;
				} else {
					long leftover = value.produced == Long.MAX_VALUE ? 0L : Math.max(0L, value.produced - value.required);
					if (leftover > 0L) leftovers.put(value.itemId, leftover);
				}
			}
		}

		private TreeNode buildDisplayTree() {
			Set<String> displayed = new LinkedHashSet<>();
			return buildNode(root.output().id(), displayed, true);
		}

		private TreeNode buildNode(String itemId, Set<String> displayed, boolean rootNode) {
			Entry value = entries.get(itemId);
			if (value == null) return null;
			if (!displayed.add(itemId) && !rootNode) return null;

			RecipeSnapshot recipe = rootNode ? root : recipeFor(value);
			boolean expandable = value.expandable && recipe != null && !recipe.ingredients().isEmpty();
			List<TreeNode> children = new ArrayList<>();
			if (expandable) {
				for (IngredientDefinition ingredient : mergeIngredients(recipe.ingredients()).values()) {
					TreeNode child = buildNode(ingredient.id(), displayed, false);
					if (child != null) children.add(child);
				}
			}

			return new TreeNode(
				itemId,
				RecipeSnapshotCatalog.itemName(itemId),
				value.required,
				value.produced,
				value.batches,
				recipe == null ? null : recipe.key(),
				recipe == null ? "Base material" : recipe.title(),
				List.copyOf(children),
				expandable,
				cyclicItems.contains(itemId),
				recipe == null || !expandable
			);
		}

		private Entry entry(String itemId) {
			return entries.computeIfAbsent(itemId, Entry::new);
		}

		private RecipeSnapshot recipeFor(Entry entry) {
			if (entry.forcedRecipe != null) return entry.forcedRecipe;
			String selectedKey = selectedRecipeKeys.get(entry.itemId);
			if (selectedKey != null) {
				RecipeSnapshot selected = RecipeSnapshotCatalog.get().byKey(selectedKey);
				if (selected != null && selected.output() != null && entry.itemId.equals(selected.output().id()) && LegendsConfig.get().isFavorite(selected.key())) {
					return selected;
				}
			}
			List<RecipeSnapshot> favorites = favoriteRecipesForOutput(entry.itemId);
			return favorites.isEmpty() ? null : favorites.get(0);
		}

		private void detectCycles(String itemId, List<String> path, Set<String> visiting, RecipeSnapshot forced) {
			if (visiting.contains(itemId)) {
				int start = path.indexOf(itemId);
				if (start < 0) start = 0;
				for (int index = start; index < path.size(); index++) cyclicItems.add(path.get(index));
				cyclicItems.add(itemId);
				return;
			}
			if (path.size() >= MAX_DEPTH) {
				truncated = true;
				return;
			}

			RecipeSnapshot recipe;
			if (forced != null) recipe = forced;
			else {
				Entry probe = new Entry(itemId);
				recipe = recipeFor(probe);
			}
			if (recipe == null || recipe.ingredients().isEmpty()) return;

			visiting.add(itemId);
			path.add(itemId);
			for (IngredientDefinition ingredient : mergeIngredients(recipe.ingredients()).values()) {
				detectCycles(ingredient.id(), path, visiting, null);
			}
			path.remove(path.size() - 1);
			visiting.remove(itemId);
		}
	}

	private static final class Entry {
		private final String itemId;
		private long required;
		private long produced;
		private long batches;
		private int depth = Integer.MAX_VALUE;
		private RecipeSnapshot forcedRecipe;
		private RecipeSnapshot recipe;
		private boolean expandable;

		private Entry(String itemId) {
			this.itemId = itemId;
		}
	}

	private static Map<String, IngredientDefinition> mergeIngredients(List<IngredientDefinition> ingredients) {
		Map<String, IngredientDefinition> merged = new LinkedHashMap<>();
		for (IngredientDefinition ingredient : ingredients) {
			IngredientDefinition existing = merged.get(ingredient.id());
			if (existing == null) {
				merged.put(ingredient.id(), ingredient);
			} else {
				int count = (int) Math.min(Integer.MAX_VALUE, (long) existing.count() + ingredient.count());
				String label = existing.label() != null && !existing.label().isBlank() ? existing.label() : ingredient.label();
				merged.put(ingredient.id(), new IngredientDefinition(ingredient.id(), count, label));
			}
		}
		return merged;
	}

	private static long ceilDiv(long value, long divisor) {
		return value <= 0L ? 0L : 1L + ((value - 1L) / Math.max(1L, divisor));
	}

	private static long safeMultiply(long a, long b) {
		if (a == 0L || b == 0L) return 0L;
		if (a == Long.MAX_VALUE || b == Long.MAX_VALUE || a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
		return a * b;
	}

	private static long safeAdd(long a, long b) {
		if (a == Long.MAX_VALUE || b == Long.MAX_VALUE || Long.MAX_VALUE - a < b) return Long.MAX_VALUE;
		return a + b;
	}

	public record TreeNode(
		String itemId,
		String name,
		long required,
		long produced,
		long batches,
		String recipeKey,
		String recipeTitle,
		List<TreeNode> children,
		boolean expandable,
		boolean cycle,
		boolean baseMaterial
	) {
		public TreeNode {
			name = name == null || name.isBlank() ? RecipeSnapshotCatalog.itemName(itemId) : name;
			recipeTitle = recipeTitle == null || recipeTitle.isBlank() ? "Base material" : recipeTitle;
			children = children == null ? List.of() : List.copyOf(children);
		}

		public long leftover() {
			if (produced == Long.MAX_VALUE) return 0L;
			return Math.max(0L, produced - required);
		}
	}

	public record Result(
		TreeNode root,
		Map<String, Long> rawTotals,
		Map<String, Long> leftovers,
		boolean truncated,
		int nodeCount,
		int maxDepth
	) {
		public static Result empty() {
			return new Result(null, Map.of(), Map.of(), false, 0, 0);
		}
	}
}
