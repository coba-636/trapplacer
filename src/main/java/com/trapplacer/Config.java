package com.trapplacer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Server settings, read from {@code config/trapplacer.properties} (created on first run). */
public final class Config {
	/** Survival players pay for the blocks and dispenser contents (terrain blocks are free). */
	public static boolean survivalRequiresMaterials = true;
	/** How long a build takes, in ticks (20 = 1 second). 0 places instantly. */
	public static int buildTicks = 40;
	/** How far away (in blocks) the wand can place a trap. */
	public static int maxReach = 64;

	private Config() {
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("trapplacer.properties");
		Properties properties = new Properties();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			} catch (IOException e) {
				TrapPlacer.LOGGER.warn("Couldn't read {}, using defaults", path, e);
			}
		}

		survivalRequiresMaterials = Boolean.parseBoolean(properties.getProperty("survivalRequiresMaterials", Boolean.toString(survivalRequiresMaterials)));
		buildTicks = parseInt(properties, "buildTicks", buildTicks, 0, 20 * 60);
		maxReach = parseInt(properties, "maxReach", maxReach, 4, 256);

		properties.setProperty("survivalRequiresMaterials", Boolean.toString(survivalRequiresMaterials));
		properties.setProperty("buildTicks", Integer.toString(buildTicks));
		properties.setProperty("maxReach", Integer.toString(maxReach));
		try (Writer writer = Files.newBufferedWriter(path)) {
			properties.store(writer, "Trap Placer settings");
		} catch (IOException e) {
			TrapPlacer.LOGGER.warn("Couldn't write {}", path, e);
		}
	}

	private static int parseInt(Properties properties, String key, int fallback, int min, int max) {
		try {
			return Math.clamp(Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim()), min, max);
		} catch (NumberFormatException e) {
			TrapPlacer.LOGGER.warn("Bad value for {} in trapplacer.properties, using {}", key, fallback);
			return fallback;
		}
	}
}
