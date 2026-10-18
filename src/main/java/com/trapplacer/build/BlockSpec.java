package com.trapplacer.build;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One block of a blueprint. Either a fixed block state, or a terrain-matching
 * "camo" block that is picked when the trap is placed. {@code free} blocks cost
 * nothing in survival.
 */
public record BlockSpec(@Nullable BlockState state, Camo.@Nullable Kind camo, boolean free) {
	public static BlockSpec of(BlockState state) {
		return new BlockSpec(state, null, false);
	}

	public static BlockSpec camo(Camo.Kind kind) {
		return new BlockSpec(null, kind, true);
	}

	/** Natural ground that comes with a schematic dug into the ground: placed as saved, but free like camo. */
	public static BlockSpec ground(BlockState state) {
		return new BlockSpec(state, null, true);
	}

	public boolean isCamo() {
		return this.camo != null;
	}

	public BlockState resolve(Camo camo) {
		return this.camo != null ? camo.get(this.camo) : this.state;
	}
}
