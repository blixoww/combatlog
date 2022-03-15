package fr.blixow.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class CombatMain extends JavaPlugin implements Listener {

	private WorldGuardPlugin worldGuard;
	private String prefix;
	HashMap<Player, Integer> inBattle;
	public String[] listCommand = new String[] { "pl", "plugins", "ver", "version", "about", "bukkit:?", "bukkit:pl",
			"bukkit:plugins", "bukkit:about", "bukkit:help", "bukkit:ver", "bukkit:version", "?", "help", "nuke",
			"give", "pex", "ifo", "info", "afk", "eabout", "abouts", "news", "lag", "mem", "memory", "var", "seen",
			"ec", "enderchest", "poubelle", "trade", "dons", "shop", "cduel", "csp", "lobby" };
	public ArrayList<String> commandList = new ArrayList<String>(Arrays.asList(listCommand));

	public CombatMain() {
		this.prefix = "§8[§cCombat§8]§7 ";
		this.inBattle = new HashMap<Player, Integer>();
	}

	public HashMap<Player, Integer> getBattle() {
		return this.inBattle;
	}

	public WorldGuardPlugin getWG() {
		return this.worldGuard;
	}

	public void onEnable() {
		super.onEnable();
		getServer().getConsoleSender().sendMessage("§8§m--------------------------");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage("§8[§cCombatLog§8] §cLancement...");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage("§8§m--------------------------");
		this.worldGuard = WorldGuardPlugin.inst();
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(this, this);
	}

	public int manageCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if ((sender instanceof Player)) {
			Player p = (Player) sender;
			if (cmd.getName().equalsIgnoreCase("ct")) {
				if (p.isOp()) {
					p.sendMessage(this.prefix + "Vous êtes OP, ça ne vous affecte pas !");
				} else if ((getBattle().containsKey(p)) && (!p.isOp())) {
					p.sendMessage(this.prefix + "Tu es actuellement en combat !");
				} else {
					p.sendMessage(this.prefix + "Tu n'es pas en combat !");
				}
				return 0;
			}
		}
		return -1;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("ct")) {
			manageCommand(sender, cmd, label, args);
		}
		return false;
	}

	public void managePlayer(final Player p) {
		if (!p.isOp() || (!p.hasPermission("celestiam.admin"))) {
			if (!this.getBattle().containsKey(p)) {
				p.sendMessage(this.prefix + "Tu viens d'entrer en combat, ne te déconnecte pas !");
			} else {
				int ID = ((Integer) this.getBattle().get(p)).intValue();
				Bukkit.getServer().getScheduler().cancelTask(ID);
				this.getBattle().remove(p);
			}
			int taskId = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					CombatMain.this.getBattle().remove(p);
					p.sendMessage(CombatMain.this.prefix + "Tu n'es plus en combat, tu peux te déconnecter");
				}
			}, 600L);
			this.getBattle().put(p, Integer.valueOf(taskId));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		if (!(e.getDamager() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getEntity();
		Player attacker = (Player) e.getDamager();

		if ((p != null) && (e.getFinalDamage() > 0.0D) && (e.getDamage() > 0.0D) && (!e.isCancelled())
				&& ((e.getEntity() instanceof Player)) && ((e.getDamager() instanceof Player))) {

			if (hasPvP(attacker)) {
				managePlayer(p);
			}
			if (hasPvP(p)) {
				managePlayer(attacker);
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (this.getBattle().containsKey(e.getPlayer())) {
			e.getPlayer().setHealth(0);

			this.getBattle().remove(e.getPlayer());
			Bukkit.broadcastMessage("§c" + e.getPlayer().getName() + " s'est déconnecté en combat");
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player p = event.getPlayer();
		if (this.getBattle().containsKey(p)) {
			for (String command : commandList) {
				if (event.getMessage().toLowerCase().equals("/" + command)
						|| (event.getMessage().toLowerCase().startsWith("/" + command + " "))) {
					if (!p.hasPermission("celestiam.admin") || (!p.isOp())) {
						event.setCancelled(true);
						p.sendMessage("§cVous ne pouvez pas faire cette commande en combat !.");
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public boolean hasPvP(Player p) {
		RegionManager regionManager = getWG().getRegionManager(p.getWorld());
		ApplicableRegionSet set = regionManager.getApplicableRegions(p.getLocation());
		if (set.size() == 0) {
			return true;
		}
		if (set.allows(DefaultFlag.PVP)) {
			return true;
		}
		return false;
	}

}