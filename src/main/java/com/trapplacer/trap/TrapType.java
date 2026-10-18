package com.trapplacer.trap;

import java.util.Map;
import java.util.function.Supplier;

import com.trapplacer.build.Blueprint;
import com.trapplacer.build.MaterialCost;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** A trap the Trap Placer can build. Text lives in the lang file under {@code trap.trapplacer.<id>}. */
public final class TrapType {
	private final String id;
	private final Supplier<Blueprint> factory;
	private Blueprint blueprint;
	private Map<Item, Integer> cost;

	public TrapType(String id, Supplier<Blueprint> factory) {
		this.id = id;
		this.factory = factory;
	}

	public String id() {
		return this.id;
	}

	public Component name() {
		return Component.translatable("trap.trapplacer." + this.id);
	}

	public Component description() {
		return Component.translatable("trap.trapplacer." + this.id + ".desc");
	}

	public Component trigger() {
		return Component.translatable("trap.trapplacer." + this.id + ".trigger");
	}

	public synchronized Blueprint blueprint() {
		if (this.blueprint == null) {
			this.blueprint = this.factory.get();
		}
		return this.blueprint;
	}

	/** Items a survival player needs to build this trap (terrain-matching blocks are free). */
	public synchronized Map<Item, Integer> cost() {
		if (this.cost == null) {
			this.cost = MaterialCost.of(this.blueprint());
		}
		return this.cost;
	}
}
