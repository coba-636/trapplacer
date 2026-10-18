package com.trapplacer.build;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Works out (and takes) the items a survival player pays to place a trap. */
public final class MaterialCost {
	private MaterialCost() {
	}

	public static Map<Item, Integer> of(Blueprint blueprint) {
		Map<Item, Integer> cost = new LinkedHashMap<>();
		for (BlockSpec spec : blueprint.blocks().values()) {
			if (spec.isCamo()) {
				continue;
			}
			Item item = itemFor(spec.state());
			if (item != Items.AIR) {
				cost.merge(item, 1, Integer::sum);
			}
		}
		for (List<ItemStack> stacks : blueprint.contents().values()) {
			for (ItemStack stack : stacks) {
				cost.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		// Imported schematics: what's saved inside containers, and entities that are items
		// (arrows, boats, minecarts...) are paid for too.
		for (CompoundTag blockEntity : blueprint.blockEntityData().values()) {
			ListTag items = blockEntity.getListOrEmpty("Items");
			for (int i = 0; i < items.size(); i++) {
				CompoundTag item = items.getCompoundOrEmpty(i);
				Item type = itemById(item.getStringOr("id", ""));
				if (type != Items.AIR) {
					cost.merge(type, item.getIntOr("count", 1), Integer::sum);
				}
			}
		}
		for (Blueprint.EntitySpec entity : blueprint.entities()) {
			Item type = itemById(entity.nbt().getStringOr("id", ""));
			if (type != Items.AIR) {
				cost.merge(type, 1, Integer::sum);
			}
		}

		Map<Item, Integer> sorted = new LinkedHashMap<>();
		cost.entrySet().stream()
			.sorted(Comparator.<Map.Entry<Item, Integer>>comparingInt(Map.Entry::getValue).reversed()
				.thenComparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()))
			.forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	private static Item itemFor(BlockState state) {
		if (state.isAir()
			|| state.getBlock() instanceof LiquidBlock
			|| state.getBlock() instanceof PistonHeadBlock
			|| state.getBlock() instanceof MovingPistonBlock) {
			return Items.AIR;
		}
		// Two-block things (beds, doors) are paid for once.
		if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
			return Items.AIR;
		}
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
			return Items.AIR;
		}
		// Some blocks can't be picked up without Silk Touch, so charge what they drop instead.
		if (state.is(Blocks.DIAMOND_ORE)) {
			return Items.DIAMOND;
		}
		if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) {
			return Items.AMETHYST_SHARD;
		}
		return state.getBlock().asItem();
	}

	private static Item itemById(String id) {
		Identifier identifier = Identifier.tryParse(id);
		return identifier == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(identifier);
	}

	/** What the player is short of, or an empty map if they can afford it. */
	public static Map<Item, Integer> missing(Player player, Map<Item, Integer> cost) {
		Map<Item, Integer> missing = new LinkedHashMap<>();
		cost.forEach((item, needed) -> {
			int have = count(player.getInventory(), item);
			if (have < needed) {
				missing.put(item, needed - have);
			}
		});
		return missing;
	}

	public static void take(Player player, Map<Item, Integer> cost) {
		Inventory inventory = player.getInventory();
		cost.forEach((item, needed) -> {
			int left = needed;
			for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.is(item)) {
					int taken = Math.min(left, stack.getCount());
					stack.shrink(taken);
					left -= taken;
				}
			}
		});
		inventory.setChanged();
	}

	private static int count(Inventory inventory, Item item) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
