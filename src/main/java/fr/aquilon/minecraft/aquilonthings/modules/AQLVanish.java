package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

@AQLThingsModule(
        name = "AQLVanish",
        cmds = {
                @Cmd(value = AQLVanish.CMD_VAN, desc = "Devenir invisible pour les joueurs"),
                @Cmd(value = AQLVanish.CMD_PVAN, desc = "Inverser la visibilité avec les joueurs, en explosant"),
                @Cmd(value = AQLVanish.CMD_UNVAN, desc = "Redevenir visible pour les joueurs")
        }
)
public class AQLVanish implements IModule {
    public static final String CMD_VAN = "van";
    public static final String CMD_PVAN = "pvan";
    public static final String CMD_UNVAN = "unvan";
    public static final String PERM_VANISH_VAN = AquilonThings.PERM_ROOT+".vanish.van";
    public static final String PERM_VANISH_SEE = AquilonThings.PERM_ROOT+".vanish.see";

    private DatabaseConnector db;
    private static HashMap<String, Boolean> vanished = new HashMap<String, Boolean>();

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        this.db = db;
        for (Player p : Bukkit.getOnlinePlayers()){
            boolean isVanished = this.getRecordedVanish(p.getUniqueId());
            vanished.put(p.getUniqueId().toString(), isVanished);
        }
        return true;
    }

    @Override
    public boolean onStop() {return true;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
        if (!sender.hasPermission(PERM_VANISH_VAN)) {
            sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "La console ne peut pas faire ça...");
            return true;
        }

        Player p = (Player) sender;

        if (args.length > 1) return false;

        if (args.length == 1 && !args[0].equalsIgnoreCase("list")) {
            p = Bukkit.getPlayer(args[0]);

            if (p == null) {
                sender.sendMessage(ChatColor.RED + "Le joueur sélectionné est introuvable");
                return true;
            }
        }

        if (cmd.equalsIgnoreCase("pvan")) {

            if (isVanished(p)) {
                this.setPoof(p);
                this.setShowPlayer(p);

                if (p == sender) {
                    sender.sendMessage(ChatColor.GRAY + "*poof* Vous êtes désormais " + ChatColor.GREEN + "visible" + ChatColor.GRAY + " aux joueurs normaux");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "*poof* Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " est désormais visible");
                    p.sendMessage(ChatColor.GRAY + "*poof* Vous êtes désormais " + ChatColor.GREEN + "visible" + ChatColor.GRAY + " aux joueurs normaux");
                }
            } else {
                this.setPoof(p);
                this.setHidePlayer(p);

                if (p == sender) {
                    sender.sendMessage(ChatColor.GRAY + "*poof* Vous êtes désormais " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " aux joueurs normaux");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "*poof* Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " est désormais invisible");
                    p.sendMessage(ChatColor.GRAY + "*poof* Vous êtes désormais " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " aux joueurs normaux");
                }
            }

            return true;

        } else if (cmd.equalsIgnoreCase("van")) {

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessage(ChatColor.YELLOW + "Liste des invisibles :");

                    for (Player vanish : Bukkit.getOnlinePlayers()) {
                        if (isVanished(vanish)) {
                            sender.sendMessage(ChatColor.YELLOW + "- " + Utils.getPlayerColor(vanish) + vanish.getName());
                        }
                    }

                    return true;
                }
            }

            if (isVanished(p)) {

                if (p == sender) {
                    sender.sendMessage(ChatColor.YELLOW + "Vous êtes déjà invisible");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Le joueur " + p.getName() + " est déjà invisible");
                }

                return true;
            }

            this.setHidePlayer(p);

            if (p == sender) {
                sender.sendMessage(ChatColor.GRAY + "Vous êtes désormais " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " aux joueurs normaux");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " est désormais invisible");
                p.sendMessage(ChatColor.GRAY + "Vous êtes désormais " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " aux joueurs normaux");
            }

            return true;

        } else if (cmd.equalsIgnoreCase("unvan")) {

            if (!isVanished(p)) {
                if (p == sender) {
                    sender.sendMessage(ChatColor.YELLOW + "Vous êtes déjà visible");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Le joueur " + p.getName() + ChatColor.YELLOW + " est déjà visible");
                }

                return true;
            }

            this.setShowPlayer(p);

            if (p == sender) {
                sender.sendMessage(ChatColor.GRAY + "Vous êtes désormais " + ChatColor.GREEN + "visible" + ChatColor.GRAY + " aux joueurs normaux");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " est désormais visible");
                p.sendMessage(ChatColor.GRAY + "Vous êtes désormais " + ChatColor.GREEN + "visible" + ChatColor.GRAY + " aux joueurs normaux");
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(player.hasPermission(PERM_VANISH_VAN)) {
            if (this.checkExistsByUUID(player.getUniqueId())) {
                boolean isVanished = this.getRecordedVanish(player.getUniqueId());
                vanished.put(event.getPlayer().getUniqueId().toString(), isVanished);
            } else {
                this.addPlayerVanish(event.getPlayer(), false);
                vanished.put(event.getPlayer().getUniqueId().toString(), false);
            }

            if (isVanished(player)) {
                this.setHidePlayer(player);
                player.sendMessage(ChatColor.GRAY + "Vous êtes toujours " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " pour les joueurs normaux");
            }
        } else {
            if (this.checkExistsByUUID(player.getUniqueId())) {
                this.removePlayerVanish(player);
            }

            for(String s : vanished.keySet()){
                Player p = Bukkit.getPlayer(UUID.fromString(s));
                boolean vanish = vanished.get(s);
                if(vanish) {
                    player.hidePlayer(AquilonThings.instance, p);
                }
            }
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        final Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(AquilonThings.instance, new Runnable() {
            @Override
            public void run() {
                vanished.remove(p.getPlayer().getUniqueId().toString());
            }
        }, 10);
    }

    @EventHandler
    public void onFoodLevel(FoodLevelChangeEvent event) {
        Player player = (Player) event.getEntity();
        if (isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isVanished((Player) event.getEntity())) {
            event.getEntity().setFireTicks(0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCollision(VehicleEntityCollisionEvent event) {
        if (event.getEntity() instanceof Player && isVanished((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickUp(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player p = (Player) entity;
        if (isVanished(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && isVanished((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    public static boolean isVanished(Player player) {
        if(vanished.get(player.getUniqueId().toString()) != null) {
            return vanished.get(player.getUniqueId().toString());
        } else return false;
    }

    public void setHidePlayer(Player player) {
        vanished.put(player.getUniqueId().toString(), true);

        if (player.hasPermission(PERM_VANISH_VAN)) {
            this.updatePlayerVanish(player, true);
        }

        for (Player p : Bukkit.getOnlinePlayers()){
            if (!p.hasPermission(PERM_VANISH_SEE)) {
                p.hidePlayer(AquilonThings.instance, player);
            }
        }
    }

    public void setShowPlayer(Player player){
        if (!player.hasPermission(PERM_VANISH_VAN)) {
            vanished.remove(player.getUniqueId().toString());
        } else {
            vanished.put(player.getUniqueId().toString(), false);
            this.updatePlayerVanish(player, false);
        }

        for (Player p : Bukkit.getOnlinePlayers()){
            if (!p.hasPermission(PERM_VANISH_SEE)) {
                p.showPlayer(AquilonThings.instance, player);
            }
        }
    }

    public void setPoof(Player player){
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 20, 1);
        for(double d = 0; d<2; d=d+0.125){
            Location loc = player.getLocation();
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc.add(0, d, 0), 4);
        }

    }

    public boolean checkExistsByUUID(UUID uuid) {
        Connection con = db.startTransaction();
        int res = 0;
        String sql = "SELECT EXISTS(SELECT 1 FROM aqlvanish WHERE uuid = ? LIMIT 1);";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getInt(1);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }

        db.endTransaction(con);
        return res == 1;
    }

    public void addPlayerVanish(Player player, boolean isVanished) {
        Connection con = db.startTransaction();
        String sql = "INSERT INTO aqlvanish VALUES (?, ?);";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setBoolean(2, isVanished);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return;
        }
        db.endTransaction(con);
    }

    public void removePlayerVanish(Player player) {
        Connection con = db.startTransaction();
        String sql = "DELETE FROM aqlvanish WHERE uuid = ?;";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return;
        }
        db.endTransaction(con);
    }

    public void updatePlayerVanish(Player player, boolean isVanished) {
        Connection con = db.startTransaction();
        String sql = "UPDATE aqlvanish SET vanished = ? WHERE uuid = ?;";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setBoolean(1, isVanished);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return;
        }
        db.endTransaction(con);
    }

    public boolean getRecordedVanish(UUID uuid) {
        Connection con = db.startTransaction();
        boolean res = false;
        String sql = "SELECT vanished FROM aqlvanish WHERE uuid = ?;";
        try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getBoolean(1);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return res;
    }
}

