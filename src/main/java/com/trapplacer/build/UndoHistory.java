package com.trapplacer.build;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.trapplacer.trap.TrapType;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Remembers what each player's last few traps replaced, so they can be taken down again. */
public final class UndoHistory {
	public static final int MAX_ENTRIES_PER_PLAYER = 10;
	/** Like a quiet placement, but also skip dropping the trap's container contents (no free TNT). */
	private static final int RESTORE_FLAGS = BlockSettler.QUIET_FLAGS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

	public record Snapshot(BlockPos pos, BlockState previous) {
	}

	/** {@code entities} collects the ids of entities the trap spawned, so undo can remove them too. */
	public record Entry(ResourceKey<Level> dimension, TrapType trap, List<Snapshot> snapshot, List<UUID> entities) {
	}

	private final Map<UUID, Deque<Entry>> history = new HashMap<>();

	public synchronized void push(UUID owner, Entry entry) {
		Deque<Entry> entries = this.history.computeIfAbsent(owner, id -> new ArrayDeque<>());
		entries.push(entry);
		while (entries.size() > MAX_ENTRIES_PER_PLAYER) {
			entries.removeLast();
		}
	}

	/** Restores the most recent trap placed by {@code owner}. Returns the undone entry, if any. */
	@Nullable
	public synchronized Entry undo(MinecraftServer server, UUID owner) {
		Deque<Entry> entries = this.history.get(owner);
		if (entries == null || entries.isEmpty()) {
			return null;
		}
		Entry entry = entries.pop();
		ServerLevel level = server.getLevel(entry.dimension());
		if (level == null) {
			return entry;
		}

		for (UUID id : entry.entities()) {
			Entity entity = level.getEntity(id);
			if (entity != null) {
				entity.discard();
			}
		}

		List<BlockPos> positions = new ArrayList<>(entry.snapshot().size());
		// Top-down so nothing is left hanging mid-restore.
		for (int i = entry.snapshot().size() - 1; i >= 0; i--) {
			Snapshot snapshot = entry.snapshot().get(i);
			level.setBlock(snapshot.pos(), snapshot.previous(), RESTORE_FLAGS);
			positions.add(snapshot.pos());
		}
		BlockSettler.settle(level, positions);
		return entry;
	}

	public synchronized void clear() {
		this.history.clear();
	}
}
