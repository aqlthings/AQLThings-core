package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.utils.Utils;
import org.json.JSONObject;

import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * Created by Billi on 20/04/2017.
 *
 * @author Billi
 */
public class WebsocketLogHandler extends Handler {
    private final IWebsocket parent;

    public WebsocketLogHandler(IWebsocket websocket) {
        parent = Objects.requireNonNull(websocket);
    }

    public IWebsocket getParent() {
        return parent;
    }

    @Override
    public void publish(LogRecord record) {
        JSONObject logJSON = new JSONObject();
        logJSON.put("logger",record.getLoggerName());
        logJSON.put("level",record.getLevel().getName());
        logJSON.put("class",record.getSourceClassName());
        logJSON.put("method",record.getSourceMethodName());
        logJSON.put("thread", Utils.threadFromID(record.getThreadID()).getName());
        logJSON.put("message",record.getMessage());
        logJSON.put("time", jsonDate(record.getMillis()));
        parent.submitWSMessage("log."+record.getLoggerName().toLowerCase()+'.'+record.getLevel().getName().toLowerCase(), logJSON);
    }

    @Override
    public void flush() {}
    @Override
    public void close() throws SecurityException {}
}
