package com.trapplacer.build;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.trapplacer.trap.TrapType;

import net.minecraft.server.level.ServerLevel;

/** Runs the in-progress trap builds, a few blocks per server tick. */
public final class BuildManager {
	private static final List<BuildJob> JOBS = new ArrayList<>();
	private static final UndoHistory UNDO = new UndoHistory();

	private BuildManager() {
	}

	public static UndoHistory undoHistory() {
		return UNDO;
	}

	public static boolean isBuilding(UUID owner) {
		for (BuildJob job : JOBS) {
			if (job.owner().equals(owner)) {
				return true;
			}
		}
		return false;
	}

	public static void start(ServerLevel level, TrapType trap, Placement placement, Camo camo, UUID owner, int durationTicks) {
		BuildJob job = BuildJob.start(level, trap, placement, camo, owner, durationTicks, UNDO);
		if (durationTicks <= 0) {
			// Instant: finish in this tick.
			while (!job.tick()) {
				// keep placing
			}
			return;
		}
		JOBS.add(job);
	}

	/** Finishes any build still running for {@code owner} straight away (used before undo). */
	public static void finishNow(UUID owner) {
		Iterator<BuildJob> iterator = JOBS.iterator();
		while (iterator.hasNext()) {
			BuildJob job = iterator.next();
			if (job.owner().equals(owner)) {
				while (!job.tick()) {
					// keep placing
				}
				iterator.remove();
			}
		}
	}

	public static void tick() {
		if (JOBS.isEmpty()) {
			return;
		}
		// Copy so a job finishing can't trip over list changes.
		List<BuildJob> running = new ArrayList<>(JOBS);
		for (BuildJob job : running) {
			if (job.tick()) {
				JOBS.remove(job);
			}
		}
	}

	public static void clear() {
		JOBS.clear();
		UNDO.clear();
	}
}
