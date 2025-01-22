package fr.aquilon.minecraft.aquilonthings.modules.AQLNames;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@AQLThingsModule(
		name = "AQLNames",
		cmds = @Cmd(value = "nom", desc = "Gestion des noms RP")
)
public class AQLNames implements IModule {
	private static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String PERM_SEARCH_NAMES = AquilonThings.PERM_ROOT+".nom.search";
	public static final String PERM_LIST_NAMES = AquilonThings.PERM_ROOT+".nom.list";
	public static final String PERM_EDIT_OTHERS = AquilonThings.PERM_ROOT+".nom.setx";
	public static final String PERM_EDIT_NAME = AquilonThings.PERM_ROOT+".nom.name.edit";
	public static final String PERM_EDIT_DESC = AquilonThings.PERM_ROOT+".nom.desc.edit";
	public static final String PERM_COLORED_NAME = AquilonThings.PERM_ROOT+".nom.name.colors";
	public static final String PERM_COLORED_DESC = AquilonThings.PERM_ROOT+".nom.desc.colors";

	private static final String SQL_FIND_PLAYER_INFO = "SELECT * FROM aqlnames_names WHERE player = ? ";
	private static final String SQL_UPDATE_PLAYER_INFO = "INSERT INTO aqlnames_names VALUES " +
			"(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = ?, " +
			"display_name = ?, description = ?, updated = ?";

	private DatabaseConnector db;
	private HashMap<String, PlayerInfo> playerInfos;

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		playerInfos = new HashMap<>();
		this.db = db;
		for (Player p : Bukkit.getOnlinePlayers()) {
			PlayerInfo info = retrievePlayerInfo(p.getUniqueId());
			if (info != null) {
				p.setDisplayName(info.getName());
			}
		}
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player target = event.getPlayer();
		PlayerInfo info = retrievePlayerInfo(target.getUniqueId());
		if (info != null) {
			target.setDisplayName(info.getName());
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdName, String[] args) {
		if (!cmdName.equals("nom")) return false;
		final String USAGE = "/nom (list)|(set|setx|desc|search <texte>)";
		if (args.length < 1) {
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+USAGE);
			return true;
		}
		if (args[0].equals("setx")) {
			if (!sender.hasPermission(PERM_EDIT_OTHERS)) {
				sender.sendMessage(ChatColor.YELLOW+"Je pense pas que tu ai le droit de toucher à ça !");
				return true;
			}
			if (args.length < 3) {
				sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/nom setx <player> <nom>;<desc>");
				return true;
			}
			// TODO: search a player by name and set his name and description
			sender.sendMessage("Not implemented yet !");
			return true;
		} else if (args[0].equals("list")) {
			if (!sender.hasPermission(PERM_LIST_NAMES)) {
				sender.sendMessage(ChatColor.YELLOW+"Je pense pas que tu ai le droit de toucher à ça !");
				return true;
			}
			sender.sendMessage(ChatColor.YELLOW+"Liste des noms:");
			for (Player p : Bukkit.getOnlinePlayers()) {
				PlayerInfo info = getPlayerInfo(p.getUniqueId());
				sender.sendMessage("  "+Utils.decoratePlayerName(p)+ChatColor.GRAY+" - "+ChatColor.WHITE+info.getName());
			}
			return true;
		} else if (args[0].equals("search")) {
			if (!sender.hasPermission(PERM_SEARCH_NAMES)) {
				sender.sendMessage(ChatColor.YELLOW+"Je pense pas que tu ai le droit de toucher à ça !");
				return true;
			}
			// TODO: search a player by its RP name
			sender.sendMessage("Not implemented yet !");
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage("Commande inaccessible depuis la console");
			return true;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+USAGE);
			return true;
		}
		Player target = (Player) sender;
		PlayerInfo infos = getPlayerInfo(target.getUniqueId());
		if (args[0].equals("set")) {
			if (!sender.hasPermission(PERM_EDIT_NAME)) {
				sender.sendMessage(ChatColor.YELLOW+"Je pense pas que tu ai le droit de toucher à ça !");
				return true;
			}
			String name = Utils.joinStrings(args, " ", 1);
			if (name.length() < 3 || name.length() > 64) {
				sender.sendMessage(ChatColor.YELLOW+"Nom trop court ou trop long");
				return true;
			}
			if (sender.hasPermission(PERM_COLORED_NAME)) {
				name = ChatColor.translateAlternateColorCodes('&', name);
			}
			infos.setName(name);
			target.setDisplayName(name);
			updatePlayerInfo(infos);
			sender.sendMessage(ChatColor.YELLOW+"Nom enregistré: "+ChatColor.WHITE+name);
		} else if (args[0].equals("desc")) {
			if (!sender.hasPermission(PERM_EDIT_DESC)) {
				sender.sendMessage(ChatColor.YELLOW+"Je pense pas que tu ai le droit de toucher à ça !");
				return true;
			}
			String desc = Utils.joinStrings(args, " ", 1);
			if (desc.length() > 256) {
				sender.sendMessage(ChatColor.YELLOW+"Description trop longue");
				return true;
			}
			if (sender.hasPermission(PERM_COLORED_DESC)) {
				desc = ChatColor.translateAlternateColorCodes('&', desc);
			}
			infos.setDescription(desc);
			updatePlayerInfo(infos);
			sender.sendMessage(ChatColor.YELLOW+"Description enregistréé: "+ChatColor.WHITE+desc);
		} else {
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+USAGE);
			return true;
		}
		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEntityEvent e) {
		Entity targetEntity = e.getRightClicked();
		if (!(targetEntity instanceof Player)) return;
		if (e.getHand() != EquipmentSlot.HAND) return;
		Player target = (Player) targetEntity;
		Player source = e.getPlayer();
		PlayerInfo targetInfo = getPlayerInfo(target.getUniqueId());
		if (targetInfo == null || targetInfo.getName() == null) return; // it can happen when the player model is used for an NPC
		String description = ChatColor.WHITE+targetInfo.getName()+ChatColor.GRAY+": * "+ChatColor.ITALIC+
				targetInfo.getDescription("Une personne comme une autre") +ChatColor.RESET+ChatColor.GRAY+" *";
		source.sendMessage(ChatColor.translateAlternateColorCodes('&', description));
	}

	public PlayerInfo getPlayerInfo(UUID player) {
		return playerInfos.computeIfAbsent(player.toString().replaceAll("-", ""),
				k -> new PlayerInfo(player));
	}

	/**
	 * Retrieves the player info from the database, and updates the cache if info were found
	 * @param player The id of the player
	 * @return {@link PlayerInfo} if found, <code>null</code> otherwise
	 */
	private PlayerInfo retrievePlayerInfo(UUID player) {
		String uuid = player.toString().replaceAll("-","");
		Connection con = db.startTransaction();
		PlayerInfo res = null;
		try {
			PreparedStatement stmt = db.prepare(con, SQL_FIND_PLAYER_INFO);
			stmt.setString(1, uuid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = new PlayerInfo(player,
						rs.getString("display_name"),
						rs.getString("description"));
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_UPDATE_PLAYER_INFO);
			return null;
		}
		db.endTransaction(con);
		if (res != null) playerInfos.put(uuid, res);
		return res;
	}

	/**
	 * Update a player info in the database and in cache
	 * @param info The player info to save
	 */
	private void updatePlayerInfo(PlayerInfo info) {
		String uuid = info.getPlayerUUID().toString().replaceAll("-","");
		Connection con = db.startTransaction();
		try {
			PreparedStatement stmt = db.prepare(con, SQL_UPDATE_PLAYER_INFO);
			stmt.setString(1, uuid);
			String playerName = info.getPlayer().getName();
			stmt.setString(2, playerName);
			stmt.setString(6, playerName);
			stmt.setString(3, info.getName());
			stmt.setString(7, info.getName());
			stmt.setString(4, info.getDescription(null));
			stmt.setString(8, info.getDescription(null));
			Timestamp time = Timestamp.from(Instant.now());
			stmt.setTimestamp(5, time);
			stmt.setTimestamp(9, time);
			stmt.executeUpdate();
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_UPDATE_PLAYER_INFO);
			return;
		}
		db.endTransaction(con);
		playerInfos.put(uuid, info);
	}
}
