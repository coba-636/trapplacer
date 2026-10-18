package com.trapplacer.build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;

/**
 * Small DSL for writing trap blueprints in the local frame described in {@link Placement}:
 * x = right, y = up (0 is the clicked ground block), z = forward.
 */
public final class BlueprintBuilder {
	/** Canonical directions for a player facing north; rotated at placement time. */
	public static final Direction FWD = Direction.NORTH;
	public static final Direction BACK = Direction.SOUTH;
	public static final Direction LEFT = Direction.WEST;
	public static final Direction RIGHT = Direction.EAST;
	public static final Direction UP = Direction.UP;
	public static final Direction DOWN = Direction.DOWN;

	private final Map<BlockPos, BlockSpec> blocks = new LinkedHashMap<>();
	private final Map<BlockPos, List<ItemStack>> contents = new LinkedHashMap<>();
	private final Map<BlockPos, CompoundTag> blockEntityData = new LinkedHashMap<>();
	private final List<Blueprint.EntitySpec> entities = new ArrayList<>();
	private boolean exact;

	public Blueprint build() {
		return new Blueprint(new LinkedHashMap<>(this.blocks), new LinkedHashMap<>(this.contents),
			new LinkedHashMap<>(this.blockEntityData), this.entities, this.exact);
	}

	/** Place exactly as written, without re-settling the redstone afterwards (for imported schematics). */
	public BlueprintBuilder exact() {
		this.exact = true;
		return this;
	}

	/** Saved block entity data for the block at this position. */
	public BlueprintBuilder blockEntityData(int x, int y, int z, CompoundTag tag) {
		this.blockEntityData.put(new BlockPos(x, y, z), tag.copy());
		return this;
	}

	/** An entity at a local position (continuous coordinates: block (x, y, z) spans x..x+1 etc.). */
	public BlueprintBuilder entity(Vec3 pos, CompoundTag tag) {
		this.entities.add(new Blueprint.EntitySpec(pos, tag.copy()));
		return this;
	}

	/** Offset of a canonical direction in local coordinates. */
	public static BlockPos step(Direction direction) {
		return switch (direction) {
			case NORTH -> new BlockPos(0, 0, 1);
			case SOUTH -> new BlockPos(0, 0, -1);
			case EAST -> new BlockPos(1, 0, 0);
			case WEST -> new BlockPos(-1, 0, 0);
			case UP -> new BlockPos(0, 1, 0);
			case DOWN -> new BlockPos(0, -1, 0);
		};
	}

	// ---------------------------------------------------------------- basic blocks

	public BlueprintBuilder set(int x, int y, int z, BlockSpec spec) {
		BlockPos pos = new BlockPos(x, y, z);
		// Re-insert so the latest write also decides build order.
		this.blocks.remove(pos);
		this.blocks.put(pos, spec);
		if (spec.isCamo() || !(spec.state().getBlock() instanceof EntityBlock)) {
			this.contents.remove(pos);
		}
		return this;
	}

	public BlueprintBuilder set(int x, int y, int z, BlockState state) {
		return this.set(x, y, z, BlockSpec.of(state));
	}

	public BlueprintBuilder set(int x, int y, int z, Block block) {
		return this.set(x, y, z, block.defaultBlockState());
	}

	public BlueprintBuilder set(int x, int y, int z, Camo.Kind camo) {
		return this.set(x, y, z, BlockSpec.camo(camo));
	}

	public BlueprintBuilder air(int x, int y, int z) {
		return this.set(x, y, z, Blocks.AIR);
	}

	public BlueprintBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockSpec spec) {
		for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					this.set(x, y, z, spec);
				}
			}
		}
		return this;
	}

	public BlueprintBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		return this.fill(x1, y1, z1, x2, y2, z2, BlockSpec.of(state));
	}

	public BlueprintBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		return this.fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
	}

	public BlueprintBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, Camo.Kind camo) {
		return this.fill(x1, y1, z1, x2, y2, z2, BlockSpec.camo(camo));
	}

	public BlueprintBuilder fillAir(int x1, int y1, int z1, int x2, int y2, int z2) {
		return this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
	}

	/** Hollow box: only the outer shell. */
	public BlueprintBuilder walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					if (x == x1 || x == x2 || z == z1 || z == z2) {
						this.set(x, y, z, state);
					}
				}
			}
		}
		return this;
	}

	public BlueprintBuilder contents(int x, int y, int z, ItemStack... stacks) {
		List<ItemStack> list = new ArrayList<>();
		for (ItemStack stack : stacks) {
			list.add(stack.copy());
		}
		this.contents.put(new BlockPos(x, y, z), list);
		return this;
	}

	// ---------------------------------------------------------------- redstone helpers

	public BlueprintBuilder wire(int x, int y, int z) {
		return this.set(x, y, z, Blocks.REDSTONE_WIRE);
	}

	/** A repeater whose output points in {@code output}. */
	public BlueprintBuilder repeater(int x, int y, int z, Direction output, int delay) {
		return this.set(x, y, z, Blocks.REPEATER.defaultBlockState()
			.setValue(RepeaterBlock.FACING, output.getOpposite())
			.setValue(RepeaterBlock.DELAY, delay));
	}

	/** A comparator in compare mode whose output points in {@code output}. */
	public BlueprintBuilder comparator(int x, int y, int z, Direction output) {
		return this.set(x, y, z, Blocks.COMPARATOR.defaultBlockState()
			.setValue(ComparatorBlock.FACING, output.getOpposite())
			.setValue(ComparatorBlock.MODE, ComparatorMode.COMPARE));
	}

	/** A standing redstone torch (attached to the block below it). */
	public BlueprintBuilder torch(int x, int y, int z, boolean lit) {
		return this.set(x, y, z, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT, lit));
	}

	/** A wall redstone torch pointing away from its wall in {@code facing}. */
	public BlueprintBuilder wallTorch(int x, int y, int z, Direction facing, boolean lit) {
		return this.set(x, y, z, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
			.setValue(RedstoneWallTorchBlock.FACING, facing)
			.setValue(RedstoneWallTorchBlock.LIT, lit));
	}

	/** An observer watching the block in {@code watching}; it outputs on the opposite side. */
	public BlueprintBuilder observer(int x, int y, int z, Direction watching) {
		return this.set(x, y, z, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, watching));
	}

	/** A piston; when extended its head is placed in front of it as well. */
	public BlueprintBuilder piston(int x, int y, int z, Direction facing, boolean sticky, boolean extended) {
		Block base = sticky ? Blocks.STICKY_PISTON : Blocks.PISTON;
		this.set(x, y, z, base.defaultBlockState()
			.setValue(PistonBaseBlock.FACING, facing)
			.setValue(PistonBaseBlock.EXTENDED, extended));
		if (extended) {
			BlockPos head = step(facing);
			this.set(x + head.getX(), y + head.getY(), z + head.getZ(), Blocks.PISTON_HEAD.defaultBlockState()
				.setValue(PistonHeadBlock.FACING, facing)
				.setValue(PistonHeadBlock.TYPE, sticky ? PistonType.STICKY : PistonType.DEFAULT)
				.setValue(PistonHeadBlock.SHORT, false));
		}
		return this;
	}

	public BlueprintBuilder dispenser(int x, int y, int z, Direction facing, ItemStack... items) {
		this.set(x, y, z, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing));
		if (items.length > 0) {
			this.contents(x, y, z, items);
		}
		return this;
	}

	/** An upward pointed-dripstone tip sitting on a dripstone block. */
	public BlueprintBuilder stalagmite(int x, int y, int z) {
		this.set(x, y - 1, z, Blocks.DRIPSTONE_BLOCK);
		return this.set(x, y, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
			.setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP));
	}
}
