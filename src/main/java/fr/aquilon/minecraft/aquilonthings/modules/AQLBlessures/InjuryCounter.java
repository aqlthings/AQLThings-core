package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Holds player states and configuration of an injury counter
 * @author Billi
 */
public class InjuryCounter implements JSONExportable {
    private final String name;
    private AQLBlessures module;
    private InjuryConfig config;
    private HashMap<String, PlayerState> playerStates;
    private boolean started;
    private boolean paused;

    public InjuryCounter(String name, AQLBlessures module, InjuryConfig config) {
        this.name = Objects.requireNonNull(name);
        this.module = Objects.requireNonNull(module);
        this.config = Objects.requireNonNull(config);
        this.playerStates = new HashMap<>();
        this.started = false;
        this.paused = false;
    }

    public String getName() {
        return name;
    }

    public PlayerState getPlayerState(UUID playerId) {
        return getPlayerState(playerId.toString().replaceAll("-", ""));
    }

    public PlayerState getPlayerState(String shortUUID) {
        return playerStates.get(shortUUID);
    }

    public void putPlayerState(PlayerState pState) {
        playerStates.put(pState.getPlayerUUID().toString().replace("-",""), pState);
    }

    public void removePlayerState(String shortUUID) {
        playerStates.remove(shortUUID);
    }

    public Iterator<PlayerState> getPlayerStateIterator() {
        return playerStates.values().iterator();
    }

    public int getPlayerCount() {
        return playerStates.size();
    }

    public List<PlayerState> getPlayerStates() {
        return Collections.unmodifiableList(new ArrayList<>(playerStates.values()));
    }

    public void setModule(AQLBlessures module) {
        this.module = module;
    }

    public AQLBlessures getModule() {
        return module;
    }

    public void setConfig(InjuryConfig config) {
        this.config = config;
    }

    public InjuryConfig getConfig() {
        return config;
    }

    public boolean isActive() {
        return started && !paused;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isPaused() {
        return started && paused;
    }

    public void start() {
        if (isActive()) return;
        if (isPaused()) paused = false;
        else if (!isStarted()) {
            started = true;
            paused = false;
        }
        update();
    }

    public void pause() {
        if (!isActive() || isPaused()) return;
        paused = true;
    }

    public void stop() {
        if (!isStarted()) return;
        started = false;
        paused = false;
    }

    public void update() {
        for (PlayerState p : getPlayerStates()) {
            p.updatePlayer(this, p.getPlayer());
        }
    }

    @Override
    public JSONObject toJSON() {
        return toJSON(false);
    }

    public JSONObject toJSON(boolean detailed) {
        JSONObject res = new JSONObject();
        res.put("name", name);
        res.put("started", started);
        res.put("paused", paused);
        if (detailed) {
            JSONObject players = new JSONObject();
            playerStates.values()
                    .forEach(s -> players.put(s.getPlayerUUID().toString().replaceAll("-", ""), s.toJSON()));
            res.put("players", players);
            res.put("config", config.toJSON());
        }
        return res;
    }
}
