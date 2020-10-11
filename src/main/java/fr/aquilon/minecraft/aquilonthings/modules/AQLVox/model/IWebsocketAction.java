package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import org.json.JSONObject;

/**
 * Represents a method to be called when a specific action is dispatched through a websocket
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
@FunctionalInterface
public interface IWebsocketAction {
    void action(APIWebSocket client, String action, String topic, JSONObject request) throws APIException;
}
