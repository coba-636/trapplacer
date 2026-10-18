package com.trapplacer.build;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Picks blocks that match the terrain a trap is placed on, so the visible parts
 * of a trap blend in with the ground around it.
 */
public final class Camo {
	public enum Kind {
		/** The ground block the player clicked (grass, sand, stone...). */
		SURFACE,
		/** What is usually under the surface (dirt under grass, sandstone under sand...). */
		SUBSOIL,
		/** Like SURFACE, but always a redstone-conducting full block. */
		CONDUCTIVE_SURFACE
	}

	private static final int MAX_SEARCH_DEPTH = 4;

	private final BlockState surface;
	private final BlockState subsoil;
	private final BlockState conductive;

	private Camo(BlockState surface, BlockState subsoil, BlockState conductive) {
		this.surface = surface;
		this.subsoil = subsoil;
		this.conductive = conductive;
	}

	public static Camo defaults() {
		return new Camo(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState());
	}

	/** Looks at the clicked block (and a few blocks under it) to find the local ground material. */
	public static Camo sample(ServerLevel level, BlockPos clicked) {
		BlockState surface = null;
		for (int i = 0; i < MAX_SEARCH_DEPTH && surface == null; i++) {
			BlockPos pos = clicked.below(i);
			BlockState state = level.getBlockState(pos);
			if (isUsableGround(level, pos, state)) {
				surface = solidify(state);
			}
		}
		if (surface == null) {
			return defaults();
		}

		BlockState subsoil = subsoilFor(surface);
		BlockState conductive = surface.isRedstoneConductor(level, clicked) ? surface : subsoil;
		if (!conductive.isRedstoneConductor(level, clicked)) {
			conductive = Blocks.STONE.defaultBlockState();
		}
		return new Camo(surface, subsoil, conductive);
	}

	public BlockState get(Kind kind) {
		return switch (kind) {
			case SURFACE -> this.surface;
			case SUBSOIL -> this.subsoil;
			case CONDUCTIVE_SURFACE -> this.conductive;
		};
	}

	private static boolean isUsableGround(ServerLevel level, BlockPos pos, BlockState state) {
		return !state.isAir()
			&& !state.hasBlockEntity()
			&& state.getFluidState().isEmpty()
			&& state.isCollisionShapeFullBlock(level, pos)
			&& state.getDestroySpeed(level, pos) >= 0
			&& !state.is(BlockTags.LEAVES)
			&& !state.is(Blocks.POWDER_SNOW)
			&& !state.is(Blocks.TNT);
	}

	/** Gravity blocks would fall into the trap's pits, so swap them for a solid look-alike. */
	private static BlockState solidify(BlockState state) {
		if (!(state.getBlock() instanceof FallingBlock)) {
			return state;
		}
		Block block = state.getBlock();
		if (block == Blocks.RED_SAND) {
			return Blocks.RED_SANDSTONE.defaultBlockState();
		}
		if (state.is(BlockTags.SAND)) {
			return Blocks.SANDSTONE.defaultBlockState();
		}
		if (block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		return Blocks.STONE.defaultBlockState();
	}

	private static BlockState subsoilFor(BlockState surface) {
		if (surface.is(BlockTags.DIRT) || surface.is(Blocks.SNOW_BLOCK) || surface.is(Blocks.DIRT_PATH) || surface.is(Blocks.FARMLAND)) {
			return Blocks.DIRT.defaultBlockState();
		}
		return surface;
	}
}
