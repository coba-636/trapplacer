package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.LEFT;
import static com.trapplacer.build.BlueprintBuilder.RIGHT;

import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The hidden floor shared by the door, chest and crafting-table traps.
 *
 * <p>A 2-wide strip of floor (x = 0..1, z = zFrom..zTo) is really the heads of
 * normal pistons lying at ground level with carpet on top. The piston bases sit at
 * x = -1 and x = 2 (also carpeted). Each base is held extended by a redstone torch
 * directly underneath it. Powering the wire lines at x = -4 and x = 5 (y = -1)
 * switches every torch off: the heads snap back, the carpet pops off and whoever
 * was standing there drops onto stalagmites. When the signal ends the heads come
 * back out and seal the pit.
 *
 * <p>Callers must bring their trigger signal to (-4, -1, z) and (5, -1, z) for
 * some z in [zFrom, zTo] (or extend those wire lines).
 */
final class DropFloor {
	/** Pit floor depth: a ~15 block drop onto pointed dripstone. */
	static final int DEPTH = 16;

	private DropFloor() {
	}

	static void add(BlueprintBuilder b, int zFrom, int zTo, Block carpet) {
		int tipY = -(DEPTH - 1);

		// Pit shaft walls and floor, so caves or water next to the pit can't break it.
		b.fill(-1, -2, zFrom - 1, 2, -DEPTH - 1, zTo + 1, Camo.Kind.SUBSOIL);
		b.fillAir(0, -1, zFrom, 1, tipY + 1, zTo);

		for (int z = zFrom; z <= zTo; z++) {
			for (int x = 0; x <= 1; x++) {
				b.stalagmite(x, tipY, z);
			}

			// Left side: base at x=-1 pushes its head into x=0.
			b.piston(-1, 0, z, RIGHT, false, true);
			b.wallTorch(-1, -1, z, RIGHT, true);
			b.set(-2, -1, z, Blocks.STONE);
			b.repeater(-3, -1, z, RIGHT, 1);
			b.wire(-4, -1, z);

			// Right side: base at x=2 pushes its head into x=1.
			b.piston(2, 0, z, LEFT, false, true);
			b.wallTorch(2, -1, z, LEFT, true);
			b.set(3, -1, z, Blocks.STONE);
			b.repeater(4, -1, z, LEFT, 1);
			b.wire(5, -1, z);

			for (int x = -1; x <= 2; x++) {
				b.set(x, 1, z, carpet);
			}
		}

		// Something solid for the outer redstone to sit on.
		b.fill(-4, -2, zFrom, -3, -2, zTo, Camo.Kind.SUBSOIL);
		b.fill(4, -2, zFrom, 5, -2, zTo, Camo.Kind.SUBSOIL);
	}
}
