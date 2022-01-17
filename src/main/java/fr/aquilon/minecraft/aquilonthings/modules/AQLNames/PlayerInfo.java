package fr.aquilon.minecraft.aquilonthings.modules.AQLNames;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Holds a player infos
 * <p>
 *     This means, it's display name and description
 * </p>
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class PlayerInfo {
    private final UUID player;
    private String name;
    private String description;

    public PlayerInfo(UUID player, String name, String description) {
        this.player = Objects.requireNonNull(player);
        this.name = name;
        this.description = description;
    }

    public PlayerInfo(UUID player) {
        this(player, null, null);
    }

    public UUID getPlayerUUID() {
        return player;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
    }

    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(player);
    }

    public String getName() {
        return name != null ? name : getOfflinePlayer().getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription(String def) {
        return description != null ? description : def;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
