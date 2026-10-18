package com.trapplacer.trap.designs;

import static com.trapplacer.build.BlueprintBuilder.BACK;
import static com.trapplacer.build.BlueprintBuilder.DOWN;
import static com.trapplacer.build.BlueprintBuilder.FWD;
import static com.trapplacer.build.BlueprintBuilder.LEFT;
import static com.trapplacer.build.BlueprintBuilder.RIGHT;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.BlueprintBuilder;
import com.trapplacer.build.Camo;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * One-shot arrow railgun in the style of ProfessorTNT's video: TNT launches an
 * arrow so fast its damage (which scales with speed) one-shots most players.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li><b>Charge</b>: a 1x1 water shaft (x=0, z=0) with 27 dispensers around it.
 *   Every TNT they drop sinks to the same spot at the bottom (cell C at y=2, on an
 *   amethyst bud that puts it level with the arrow), so they all explode at one
 *   point and don't scatter each other. The explosion is underwater, so no blocks
 *   are destroyed.</li>
 *   <li><b>Loader</b>: a dispenser shoots an arrow straight down onto a closed
 *   iron trapdoor (cell A, two blocks in front of C), where it sticks. The top-half
 *   trapdoor S between C and A holds the TNT back but leaves a gap for the blast.</li>
 *   <li><b>Fire</b>: 82 game ticks after the button, a long repeater line opens
 *   trapdoor A. The arrow comes loose and floats in the air for a moment, and the
 *   TNT (lit 85 ticks after the button) goes off right then, launching it
 *   forward through the barrel at about 20 blocks per tick (around 40 damage).</li>
 * </ul>
 *
 * <p>Both dispenser columns are fed by a "dust staircase" bus going up beside the
 * shaft, so every dispenser receives power in the same tick.
 */
public final class ArrowRailgun {
	/** Dispenser levels; y=2 is cell C at the bottom of the shaft. */
	private static final int BOTTOM = 2;
	private static final int TOP = 14;
	private static final int TNT_PER_DISPENSER = 4;
	private static final int ARROWS = 16;

	private ArrowRailgun() {
	}

	public static Blueprint build() {
		BlueprintBuilder b = new BlueprintBuilder();
		BlockState body = body();

		// Ground under the gun, and a clear redstone layer on top of it.
		b.fill(-9, 0, -8, 6, 0, 8, Camo.Kind.SURFACE);
		b.fillAir(-9, 1, -8, 6, TOP + 2, 8);

		// Tower body (glass doesn't conduct, so stray dust can't power anything through it).
		b.fill(-3, 1, -1, 3, TOP + 1, 1, body);

		// The water shaft. C (bottom) holds a waterlogged amethyst bud: the TNT rests on
		// it at exactly the height of the loaded arrow, so the blast pushes it dead level.
		for (int y = BOTTOM + 1; y <= TOP; y++) {
			b.set(0, y, 0, Blocks.WATER);
		}
		b.set(0, BOTTOM, 0, Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState()
			.setValue(AmethystClusterBlock.FACING, Direction.UP)
			.setValue(AmethystClusterBlock.WATERLOGGED, true));
		b.set(0, TOP + 1, 0, Blocks.IRON_BLOCK);

		// Dispensers on every other level, all facing into the shaft.
		for (int y = BOTTOM; y <= TOP; y += 2) {
			b.dispenser(-1, y, 0, RIGHT, tnt());
			b.dispenser(1, y, 0, LEFT, tnt());
			b.dispenser(0, y, -1, FWD, tnt());
			if (y > BOTTOM) {
				b.dispenser(0, y, 1, BACK, tnt());
				// Front-right corner powers the right and front dispensers of this level.
				b.set(1, y, 1, Blocks.IRON_BLOCK);
			}
			// Back-left corner powers the left and back dispensers of this level.
			b.set(-1, y, -1, Blocks.IRON_BLOCK);
		}

		// Dust staircases ("buses") climbing beside the shaft. Dust on the even
		// levels points into the iron corner; glass breaks the dust's upward cut-off.
		for (int y = 1; y <= TOP; y++) {
			boolean even = y % 2 == 0;
			// Back-left bus: x=-2 on even levels, x=-3 on odd ones, z=-1.
			b.set(-2, y, -1, even ? Blocks.REDSTONE_WIRE.defaultBlockState() : Blocks.GLASS.defaultBlockState());
			b.set(-3, y, -1, even ? Blocks.GLASS.defaultBlockState() : Blocks.REDSTONE_WIRE.defaultBlockState());
			// Front-right bus: x=2 on even levels, x=3 on odd ones, z=1.
			b.set(2, y, 1, even ? Blocks.REDSTONE_WIRE.defaultBlockState() : Blocks.GLASS.defaultBlockState());
			b.set(3, y, 1, even ? Blocks.GLASS.defaultBlockState() : Blocks.REDSTONE_WIRE.defaultBlockState());
		}

		// Separator S (top-half trapdoor) and arrow cell A (bottom trapdoor, opens to the left side).
		b.set(0, BOTTOM, 1, trapdoor(Half.TOP, false));
		b.set(0, BOTTOM, 2, trapdoor(Half.BOTTOM, false));
		b.dispenser(0, BOTTOM + 1, 2, DOWN, new ItemStack(Items.ARROW, ARROWS));
		b.set(-1, BOTTOM, 2, Blocks.SMOOTH_STONE);
		b.set(1, BOTTOM, 2, Blocks.SMOOTH_STONE);

		// Barrel in front of A.
		for (int z = 3; z <= 6; z++) {
			b.air(0, BOTTOM, z);
			b.set(-1, BOTTOM, z, body);
			b.set(1, BOTTOM, z, body);
			b.set(0, BOTTOM + 1, z, body);
			b.set(-1, 1, z, body);
			b.set(1, 1, z, body);
		}

		redstone(b);
		return b.build();
	}

	private static void redstone(BlueprintBuilder b) {
		// Fire button on a post behind the gun. The post powers the dust under it.
		b.set(0, 2, -7, Blocks.POLISHED_ANDESITE);
		b.set(0, 2, -8, Blocks.STONE_BUTTON.defaultBlockState()
			.setValue(ButtonBlock.FACE, AttachFace.WALL)
			.setValue(ButtonBlock.FACING, BACK));
		b.wire(0, 1, -7);

		// Left: along z=-7 to x=-8 (delay line), and up x=-5 to the back-left bus repeater.
		for (int x = -8; x <= -1; x++) {
			b.wire(x, 1, -7);
		}
		for (int z = -6; z <= -1; z++) {
			b.wire(-5, 1, z);
		}
		b.repeater(-4, 1, -1, RIGHT, 1);

		// Right: along z=-7 and up x=5 to the front-right bus repeater.
		for (int x = 1; x <= 5; x++) {
			b.wire(x, 1, -7);
		}
		for (int z = -6; z <= 1; z++) {
			b.wire(5, 1, z);
		}
		b.repeater(4, 1, 1, LEFT, 1);

		// The bottom-right dispenser (E at C) gets its own repeater into the iron under it.
		for (int z = -6; z <= -2; z++) {
			b.wire(1, 1, z);
		}
		b.repeater(1, 1, -1, FWD, 1);
		b.set(1, 1, 0, Blocks.IRON_BLOCK);

		// Delay line: 10 repeaters at 4 ticks + the 1-tick release = 82 game ticks.
		b.repeater(-8, 1, -6, FWD, 4);
		b.wire(-8, 1, -5);
		for (int z = -4; z <= 4; z++) {
			b.repeater(-8, 1, z, FWD, 4);
		}
		b.wire(-8, 1, 5);
		b.wire(-8, 1, 6);
		for (int x = -7; x <= 0; x++) {
			b.wire(x, 1, 6);
		}
		b.wire(0, 1, 5);
		b.wire(0, 1, 4);
		b.repeater(0, 1, 3, BACK, 1);
		b.set(0, 1, 2, Blocks.IRON_BLOCK);

		// Arrow loader: branch off after the first repeater and climb a dust staircase
		// to the top of the block above the arrow dispenser. That fires the dispenser by
		// quasi-connectivity without powering the dispenser itself, which would also
		// open trapdoor A right under it and drop the arrow before the shot.
		b.wire(-7, 1, -5);
		for (int z = -4; z <= 2; z++) {
			b.wire(-7, 1, z);
		}
		b.repeater(-7, 1, -1, FWD, 1);
		for (int x = -6; x <= -4; x++) {
			b.wire(x, 1, 2);
		}
		int y = 2;
		for (int x = -3; x <= 0; x++, y++) {
			b.set(x, y - 1, 2, x == -3 || x == -2 ? Blocks.SMOOTH_STONE.defaultBlockState() : body());
			b.wire(x, y, 2);
			b.air(x - 1, y, 2);
		}
	}

	private static BlockState body() {
		return Blocks.GLASS.defaultBlockState();
	}

	private static BlockState trapdoor(Half half, boolean waterlogged) {
		return Blocks.IRON_TRAPDOOR.defaultBlockState()
			.setValue(TrapDoorBlock.FACING, RIGHT)
			.setValue(TrapDoorBlock.HALF, half)
			.setValue(TrapDoorBlock.OPEN, false)
			.setValue(TrapDoorBlock.WATERLOGGED, waterlogged);
	}

	private static ItemStack tnt() {
		return new ItemStack(Items.TNT, TNT_PER_DISPENSER);
	}
}
