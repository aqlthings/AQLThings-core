package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.json.JSONObject;

/**
 * Created on 14/07/2017.
 *
 * @author Billi
 */
public class PlayerLeavesPlaceEvent extends PlayerEvent implements AquilonEvent<AQLPlaces> {
    private Place place;

    private static HandlerList handlers = new HandlerList();

    public PlayerLeavesPlaceEvent(Player who, Place p) {
        super(who);
        this.place = p;
    }

    public void call(AQLPlaces m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("place", place.toJSON());
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
