package fr.aquilon.minecraft.aquilonthings.modules.AQLNames;

import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.MinecraftParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.json.JSONObject;

import java.util.Objects;

/**
 * An event dispatched when a player changes it's description
 * @author Billi
 */
public class PlayerDescriptionChangeEvent extends PlayerEvent implements AquilonEvent<AQLNames> {
    private static final HandlerList handlers = new HandlerList();

    private final String oldDesc;
    private final String newDesc;

    public PlayerDescriptionChangeEvent(Player who, String oldDesc, String newDesc) {
        super(who);
        this.oldDesc = oldDesc;
        this.newDesc = Objects.requireNonNull(newDesc);
    }

    public PlayerDescriptionChangeEvent(Player who, String newDesc) {
        this(who, null, newDesc);
    }

    public String getOldDescription() {
        return oldDesc;
    }

    public String getNewDescription() {
        return newDesc;
    }

    public void call(AQLNames m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        if (oldDesc!=null) {
            JSONObject oldName = new JSONObject();
            oldName.put("mc", oldDesc);
            oldName.put("text", ChatColor.stripColor(oldDesc));
            oldName.put("html", MinecraftParser.parseHTML(oldDesc));
            res.put("oldName", oldName);
        } else res.put("oldDesc", JSONObject.NULL);
        JSONObject newName = new JSONObject();
        newName.put("mc", newDesc);
        newName.put("text", ChatColor.stripColor(newDesc));
        newName.put("html", MinecraftParser.parseHTML(newDesc));
        res.put("newDesc", newName);
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
