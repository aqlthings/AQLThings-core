package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.json.JSONObject;

/**
 * Created on 14/07/2017.
 *
 * @author Billi
 */
public class PlayerEntersBiomeEvent extends PlayerEvent implements AquilonEvent<AQLPlaces> {
    private Biome newBiome;
    private Biome oldBiome;
    private Location pos;

    private static HandlerList handlers = new HandlerList();

    public PlayerEntersBiomeEvent(Player who, Biome newBiome, Biome oldBiome) {
        super(who);
        this.newBiome = newBiome;
        this.oldBiome = oldBiome;
        this.pos = who.getLocation();
    }

    public void call(AQLPlaces m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("position", JSONUtils.jsonLocation(pos));
        res.put("newBiome", newBiome.name());
        res.put("oldBiome", (oldBiome!=null?oldBiome.name(): JSONObject.NULL));
        res.put("infos", JSONUtils.jsonBlockEnvironment(pos.getBlock()));
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
