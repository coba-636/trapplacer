package com.trapplacer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * Debug aid for tuning the railgun: with {@code -Dtrapplacer.debugArrows=true}
 * every arrow's position and motion, and every TNT about to explode, is logged each tick.
 */
final class ArrowTracker {
	static final boolean ENABLED = Boolean.getBoolean("trapplacer.debugArrows");

	private ArrowTracker() {
	}

	static void tick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof AbstractArrow arrow) {
					Vec3 motion = arrow.getDeltaMovement();
					if (motion.lengthSqr() > 0 || arrow.tickCount < 3) {
						TrapPlacer.LOGGER.info("[arrow] tick={} pos={} speed={} motion={}",
							server.getTickCount(), format(arrow.position()), String.format("%.3f", motion.length()), format(motion));
					}
				} else if (entity instanceof PrimedTnt tnt && tnt.getFuse() <= 2) {
					TrapPlacer.LOGGER.info("[tnt] tick={} fuse={} pos={}", server.getTickCount(), tnt.getFuse(), format(tnt.position()));
				}
			}
		}
	}

	private static String format(Vec3 vec) {
		return String.format("(%.3f, %.3f, %.3f)", vec.x, vec.y, vec.z);
	}
}
