package me.RocketRat23.LavaArena;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.BlockIterator;

public class Listeners implements Listener {
	LavaArena plugin;

	public Listeners(LavaArena instance) {
		plugin = instance;
	}

	@EventHandler
	public void onProjectileHitLa(ProjectileHitEvent event) {
		if (plugin.players.contains(event.getEntity().getShooter()) && event.getEntity() instanceof Arrow) {
			BlockIterator iterator = new BlockIterator(event.getEntity().getLocation().getWorld(), event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0, 4);
			Block hitBlock = null;
			while (iterator.hasNext()) {
				hitBlock = iterator.next();
				if (hitBlock.getTypeId() != 0) {
					break;
				}
			}
			if (hitBlock.getType() == Material.DIAMOND_BLOCK || hitBlock.getType() == Material.GOLD_BLOCK) {
				plugin.arrowHitBlock(hitBlock.getLocation(), (Player) event.getEntity().getShooter());
				event.getEntity().remove();
			}
			((Arrow) event.getEntity()).remove();
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (plugin.players.contains(event.getEntity().getPlayer())) {
			event.setDeathMessage("");
			event.getDrops().clear();
			Player victim = event.getEntity().getPlayer();
			Player shooter = plugin.killers.get(victim);
			if (shooter == null) {
				plugin.sendMessageToGame(ChatColor.YELLOW + "[LavaArena] " + victim.getName() + " fell into lava");
			} else {
				plugin.sendMessageToGame(ChatColor.YELLOW + "[LavaArena] " + victim.getName() + " was killed by " + shooter.getName());
				plugin.points.getScore(shooter).setScore(plugin.points.getScore(shooter).getScore() + 1);
				plugin.killers.remove(victim);
			}

		}
	}

	@EventHandler
	public void onPlayerRespawnLa(PlayerRespawnEvent event) {
		if (plugin.players.contains(event.getPlayer())) {
			Location spawn = plugin.spawnLocation();
			if (spawn != null) {
				plugin.onJoin(event.getPlayer());
				spawn.setPitch(event.getPlayer().getLocation().getPitch());
				spawn.setYaw(event.getPlayer().getLocation().getYaw());
				event.setRespawnLocation(spawn);
			}

		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		if (plugin.players.contains(event.getPlayer())) {
			plugin.onLeave(event.getPlayer(), true, true);
			plugin.sendMessageToGame("[LavaArena] " + event.getPlayer().getName() + " has left the game (ragequit)");
			event.getPlayer().teleport(event.getPlayer().getLocation().getWorld().getSpawnLocation());
		}
	}

	@EventHandler
	public void onItemSpawnEvent(ItemSpawnEvent event) {
		/*
		 * if (plugin.players.contains(event.getEntity().)) {
		 * event.setCancelled(true); }
		 */
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		try {
			if (plugin.players.contains(event.getPlayer())) {
				Player player = (Player) event.getPlayer();

				if (!(player.isDead()) && plugin.laFloor1.getWorld() == event.getPlayer().getLocation().getWorld() && plugin.blockInRange(event.getPlayer().getLocation(), 20)) {
					// Player is in range of the arena
					if (player.getLocation().getBlockY() - 1 != plugin.laFloor1.getBlockY()) {
						Location l = player.getLocation();
						l.setY(l.getY() - 1);
						if (l.getBlock().getType() == Material.FENCE) {
							// Player is out of map
							player.setHealth(0);
						}
					}
				} else if (!(event.getPlayer().isDead())) {
					// Player out of range of the arena, kicking
					plugin.onLeave(event.getPlayer(), true, false);
					plugin.sendMessageToGame(ChatColor.GRAY + "[LavaArena] " + player.getName() + " has left the game (out of arena)");
				}
			}
		} catch (Exception e) {
		}
	}

	@EventHandler
	public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Arrow) {
			if (((Arrow) event.getDamager()).getShooter() instanceof Player) {
				Player shooter = (Player) ((Arrow) event.getDamager()).getShooter();
				Player victim = (Player) event.getEntity();
				if (plugin.players.contains(shooter) && plugin.players.contains(victim) && victim.getLocation().getBlock().getType() != Material.LAVA) {
					if (shooter != victim) {
						plugin.killers.remove(victim);
						plugin.killers.put(victim, shooter);
					}
					((Arrow) event.getDamager()).remove();
				}
			}
		} else if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			if (plugin.players.contains(event.getEntity()) && plugin.players.contains(event.getDamager())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Location l = event.getBlock().getLocation();
		if (plugin.blockInRange(l, 0)) {
			if (!event.getPlayer().hasPermission("lavaarena.break")) {
				event.setCancelled(true);
			}
		}
	}
}
