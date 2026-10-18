package com.trapplacer.trap;

import java.util.List;
import java.util.Optional;

import com.trapplacer.trap.designs.ArrowRailgun;
import com.trapplacer.trap.designs.BaitTraps;
import com.trapplacer.trap.designs.BedTrap;
import com.trapplacer.trap.designs.DiamondMineTrap;
import com.trapplacer.trap.designs.FrontDoorTrap;
import com.trapplacer.trap.designs.PowderSnowPit;

/** Every trap the Trap Placer can build, in the order the wand cycles through them. */
public final class Traps {
	public static final TrapType FRONT_DOOR = new TrapType("front_door", FrontDoorTrap::build);
	public static final TrapType LOOT_CHEST = new TrapType("loot_chest", BaitTraps::lootChest);
	public static final TrapType CRAFTING_TABLE = new TrapType("crafting_table", BaitTraps::craftingTable);
	public static final TrapType BED = new TrapType("bed", BedTrap::build);
	public static final TrapType DIAMOND_MINE = new TrapType("diamond_mine", DiamondMineTrap::build);
	public static final TrapType POWDER_SNOW = new TrapType("powder_snow", PowderSnowPit::build);
	public static final TrapType ARROW_RAILGUN = new TrapType("arrow_railgun", ArrowRailgun::build);

	public static final List<TrapType> ALL = List.of(
		FRONT_DOOR, LOOT_CHEST, CRAFTING_TABLE, BED, DIAMOND_MINE, POWDER_SNOW, ARROW_RAILGUN);

	private Traps() {
	}

	public static Optional<TrapType> byId(String id) {
		for (TrapType trap : ALL) {
			if (trap.id().equals(id)) {
				return Optional.of(trap);
			}
		}
		return Optional.empty();
	}

	public static TrapType next(TrapType current) {
		return ALL.get((ALL.indexOf(current) + 1) % ALL.size());
	}
}
