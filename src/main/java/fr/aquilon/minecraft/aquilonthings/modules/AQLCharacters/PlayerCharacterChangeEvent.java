package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters;

import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Character;
import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Event dispatched when a player selects a Character
 * @author Billi
 */
public class PlayerCharacterChangeEvent extends PlayerEvent implements AquilonEvent<AQLCharacters> {
    private static final HandlerList handlers = new HandlerList();

    private final Character oldChar;
    private final Character newChar;

    public PlayerCharacterChangeEvent(Player who, Character oldChar, Character newChar) {
        super(who);
        this.oldChar = oldChar;
        this.newChar = Objects.requireNonNull(newChar);
    }

    public PlayerCharacterChangeEvent(Player who, Character newChar) {
        this(who, null, newChar);
    }

    public Character getOldCharacter() {
        return oldChar;
    }

    public Character getNewCharacter() {
        return newChar;
    }

    public void call(AQLCharacters m) {
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("oldCharacter", oldChar!=null ? oldChar.toJSON(false, true) : JSONObject.NULL);
        res.put("newName", newChar.toJSON(false, true));
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
