package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

// TODO: Fix usage of AQLVanish, check if it is enabled
@AQLThingsModule(
        name = "AQLGroups",
        cmds = {
                @Cmd(value = "list", desc = "Liste des joueurs", aliases = {"who", "online", "playerlist", "players"}),
                @Cmd(value = AQLGroups.CMD_REFRESH_GROUPS, desc = "Mise à jour des rangs joueurs")
        }
)
public class AQLGroups implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();

    public static final String CMD_REFRESH_GROUPS = "refreshgroups";

    public static final String PERM_REFRESHGROUPS = AquilonThings.PERM_ROOT+".refreshgroups";

    private DatabaseConnector dbPhpBB = null;
    private LuckPerms perms;

    private boolean initDatabase(){
        // Création du connecteur BDD.
        dbPhpBB = new DatabaseConnector(
                AquilonThings.instance.getConfig().getString("databasePhpBB.address"),
                AquilonThings.instance.getConfig().getInt("databasePhpBB.port"),
                AquilonThings.instance.getConfig().getString("databasePhpBB.user"),
                AquilonThings.instance.getConfig().getString("databasePhpBB.password"),
                AquilonThings.instance.getConfig().getString("databasePhpBB.base"),
                AquilonThings.instance.getConfig().getBoolean("databasePhpBB.secure", true)
        );

        if (!dbPhpBB.validateConnection()) {
            LOGGER.mSevere("[BDD] La connexion BDD de PhpBB est inopérante.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        perms = LuckPermsProvider.get();
        return this.initDatabase();
    }

    @Override
    public boolean onStop() {
        if (dbPhpBB!=null) {
            dbPhpBB = null;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
        if (scmd.equalsIgnoreCase("list") ||
                scmd.equalsIgnoreCase("who") ||
                scmd.equalsIgnoreCase("online") ||
                scmd.equalsIgnoreCase("playerlist") ||
                scmd.equalsIgnoreCase("players")
        ) {
            if (args.length != 0) return false;
            sender.sendMessage(this.getPlayerList(sender));
            return true;
        } else if(cmd.getName().equalsIgnoreCase(CMD_REFRESH_GROUPS)) {
            if (sender.hasPermission(PERM_REFRESHGROUPS)) {
                return this.commandRefreshgroups(sender, false);
            } else {
                sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
            }
        }

        return true;
    }

    public boolean commandRefreshgroups(CommandSender sender, boolean silent) {
        UserManager users = perms.getUserManager();
        GroupManager groups = perms.getGroupManager();
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName(Utils.decoratePlayerName(player));

            String groupName = this.getPlayerGroupName(player);
            updatePlayerGroup(player, groupName, users, groups);
        }

        if (!silent) sender.sendMessage(ChatColor.YELLOW + "Rafraichissement des groupes effectué !");
        return true;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String userGroup = this.getPlayerGroupName(event.getPlayer());
        updatePlayerGroup(player, userGroup, null, null);

        String playerName = Utils.decoratePlayerName(player);
        player.setPlayerListName(playerName);

        if(player.hasPermission(AQLVanish.PERM_VANISH_VAN) && AQLVanish.isVanished(player)) {
            event.setJoinMessage("");
            for(Player p : Bukkit.getOnlinePlayers()){
                if (!p.hasPermission(AQLVanish.PERM_VANISH_SEE)) continue;
                p.sendMessage(ChatColor.GREEN  + "[VANISH]" + playerName + ChatColor.YELLOW + " s'est connecté au serveur !");
            }
        } else {
            event.setJoinMessage(playerName + ChatColor.YELLOW + " s'est connecté au serveur !");
        }

        player.sendMessage(this.getPlayerList(player));
    }

    private boolean updatePlayerGroup(Player p, String groupName, UserManager userManager, GroupManager groupManager) {
        UserManager users = userManager == null ? perms.getUserManager() : userManager;
        GroupManager groups = groupManager == null ? perms.getGroupManager() : groupManager;
        UUID playerId = p.getUniqueId();

        User usr;
        Optional<Group> optGroup;
        try {
            usr = users.loadUser(playerId).get();
            optGroup = groups.loadGroup(groupName).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.INFO, null, "Unable to load user/group permissions: "+playerId, e);
            return false;
        }
        if (!optGroup.isPresent()) {
            LOGGER.mInfo("Undefined permission group: "+groupName);
            return false;
        }
        return usr.setPrimaryGroup(groupName).wasSuccessful();
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = Utils.decoratePlayerName(player);

        if(player.hasPermission(AQLVanish.PERM_VANISH_VAN) && AQLVanish.isVanished(player)) {
            event.setQuitMessage("");
            for(Player p : Bukkit.getOnlinePlayers()){
                if (!p.hasPermission(AQLVanish.PERM_VANISH_SEE)) continue;
                p.sendMessage(ChatColor.GREEN  + "[VANISH]" + playerName + ChatColor.YELLOW + " a quitté le serveur !");
            }
        } else {
            event.setQuitMessage(playerName + ChatColor.YELLOW + " a quitté le serveur !");
        }
    }

    @EventHandler
    public void playerKickMessage(PlayerKickEvent event) {
        event.setLeaveMessage(Utils.decoratePlayerName(event.getPlayer()) + ChatColor.YELLOW + " a subi le courroux du jambon !");
    }

    public String getPlayerGroupName(Player player) {
        int user_id = 0;
        int group_id = 0;
        FileConfiguration config = AquilonThings.instance.getConfig();
        String defaultGroup = config.getString("groups.default", "Visiteur");

        if (dbPhpBB == null) {
            LOGGER.mInfo("Couldn't get player group ("+player.getUniqueId()+"), database is null !");
            return defaultGroup;
        }
        Connection con = dbPhpBB.startTransaction();
        String sql = "SELECT u.user_id, u.username, u.group_id FROM phpbb_users AS u, phpbb_profile_fields_data pf " +
                "WHERE (pf_mc_uuid = ? OR pf_mc_uuid = ? OR pf_user_minecraft = ?) AND u.user_id = pf.user_id";
        PreparedStatement stmt = dbPhpBB.prepare(con, sql);
        try {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getUniqueId().toString().replaceAll("-",""));
            stmt.setString(3, player.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user_id = rs.getInt("user_id");
                group_id = rs.getInt("group_id");
            }
        } catch (SQLException ex) {
            dbPhpBB.endTransaction(con, ex, sql);
            return defaultGroup;
        }
        dbPhpBB.endTransaction(con);

        if (user_id == 0 || group_id == 0) return defaultGroup;

        String permGroup = config.getString("groups.list."+group_id);
        if (permGroup == null) {
            LOGGER.mWarning("Undefined user group "+group_id+" !");
            return defaultGroup;
        }
        return permGroup;
    }

    public String getPlayerList(CommandSender sender) {
        List<String> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if(sender.hasPermission(AQLVanish.PERM_VANISH_SEE)){
                list.add(((AQLVanish.isVanished(p)) ? ChatColor.GREEN  + "[VANISH]" : "") + Utils.decoratePlayerName(p));
            } else {
                if (AQLVanish.isVanished(p)) continue;
                list.add(Utils.decoratePlayerName(p));
            }
        }

        String message = "Liste des joueurs connectés ("+list.size()+"/"+Bukkit.getServer().getMaxPlayers()+") :\n";
        message += String.join(ChatColor.YELLOW+", ", list);
        return message;
    }
}
