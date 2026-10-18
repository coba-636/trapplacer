package com.trapplacer.trap.designs;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.world.level.block.Blocks;

/**
 * No redstone at all: a 3x3 patch of powder snow (looks just like snow) over a
 * deep shaft. Anyone without leather boots sinks straight through onto stalagmites.
 */
public final class PowderSnowPit {
	private static final int TIP_Y = -14;

	private PowderSnowPit() {
	}

	public static Blueprint build() {
		BlueprintBuilder b = new BlueprintBuilder();
		b.fillAir(-2, 1, -1, 2, 2, 3);
		b.fill(-2, 0, -1, 2, 0, 3, Camo.Kind.SURFACE);
		b.fill(-2, -1, -1, 2, TIP_Y - 2, 3, Camo.Kind.SUBSOIL);
		b.fillAir(-1, -1, 0, 1, TIP_Y + 1, 2);
		for (int x = -1; x <= 1; x++) {
			for (int z = 0; z <= 2; z++) {
				b.stalagmite(x, TIP_Y, z);
			}
		}
		b.fill(-1, 0, 0, 1, 0, 2, Blocks.POWDER_SNOW);
		return b.build();
	}
}
