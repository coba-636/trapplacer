package com.trapplacer.build;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.trapplacer.trap.TrapType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Places one trap a slice at a time, bottom layer first, then switches its redstone on. */
public final class BuildJob {
	private record Step(BlockPos pos, BlockState state, int localY) {
	}

	private final ServerLevel level;
	private final TrapType trap;
	private final UUID owner;
	private final List<Step> steps;
	private final Map<BlockPos, List<ItemStack>> contents;
	private final int perTick;
	private int next;

	private BuildJob(ServerLevel level, TrapType trap, UUID owner, List<Step> steps, Map<BlockPos, List<ItemStack>> contents, int durationTicks) {
		this.level = level;
		this.trap = trap;
		this.owner = owner;
		this.steps = steps;
		this.contents = contents;
		this.perTick = Math.max(1, (int)Math.ceil(steps.size() / (double)Math.max(1, durationTicks)));
	}

	/**
	 * Resolves the blueprint against the world and snapshots what it will replace.
	 * Nothing is changed until {@link #tick()} runs.
	 */
	public static BuildJob start(ServerLevel level, TrapType trap, Placement placement, Camo camo, UUID owner, int durationTicks, UndoHistory undo) {
		Blueprint blueprint = trap.blueprint();
		List<Step> steps = new ArrayList<>(blueprint.size());
		blueprint.blocks().forEach((local, spec) ->
			steps.add(new Step(placement.toWorld(local), placement.transform(spec.resolve(camo)), local.getY())));
		// Stable sort keeps the blueprint's own order inside each layer.
		steps.sort(Comparator.comparingInt(Step::localY));

		Map<BlockPos, List<ItemStack>> contents = new java.util.LinkedHashMap<>();
		blueprint.contents().forEach((local, stacks) -> contents.put(placement.toWorld(local), stacks));

		List<UndoHistory.Snapshot> snapshot = new ArrayList<>(steps.size());
		for (Step step : steps) {
			snapshot.add(new UndoHistory.Snapshot(step.pos(), level.getBlockState(step.pos())));
		}
		undo.push(owner, new UndoHistory.Entry(level.dimension(), trap, snapshot));

		return new BuildJob(level, trap, owner, steps, contents, durationTicks);
	}

	public UUID owner() {
		return this.owner;
	}

	/** Places the next slice. Returns true once the trap is finished. */
	public boolean tick() {
		int end = Math.min(this.steps.size(), this.next + this.perTick);
		Step last = null;
		for (int i = this.next; i < end; i++) {
			Step step = this.steps.get(i);
			this.level.setBlock(step.pos(), step.state(), BlockSettler.QUIET_FLAGS);
			if (!step.state().isAir()) {
				last = step;
			}
		}
		this.next = end;

		if (last != null) {
			SoundType sound = last.state().getSoundType();
			this.level.playSound(null, last.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, 0.6F, sound.getPitch());
			if (last.localY() >= 0) {
				this.level.sendParticles(ParticleTypes.CLOUD, last.pos().getX() + 0.5, last.pos().getY() + 1.0, last.pos().getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.01);
			}
		}

		if (this.next < this.steps.size()) {
			return false;
		}
		this.finish();
		return true;
	}

	private void finish() {
		List<BlockPos> positions = new ArrayList<>(this.steps.size());
		for (Step step : this.steps) {
			positions.add(step.pos());
		}
		BlockSettler.settle(this.level, positions);
		fillContainers(this.level, this.contents);

		BlockPos center = this.steps.get(this.steps.size() - 1).pos();
		this.level.playSound(null, center, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
		ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(this.owner);
		if (player != null) {
			player.displayClientMessage(Component.translatable("message.trapplacer.built", this.trap.name(), this.steps.size()), true);
		}
	}

	static void fillContainers(ServerLevel level, Map<BlockPos, List<ItemStack>> contents) {
		contents.forEach((pos, stacks) -> {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof Container container) {
				container.clearContent();
				for (int slot = 0; slot < stacks.size() && slot < container.getContainerSize(); slot++) {
					container.setItem(slot, stacks.get(slot).copy());
				}
				blockEntity.setChanged();
			}
		});
	}
}
