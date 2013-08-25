package me.RocketRat23.LavaArena;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
	LavaArena plugin;

	public Config(LavaArena instance) {
		plugin = instance;
	}

	public void load() {
		FileConfiguration config = plugin.getConfig();
		plugin.saveDefaultConfig();

		String world;
		Integer l1X, l1Y, l1Z, l2X, l2Y, l2Z, topScore;
		Boolean firstlaunch, debug;
		world = config.getString("Arena.World");
		l1X = config.getInt("Arena.L1.x");
		l1Y = config.getInt("Arena.L1.y");
		l1Z = config.getInt("Arena.L1.z");
		l2X = config.getInt("Arena.L2.x");
		l2Y = config.getInt("Arena.L2.y");
		l2Z = config.getInt("Arena.L2.z");
		topScore = config.getInt("TopScore");
		firstlaunch = config.getBoolean("FirstLaunch");
		debug = config.getBoolean("Debug");

		if (!firstlaunch) {
			plugin.world = plugin.getServer().getWorld(world);
			plugin.laFloor1 = new Location(plugin.world, l1X, l1Y, l1Z);
			plugin.laFloor2 = new Location(plugin.world, l2X, l2Y, l2Z);
			plugin.topscore = topScore;
			plugin.gameEnabled = true;
			plugin.debug = debug;
		}
	}

	public void save() {
		FileConfiguration config = plugin.getConfig();

		// List of configuration linking to variables
		if (plugin.gameEnabled) {
			config.set("Arena.L1.x", plugin.laFloor1.getBlockX());
			config.set("Arena.L1.y", plugin.laFloor1.getBlockY());
			config.set("Arena.L1.z", plugin.laFloor1.getBlockZ());
			config.set("Arena.L2.x", plugin.laFloor2.getBlockX());
			config.set("Arena.L2.y", plugin.laFloor2.getBlockY());
			config.set("Arena.L2.z", plugin.laFloor2.getBlockZ());
			config.set("Arena.World", plugin.world.getName());
			config.set("TopScore", plugin.topscore);
			config.set("FirstLaunch", false);
			config.set("Debug", plugin.debug);
			plugin.saveConfig();
		}else{
			System.out.println("[Lavaarena] Config can't be saved (arena not configured)");
		}

	}

}
