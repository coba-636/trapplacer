package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.BACK;
import static com.trapplacer.build.BlueprintBuilder.DOWN;
import static com.trapplacer.build.BlueprintBuilder.RIGHT;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;

/**
 * A rocky outcrop with a little mine tunnel and a diamond ore at the end.
 * Mining the diamond trips an observer: a piston + slime block slams the tunnel
 * shut behind the miner and dispensers in the ceiling pour lava on them.
 */
public final class DiamondMineTrap {
	private static final int TUNNEL_END = 5;

	private DiamondMineTrap() {
	}

	public static Blueprint build() {
		BlueprintBuilder b = new BlueprintBuilder();
		b.fillAir(-6, 1, -2, 6, 6, 9);
		b.fill(-6, 0, -2, 6, 0, 9, Camo.Kind.SURFACE);
		b.fill(-5, 0, 0, 5, 0, 8, Blocks.STONE);

		// The outcrop, with knocked-off corners so it doesn't look like a box.
		for (int y = 1; y <= 5; y++) {
			for (int x = -5; x <= 5; x++) {
				for (int z = 0; z <= 8; z++) {
					boolean edgeX = Math.abs(x) == 5;
					boolean edgeZ = z == 0 || z == 8;
					if (y == 5 && (edgeX || edgeZ || (Math.abs(x) == 4 && (z == 1 || z == 7)))) {
						continue;
					}
					if (y == 4 && edgeX && edgeZ) {
						continue;
					}
					b.set(x, y, z, rock(x, y, z));
				}
			}
		}

		// Tunnel with the bait at the end: the eye-level ore is watched from above.
		b.fillAir(0, 1, 0, 0, 2, TUNNEL_END);
		b.fill(0, 0, 0, 0, 0, TUNNEL_END, Blocks.COBBLESTONE);
		b.set(0, 1, TUNNEL_END + 1, Blocks.STONE);
		b.set(0, 2, TUNNEL_END + 1, Blocks.DIAMOND_ORE);
		b.observer(0, 3, TUNNEL_END + 1, DOWN);
		b.set(-1, 2, -1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, BACK));
		b.set(1, 2, -1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, BACK));

		// Lava dispensers in the ceiling over where the miner stands.
		for (int z = 3; z <= TUNNEL_END; z++) {
			b.dispenser(0, 3, z, DOWN, new ItemStack(Items.LAVA_BUCKET));
		}

		// Dust on top of the ceiling carries the observer pulse forward and left.
		for (int z = 2; z <= TUNNEL_END + 1; z++) {
			b.wire(0, 4, z);
		}
		b.wire(-1, 4, 2);
		b.wire(-2, 4, 2);
		b.air(-3, 4, 2);
		b.wire(-3, 3, 2);

		// Door slam: piston pushes a slime block, which drags the block above it along,
		// so both tunnel blocks at z=2 get filled at once. Obsidian keeps the slime from
		// grabbing anything else.
		b.piston(-3, 1, 2, RIGHT, false, false);
		b.set(-2, 1, 2, Blocks.SLIME_BLOCK);
		b.set(-2, 0, 2, Blocks.OBSIDIAN);
		b.set(-2, 1, 1, Blocks.OBSIDIAN);
		b.set(-2, 1, 3, Blocks.OBSIDIAN);
		b.set(-1, 1, 2, Blocks.STONE);
		b.set(-2, 2, 2, Blocks.STONE);
		b.set(-1, 2, 2, Blocks.STONE);
		b.set(-3, 2, 2, Blocks.STONE);
		return b.build();
	}

	private static Block rock(int x, int y, int z) {
		int hash = Math.floorMod(x * 73856093 ^ y * 19349663 ^ z * 83492791, 100);
		if (hash < 55) {
			return Blocks.STONE;
		} else if (hash < 75) {
			return Blocks.ANDESITE;
		} else if (hash < 90) {
			return Blocks.COBBLESTONE;
		}
		return Blocks.MOSSY_COBBLESTONE;
	}
}
