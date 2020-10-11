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

/**
 * An event dispatched when a player changes it's display name
 * @author Billi
 */
public class PlayerNameChangeEvent extends PlayerEvent implements AquilonEvent<AQLNames> {
    private static final HandlerList handlers = new HandlerList();

    private final String oldName;
    private final String newName;

    public PlayerNameChangeEvent(Player who, String oldName) {
        super(who);
        this.oldName = oldName;
        this.newName = who.getDisplayName();
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void call(AQLNames m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        if (getOldName()!=null) {
            JSONObject oldName = new JSONObject();
            oldName.put("mc", getOldName());
            oldName.put("text", ChatColor.stripColor(getOldName()));
            oldName.put("html", MinecraftParser.parseHTML(getOldName()));
            res.put("oldName", oldName);
        } else res.put("oldName", JSONObject.NULL);
        JSONObject newName = new JSONObject();
        newName.put("mc", getNewName());
        newName.put("text", ChatColor.stripColor(getNewName()));
        newName.put("html", MinecraftParser.parseHTML(getNewName()));
        res.put("newName", newName);
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
