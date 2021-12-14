package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

/**
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
@AQLThingsModule(
		name = "AQLNobles",
		cmds = @Cmd(value = AQLNobles.COMMAND, desc = "Commande pour les nobles"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY)
)
public class AQLNobles implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();
	public static final String COMMAND = "noble";

	private HashMap<String, Location> prevPos;

	public AQLNobles() {
		this.prevPos = new HashMap<>();
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		//logger.mInfo("Lancement du module AQLNobles");
		return true;
	}

	@Override
	public boolean onStop() {
		//logger.mInfo("Arrêt du module AQLNobles");
		return true;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		if (cmd.getName().equalsIgnoreCase(COMMAND)) {
			if (!sender.hasPermission(AquilonThings.PERM_NOBLE)) {
				sender.sendMessage(ChatColor.YELLOW + "Ça me semblait évident que c'était reservé aux nobles ...");
				LOGGER.mInfo("Accès à la commande /noble interdit à : "+sender.getName());
				return true;
			}
			if (args.length<1) return false;
			
			if (args[0].equalsIgnoreCase("tp")) {
				return cmdTeleport(sender, args);
			}

			if (args[0].equalsIgnoreCase("bring")) {
				return cmdBring(sender, args);
			}

			if (args[0].equalsIgnoreCase("tpmap")) {
				return cmdTeleportMap(sender, args);
			}

			if (args[0].equalsIgnoreCase("back")) {
				return cmdTeleportBack(sender, args);
			}
		}
		return true;
	}

	private boolean cmdTeleport(CommandSender sender, String[] args) {
		final String USAGE = "/noble tp <visiteur>";

		if (!sender.hasPermission(AquilonThings.PERM_NOBLE)) {
			sender.sendMessage(ChatColor.YELLOW + "Vous n'avez pas la permission d'utiliser cette commande !");
			return true;
		}

		if (args.length<2) {
			sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + USAGE);
			return true;
		}

		Player cible = sender.getServer().getPlayerExact(args[1]);
		if (cible==null) {
			sender.sendMessage(ChatColor.YELLOW + "Aucun joueur avec le nom: " + ChatColor.WHITE + args[1]);
			return true;
		}

		if (!cible.hasPermission(AquilonThings.PERM_VISITEUR)) {
			sender.sendMessage(ChatColor.YELLOW + "Eh ! Tu ne peux aller voir que les visiteurs ...");
			return true;
		}

		if (!(sender instanceof Player)) {
			LOGGER.mInfo("L'emetteur de la commande n'est pas un joueur. Casse toi");
			return true;
		}
		Player noble = (Player) sender;

		Utils.warnStaff(this.getClass(),ChatColor.YELLOW+"Téléportation noble "+ChatColor.WHITE+sender.getName()+ChatColor.YELLOW+" vers visiteur "+ChatColor.WHITE+cible.getName()+ChatColor.YELLOW+".");
		Location pos = noble.getLocation();
		if (noble.teleport(cible)) {
			prevPos.put(noble.getUniqueId().toString(),pos);
			sender.sendMessage(ChatColor.YELLOW + "Vous avez été téléporté vers " + ChatColor.WHITE + cible.getName() + ChatColor.YELLOW + ".");
		} else {
			sender.sendMessage(ChatColor.YELLOW + "Erreur lors de la téléportation vers " + ChatColor.WHITE + cible.getName() + ChatColor.YELLOW + " !");
			LOGGER.mWarning("Echec de la téléportation noble '"+sender.getName()+"' vers visiteur '"+cible.getName()+"' !");
		}

		return true;
	}

	private boolean cmdTeleportBack(CommandSender sender, String[] args) {
		if (!sender.hasPermission(AquilonThings.PERM_NOBLE)) {
			sender.sendMessage(ChatColor.YELLOW + "Vous n'avez pas la permission d'utiliser cette commande !");
			return true;
		}

		if (!(sender instanceof Player)) {
			LOGGER.mInfo("L'emetteur de la commande n'est pas un joueur. Casse toi");
			return true;
		}
		Player noble = (Player) sender;

		Location pos = prevPos.get(noble.getUniqueId().toString());
		if (pos!=null) {
			prevPos.remove(noble.getUniqueId().toString());
			if (noble.teleport(pos)) sender.sendMessage(ChatColor.YELLOW + "Vous avez été téléporté.");
			else sender.sendMessage(ChatColor.RED + "Erreur lors de la téléportation !");
		} else {
			sender.sendMessage(ChatColor.YELLOW + "Pas de point d'origine enregistré !");
		}

		return true;
	}

	private boolean cmdBring(CommandSender sender, String[] args) {
		final String USAGE = "/noble bring <joueur>";

		if (!sender.hasPermission(AquilonThings.PERM_NOBLE)) {
			sender.sendMessage(ChatColor.YELLOW + "Vous n'avez pas la permission d'utiliser cette commande !");
			return true;
		}
		
		if (args.length<2) {
			sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + USAGE);
			return true;
		}

		Player cible = sender.getServer().getPlayerExact(args[1]);
		if (cible==null) {
			sender.sendMessage(ChatColor.YELLOW + "Aucun joueur avec le nom: " + ChatColor.WHITE + args[1]);
			return true;
		}
		
		if (!cible.hasPermission(AquilonThings.PERM_VISITEUR)) {
			sender.sendMessage(ChatColor.YELLOW + "Eh ! Tu ne peux ramener que les visiteurs ...");
			return true;
		}

		if (!(sender instanceof Player)) {
			LOGGER.mInfo("L'emetteur de la commande n'est pas un joueur. Casse toi");
			return true;
		}
		Player noble = (Player) sender;

		Utils.warnStaff(this.getClass(),ChatColor.YELLOW+"Téléportation visiteur "+ChatColor.WHITE+cible.getName()+ChatColor.YELLOW+" vers noble "+ChatColor.WHITE+sender.getName()+ChatColor.YELLOW+".");
		if (cible.teleport(noble)) {
			cible.sendMessage("Vous avez été téléporté par le joueur " + ChatColor.WHITE + sender.getName() + ChatColor.YELLOW + " pour une visite.");
			sender.sendMessage(ChatColor.YELLOW + "Le joueur " + ChatColor.WHITE + cible.getName() + ChatColor.YELLOW + " a été téléporté vers vous.");
		} else {
			sender.sendMessage(ChatColor.YELLOW + "Erreur lors de la téléportation de " + ChatColor.WHITE + cible.getName() + ChatColor.YELLOW + " !");
			LOGGER.mWarning("Echec de la téléportation visiteur '"+cible.getName()+"' vers noble '"+sender.getName()+"' !");
		}
		
		return true;
	}

	private boolean cmdTeleportMap(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			LOGGER.mInfo("L'emetteur de la commande n'est pas un joueur. Casse toi");
			return true;
		}
		Player noble = (Player) sender;
		FileConfiguration conf = AquilonThings.instance.getConfig();

		String mapName;
		if (args.length==2) {
			mapName = args[1];
		} else {
			if (conf.getString("nobles.mapNoble")==null) {
				LOGGER.mWarning("Map noble non définie dans la config (nobles.mapNoble)");
				sender.sendMessage(ChatColor.YELLOW+"Map par défaut non définie.\nUtilisation: "+
						ChatColor.WHITE+"/noble tpmap <map>");
				return true;
			}
			mapName = AquilonThings.instance.getConfig().getString("nobles.mapNoble");
		}

		World world = Bukkit.getWorld(mapName);
		if (world==null) {
			sender.sendMessage(ChatColor.YELLOW + "Map inexistante !");
			return true;
		}

		List<String> allowedMaps = conf.getStringList("nobles.allowedMaps");
		if (allowedMaps == null) {
			LOGGER.mWarning("Liste des maps autorisées aux nobles non définie dans la config (nobles.allowedMaps)");
			sender.sendMessage(ChatColor.RED+"Problème de configuration du plugin !");
			return true;
		}
		String defaultMap = AquilonThings.instance.getConfig().getString("mainMap", "world");
		if (!world.getName().equals(defaultMap) && !allowedMaps.contains(world.getName())) {
			sender.sendMessage(ChatColor.RED + "Vous n'êtes pas autorisé à vous rendre sur le monde: " + ChatColor.WHITE + world.getName());
			String msg = "Accès interdit à "+sender.getName()+" au monde "+world.getName();
			LOGGER.mInfo(msg);
			Utils.warnStaff(this.getClass(),ChatColor.RED+msg);
			return true;
		}
		
		Location worldSpawn = world.getSpawnLocation();
		Utils.warnStaff(this.getClass(),ChatColor.YELLOW+"Téléportation noble "+
				ChatColor.WHITE+sender.getName()+ChatColor.YELLOW+" vers monde "+
				ChatColor.WHITE+world.getName()+ChatColor.YELLOW+".");
		Location pos = noble.getLocation();
		if (noble.teleport(worldSpawn)) {
			prevPos.put(noble.getUniqueId().toString(),pos);
			sender.sendMessage(ChatColor.YELLOW + "Vous avez bien été téléporté sur le monde : "+
					ChatColor.WHITE + world.getName());
			LOGGER.mInfo("Téléportation noble '"+sender.getName()+"' vers monde '"+world.getName()+"'.");
		} else {
			sender.sendMessage(ChatColor.YELLOW + "Erreur lors de la téléportation vers le monde : "+
					ChatColor.WHITE + world.getName());
			LOGGER.mWarning("Echec de la téléportation noble '"+sender.getName()+"' vers monde '"+
					world.getName()+"' !");
		}
		
		return true;
	}
	
}
