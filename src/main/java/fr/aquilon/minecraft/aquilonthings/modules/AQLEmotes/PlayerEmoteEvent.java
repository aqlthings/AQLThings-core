package fr.aquilon.minecraft.aquilonthings.modules.AQLEmotes;

import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.json.JSONObject;

/**
 * Created on 22/05/2017.
 * @author Billi
 */
public class PlayerEmoteEvent extends PlayerEvent implements AquilonEvent<AQLEmotes> {
    private static HandlerList handlers = new HandlerList();

    private String emote;

    public PlayerEmoteEvent(Player who, String emote) {
        super(who);
        this.emote = emote;
    }

    public String getEmote() {
        return emote;
    }

    public void call(AQLEmotes m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("emote", emote);
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
