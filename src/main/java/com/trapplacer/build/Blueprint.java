package com.trapplacer.build;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A finished trap design in local coordinates (see {@link Placement}).
 * Built once per trap with a {@link BlueprintBuilder} and reused for every placement.
 */
public final class Blueprint {
	private final Map<BlockPos, BlockSpec> blocks;
	private final Map<BlockPos, List<ItemStack>> contents;
	private final BoundingBox bounds;

	Blueprint(Map<BlockPos, BlockSpec> blocks, Map<BlockPos, List<ItemStack>> contents) {
		this.blocks = Collections.unmodifiableMap(blocks);
		this.contents = Collections.unmodifiableMap(contents);
		this.bounds = BoundingBox.encapsulatingPositions(blocks.keySet())
			.orElseThrow(() -> new IllegalStateException("Empty blueprint"));
	}

	/** Blocks in the order they were added; later writes to a position replace earlier ones. */
	public Map<BlockPos, BlockSpec> blocks() {
		return this.blocks;
	}

	/** Items to put into containers (dispensers, chests...) after the trap is built. */
	public Map<BlockPos, List<ItemStack>> contents() {
		return this.contents;
	}

	public BoundingBox bounds() {
		return this.bounds;
	}

	public int size() {
		return this.blocks.size();
	}
}
