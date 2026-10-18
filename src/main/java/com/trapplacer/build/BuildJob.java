package com.trapplacer.build;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.trapplacer.trap.TrapType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Places one trap a slice at a time, bottom layer first, then switches its redstone on. */
public final class BuildJob {
	private record Step(BlockPos pos, BlockState state, int localY) {
	}

	private final ServerLevel level;
	private final TrapType trap;
	private final Placement placement;
	private final UUID owner;
	private final List<Step> steps;
	private final UndoHistory.Entry undoEntry;
	private final int perTick;
	private int next;

	private BuildJob(ServerLevel level, TrapType trap, Placement placement, UUID owner, List<Step> steps, UndoHistory.Entry undoEntry, int durationTicks) {
		this.level = level;
		this.trap = trap;
		this.placement = placement;
		this.owner = owner;
		this.steps = steps;
		this.undoEntry = undoEntry;
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

		List<UndoHistory.Snapshot> snapshot = new ArrayList<>(steps.size());
		for (Step step : steps) {
			snapshot.add(new UndoHistory.Snapshot(step.pos(), level.getBlockState(step.pos())));
		}
		UndoHistory.Entry entry = new UndoHistory.Entry(level.dimension(), trap, snapshot, new ArrayList<>());
		undo.push(owner, entry);

		return new BuildJob(level, trap, placement, owner, steps, entry, durationTicks);
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
		Blueprint blueprint = this.trap.blueprint();
		if (!blueprint.exact()) {
			List<BlockPos> positions = new ArrayList<>(this.steps.size());
			for (Step step : this.steps) {
				positions.add(step.pos());
			}
			BlockSettler.settle(this.level, positions);
		}
		this.fillContainers(blueprint);
		this.loadBlockEntityData(blueprint);
		this.spawnEntities(blueprint);
		this.scheduleTicks(blueprint);

		BlockPos center = this.steps.get(this.steps.size() - 1).pos();
		this.level.playSound(null, center, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
		ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(this.owner);
		if (player != null) {
			player.displayClientMessage(Component.translatable("message.trapplacer.built", this.trap.name(), this.steps.size()), true);
		}
	}

	private void fillContainers(Blueprint blueprint) {
		blueprint.contents().forEach((local, stacks) -> {
			BlockEntity blockEntity = this.level.getBlockEntity(this.placement.toWorld(local));
			if (blockEntity instanceof Container container) {
				container.clearContent();
				for (int slot = 0; slot < stacks.size() && slot < container.getContainerSize(); slot++) {
					container.setItem(slot, stacks.get(slot).copy());
				}
				blockEntity.setChanged();
			}
		});
	}

	private void loadBlockEntityData(Blueprint blueprint) {
		blueprint.blockEntityData().forEach((local, tag) -> {
			BlockEntity blockEntity = this.level.getBlockEntity(this.placement.toWorld(local));
			if (blockEntity != null) {
				blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, this.level.registryAccess(), tag));
				blockEntity.setChanged();
			}
		});
	}

	private void scheduleTicks(Blueprint blueprint) {
		for (Blueprint.TickSpec tick : blueprint.ticks()) {
			BlockPos pos = this.placement.toWorld(tick.pos());
			if (this.level.getBlockState(pos).is(tick.block())) {
				this.level.scheduleTick(pos, tick.block(), tick.delay(), tick.priority());
			}
		}
	}

	private void spawnEntities(Blueprint blueprint) {
		Rotation rotation = this.placement.rotation();
		for (Blueprint.EntitySpec spec : blueprint.entities()) {
			Vec3 pos = this.placement.toWorld(spec.pos());
			CompoundTag tag = spec.nbt().copy();
			tag.remove("UUID");
			EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, this.level.registryAccess(), tag), this.level, EntitySpawnReason.STRUCTURE)
				.ifPresent(entity -> {
					// Same as vanilla structure placement: turn the entity with the structure.
					float yaw = entity.rotate(rotation);
					entity.snapTo(pos.x, pos.y, pos.z, yaw, entity.getXRot());
					entity.setYBodyRot(yaw);
					entity.setYHeadRot(yaw);
					if (this.level.addFreshEntity(entity)) {
						this.undoEntry.entities().add(entity.getUUID());
					}
				});
		}
	}
}
