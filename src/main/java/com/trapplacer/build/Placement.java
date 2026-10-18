package com.trapplacer.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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

	/**
	 * Continuous version of {@link #toWorld(BlockPos)}: local block (x, y, z) spans
	 * [x, x+1) etc., and lands on exactly the world block toWorld gives for it.
	 */
	public Vec3 toWorld(Vec3 local) {
		// Flip z like toWorld(BlockPos) does, then rotate around the centre of the origin block.
		double cx = local.x - 0.5;
		double cz = (1.0 - local.z) - 0.5;
		double rx;
		double rz;
		switch (this.rotation()) {
			case CLOCKWISE_90 -> {
				rx = -cz;
				rz = cx;
			}
			case CLOCKWISE_180 -> {
				rx = -cx;
				rz = -cz;
			}
			case COUNTERCLOCKWISE_90 -> {
				rx = cz;
				rz = -cx;
			}
			default -> {
				rx = cx;
				rz = cz;
			}
		}
		return new Vec3(this.origin.getX() + rx + 0.5, this.origin.getY() + local.y, this.origin.getZ() + rz + 0.5);
	}

	public BlockState transform(BlockState state) {
		return state.rotate(this.rotation());
	}
}
