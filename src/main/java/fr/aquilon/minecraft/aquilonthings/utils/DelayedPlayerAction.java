package fr.aquilon.minecraft.aquilonthings.utils;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerEvent;

import java.util.UUID;

/**
 * Created by Billi on 02/02/2019.
 *
 * @author Billi
 */
public class DelayedPlayerAction {
    private Class<? extends PlayerEvent> event;
    private String playerUUID;
    private int delay;
    private Runnable action;

    public DelayedPlayerAction(Class<? extends PlayerEvent> event, UUID player, int delay, Runnable action) {
        this.event = event;
        this.playerUUID = player.toString().replace("-","");
        this.delay = delay;
        this.action = action;
    }

    public DelayedPlayerAction(Class<? extends PlayerEvent> event, UUID player, Runnable action) {
        this(event,player, 0,action);
    }

    public Class<? extends PlayerEvent> getEvent() {
        return event;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public int getDelay() {
        return delay;
    }

    public Runnable getAction() {
        return action;
    }

    public void run() {
        if (delay>0) {
            Bukkit.getScheduler().runTaskLater(AquilonThings.instance, () -> {
                action.run();
            }, delay);
        } else action.run();
    }
}
