package fr.aquilon.minecraft.aquilonthings.modules.AQLChat;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.aquilonthings.utils.Rank;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.InvalidArgumentEx;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import fr.aquilon.minecraft.utils.MinecraftParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * An event dispatched by AQLChat when a message is sent to a channel
 * @author Billi
 */
public class AquilonChatEvent extends Event implements Cancellable, AquilonEvent<AQLChat> {
    public static final int MAX_LENGTH_MESSAGE = 500;

    public static final int EX_CODE_BAD_ANONYMOUS_CHANNEL = 1;
    public static final int EX_CODE_MESSAGE_TOO_LONG = 2;
    public static final int EX_CODE_NAME_TOO_LONG = 3;
    public static final int EX_CODE_MESSAGE_EMPTY = 4;

    public static final String FORMAT_LOG = "[{nick}] {sender_name} ({sender_rp}): {message}";

    private static final HandlerList handlers = new HandlerList();

    private final long when;
    private boolean cancelled;

    private static final String ANONYMOUS_NAME = "Console";
    private static final ChatColor ANONYMOUS_COLOR = ChatColor.RED;
    private static final ChatColor DEFAULT_COLOR = ChatColor.DARK_GRAY;

    private final AQLChat chat;

    private final Player sender;
    private final ChatChannel channel;
    private final String message;
    private final Set<Player> recipients;

    private ChatColor pColor;
    private String pPrefix;
    private String pSuffix;
    private String pName;
    private String pDisplayName;

    public AquilonChatEvent(Player sender, ChatChannel chan, String msg) throws InvalidArgumentEx {
        chat = AquilonThings.instance.getModuleData(AQLChat.class);
        if (chat == null) throw new IllegalStateException("Cannot create a chat event when AQLChat isn't loaded");
        this.sender = sender;
        if (sender==null && chan.getDistance()!=0) throw new InvalidArgumentEx(EX_CODE_BAD_ANONYMOUS_CHANNEL, "Anonymous is allowed in global channels only.");
        this.channel = chan;
        if (msg.length()<1) throw new InvalidArgumentEx(EX_CODE_MESSAGE_EMPTY, "Message is empty");
        if (msg.length()>MAX_LENGTH_MESSAGE) throw new InvalidArgumentEx(EX_CODE_MESSAGE_TOO_LONG, "Message is too long");
        this.message = msg;
        this.recipients = new HashSet<>();
        this.when = System.currentTimeMillis();
        pColor = ANONYMOUS_COLOR;
        pPrefix = null;
        pSuffix = null;
        if (sender != null) {
            pColor = DEFAULT_COLOR;
            Rank rank = Utils.getPlayerRank(sender.getUniqueId());
            if (rank != null) {
                pColor = rank.getColor();
                pPrefix = rank.getPrefix();
                pSuffix = rank.getSuffix();
            }
        }
        pName        = (sender!=null ? sender.getName() : ANONYMOUS_NAME);
        pDisplayName = (sender!=null ? sender.getDisplayName() : ANONYMOUS_NAME);
    }

    public AquilonChatEvent(String name, ChatColor color, ChatChannel chan, String msg) throws InvalidArgumentEx {
        this(null, chan, msg);
        if (name.length()>15) throw new InvalidArgumentEx(EX_CODE_NAME_TOO_LONG, "Username is too long");
        if (color!=null) this.pColor = color;
        if (name!=null) {
            this.pName = name;
            this.pDisplayName = name;
        }
    }

    public boolean isConsole() {
        return sender==null;
    }

    public String getSenderName() {
        return pName;
    }

    public ChatChannel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public final Player getSender() {
        return sender;
    }

    public ChatColor getSenderColor() {
        return pColor;
    }

    public String getSenderPrefix() {
        return pPrefix;
    }

    public String getSenderSuffix() {
        return pSuffix;
    }

    public String getSenderDisplayName() {
        return pDisplayName;
    }

    /**
     * Whether the message content should be decoded for color formatting
     * @return <code>true</code> if the sender is console or has the permission, <code>false</code> otherwise
     */
    public boolean isMessageColored() {
        return isConsole() || sender.hasPermission(channel.getFormatPermission());
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Set<Player> getRecipients() {
        return recipients;
    }

    public void addRecipient(Player p) {
        recipients.add(p);
    }

    public void addAllRecipient(Collection<Player> p) {
        recipients.addAll(p);
    }

    public void clearRecipients() {
        recipients.clear();
    }

    public void fillRecipients() {
        for (Player p: Bukkit.getOnlinePlayers()) {
            addRecipient(p);
        }
    }

    public void call(AQLChat m) {
        if (isCancelled()) return;
        Bukkit.getScheduler().runTask(AquilonThings.instance,
                () -> Bukkit.getServer().getPluginManager().callEvent(this));
    }

    public void handleEvent() {
        if (isCancelled()) return;

        String msg = toMC();
        Iterator<Player> iter = recipients.iterator();
        while (iter.hasNext()) {
            Player p = iter.next();
            Location loc = p.getLocation();
            if (channel.getDistance() > 0) {
                if (sender.getLocation().getWorld()!=loc.getWorld()) continue;
                if (sender.getLocation().distance(loc) > channel.getDistance()) continue;
            }
            if (!p.hasPermission(channel.getReadPermission())) continue;
            if (!chat.getPlayerInfos(p).isInChannel(channel)) continue;
            p.sendMessage(msg);
        }
        ModuleLogger.logStatic(AQLChat.class, Level.INFO, null, toString(), null);
    }

    public String toMC() {
        return this.toMC(isMessageColored());
    }

    public String toMC(boolean colored) {
        return channel.computeMessage(message, pName, pDisplayName, pPrefix, pSuffix, pColor, colored);
    }

    public static String getDecoratedSenderName(String pName, String pPrefix, String pSuffix, ChatColor pColor) {
        return (pPrefix != null ? pPrefix : "") +
                (pColor != null ? pColor.toString() : "") +
                pName + (pSuffix != null ? pSuffix : "");
    }

    public String toHTML() {
        return MinecraftParser.parseHTML(this.toMC());
    }

    public String toUnix() {
        return MinecraftParser.parseUnix(this.toMC());
    }

    @Override
    public String toString() {
        return FORMAT_LOG
                .replace("{nick}", channel.getNick())
                .replace("{sender_rp}", pDisplayName)
                .replace("{sender_name}", pName)
                .replace("{message}", message);
    }

    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        if (sender!=null) res.put("sender", JSONPlayer.toJSON(sender, false));
        else {
            JSONObject sender = new JSONObject();
            sender.put("pseudoUser", true);
            sender.put("name", pName);
            sender.put("color", JSONUtils.jsonColor(pColor.getChar()));
            res.put("sender", sender);
        }
        res.put("message", JSONUtils.jsonColoredString(getMessage()));
        res.put("output", JSONUtils.jsonColoredString(toMC()));
        res.put("channel", getChannel().toJSON());
        res.put("time", jsonDate(when));
        return res;
    }
}
