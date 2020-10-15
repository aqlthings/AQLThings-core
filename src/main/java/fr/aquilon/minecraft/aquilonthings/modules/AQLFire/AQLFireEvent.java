package fr.aquilon.minecraft.aquilonthings.modules.AQLFire;

import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockIgniteEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created on 13/07/2017.
 *
 * @author Billi
 */

public class AQLFireEvent extends Event implements AquilonEvent<AQLFire> {
    private Location position;
    private BlockIgniteEvent.IgniteCause cause;
    private Player player;
    private BlockIgniteEvent event;

    private ArrayList<JSONPlayer> targetsNear;
    private ArrayList<JSONPlayer> targetsFar;
    private int fireCount = -1;

    private static HandlerList handlers = new HandlerList();

    public AQLFireEvent(BlockIgniteEvent event) {
        this.event = event;
        this.position = event.getBlock().getLocation();
        this.cause = event.getCause();
        this.player = event.getPlayer();
        targetsNear = new ArrayList<>();
        targetsFar = new ArrayList<>();
        cancelFire(event);
    }

    private void cancelFire(BlockIgniteEvent event) {
        boolean cancel = true; // Par défaut on annule
        if (cause == BlockIgniteEvent.IgniteCause.LAVA || event.getBlock().getWorld().getEnvironment() != World.Environment.NORMAL) {
            // Pas d'incendie ailleurs que dans l'overworld / Pas d'incendie dus à la lave.
            cancel = true;
        }
        if (cause.equals(BlockIgniteEvent.IgniteCause.LIGHTNING)) {
            Random random = new Random();
            // Les éclairs déclenchent un feu qu'une fois sur 10
            cancel = !(random.nextInt(10) == 0);
        }
        // On autorise les membres à poser du feu mais on prévient le staff
        if (cause.equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)) {
            cancel = false;
        }
        // Si le feu se répands
        if (cause.equals(BlockIgniteEvent.IgniteCause.SPREAD)) {
            cancel = false;
        }
        if (cancel) event.setCancelled(true);
    }

    public void call(AQLFire f) {
        if (event.isCancelled()) return;
        boolean bubble = false;
        if (cause.equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) && !player.hasPermission(AQLFire.PERM_ALLOWED)) {
            Utils.warnStaff(AQLFire.class, Utils.decoratePlayerName(player) + ChatColor.RED + " est en train d'utiliser du feu ! " +
                    ChatColor.WHITE+"/tpfire"+ChatColor.RED+" pour vous y téléporter.");
            f.registerAlert(position, true);
            bubble = true;
        } else if (cause.equals(BlockIgniteEvent.IgniteCause.SPREAD) && f.checkAlert(position)) {
            //On vérifie le nombre de blocs de feu dans les blocs alentours
            int blockX = position.getBlockX();
            int blockY = position.getBlockY();
            int blockZ = position.getBlockZ();
            fireCount = 0;
            int r = f.getRadiusCheck();
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (position.getWorld().getBlockAt(blockX + x, blockY + y, blockZ + z).getType().equals(Material.FIRE)) {
                            fireCount++;
                        }
                    }
                }
            }

            ArrayList<String> ignoreList = new ArrayList<>();
            if (fireCount>=f.getMinBlocksAlertPlayers()) {
                for (Player currentPlayer : Bukkit.getOnlinePlayers()) {
                    Location pLoc = currentPlayer.getLocation();
                    if (pLoc.getWorld()!=position.getWorld()) continue;
                    if (pLoc.distance(position) <= f.getRadiusAlert()) {
                        //Envoi du message en rouge
                        currentPlayer.sendMessage(ChatColor.RED + "[Event] Un incendie semble s'être déclaré aux alentours proches !");
                        targetsNear.add(new JSONPlayer(currentPlayer));
                    } else if (pLoc.distance(position) <= f.getRadiusAlertFar()) {
                        //Envoi du message en bleu
                        currentPlayer.sendMessage(ChatColor.DARK_AQUA + "[Event] Un incendie semble s'être déclaré aux alentours !");
                        targetsFar.add(new JSONPlayer(currentPlayer));
                    }
                    if (Utils.playerHasWarningPerm(currentPlayer, AQLFire.class))
                        ignoreList.add(currentPlayer.getName());
                }
                f.registerAlert(position);
                bubble = true;
            }
            if (fireCount>=f.getMinBlocksAlertStaff()) {
                Utils.warnStaff(AQLFire.class,
                        ChatColor.RED.toString() + "Un incendie semble s'être déclaré en " + blockX + "/" + blockY + "/" + blockZ +
                                " (" + ChatColor.GRAY.toString() + position.getWorld().getName() + ChatColor.RED.toString() + ") ! " +
                                ChatColor.WHITE.toString() + "/tpfire" + ChatColor.RED.toString()+" pour vous y téléporter.",
                        ignoreList.toArray(new String[0])
                );
                f.registerAlert(position, true);
                bubble = true;
            }
        }

        if (bubble) Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("type", cause.name());
        res.put("position", JSONUtils.jsonLocation(position));
        res.put("player", (event.getPlayer()!=null?JSONPlayer.toJSON(event.getPlayer(), false):JSONObject.NULL));
        JSONObject targets = new JSONObject();
        if (!targetsNear.isEmpty()) {
            targets.put("near", JSONUtils.jsonArray(targetsNear));
        }
        if (!targetsFar.isEmpty()) {
            targets.put("far", JSONUtils.jsonArray(targetsFar));
        }
        if (!targets.isEmpty()) res.put("targetedPlayers", targets);
        if (fireCount!=-1) res.put("fireCount", fireCount);
        return res;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
