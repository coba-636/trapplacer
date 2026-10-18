package com.trapplacer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.trapplacer.build.BuildManager;
import com.trapplacer.build.Placement;
import com.trapplacer.item.TrapPlacerItem;
import com.trapplacer.trap.TrapType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * While a player holds a Trap Placer, outlines (with particles only they can see)
 * the ground the selected trap would cover, and shows its name above the hotbar.
 */
public final class PreviewTicker {
	private static final int INTERVAL = 5;
	private static final DustParticleOptions OUTLINE = new DustParticleOptions(0xFF3030, 1.0F);
	private static final DustParticleOptions FRONT = new DustParticleOptions(0xFFD030, 1.2F);

	/** Players whose own messages (built, missing items...) shouldn't be covered up yet. */
	private static final Map<UUID, Integer> QUIET_UNTIL = new HashMap<>();

	private PreviewTicker() {
	}

	/** Keep the preview from overwriting the action bar for a few seconds. */
	public static void hold(ServerPlayer player) {
		QUIET_UNTIL.put(player.getUUID(), player.level().getServer().getTickCount() + 60);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0) {
			return;
		}
		QUIET_UNTIL.values().removeIf(until -> until < server.getTickCount());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ItemStack stack = player.getMainHandItem().is(ModContent.TRAP_PLACER) ? player.getMainHandItem() : player.getOffhandItem();
			if (stack.is(ModContent.TRAP_PLACER) && !player.isSpectator()) {
				show(player, TrapPlacerItem.selected(stack));
			}
		}
	}

	private static void show(ServerPlayer player, TrapType trap) {
		if (!QUIET_UNTIL.containsKey(player.getUUID()) && !BuildManager.isBuilding(player.getUUID())) {
			player.displayClientMessage(Component.translatable("message.trapplacer.preview", trap.name()), true);
		}
		HitResult hit = player.pick(Config.maxReach, 1.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			return;
		}

		Placement placement = new Placement(blockHit.getBlockPos(), player.getDirection());
		BoundingBox local = trap.blueprint().bounds();
		BlockPos[] cells = {
			placement.toWorld(new BlockPos(local.minX(), 0, local.minZ())),
			placement.toWorld(new BlockPos(local.maxX(), 0, local.minZ())),
			placement.toWorld(new BlockPos(local.maxX(), 0, local.maxZ())),
			placement.toWorld(new BlockPos(local.minX(), 0, local.maxZ()))
		};
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPos cell : cells) {
			minX = Math.min(minX, cell.getX());
			minZ = Math.min(minZ, cell.getZ());
			maxX = Math.max(maxX, cell.getX());
			maxZ = Math.max(maxZ, cell.getZ());
		}
		// Outer corner of each corner cell, sitting on top of the ground.
		double y = blockHit.getBlockPos().getY() + 1.05;
		Vec3[] corners = new Vec3[4];
		for (int i = 0; i < 4; i++) {
			BlockPos cell = cells[i];
			corners[i] = new Vec3(cell.getX() == minX ? minX : maxX + 1, y, cell.getZ() == minZ ? minZ : maxZ + 1);
		}

		ServerLevel level = player.level();
		line(level, player, corners[0], corners[1], OUTLINE);
		line(level, player, corners[1], corners[2], OUTLINE);
		// The far edge (the way the trap faces) in yellow.
		line(level, player, corners[2], corners[3], FRONT);
		line(level, player, corners[3], corners[0], OUTLINE);
	}

	private static void line(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to, DustParticleOptions particle) {
		double length = from.distanceTo(to);
		int steps = Math.max(1, (int)Math.ceil(length * 2));
		for (int i = 0; i <= steps; i++) {
			Vec3 point = from.lerp(to, i / (double)steps);
			level.sendParticles(player, particle, true, false, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
