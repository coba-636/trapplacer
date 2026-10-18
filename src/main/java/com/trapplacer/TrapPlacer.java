package com.trapplacer;

import com.trapplacer.build.BuildManager;
import com.trapplacer.command.TrapCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrapPlacer implements ModInitializer {
	public static final String MOD_ID = "trapplacer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Config.load();
		ModContent.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TrapCommand.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			BuildManager.tick();
			PreviewTicker.tick(server);
			if (ArrowTracker.ENABLED) {
				ArrowTracker.tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BuildManager.clear());
	}
}
