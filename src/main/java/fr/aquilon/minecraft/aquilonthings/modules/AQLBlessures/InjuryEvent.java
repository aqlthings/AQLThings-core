package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
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
public class InjuryEvent extends PlayerEvent implements AquilonEvent<AQLBlessures> {
    private boolean auto;
    private Injury injury;
    private PlayerState pState;

    private static HandlerList handlers = new HandlerList();

    public InjuryEvent(Player who, Injury injury) {
        super(who);
        this.auto = false;
        this.injury = injury;
    }

    public InjuryEvent(PlayerState pState) {
        super(pState.getPlayer());
        this.auto = true;
        this.pState = pState;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res;
        if (auto) {
            res = pState.toJSON();
        } else {
            res = new JSONObject();
            res.put("player", JSONPlayer.toJSON(getPlayer(), false));
            res.put("injury", injury.toJSON());
            res.put("position", JSONUtils.jsonLocation(getPlayer().getLocation()));
        }
        res.put("auto", auto);
        return res;
    }

    public void call(AQLBlessures m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
