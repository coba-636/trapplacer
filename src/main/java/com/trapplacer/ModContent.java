package com.trapplacer;

import com.mojang.serialization.Codec;
import com.trapplacer.item.TrapPlacerItem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModContent {
	/** Id of the trap selected on a Trap Placer. */
	public static final DataComponentType<String> SELECTED_TRAP = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		TrapPlacer.id("selected_trap"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	public static final Item TRAP_PLACER = registerItem();

	private ModContent() {
	}

	private static Item registerItem() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TrapPlacer.id("trap_placer"));
		Item item = new TrapPlacerItem(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(TRAP_PLACER));
	}
}
