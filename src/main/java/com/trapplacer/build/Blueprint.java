package com.trapplacer.build;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * A finished trap design in local coordinates (see {@link Placement}).
 * Built once per trap and reused for every placement.
 */
public final class Blueprint {
	/** An entity to spawn with the trap, at a position in local (continuous) coordinates. */
	public record EntitySpec(Vec3 pos, CompoundTag nbt) {
	}

	private final Map<BlockPos, BlockSpec> blocks;
	private final Map<BlockPos, List<ItemStack>> contents;
	private final Map<BlockPos, CompoundTag> blockEntityData;
	private final List<EntitySpec> entities;
	private final boolean exact;
	private final BoundingBox bounds;

	Blueprint(Map<BlockPos, BlockSpec> blocks, Map<BlockPos, List<ItemStack>> contents,
			Map<BlockPos, CompoundTag> blockEntityData, List<EntitySpec> entities, boolean exact) {
		this.blocks = Collections.unmodifiableMap(blocks);
		this.contents = Collections.unmodifiableMap(contents);
		this.blockEntityData = Collections.unmodifiableMap(blockEntityData);
		this.entities = List.copyOf(entities);
		this.exact = exact;
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

	/** Saved block entity data (from imported schematics), loaded into the block entities after building. */
	public Map<BlockPos, CompoundTag> blockEntityData() {
		return this.blockEntityData;
	}

	public List<EntitySpec> entities() {
		return this.entities;
	}

	/**
	 * Exact blueprints (imported schematics) are placed precisely as saved: the
	 * redstone isn't re-settled afterwards, since the saved states already are.
	 */
	public boolean exact() {
		return this.exact;
	}

	public BoundingBox bounds() {
		return this.bounds;
	}

	public int size() {
		return this.blocks.size();
	}
}
