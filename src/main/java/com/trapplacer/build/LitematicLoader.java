package com.trapplacer.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;

/**
 * Turns a Litematica schematic (.litematic) bundled with the mod into a {@link Blueprint}.
 *
 * <p>The schematic is turned so that its {@code forward} side (the way it fires)
 * points the way the player looks, then sits on the clicked ground block with its
 * back edge centred there. Blocks, block entity data (dispenser contents...),
 * entities and pending block ticks are all kept, and it's pasted exactly as saved.
 */
public final class LitematicLoader {
	private record Block(BlockPos pos, BlockState state) {
	}

	private record EntityData(Vec3 pos, CompoundTag nbt) {
	}

	private record TickData(BlockPos pos, net.minecraft.world.level.block.Block block, int delay, TickPriority priority) {
	}

	private LitematicLoader() {
	}

	public static Blueprint load(String resource, Direction forward) {
		CompoundTag root;
		try (InputStream in = LitematicLoader.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Missing schematic " + resource);
			}
			root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't read schematic " + resource, e);
		}

		// Turn the schematic so `forward` becomes north (the canonical "forward" of a blueprint).
		Rotation rotation = switch (forward) {
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_180;
			case WEST -> Rotation.CLOCKWISE_90;
			default -> Rotation.NONE;
		};

		List<Block> blocks = new ArrayList<>();
		Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();
		List<EntityData> entities = new ArrayList<>();
		List<TickData> ticks = new ArrayList<>();
		CompoundTag regions = root.getCompoundOrEmpty("Regions");
		for (String name : regions.keySet()) {
			readRegion(regions.getCompoundOrEmpty(name), rotation, blocks, blockEntities, entities, ticks);
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		for (Block block : blocks) {
			minX = Math.min(minX, block.pos().getX());
			minY = Math.min(minY, block.pos().getY());
			minZ = Math.min(minZ, block.pos().getZ());
			maxX = Math.max(maxX, block.pos().getX());
			maxZ = Math.max(maxZ, block.pos().getZ());
		}
		// Local frame: x centred, schematic's bottom layer one above the ground, back (south) edge at z=0.
		int centerX = (minX + maxX) / 2;

		BlueprintBuilder b = new BlueprintBuilder().exact();
		b.fill(minX - centerX, 0, 0, maxX - centerX, 0, maxZ - minZ, Camo.Kind.SURFACE);
		for (Block block : blocks) {
			BlockPos p = block.pos();
			b.set(p.getX() - centerX, p.getY() - minY + 1, maxZ - p.getZ(), block.state());
		}
		final int cx = centerX;
		final int my = minY;
		final int mz = maxZ;
		blockEntities.forEach((p, tag) -> b.blockEntityData(p.getX() - cx, p.getY() - my + 1, mz - p.getZ(), tag));
		for (EntityData entity : entities) {
			Vec3 p = entity.pos();
			b.entity(new Vec3(p.x - cx, p.y - my + 1, (mz + 1) - p.z), entity.nbt());
		}
		for (TickData tick : ticks) {
			BlockPos p = tick.pos();
			b.scheduledTick(p.getX() - cx, p.getY() - my + 1, mz - p.getZ(), tick.block(), tick.delay(), tick.priority());
		}
		return b.build();
	}

	private static void readRegion(CompoundTag region, Rotation rotation, List<Block> blocks, Map<BlockPos, CompoundTag> blockEntities,
			List<EntityData> entities, List<TickData> ticks) {
		CompoundTag position = region.getCompoundOrEmpty("Position");
		CompoundTag size = region.getCompoundOrEmpty("Size");
		int px = position.getIntOr("x", 0);
		int py = position.getIntOr("y", 0);
		int pz = position.getIntOr("z", 0);
		int sx = size.getIntOr("x", 0);
		int sy = size.getIntOr("y", 0);
		int sz = size.getIntOr("z", 0);
		// A negative size means the region extends the other way from its position.
		int minX = sx < 0 ? px + sx + 1 : px;
		int minY = sy < 0 ? py + sy + 1 : py;
		int minZ = sz < 0 ? pz + sz + 1 : pz;
		int width = Math.abs(sx);
		int height = Math.abs(sy);
		int length = Math.abs(sz);

		ListTag paletteTag = region.getListOrEmpty("BlockStatePalette");
		BlockState[] palette = new BlockState[paletteTag.size()];
		for (int i = 0; i < palette.length; i++) {
			palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, paletteTag.getCompoundOrEmpty(i)).rotate(rotation);
		}

		// Litematica packs the palette indices tightly, so one entry may straddle two longs.
		long[] packed = region.getLongArray("BlockStates").orElse(new long[0]);
		int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.length - 1)));
		long mask = (1L << bits) - 1;
		int volume = width * height * length;
		for (int i = 0; i < volume; i++) {
			long start = (long)i * bits;
			int index = (int)(start >> 6);
			int offset = (int)(start & 63);
			int endIndex = (int)((start + bits - 1) >> 6);
			long value = packed[index] >>> offset;
			if (endIndex != index) {
				value |= packed[endIndex] << (64 - offset);
			}
			int paletteIndex = (int)(value & mask);
			int y = i / (width * length);
			int rest = i % (width * length);
			int z = rest / width;
			int x = rest % width;
			blocks.add(new Block(new BlockPos(minX + x, minY + y, minZ + z).rotate(rotation), palette[paletteIndex]));
		}

		ListTag tileEntities = region.getListOrEmpty("TileEntities");
		for (int i = 0; i < tileEntities.size(); i++) {
			CompoundTag tag = tileEntities.getCompoundOrEmpty(i);
			BlockPos pos = new BlockPos(minX + tag.getIntOr("x", 0), minY + tag.getIntOr("y", 0), minZ + tag.getIntOr("z", 0));
			blockEntities.put(pos.rotate(rotation), tag);
		}

		// Updates that were due when the schematic was saved, e.g. a running observer clock.
		ListTag tickList = region.getListOrEmpty("PendingBlockTicks");
		for (int i = 0; i < tickList.size(); i++) {
			CompoundTag tag = tickList.getCompoundOrEmpty(i);
			Identifier id = Identifier.tryParse(tag.getStringOr("Block", ""));
			if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
				continue;
			}
			BlockPos pos = new BlockPos(minX + tag.getIntOr("x", 0), minY + tag.getIntOr("y", 0), minZ + tag.getIntOr("z", 0));
			ticks.add(new TickData(pos.rotate(rotation), BuiltInRegistries.BLOCK.getValue(id),
				Math.max(1, tag.getIntOr("Time", 1)), TickPriority.byValue(tag.getIntOr("Priority", 0))));
		}

		// Entity positions are relative to the region's Position, not its corner.
		ListTag entityList = region.getListOrEmpty("Entities");
		for (int i = 0; i < entityList.size(); i++) {
			CompoundTag tag = entityList.getCompoundOrEmpty(i).copy();
			ListTag pos = tag.getListOrEmpty("Pos");
			Vec3 abs = new Vec3(px + pos.getDoubleOr(0, 0), py + pos.getDoubleOr(1, 0), pz + pos.getDoubleOr(2, 0));
			entities.add(new EntityData(rotate(abs, rotation), turnYaw(tag, rotation)));
		}
	}

	/** Rotates a point the same way BlockPos.rotate turns the block it's in. */
	private static Vec3 rotate(Vec3 p, Rotation rotation) {
		double x = p.x - 0.5;
		double z = p.z - 0.5;
		return switch (rotation) {
			case CLOCKWISE_90 -> new Vec3(-z + 0.5, p.y, x + 0.5);
			case CLOCKWISE_180 -> new Vec3(-x + 0.5, p.y, -z + 0.5);
			case COUNTERCLOCKWISE_90 -> new Vec3(z + 0.5, p.y, -x + 0.5);
			case NONE -> p;
		};
	}

	private static CompoundTag turnYaw(CompoundTag tag, Rotation rotation) {
		ListTag angles = tag.getListOrEmpty("Rotation");
		if (rotation == Rotation.NONE || angles.size() < 2) {
			return tag;
		}
		float turn = switch (rotation) {
			case CLOCKWISE_90 -> 90.0F;
			case CLOCKWISE_180 -> 180.0F;
			case COUNTERCLOCKWISE_90 -> 270.0F;
			case NONE -> 0.0F;
		};
		ListTag turned = new ListTag();
		turned.add(FloatTag.valueOf(angles.getFloatOr(0, 0.0F) + turn));
		turned.add(FloatTag.valueOf(angles.getFloatOr(1, 0.0F)));
		tag.put("Rotation", turned);
		return tag;
	}
}
