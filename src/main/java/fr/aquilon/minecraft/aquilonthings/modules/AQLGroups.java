package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.utils.Utils;
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
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        if (scmd.equalsIgnoreCase("list") || scmd.equalsIgnoreCase("who") || scmd.equalsIgnoreCase("online") || scmd.equalsIgnoreCase("playerlist") || scmd.equalsIgnoreCase("players")) {
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
        Collection<String> groups = PermissionsEx.getPermissionManager().getGroupNames();
        for(Player player : Bukkit.getOnlinePlayers()) {
            String userGroup = this.getPlayerPEXGroup(player);
            PermissionUser pUser = PermissionsEx.getUser(player);
            for (String g : groups) if (!g.equals(userGroup)) pUser.removeGroup(g);
            pUser.addGroup(userGroup);

            String prefix = Utils.getPlayerColor(player);
            player.setPlayerListName(prefix + player.getName() + ChatColor.WHITE);
        }

        if (!silent) sender.sendMessage(ChatColor.YELLOW + "Rafraichissement des groupes effectué !");
        return true;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        String userGroup = this.getPlayerPEXGroup(event.getPlayer());
        Collection<String> groups = PermissionsEx.getPermissionManager().getGroupNames();
        PermissionUser pUser = PermissionsEx.getUser(event.getPlayer());
        for (String g : groups) if (!g.equals(userGroup)) pUser.removeGroup(g);
        pUser.addGroup(userGroup);

        String prefix = Utils.getPlayerColor(event.getPlayer());
        event.getPlayer().setPlayerListName(prefix + event.getPlayer().getName() + ChatColor.WHITE);

        if(event.getPlayer().hasPermission(AQLVanish.PERM_VANISH_VAN) && AQLVanish.isVanished(event.getPlayer())) {
            event.setJoinMessage("");
            for(Player p : Bukkit.getOnlinePlayers()){
                if (!p.hasPermission(AQLVanish.PERM_VANISH_SEE)) continue;
                p.sendMessage(ChatColor.GREEN  + "[VANISH]" + prefix + event.getPlayer().getName() + ChatColor.YELLOW + " s'est connecté au serveur !");
            }
        } else {
            event.setJoinMessage(prefix + event.getPlayer().getName() + ChatColor.YELLOW + " s'est connecté au serveur !");
        }

        event.getPlayer().sendMessage(this.getPlayerList(event.getPlayer()));
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        String prefix = Utils.getPlayerColor(event.getPlayer());

        if(event.getPlayer().hasPermission(AQLVanish.PERM_VANISH_VAN) && AQLVanish.isVanished(event.getPlayer())) {
            event.setQuitMessage("");
            for(Player p : Bukkit.getOnlinePlayers()){
                if (!p.hasPermission(AQLVanish.PERM_VANISH_SEE)) continue;
                p.sendMessage(ChatColor.GREEN  + "[VANISH]" + prefix + event.getPlayer().getName() + ChatColor.YELLOW + " a quitté le serveur !");
            }
        } else {
            event.setQuitMessage(prefix + event.getPlayer().getName() + ChatColor.YELLOW + " a quitté le serveur !");
        }
    }

    @EventHandler
    public void playerKickMessage(PlayerKickEvent event) {
        String prefix = Utils.getPlayerColor(event.getPlayer());
        event.setLeaveMessage(prefix + event.getPlayer().getName() + ChatColor.YELLOW + " a subi le courroux du jambon !");
    }

    public String getPlayerPEXGroup(Player player) {
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
                list.add(((AQLVanish.isVanished(p)) ? ChatColor.GREEN  + "[VANISH]" : "") + Utils.getPlayerColor(p) + p.getName());
            } else {
                if (AQLVanish.isVanished(p)) continue;
                list.add(Utils.getPlayerColor(p) + p.getName());
            }
        }

        String message = "Liste des joueurs connectés ("+list.size()+"/"+Bukkit.getServer().getMaxPlayers()+") :\n";
        message += String.join(ChatColor.YELLOW+", ", list);
        return message;
    }
}
