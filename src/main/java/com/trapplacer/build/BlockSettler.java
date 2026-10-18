package com.trapplacer.build;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks are placed without neighbour updates so half-built redstone can't fire
 * while a trap is going up. Once everything is in place this brings the redstone
 * and connected blocks (dust, fences, panes...) to their real resting state.
 */
public final class BlockSettler {
	/** Place silently: no neighbour/shape updates and no onPlace() reactions. */
	public static final int QUIET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_ON_PLACE;

	private BlockSettler() {
	}

	public static void settle(ServerLevel level, List<BlockPos> positions) {
		// Connection shapes first, so dust knows which way it points.
		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			if (needsShapeUpdate(state)) {
				BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
				if (updated != state && !updated.isAir()) {
					level.setBlock(pos, updated, QUIET_FLAGS);
				}
			}
		}

		// Then let dust work out its power, then everything else react to it.
		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof RedStoneWireBlock) {
				level.neighborChanged(state, pos, state.getBlock(), null, false);
			}
		}
		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			// Observers only react to shape changes, and we never want them to fire on build.
			if (!state.isAir() && !(state.getBlock() instanceof RedStoneWireBlock) && !(state.getBlock() instanceof ObserverBlock)) {
				level.neighborChanged(state, pos, state.getBlock(), null, false);
			}
		}
	}

	private static boolean needsShapeUpdate(BlockState state) {
		Block block = state.getBlock();
		return block instanceof RedStoneWireBlock
			|| block instanceof CrossCollisionBlock
			|| block instanceof WallBlock
			|| block instanceof StairBlock
			|| block instanceof TripWireBlock
			|| block instanceof FenceGateBlock;
	}
}
