package me.RocketRat23.LavaArena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class LavaArena extends JavaPlugin {

	Listeners ls = new Listeners(this);
	Config config = new Config(this);
	Location laFloor1, laFloor2;
	ArrayList<Player> players = new ArrayList<Player>();
	HashMap<Player, Player> killers = new HashMap<Player, Player>();
	World world;
	Boolean gameEnabled, debug, generateRequired = false;
	Integer topscore = 0;

	Scoreboard scoreboard;
	Objective points;
	Objective deaths;

	ArrayList<Location> blockToChangeDiamond = new ArrayList<Location>();
	ArrayList<Location> blockToChangeTNT = new ArrayList<Location>();
	ArrayList<BukkitTask> bt = new ArrayList<BukkitTask>();

	public void onEnable() {
		config.load();
		getServer().getPluginManager().registerEvents(ls, this);

		ScoreboardManager sbManager = Bukkit.getScoreboardManager();
		scoreboard = sbManager.getNewScoreboard();
		points = scoreboard.registerNewObjective("laPoints", "dummy");
		deaths = scoreboard.registerNewObjective("laDeaths", "deathCount");
		points.setDisplaySlot(DisplaySlot.SIDEBAR);
		deaths.setDisplaySlot(DisplaySlot.BELOW_NAME);
		points.setDisplayName(ChatColor.GREEN + "Points");
		deaths.setDisplayName(ChatColor.RED + "Deaths");
		points.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "/la leave")).setScore(-1);
		;
		deaths.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "/la leave")).setScore(-1);
		;

		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if (generateRequired && gameEnabled) {
					generateFloor();
					if (debug) {
						System.out.println("[LavaArena] Generating floor (delayed task)");
					}
					generateRequired = false;
				} else {
					if (debug) {
						System.out.println("[LavaArena] No generation required (delayed task");
					}
				}
			}
		}, 0, 36000L);
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if (gameEnabled && players.size() > 0) {
					if (points.getDisplaySlot() == null) {
						points.setDisplaySlot(DisplaySlot.SIDEBAR);
						deaths.setDisplaySlot(null);
					} else {
						points.setDisplaySlot(null);
						deaths.setDisplaySlot(DisplaySlot.SIDEBAR);
					}
				}
			}
		}, 0, 100L);
	}

	public void onDisable() {
		config.save();
		while (players.size() != 0) {
			onLeave(players.get(0), true, true);
		}
	}

	public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args.length != 0) {
				if (player.hasPermission("lavaarena." + args[0].toLowerCase())) {
					switch (args[0].toLowerCase()) {
					case "set1":
						laFloor1 = player.getLocation();
						world = player.getWorld();
						if (laFloor2 != null) {
							gameEnabled = true;
						}
						break;
					case "set2":
						laFloor2 = player.getLocation();
						world = player.getWorld();
						if (laFloor1 != null) {
							gameEnabled = true;
						}
						break;
					case "generate":
						if (gameEnabled) {
							generateFloor();
						} else {
							player.sendMessage(ChatColor.RED + "Arena has not been set");
						}
						break;
					case "save":
						config.save();
						break;
					case "load":
						config.load();
						break;
					case "join":
						if (gameEnabled) {
							onJoin((Player) sender);
						} else {
							player.sendMessage(ChatColor.RED + "Arena has not been set");
						}
						break;
					case "leave":
						if (gameEnabled) {
							onLeave(player, false, true);
						} else {
							player.sendMessage(ChatColor.RED + "Arena has not been set");
						}
						break;
					case "kick":
						if (gameEnabled) {
							if (args.length == 2) {
								onLeave(Bukkit.getPlayer(args[1]), false, true);
							} else {
								player.sendMessage(ChatColor.RED + "/lavarena kick <player>");
							}
						} else {
							player.sendMessage(ChatColor.RED + "Arena has not been set");
						}
						break;
					default:
						if (player.hasPermission("lavaarena.*")) {
							player.sendMessage(ChatColor.RED + "/lavaarena <join|leave|set1|set2|generate|save|load>");
						} else {
							player.sendMessage(ChatColor.RED + "/lavaarena <join|leave>");
						}
						break;
					}
				} else {
					player.sendMessage(ChatColor.RED + "You do not have permission for that command");
				}
			} else {
				if (player.hasPermission("lavaarena.*")) {
					player.sendMessage(ChatColor.RED + "/lavaarena <join|leave|set1|set2|generate|save|load>");
				} else {
					player.sendMessage(ChatColor.RED + "/lavaarena <join|leave>");
				}
			}
		} else {
			if (args.length != 0) {
				switch (args[0].toLowerCase()) {
				case "generate":
					if (gameEnabled) {
						generateFloor();
					} else {
						System.out.println(ChatColor.RED + "Arena has not been set");
					}
					break;
				case "save":
					config.save();
					System.out.println("Config saved");
					break;
				case "load":
					config.load();
					System.out.println("Config loaded");
					break;
				case "kick":
					if (gameEnabled) {
						if (args.length == 2) {
							sendMessageToGame("[LavaArena] " + Bukkit.getPlayer(args[1]).getName() + " has been kicked from the game");
							onLeave(Bukkit.getPlayer(args[1]), true, true);
						} else {
							System.out.println(ChatColor.RED + "/lavarena kick <player>");
						}
					} else {
						System.out.println(ChatColor.RED + "Arena has not been set");
					}
					break;
				case "join":
					if (gameEnabled) {
						if (args.length == 2) {
							sendMessageToGame("[LavaArena] " + Bukkit.getPlayer(args[1]).getName() + " has been kicked from the game");
							onLeave(Bukkit.getPlayer(args[1]), true, true);
						} else {
							System.out.println(ChatColor.RED + "/lavarena kick <player>");
						}
					} else {
						System.out.println(ChatColor.RED + "Arena has not been set");
					}
					break;
				default:
					System.out.println(ChatColor.RED + "Must be a player to run that command!");
					break;
				}
			} else {
				System.out.println(ChatColor.RED + "Must be a player to run that command!");
			}
		}
		return false;
	}

	public void onJoin(Player player) {
		if (gameEnabled) {
			PlayerInventory invent = player.getInventory();
			ItemStack bow = new ItemStack(Material.BOW, 1);
			ItemStack arrow = new ItemStack(Material.ARROW, 1);
			bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
			bow.addEnchantment(Enchantment.DURABILITY, 3);

			if (players.contains(player) == false) {
				players.add(player);
				killers.put(player, null);
				player.teleport(spawnLocation());
				if (player.getGameMode() != GameMode.ADVENTURE) {
					player.setGameMode(GameMode.ADVENTURE);
				}
				sendMessageToGame("[LavaArena] " + player.getName() + " has joined the game");
				player.setScoreboard(scoreboard);
				points.getScore(player).setScore(-1);
				points.getScore(player).setScore(0);
				deaths.getScore(player).setScore(-1);
				deaths.getScore(player).setScore(0);
			}
			invent.clear();
			invent.addItem(bow);
			invent.addItem(arrow);
			generateRequired = true;
		}
	}

	public void onLeave(Player player, Boolean q, Boolean toSpawn) {
		if (gameEnabled) {
			PlayerInventory pi = player.getInventory();
			if (toSpawn || !player.hasPermission("lavaarena.boundtoarena")) {
				if (player.getGameMode() != GameMode.ADVENTURE) {
					player.setGameMode(GameMode.ADVENTURE);
				}
				player.teleport(world.getSpawnLocation());
				pi.clear();
			}
			if (!q) {
				sendMessageToGame("[LavaArena] " + player.getName() + " has left the game");
			}
			players.remove(player);
			killers.remove(player);
			player.sendMessage(ChatColor.GREEN + "[LavaArena] Kills: " + points.getScore(player).getScore() + " Deaths: " + deaths.getScore(player).getScore());
			if (points.getScore(player).getScore() > topscore) {
				Bukkit.broadcastMessage(ChatColor.GOLD + "[LavaArena] " + player.getName() + " has beaten the record of " + topscore + " with a score of " + points.getScore(player).getScore());
				topscore = points.getScore(player).getScore();
				config.save();
			}
			scoreboard.resetScores(player);
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
	}

	public void arrowHitBlock(Location location, Player player) {
		if (blockInRange(location, 0) && !player.isDead()) {
			if (location.getBlock().getType() == Material.DIAMOND_BLOCK) {
				player.setHealth(20);
				player.teleport(new Location(player.getWorld(), location.getX() + 0.5, location.getY() + 2, location.getZ() + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
				player.setFireTicks(0);
				location.getBlock().setType(Material.SMOOTH_BRICK);
				blockToChangeDiamond.add(location);
				bt.add(getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
					public void run() {
						blockToChangeDiamond.get(0).getBlock().setType(Material.DIAMOND_BLOCK);
						blockToChangeDiamond.remove(0);
						bt.remove(0);
					}
				}, 1200L));
			} else if (location.getBlock().getType() == Material.GOLD_BLOCK) {
				location.getWorld().createExplosion(location.getX(), location.getY() + 1, location.getZ(), 10, false, false);
				location.getBlock().setType(Material.SMOOTH_BRICK);
				blockToChangeTNT.add(location);
				bt.add(getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
					public void run() {
						blockToChangeTNT.get(0).getBlock().setType(Material.GOLD_BLOCK);
						blockToChangeTNT.remove(0);
						bt.remove(0);
					}
				}, 1200L));
			}
		}
	}

	public Boolean blockInRange(Location bl, int offset) {
		int mix, max, miz, maz, miy, may;
		Boolean result = true;

		if (laFloor1.getBlockX() < laFloor2.getBlockX()) {
			mix = laFloor1.getBlockX();
			max = laFloor2.getBlockX();
		} else {
			mix = laFloor2.getBlockX();
			max = laFloor1.getBlockX();
		}
		if (laFloor1.getBlockZ() < laFloor2.getBlockZ()) {
			miz = laFloor1.getBlockZ();
			maz = laFloor2.getBlockZ();
		} else {
			miz = laFloor2.getBlockZ();
			maz = laFloor1.getBlockZ();
		}

		miy = laFloor1.getBlockY() - offset;
		may = laFloor1.getBlockY() + offset;

		mix -= offset;
		max += offset;
		miz -= offset;
		maz += offset;

		if (!(mix <= bl.getBlockX() && bl.getBlockX() <= max)) {
			result = false;
		}
		if (!(miz <= bl.getBlockZ() && bl.getBlockZ() <= maz)) {
			result = false;
		}
		if (!(miy <= bl.getBlockY() && bl.getBlockY() <= may)) {
			result = false;
		}
		if (laFloor1.getWorld() != bl.getWorld()) {
			result = false;
		}
		return result;
	}

	public void generateFloor() {
		final Random rand = new Random();
		final int mix;
		final int max;
		final int miz;
		final int maz;
		cancelTasks();
		if (laFloor1.getBlockX() < laFloor2.getBlockX()) {
			mix = laFloor1.getBlockX();
			max = laFloor2.getBlockX();
		} else {
			mix = laFloor2.getBlockX();
			max = laFloor1.getBlockX();
		}
		if (laFloor1.getBlockZ() < laFloor2.getBlockZ()) {
			miz = laFloor1.getBlockZ();
			maz = laFloor2.getBlockZ();
		} else {
			miz = laFloor2.getBlockZ();
			maz = laFloor1.getBlockZ();
		}
		getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
			public void run() {
				int r;
				Location l = null;
				for (int x = mix; x <= max; x++) {
					for (int z = miz; z <= maz; z++) {
						r = rand.nextInt(6);
						if (x != mix && z != miz) {
							l = new Location(laFloor1.getWorld(), x - 1, laFloor1.getBlockY(), z);
							if (l.getBlock().getType() != Material.AIR) {
								r = rand.nextInt(3);
							}
							l = new Location(laFloor1.getWorld(), x, laFloor1.getBlockY(), z - 1);
							if (l.getBlock().getType() != Material.AIR) {
								r = rand.nextInt(3);
							}
							l = new Location(laFloor1.getWorld(), x - 1, laFloor1.getBlockY(), z - 1);
							if (l.getBlock().getType() != Material.AIR) {
								r = rand.nextInt(3);
							}
						}
						l = new Location(laFloor1.getWorld(), x, laFloor1.getBlockY(), z);
						if (r == 0) {
							r = rand.nextInt(100);
							if (r == 0) {
								l.getBlock().setType(Material.DIAMOND_BLOCK);
							} else if (r == 1) {
								l.getBlock().setType(Material.GOLD_BLOCK);
							} else {
								l.getBlock().setType(Material.SMOOTH_BRICK);
							}
						} else {
							if (players.size() > 0) {
								l.getBlock().setType(Material.GLASS);
							} else {
								l.getBlock().setType(Material.AIR);
							}
						}
					}
				}
			}
		}, 0L);

		if (players.size() > 0) {
			sendMessageToGame("Generating Floor - glass will turn to air!");
		}

		if (players.size() > 0) {
			getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
				public void run() {
					if (players.size() > 0) {
						sendMessageToGame("Glass Clearing");
					}
					for (int x = mix; x <= max; x++) {
						for (int z = miz; z <= maz; z++) {
							if (new Location(laFloor1.getWorld(), x, laFloor1.getBlockY(), z).getBlock().getType() == Material.GLASS) {
								new Location(laFloor1.getWorld(), x, laFloor1.getBlockY(), z).getBlock().setType(Material.AIR);
							}
						}
					}
				}
			}, 200L);
		}
	}

	private void cancelTasks() {
		blockToChangeDiamond.clear();
		blockToChangeTNT.clear();
		while (!(bt.isEmpty())) {
			bt.get(0).cancel();
			bt.remove(0);
		}
	}

	public void sendMessageToGame(String msg) {
		for (int i = 0; i < players.size(); i++) {
			players.get(i).sendMessage(msg);
		}
		System.out.println(msg);

	}

	public Location spawnLocation() {
		Location l, l2 = null;
		Random rand = new Random();
		Boolean foundLocation = false;
		int x, y, z, mix, max, miz, maz, loops = 0;
		World world = laFloor1.getWorld();
		y = laFloor1.getBlockY();
		if (laFloor1.getBlockX() < laFloor2.getBlockX()) {
			mix = laFloor1.getBlockX();
			max = laFloor2.getBlockX();
		} else {
			mix = laFloor2.getBlockX();
			max = laFloor1.getBlockX();
		}
		if (laFloor1.getBlockZ() < laFloor2.getBlockZ()) {
			miz = laFloor1.getBlockZ();
			maz = laFloor2.getBlockZ();
		} else {
			miz = laFloor2.getBlockZ();
			maz = laFloor1.getBlockZ();
		}
		while (!foundLocation) {
			loops += 1;
			x = mix + rand.nextInt(max - mix);
			z = miz + rand.nextInt(maz - miz);
			l = new Location(world, x, y, z);
			l2 = new Location(world, x + 0.5, y + 1, z + 0.5);
			if (l.getBlock().getType() != Material.AIR && l2.getBlock().getType() == Material.AIR) {
				foundLocation = true;
			}
			if (loops > 100) {
				sendMessageToGame(ChatColor.RED + "[LavaArena] No spawn points");
				while (players.size() != 0) {
					onLeave(players.get(0), true, true);
				}
				return null;
			}
		}
		return l2;
	}
}
