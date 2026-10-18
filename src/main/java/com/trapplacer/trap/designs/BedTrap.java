package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.BACK;
import static com.trapplacer.build.BlueprintBuilder.FWD;
import static com.trapplacer.build.BlueprintBuilder.RIGHT;
import static com.trapplacer.build.BlueprintBuilder.UP;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * A tiny cabin with a bed. Lying down flips the bed's "occupied" state, an
 * observer under the bed sees it and lights a whole floor of TNT underneath.
 */
public final class BedTrap {
	private BedTrap() {
	}

	public static Blueprint build() {
		BlueprintBuilder b = new BlueprintBuilder();
		BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

		b.fill(-3, -4, -1, 3, -1, 6, Camo.Kind.SUBSOIL);
		b.fill(-4, 0, -2, 4, 0, 7, Camo.Kind.SURFACE);
		b.fillAir(-3, 1, -2, 3, 4, 7);

		// Cabin: floor, walls, corner logs, slab roof.
		b.fill(-2, 0, 0, 2, 0, 5, Blocks.COBBLESTONE);
		b.fill(-1, 0, 1, 1, 0, 4, planks);
		b.walls(-2, 1, 0, 2, 3, 5, planks);
		for (int[] corner : new int[][] {{-2, 0}, {2, 0}, {-2, 5}, {2, 5}}) {
			b.fill(corner[0], 1, corner[1], corner[0], 3, corner[1], log);
		}
		b.fill(-3, 4, -1, 3, 4, 6, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
		b.set(-2, 2, 2, pane.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
		b.set(2, 2, 2, pane.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));

		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, FWD).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		b.set(0, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b.set(0, 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

		// The bed (foot -> head points forward) and some homely bits.
		BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, FWD);
		b.set(0, 1, 2, bed.setValue(BedBlock.PART, BedPart.FOOT));
		b.set(0, 1, 3, bed.setValue(BedBlock.PART, BedPart.HEAD));
		b.set(-1, 1, 4, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, RIGHT));
		b.set(1, 1, 4, Blocks.CRAFTING_TABLE);
		b.set(-1, 2, 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, FWD));
		b.set(1, 2, 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, FWD));
		b.set(-1, 2, 4, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, BACK));
		b.set(1, 2, 4, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, BACK));

		// Observer watching the bed head; it fires straight down into a dust mat. The
		// dust sits on stone rather than on the TNT (otherwise the first TNT to light
		// takes its dust with it and cuts the circuit); the stone passes the signal down.
		b.observer(0, 0, 3, UP);
		for (int x = -1; x <= 1; x++) {
			for (int z = 1; z <= 4; z++) {
				b.set(x, -3, z, Blocks.TNT);
				b.set(x, -2, z, Blocks.STONE);
				b.wire(x, -1, z);
			}
		}
		return b.build();
	}
}
