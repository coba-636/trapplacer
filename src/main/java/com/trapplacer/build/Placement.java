package com.trapplacer.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Maps blueprint-local coordinates to the world.
 *
 * <p>Blueprints are written in a local frame where +x is to the player's right,
 * +y is up and +z is forward (the direction the player is looking). Block states
 * in a blueprint use {@link BlueprintBuilder#FWD} and friends, which are the
 * canonical directions for a player facing north; they get rotated to match the
 * real facing here.
 */
public record Placement(BlockPos origin, Direction facing) {
	public Placement {
		if (!facing.getAxis().isHorizontal()) {
			throw new IllegalArgumentException("Trap facing must be horizontal, got " + facing);
		}
	}

	public Rotation rotation() {
		return switch (this.facing) {
			case EAST -> Rotation.CLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_180;
			case WEST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public BlockPos toWorld(BlockPos local) {
		BlockPos canonical = new BlockPos(local.getX(), local.getY(), -local.getZ());
		return this.origin.offset(canonical.rotate(this.rotation()));
	}

	public BlockState transform(BlockState state) {
		return state.rotate(this.rotation());
	}
}
