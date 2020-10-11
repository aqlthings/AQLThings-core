package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import org.json.JSONObject;

import java.util.List;

/**
 * An interface to be implemented by Websocket handlers
 * @author Billi
 */
public interface IWebsocket {
    List<APIWebSocket> getWsClients();

    void registerWSClient(APIWebSocket c);

    void unregisterWSClient(APIWebSocket c);

    void submitWSMessage(String topic, JSONObject data);

    void registerAction(String key, IWebsocketAction action);

    void dispatchAction(APIWebSocket client, String action, String topic, JSONObject request) throws APIException;
}
