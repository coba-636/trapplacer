package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.BACK;
import static com.trapplacer.build.BlueprintBuilder.FWD;
import static com.trapplacer.build.BlueprintBuilder.UP;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Outdoor bait traps: something a player wants (a loot chest, a crafting setup)
 * on a mossy patch whose middle is the hidden {@link DropFloor}.
 *
 * <p>Layout: drop floor rows z = 1..2 (x = 0..1), the bait sits at z = 3 right
 * behind them, so whoever uses it is standing on the trap.
 */
public final class BaitTraps {
	private BaitTraps() {
	}

	/** Opening the (trapped) double chest drops the floor in front of it. */
	public static Blueprint lootChest() {
		BlueprintBuilder b = mossPatch();

		b.set(0, 1, 3, Blocks.TRAPPED_CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, BACK)
			.setValue(ChestBlock.TYPE, ChestType.RIGHT));
		b.set(1, 1, 3, Blocks.TRAPPED_CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, BACK)
			.setValue(ChestBlock.TYPE, ChestType.LEFT));

		// The chest strongly powers the moss under it, which powers the dust below at
		// strength 1 (one viewer). Repeaters right away bring it back up to 15.
		b.wire(0, -1, 3);
		b.wire(1, -1, 3);
		b.repeater(0, -1, 4, FWD, 1);
		b.repeater(1, -1, 4, FWD, 1);
		feed(b);
		return b.build();
	}

	/**
	 * Breaking the crafting table, or starting to smelt in the furnace, flips a copper
	 * bulb latch that opens the floor. It stays open until the next change (e.g. the
	 * owner putting the crafting table back), which closes it again.
	 */
	public static Blueprint craftingTable() {
		BlueprintBuilder b = mossPatch();

		b.set(0, 1, 3, Blocks.CRAFTING_TABLE);
		b.set(1, 1, 3, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, BACK));
		b.set(-1, 1, 3, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, UP));
		b.set(-1, 2, 3, Blocks.LANTERN);

		// Observers under the bait fire downwards into the latch.
		b.observer(0, 0, 3, UP);
		b.observer(1, 0, 3, UP);
		b.set(0, -1, 3, Blocks.WAXED_COPPER_BULB);
		b.set(1, -1, 3, Blocks.STONE);
		b.comparator(0, -1, 4, FWD);
		b.set(1, -1, 4, Blocks.STONE);
		feed(b);
		return b.build();
	}

	private static BlueprintBuilder mossPatch() {
		BlueprintBuilder b = new BlueprintBuilder();
		b.fill(-5, -3, -1, 6, -1, 6, Camo.Kind.SUBSOIL);
		b.fill(-5, 0, -1, 6, 0, 6, Camo.Kind.SURFACE);
		b.fillAir(-3, 1, -1, 4, 3, 5);

		b.fill(-2, 0, 0, 3, 0, 4, Blocks.MOSS_BLOCK);
		b.fill(-2, 1, 0, 3, 1, 3, Blocks.MOSS_CARPET);
		b.fill(-1, 1, 4, 2, 1, 4, Blocks.MOSS_CARPET);
		DropFloor.add(b, 1, 2, Blocks.MOSS_CARPET);
		return b;
	}

	/** Carries the bait's signal from (0..1, -1, 5) round to both sides of the drop floor. */
	private static void feed(BlueprintBuilder b) {
		for (int x = -4; x <= 5; x++) {
			b.wire(x, -1, 5);
		}
		for (int z = 3; z <= 4; z++) {
			b.wire(-4, -1, z);
			b.wire(5, -1, z);
		}
	}
}
