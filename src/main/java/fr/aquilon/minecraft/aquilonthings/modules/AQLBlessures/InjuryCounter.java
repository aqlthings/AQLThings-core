package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import java.util.HashMap;

/**
 * Created by Billi on 03/05/2017.
 *
 * @author Billi
 */
public class InjuryCounter {
    private AQLBlessures module;
    private InjuryConfig config;
    private HashMap<String, PlayerState> playerStates;
    private boolean started;
    private boolean paused;

    public InjuryCounter(AQLBlessures module, InjuryConfig config) {
        this.module = module;
        this.config = config;
        this.playerStates = new HashMap<>();
        this.started = false;
        this.paused = false;
    }

    public void setPlayerStates(HashMap<String, PlayerState> playerStates) {
        this.playerStates = playerStates;
    }

    public void addPlayerState(PlayerState pState) {
        playerStates.put(pState.getPlayerUUID().toString().replace("-",""), pState);
    }

    public HashMap<String, PlayerState> getPlayerStates() {
        return playerStates;
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
        for (PlayerState p : getPlayerStates().values()) {
            p.updatePlayer(this, p.getPlayer());
        }
    }
}
