package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

@AQLThingsModule(
		name = "AQLMisc",
		cmds = {
				@Cmd(value = AQLMisc.CMD_PINFO, desc = "Un truc ..."),
				@Cmd(value = AQLMisc.CMD_PWEATHER, desc = "Definition de la météo pour un joueur"),
				@Cmd(value = AQLMisc.CMD_BROUILLARD, desc = "Définition du brouillard pour un joueur"),
				@Cmd(value = AQLMisc.CMD_SUMMON_AREA, desc = "Invocation de mobs dans une zone")
		},
		inPackets = { @InPacket(AquilonThings.CHANNEL_READY), @InPacket(AQLMisc.CHANNEL_PLAYERINFO) },
		outPackets = { @OutPacket(AQLMisc.CHANNEL_PLAYERINFO), @OutPacket(AQLMisc.CHANNEL_BROUILLARD) }
)
public class AQLMisc implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String CMD_PINFO = "pinfo";
	public static final String CMD_PWEATHER = "pweather";
	public static final String CMD_BROUILLARD = "brouillard";
	public static final String CMD_SUMMON_AREA = "summonarea";

	public static final String CONFIG_FILE = "AQLMisc.yml";

	public static final String CHANNEL_PLAYERINFO = "playerinfo";
	public static final String CHANNEL_BROUILLARD = "brouillard";

	public static final String PERM_PINFO = AquilonThings.PERM_ROOT+".pinfo";
	public static final String PERM_BROUILLARD = AquilonThings.PERM_ROOT+".brouillard";
	public static final String PERM_PWEATHER = AquilonThings.PERM_ROOT+".pweather";

	private FileConfiguration config;
    private final Map<String, Integer> playerFood = new HashMap<>();
	private final Map<String, Integer> playerBrouillard = new HashMap<>();
	private final Set<String> playerWeather = new HashSet<>();
	private CommandSender pinfoRequester;

	public static final String PERM_PREMIUM = AquilonThings.PERM_ROOT+".premium";

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		return init();
	}

	@Override
	public boolean onStop() {
		return true;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player p, byte[] cmdString) {
		if(channel.equals(AquilonThings.CHANNEL_READY)){
			if(playerWeather.contains(p.getUniqueId().toString())){
				p.setPlayerWeather(WeatherType.DOWNFALL);
			}

			if(playerBrouillard.containsKey(p.getUniqueId().toString())){
				String density = String.valueOf(playerBrouillard.get(p.getUniqueId().toString()));
				p.sendPluginMessage(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+CHANNEL_BROUILLARD, density.getBytes());
			}
		} else if(channel.equals(CHANNEL_PLAYERINFO) && cmdString.length > 0) {
		    String data = new String(cmdString).substring(1).split(":")[1];
			if (pinfoRequester!=null) pinfoRequester.sendMessage(ChatColor.YELLOW+"Info joueur "+
					Utils.getPlayerColor(p)+p.getName()+ChatColor.YELLOW+" : "+
					ChatColor.WHITE + data.replace("*;",ChatColor.RED+"*"+ChatColor.WHITE+";"));
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
		if (cmd.getName().equalsIgnoreCase(CMD_PINFO)) {
			if (!sender.hasPermission(PERM_PINFO)) {
				sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
				return true;
			}

			if (args.length < 1) {
				sender.sendMessage(ChatColor.RED + "C'est pas compliqué ! Un nom !");
				return true;
			}

			if (args[0].equals("*")) {
				pinfoRequester = sender;
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.hasPermission(PERM_PINFO)) continue;
					this.sendPlayerInfoPacket(sender, p.getName());
				}
			} else {
				if (this.sendPlayerInfoPacket(sender, args[0]))
					pinfoRequester = sender;
				else
					sender.sendMessage(ChatColor.RED + "Erreur dans l'envoi du packet.");
			}
			return true;
		} else if(scmd.equalsIgnoreCase(CMD_SUMMON_AREA)) {
			return summonArea(sender, args);
		} else if(scmd.equalsIgnoreCase(CMD_BROUILLARD)) {
			if(!sender.hasPermission(PERM_BROUILLARD)) {
				sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
				return true;
			}

			if(args.length < 1)
				return false;

			if(args[0].equalsIgnoreCase("clear")){
				playerBrouillard.clear();
				sender.sendMessage(ChatColor.YELLOW + "Le brouillard vient d'être retiré sur l'ensemble des joueurs !");
				return true;
			}

			if(args.length < 2)
				return false;

			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage(ChatColor.RED + "Le pseudo du joueur est inconnu !");
				return true;
			}

			int density;
			try {
				density = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "La densité doit correspondre à un nombre entier...");
				return true;
			}

			if (density <= 0)
				playerBrouillard.remove(player.getUniqueId().toString());
			else
				playerBrouillard.put(player.getUniqueId().toString(), density);

			player.sendPluginMessage(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+CHANNEL_BROUILLARD, args[1].getBytes());
			sender.sendMessage(ChatColor.YELLOW + "Le brouillard est défini avec succès sur le joueur "+
					Utils.getPlayerColor(player) + player.getName() + ChatColor.YELLOW +  " (" + density + ")");
			return true;
		} else if(scmd.equalsIgnoreCase(CMD_PWEATHER)) {
			if (!sender.hasPermission(PERM_PWEATHER)) {
				sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
				return true;
			}

			if (args.length < 1) return false;

			if (args[0].equalsIgnoreCase("clear")) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (!playerWeather.contains(p.getUniqueId().toString())) continue;
					p.resetPlayerWeather();
				}
				playerWeather.clear();
				sender.sendMessage(ChatColor.YELLOW + "La pluie vient d'être retiré sur l'ensemble des joueurs !");
				return true;
			}

			if (args.length < 2) return false;

			Player player = Bukkit.getPlayer(args[0]);
			if (player == null) {
				sender.sendMessage(ChatColor.RED + "Le pseudo du joueur est inconnu !");
				return true;
			}

			boolean weather;
			switch (args[1]) {
				case "true":
				case "True":
				case "pluie":
				case "neige":
				case "y":
				case "Y":
                case "on":
				case "oui":
				case "Oui":
				case "o":
				case "O":
				case "1":
					weather = true;
					break;
				case "false":
				case "False":
				case "soleil":
				case "off":
				case "n":
				case "N":
				case "non":
				case "Non":
				case "0":
					weather = false;
					break;
				default:
					return false;
			}

			if (!weather) {
				playerWeather.remove(player.getUniqueId().toString());
				player.resetPlayerWeather();
			} else {
				playerWeather.add(player.getUniqueId().toString());
				player.setPlayerWeather(WeatherType.DOWNFALL);
			}

			sender.sendMessage(ChatColor.YELLOW + "La météo est définie avec succès sur le joueur "+
					Utils.getPlayerColor(player) + player.getName() + ChatColor.YELLOW + " (" + (weather ? "pluie/neige" : "normal") + ")");
			return true;
		}

		return false;
	}

	public static final String[] POS_ARGS_NAMES = {"X","Y","Z"};
	public boolean summonArea(CommandSender sender, String[] args) {
		if (args.length<5) {
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+
					"/summonarea <x> <y> <z> <radius> <entity> (<count> (<nbt>))");
			return true;
		}
		int[] senderPos;
		World world;
		if (sender instanceof BlockCommandSender) {
			Block b = ((BlockCommandSender) sender).getBlock();
			senderPos = Utils.blockToPositionArray(b);
			world = b.getWorld();
		} else if (sender instanceof Player) {
			Player p = (Player) sender;
			world = p.getWorld();
			senderPos = Utils.blockToPositionArray(p.getWorld().getBlockAt(p.getLocation()));
		} else {
			sender.sendMessage("Missing world context !");
			return true;
		}
		int[] center = new int[3];
		for (int i=0; i<3; i++) {
			String s = args[i].replace("~","");
			if (s.length()==0) center[i] = 0;
			else try {
				center[i] = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW+"Invalid "+POS_ARGS_NAMES[i]+" position, expecting number !");
				return true;
			}
			if (args[i].startsWith("~")) center[i] += senderPos[i];
		}
		int radius;
		try {
			radius = Integer.parseUnsignedInt(args[3]);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.YELLOW+"Invalid radius, expecting number !");
			return true;
		}
		int count = 1;
		if (args.length>5) try {
			count = Integer.parseUnsignedInt(args[5]);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.YELLOW+"Invalid count, expecting number !");
			return true;
		}
		Random alea = new Random();
		int success = 0;
		for (int n=0; n<count; n++) {
			for (int i=0; i<radius*2; i++) {
				int[] pos = new int[2];
				pos[0] = center[0] - (radius/2) + alea.nextInt(radius);
				pos[1] = center[2] - (radius/2) + alea.nextInt(radius);
				Block b = world.getBlockAt(pos[0], center[1], pos[1]);
				if (b.getType() != Material.AIR && b.getType() != Material.WATER) continue;
				Bukkit.dispatchCommand(sender, "summon "+args[4]+" "+pos[0]+" "+center[1]+" "+pos[1]+
						(args.length>6?" "+String.join(" ", Arrays.copyOfRange(args,6, args.length)):""));
				success++;
				break;
			}
		}
		if (success==count) {
			sender.sendMessage(ChatColor.YELLOW+"Entities summoned successfully.");
		} else {
			sender.sendMessage(ChatColor.YELLOW+"Unable to summon entities ! "+success+"/"+count);
		}
		return true;
	}

	public boolean sendPlayerInfoPacket(CommandSender sender, String nameCheck) {
		Player player = Bukkit.getPlayer(nameCheck);
		if (player != null) {
			player.sendPluginMessage(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+CHANNEL_PLAYERINFO, sender.getName().getBytes());
		} else {
			sender.sendMessage(ChatColor.RED + "Erreur dans le pseudo du joueur...");
		}

		return true;
	}

	@EventHandler
	public static void playerKickMessage(PlayerKickEvent event) {
		// On interdit le kick des "staff premium"
		if (event.getPlayer().hasPermission(PERM_PREMIUM)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player && config.getBoolean("food.disableVisitors", false)) {
			// Prevent visitor from loosing food
			Player player = (Player) event.getEntity();
			if (player.hasPermission(AquilonThings.PERM_VISITEUR)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDoorBroken(EntityBreakDoorEvent event) {
		if (config.getBoolean("door.preventBreaking", false)) {
			// Prevent zombies from breaking doors
			if (!(event.getEntity() instanceof Player)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event){
		final Player p = event.getPlayer();
		if (config.getBoolean("food.preventSuicideRegen", false)) {
			// If the player killed himself to regen food, set it's food level to 6
			if (playerFood.containsKey(p.getUniqueId().toString())) {
				Bukkit.getScheduler().runTaskLater(AquilonThings.instance, () -> {
					int food = playerFood.get(p.getUniqueId().toString());
					p.setFoodLevel(Math.max(food, 6));
					playerFood.remove(p.getUniqueId().toString());
				}, 5);
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player p = event.getEntity();

		if (config.getBoolean("food.preventSuicideRegen", false)) {
			// Save food level before death
			playerFood.put(p.getUniqueId().toString(), p.getFoodLevel());
		}

		int dropHeadChance = config.getInt("death.dropHeadChance", 0);
		if (event.getEntity().getKiller() != null && dropHeadChance > 0) {
			// Check if we drop the player head when he was killed
			if (new Random().nextInt(dropHeadChance) == 0) {
				ItemStack head = new ItemStack(Material.PLAYER_HEAD);
				SkullMeta headMeta = (SkullMeta) head.getItemMeta();
				headMeta.setOwningPlayer(p);
				head.setItemMeta(headMeta);
				p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(head));
			}
		}

		// Color the dead player name
		String playerColor = Utils.getPlayerColor(p);
		event.setDeathMessage(ChatColor.YELLOW + event.getDeathMessage()
				.replace(p.getName(), playerColor + p.getName() + ChatColor.YELLOW));

		int deathMessageRadius = config.getInt("death.messageRadius", -1);
		if (deathMessageRadius >= 0) {
			// Cancel general death message
			String msg = event.getDeathMessage();
			event.setDeathMessage("");

			if (deathMessageRadius > 0) {
				// Send death message to players in radius
				List<Entity> targets = p.getNearbyEntities(deathMessageRadius, deathMessageRadius, deathMessageRadius);
				for (Entity e : targets) {
					if (!(e instanceof Player)) continue;
					if (Utils.playerHasWarningPerm(e, AQLMisc.class)) continue;
					if (e == event.getEntity()) continue;
					e.sendMessage(msg);
				}
				// To the dead player
				p.sendMessage(msg);
				// And to staff
				Utils.warnStaff(AQLMisc.class, msg, new String[]{p.getName()});
			}
		}
	}

	/**
	 * Initialize config
	 */
	private boolean init() {
		File file = new File(AquilonThings.instance.getDataFolder(), CONFIG_FILE);
		if (!file.exists()) {
			LOGGER.mInfo("Config not found. Saving default one.");
			try {
				Utils.saveResource(CONFIG_FILE, false, LOGGER);
			} catch (Exception e) {
				LOGGER.mSevere("Unable to save default config file");
				LOGGER.log(Level.INFO, null, "Exception:", e);
			}
		}

		try {
			config = Utils.loadConfig(CONFIG_FILE);
			if (config==null) throw new IOException("Config cannot be null");
		} catch (IOException ex) {
			LOGGER.mSevere("Unable to read the config file ! Module will be disabled.");
			LOGGER.log(Level.INFO, null, "Exception:", ex);
			return false;
		}
		return true;
	}
}
