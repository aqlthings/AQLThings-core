package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIWebSocket;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocketAction;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.WebsocketLogHandler;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * An API module providing a single route to a websocket
 * TODO: Move Event Handlers in individual modules
 * @author Billi
 */
public class Websocket extends APIModule implements IWebsocket {
    public static final String MODULE_NAME = "ws";

    private final List<APIWebSocket> wsClients;
    private final Object clientListLock = new Object();
    private final Map<String, IWebsocketAction> actions;

    public Websocket(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        this.wsClients = new ArrayList<>();
        this.actions = new HashMap<>();
    }

    @Override
    public void onReady() {
        registerWebsocketRoute("websocket", "/", this);
        Logger globalLogger = Logger.getGlobal();
        Arrays.stream(globalLogger.getHandlers())
                .filter(h -> h instanceof WebsocketLogHandler && ((WebsocketLogHandler) h).getParent() == this)
                .forEach(globalLogger::removeHandler);
        globalLogger.addHandler(new WebsocketLogHandler(this));
        /*
        Enumeration<String> loggers = LogManager.getLogManager().getLoggerNames();
        while (loggers.hasMoreElements()) {
            String l = loggers.nextElement();
            Logger.getLogger(l);
        }
        */
    }

    @Override
    public void registerAction(String key, IWebsocketAction action) {
        // How to handle conflicting keys ?
        actions.put(key, action);
    }

    @Override
    public void dispatchAction(APIWebSocket client, String action, String topic, JSONObject request) throws APIException {
        if (!actions.containsKey(action)) return;
        actions.get(action).action(client, action, topic, request);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(evt.getPlayer(), false));
        res.put("time", jsonDate(System.currentTimeMillis()));
        submitWSMessage("players.join", res);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent evt) {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(evt.getPlayer(), false));
        res.put("time", jsonDate(System.currentTimeMillis()));
        submitWSMessage("players.leave", res);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent evt) {
        Player p = evt.getPlayer();
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(p, false));
        res.put("position", JSONUtils.jsonLocation(p.getLocation(), true));
        res.put("environment", JSONUtils.jsonBlockEnvironment(p.getLocation().getBlock()));
        res.put("time", jsonDate(System.currentTimeMillis()));
        submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".move", res);
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent evt) {
        Vehicle v = evt.getVehicle();
        v.getPassengers().stream()
                .filter(e -> e instanceof Player)
                .forEach(p -> onPlayerMove(new PlayerMoveEvent((Player) p, evt.getFrom(), evt.getTo())));
    }

    @Override
    public List<APIWebSocket> getWsClients() {
        List<APIWebSocket> clients;
        synchronized (clientListLock) {
            clients = Collections.unmodifiableList(new ArrayList<>(wsClients));
        }
        return clients;
    }

    @Override
    public void registerWSClient(APIWebSocket c) {
        synchronized (clientListLock) {
            wsClients.add(c);
        }
    }

    @Override
    public void unregisterWSClient(APIWebSocket c) {
        synchronized (clientListLock) {
            wsClients.remove(c);
        }
    }

    @Override
    public void submitWSMessage(String topic, JSONObject data) {
        Iterator<APIWebSocket> clients;
        synchronized (clientListLock) {
            clients = wsClients.iterator();
        }
        while (clients.hasNext()) {
            APIWebSocket client = clients.next();
            if (client.hasTopic(topic)) client.sendMessage(topic, data);
        }
    }
}
