package com.trapplacer;

import java.util.Map;
import java.util.UUID;

import com.trapplacer.build.BuildManager;
import com.trapplacer.build.Camo;
import com.trapplacer.build.MaterialCost;
import com.trapplacer.build.Placement;
import com.trapplacer.trap.TrapType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Checks that a trap can go somewhere and then starts building it. */
public final class TrapPlacing {
	private static final int MAX_MISSING_SHOWN = 4;

	private TrapPlacing() {
	}

	/**
	 * Tries to place {@code trap} with its near edge at {@code origin}, facing {@code facing}.
	 *
	 * @param player who is placing it (pays in survival, owns the undo entry); null for the console
	 * @return null on success, otherwise the reason it can't be placed
	 */
	@Nullable
	public static Component place(ServerLevel level, @Nullable ServerPlayer player, UUID owner, TrapType trap, BlockPos origin, Direction facing, boolean free) {
		if (BuildManager.isBuilding(owner)) {
			return Component.translatable("message.trapplacer.busy");
		}

		Placement placement = new Placement(origin, facing);
		Component blocked = checkSpace(level, player, trap, placement);
		if (blocked != null) {
			return blocked;
		}

		if (player != null && !free && Config.survivalRequiresMaterials && !player.hasInfiniteMaterials()) {
			Map<Item, Integer> missing = MaterialCost.missing(player, trap.cost());
			if (!missing.isEmpty()) {
				return describeMissing(missing);
			}
			MaterialCost.take(player, trap.cost());
		}

		BuildManager.start(level, trap, placement, Camo.sample(level, origin), owner, Config.buildTicks);
		return null;
	}

	@Nullable
	private static Component checkSpace(ServerLevel level, @Nullable ServerPlayer player, TrapType trap, Placement placement) {
		if (player != null && !level.mayInteract(player, placement.origin())) {
			return Component.translatable("message.trapplacer.protected");
		}
		for (BlockPos local : trap.blueprint().blocks().keySet()) {
			BlockPos pos = placement.toWorld(local);
			if (level.isOutsideBuildHeight(pos)) {
				return Component.translatable("message.trapplacer.height");
			}
			if (!level.isLoaded(pos)) {
				return Component.translatable("message.trapplacer.unloaded");
			}
			BlockState state = level.getBlockState(pos);
			if (state.getDestroySpeed(level, pos) < 0) {
				return Component.translatable("message.trapplacer.unbreakable", state.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return null;
	}

	private static Component describeMissing(Map<Item, Integer> missing) {
		MutableComponent list = Component.empty();
		int shown = 0;
		for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
			if (shown == MAX_MISSING_SHOWN) {
				list.append(Component.translatable("message.trapplacer.missing.more", missing.size() - shown));
				break;
			}
			if (shown > 0) {
				list.append(", ");
			}
			list.append(Component.literal(entry.getValue() + "x ")).append(entry.getKey().getName());
			shown++;
		}
		return Component.translatable("message.trapplacer.missing", list.withStyle(ChatFormatting.YELLOW));
	}
}
