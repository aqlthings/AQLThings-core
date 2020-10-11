package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters;

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
public class PlayerRaceEvent extends PlayerEvent implements AquilonEvent<AQLCharacters> {
    private static HandlerList handlers = new HandlerList();

    private String race;

    public PlayerRaceEvent(Player who, String race) {
        super(who);
        this.race = race;
    }

    public String getRace() {
        return race;
    }

    public void call(AQLCharacters m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("race", race);
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
