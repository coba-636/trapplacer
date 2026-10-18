package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.FWD;
import static com.trapplacer.build.BlueprintBuilder.LEFT;
import static com.trapplacer.build.BlueprintBuilder.RIGHT;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * A cosy little hut with an iron double door and pressure plates out front.
 * Stepping on a plate opens the doors like normal, but about half a second later
 * the carpet just inside the door drops away into a stalagmite pit.
 */
public final class FrontDoorTrap {
	private FrontDoorTrap() {
	}

	public static Blueprint build() {
		BlueprintBuilder b = new BlueprintBuilder();
		BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
		BlockState log = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

		// Underground: solid ground for the redstone, then the ground surface around the hut.
		b.fill(-5, -3, -1, 6, -1, 8, Camo.Kind.SUBSOIL);
		b.fill(-5, 0, -1, 6, 0, 8, Camo.Kind.SURFACE);

		// Hut floor and foundation.
		b.fill(-2, 0, 1, 3, 0, 7, Blocks.COBBLESTONE);
		b.fill(-1, 0, 2, 2, 0, 6, planks);
		// Doorstep the plates sit on (must conduct redstone).
		b.fill(-1, 0, 0, 2, 0, 0, Blocks.STONE_BRICKS);

		// Clear the build space, then walls, corner posts, roof.
		b.fillAir(-4, 1, -1, 5, 4, 8);
		b.walls(-2, 1, 1, 3, 3, 7, planks);
		for (int[] corner : new int[][] {{-2, 1}, {3, 1}, {-2, 7}, {3, 7}}) {
			b.fill(corner[0], 1, corner[1], corner[0], 3, corner[1], log);
		}
		b.fill(-3, 4, 0, 4, 4, 8, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
		b.set(-2, 2, 4, pane.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
		b.set(3, 2, 4, pane.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
		b.set(0, 2, 7, pane.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));
		b.set(1, 2, 7, pane.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));

		// Iron double door with pressure plates outside and a button inside.
		door(b, 0, DoorHingeSide.LEFT);
		door(b, 1, DoorHingeSide.RIGHT);
		b.set(0, 1, 0, Blocks.STONE_PRESSURE_PLATE);
		b.set(1, 1, 0, Blocks.STONE_PRESSURE_PLATE);
		b.set(-1, 2, 2, Blocks.STONE_BUTTON.defaultBlockState()
			.setValue(ButtonBlock.FACE, AttachFace.WALL)
			.setValue(ButtonBlock.FACING, FWD));

		// Carpet everywhere inside; the first two rows are the trap floor.
		b.fill(-1, 1, 4, 2, 1, 6, Blocks.RED_CARPET);
		DropFloor.add(b, 2, 3, Blocks.RED_CARPET);

		// Something to walk in for.
		b.set(-1, 1, 6, Blocks.CRAFTING_TABLE);
		b.set(2, 1, 6, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, FWD.getOpposite()));
		b.set(0, 3, 5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		b.set(1, 3, 5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

		// Plate signal: the block under each plate powers the dust below it, then a
		// 0.4 s repeater gives the victim time to walk through the door before the drop.
		b.wire(0, -1, 0);
		b.wire(1, -1, 0);
		b.repeater(-1, -1, 0, LEFT, 4);
		b.wire(-2, -1, 0);
		b.wire(-3, -1, 0);
		b.wire(-4, -1, 0);
		b.wire(-4, -1, 1);
		b.repeater(2, -1, 0, RIGHT, 4);
		b.wire(3, -1, 0);
		b.wire(4, -1, 0);
		b.wire(5, -1, 0);
		b.wire(5, -1, 1);
		return b.build();
	}

	private static void door(BlueprintBuilder b, int x, DoorHingeSide hinge) {
		BlockState door = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(DoorBlock.FACING, FWD)
			.setValue(DoorBlock.HINGE, hinge);
		b.set(x, 1, 1, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b.set(x, 2, 1, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}
}
