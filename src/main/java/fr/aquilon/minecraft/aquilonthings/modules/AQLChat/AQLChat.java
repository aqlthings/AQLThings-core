package fr.aquilon.minecraft.aquilonthings.modules.AQLChat;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.utils.InvalidArgumentEx;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AQLThingsModule(
        name = "AQLChat",
        cmds = {
                @Cmd(value = AQLChat.COMMAND_CH, desc = "Gestion des cannaux de chat"),
                @Cmd(value = AQLChat.COMMAND_QMSG, desc = "Envoi de messages formatés")
        }
)
public class AQLChat implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();

    public static final String COMMAND_CH = "ch";
    public static final String COMMAND_QMSG = "qmsg";

    public static final String PERM_READ = AquilonThings.PERM_ROOT + ".chat.read.";
    public static final String PERM_SPEAK = AquilonThings.PERM_ROOT + ".chat.speak.";
    public static final String PERM_JOIN = AquilonThings.PERM_ROOT + ".chat.join.";
    public static final String PERM_LEAVE = AquilonThings.PERM_ROOT + ".chat.leave.";
    public static final String PERM_CHAT_FORMATED = AquilonThings.PERM_ROOT + ".chat.formated";

    public static final String DEFAULT_CHANNEL = "Global";
    public static final String SQL_ADD_PLAYER_CHAN = "INSERT INTO aqlchat_players VALUES (?, ?);";
    public static final String SQL_UPDATE_PLAYER_CHAN = "UPDATE aqlchat_players SET channel = ? WHERE uuid = ?;";
    public static final String SQL_ADD_PLAYER_BAN = "INSERT INTO aqlchat_bans VALUES (?, ?);";
    public static final String SQL_REMOVE_PLAYER_BAN = "DELETE FROM aqlchat_bans WHERE channel = ? AND uuid = ?;";
    public static final String SQL_GET_PLAYER_CHAN = "SELECT channel FROM aqlchat_players WHERE uuid = ?;";
    public static final String SQL_GET_PLAYER_BANS = "SELECT channel FROM aqlchat_bans WHERE uuid = ?;";

    private DatabaseConnector db;
    private List<AquilonChatEvent> chatStack = new ArrayList<>();
    private final HashMap<String, ChatChannel> channelList = new HashMap<>();
    private final HashMap<String, String> channelNicks = new HashMap<>();
    private final HashMap<String, ChatPlayer> playerInfos = new HashMap<>();

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        this.db = db;
        FileConfiguration config = AquilonThings.instance.getConfig();
        ConfigurationSection channels = config.getConfigurationSection("channels");
        if (channels==null) {
            LOGGER.mSevere("No configuration found. Disabling module.");
            return false;
        }

        for(String name : channels.getKeys(false)) {
            String nick = config.getString("channels." + name + ".nick").toUpperCase();
            int distance = config.getInt("channels." + name + ".distance", ChatChannel.DEFAULT_DISTANCE);
            String color = config.getString("channels." + name + ".color", ChatChannel.DEFAULT_COLOR.getChar()+"");
            String format = config.getString("channels." + name + ".format", ChatChannel.DEFAULT_FORMAT);
            ChatChannel channel = new ChatChannel(name, nick, distance, ChatColor.getByChar(color), format);

            channelList.put(name.toLowerCase(), channel);
            channelNicks.put(nick, name);
        }

        playerInfos.clear();
        for (Player p : Bukkit.getOnlinePlayers()){
            ChatPlayer chatPlayer = this.loadPlayerInfos(p.getUniqueId());
            playerInfos.put(p.getUniqueId().toString(), chatPlayer);
        }

        return true;
    }

    @Override
    public boolean onStop() {
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
        if (scmd.equalsIgnoreCase("qmsg")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Erreur dans les arguments de la commande");
                return false;
            }

            String message = String.join(" ",Arrays.asList(args).subList(1,args.length));

            if (args[0].equals("*")) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
            } else {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Joueur introuvable");
                    return false;
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            }
            return true;
        }

        if (scmd.equalsIgnoreCase("ch")) {
            if (args.length < 1) return false;

            if (args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.YELLOW + "Liste des channels: ");

                for (ChatChannel chan : channelList.values()) {
                    if (sender.hasPermission(chan.getReadPermission())) sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + chan.getColor() + chan.getName() + " (" + chan.getNick() + ")");
                }

                return true;
            } else if (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban")) {
                boolean ban = args[0].equalsIgnoreCase("ban");

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: "+ChatColor.WHITE+"/ch (ban/unban) <joueur> <channel>");
                    return true;
                }

                Player target = null;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().equalsIgnoreCase(args[1])) {
                        target = p;
                        break;
                    }
                }
                if (target==null) {
                    sender.sendMessage(ChatColor.RED + "De qui on parle ? Je le connais pas moi.");
                    return true;
                }

                String chanName = args[2];
                if (channelNicks.containsKey(args[2].toUpperCase())) chanName = channelNicks.get(args[2].toUpperCase());
                ChatChannel chan = channelList.get(chanName.toLowerCase());

                ChatPlayer pInfos = playerInfos.get(target.getUniqueId().toString());

                boolean success;
                String act = (ban?"":"dé")+"banni";
                if (ban) {
                    pInfos.banFromChannel(chan.getName());
                    success = addPlayerChannelBan(target, chan.getName());
                } else {
                    pInfos.unbanFromChannel(chan.getName());
                    success = removePlayerChannelBan(target, chan.getName());
                }

                if (success) {
                    sender.sendMessage(Utils.getPlayerColor(target) + target.getName() + ChatColor.YELLOW +
                            " a bien été "+act+" du channel " + chan.getColor() + chan.getName());
                    LOGGER.mInfo(target.getName()+" "+(ban?"":"dé")+"banni du channel "+chan.getNick());
                    target.sendMessage(ChatColor.YELLOW+"Vous avez été "+act+" du channel "+
                            chan.getColor() + chan.getName());
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Impossible de " + act + "r " +
                            Utils.getPlayerColor(target) + target.getName() + ChatColor.YELLOW +
                            " du channel " + chan.getColor() + chan.getName());
                    LOGGER.mInfo("Impossible de "+act+"r "+target.getName()+" du channel "+chan.getNick());
                }
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Tu causes pas depuis la console !");
                return true;
            }
            Player p = (Player) sender;
            ChatPlayer pInfos = playerInfos.get(p.getUniqueId().toString());
            if(args[0].equalsIgnoreCase("leave")) {
                if (args.length < 2) return false;

                String chanName = args[1];
                if (channelNicks.containsKey(args[1].toUpperCase())) chanName = channelNicks.get(args[1].toUpperCase());
                ChatChannel chan = channelList.get(chanName.toLowerCase());
                if (chan==null) {
                    sender.sendMessage(ChatColor.RED + "Channel inexistant !");
                    return true;
                }

                if(sender.hasPermission(chan.getLeavePermission())) {
                    if (!pInfos.isChannelHidden(chan.getName())) {
                        pInfos.hideChannel(chan.getName());
                        if (pInfos.getChannel()!=null && pInfos.getChannel().equalsIgnoreCase(chan.getName())) {
                            pInfos.setChannel(null);
                            updatePlayerChannel(p, null);
                        }

                        for(Player player : Bukkit.getOnlinePlayers()){
                            if(!player.getName().equalsIgnoreCase(sender.getName())){
                                player.sendMessage(ChatColor.YELLOW + "Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " a quitté le channel " + chan.getColor() + chan.getName());
                            }
                        }

                        sender.sendMessage(ChatColor.YELLOW + "Vous venez de quitter le channel " + chan.getColor() + chan.getName());
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Vous avez déjà quitté ce channel ...");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Vous ne pouvez pas quitter ce channel !");
                    return true;
                }
            } else { // Join and focus channel
                ChatChannel newChannel;
                String currentChan = pInfos.getChannel();
                String chanName = args[0];
                if (channelNicks.containsKey(args[0].toUpperCase())) chanName = channelNicks.get(args[0].toUpperCase());
                ChatChannel chan = channelList.get(chanName.toLowerCase());
                if (chan==null) {
                    sender.sendMessage(ChatColor.RED + "Channel inexistant !");
                    return true;
                }

                if (currentChan!=null && currentChan.equalsIgnoreCase(chan.getName())) {
                    sender.sendMessage(ChatColor.YELLOW + "Vous êtes déjà dans ce channel...");
                    return true;
                } else {
                    if (p.hasPermission(chan.getJoinPermission())) {
                        if (pInfos.isBannedFromChannel(chan.getName())) {
                            sender.sendMessage(ChatColor.RED + "Vous êtes banni de ce channel !");
                            return true;
                        } else {
                            newChannel = chan;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Vous ne pouvez pas rejoindre ce channel !");
                        return true;
                    }
                }

                pInfos.setChannel(newChannel.getName());
                this.updatePlayerChannel(p, newChannel.getName());

                if(pInfos.isChannelHidden(newChannel.getName())) {
                    pInfos.showChannel(newChannel.getName());
                    for(Player player : Bukkit.getOnlinePlayers()){
                        if(player.getName().equalsIgnoreCase(sender.getName())) continue;
                        player.sendMessage(ChatColor.YELLOW + "Le joueur " + Utils.getPlayerColor(p) + p.getName() + ChatColor.YELLOW + " a rejoint le channel " + newChannel.getColor() + newChannel.getName());
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Vous avez rejoint le canal " + newChannel.getColor() + newChannel.getName());
                }

                sender.sendMessage(ChatColor.YELLOW + "Vous parlez dans le canal " + newChannel.getColor() + newChannel.getName());
                return true;
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBeforeCommand(PlayerCommandPreprocessEvent evt) {
        String parts[] = evt.getMessage().split(" ",2);
        String cmd = parts[0];
        Player p = evt.getPlayer();
        if (evt.isCancelled()) return;
        if (!cmd.startsWith("/")) return;
        String cmdName = cmd.substring(1);
        if (!channelNicks.containsKey(cmdName.toUpperCase())) return;
        evt.setCancelled(true);
        String message = (parts.length>1?parts[1]:"");

        if (message.isEmpty()) {
            p.sendMessage(ChatColor.RED + "C'est mieux si le message est pas vide !");
            return;
        }

        ChatPlayer pInfos = playerInfos.get(p.getUniqueId().toString());
        String channelDefault = pInfos.getChannel();
        String channelName = channelNicks.get(cmdName.toUpperCase());
        ChatChannel channel = channelList.get(channelName.toLowerCase());

        if (channel!= null) {
            if (pInfos.isInChannel(channel)) {
                if (p.hasPermission(channel.getSpeakPermission())) {
                    pInfos.setChannel(channel.getName());
                } else {
                    p.sendMessage(ChatColor.RED + "Vous n'avez pas la permission pour parler dans ce channel !");
                    return;
                }
            } else if (pInfos.isBannedFromChannel(channel.getName())) {
                p.sendMessage(ChatColor.RED + "Vous êtes banni de ce channel...");
                return;
            } else {
                p.sendMessage(ChatColor.RED + "Vous avez quitté ce channel...");
                return;
            }
        }

        p.chat(message);
        playerInfos.get(p.getUniqueId().toString()).setChannel(channelDefault);
    }

    @EventHandler(ignoreCancelled = true)
    private void stackChat(AquilonChatEvent e) {
        if (chatStack.size()>=15) {
            chatStack = new ArrayList<>(chatStack.subList(chatStack.size() - 14, chatStack.size()));
        }
        chatStack.add(e);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ChatPlayer pInfos = loadPlayerInfos(event.getPlayer().getUniqueId());
        if(pInfos==null) {
            this.addPlayerChannel(event.getPlayer(), DEFAULT_CHANNEL);
            pInfos = new ChatPlayer(DEFAULT_CHANNEL);
        }
        playerInfos.put(event.getPlayer().getUniqueId().toString(), pInfos);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerInfos.remove(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent chatEvent) {
        chatEvent.setCancelled(true);
        Player p = chatEvent.getPlayer();
        ChatPlayer pInfos = playerInfos.get(p.getUniqueId().toString());
        String msg = chatEvent.getMessage();

        if(pInfos.getChannel() == null){
            p.sendMessage(ChatColor.RED + "Vous avez quitté ce channel...");
        } else {
            ChatChannel chan = channelList.get(pInfos.getChannel().toLowerCase());
            if (chan == null) {
                LOGGER.mSevere("Channel inexistant ("+pInfos.getChannel()+")");
                return;
            }
            AquilonChatEvent chat = null;
            try {
                chat = new AquilonChatEvent(p, chan, msg);
            } catch (InvalidArgumentEx e) {
                if (e.getCode()==AquilonChatEvent.EX_CODE_MESSAGE_TOO_LONG) {
                    p.sendMessage(ChatColor.RED+"Message trop long. (max "+AquilonChatEvent.MAX_LENGTH_MESSAGE+")");
                    return;
                }
                return;
            }

            chat.clearRecipients();
            chat.addAllRecipient(chatEvent.getRecipients());
            chat.call(this);
        }
    }

    public ChatPlayer getPlayerInfos(Player p) {
        return playerInfos.get(p.getUniqueId().toString());
    }

    public void addPlayerChannel(Player player, String channel) {
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_ADD_PLAYER_CHAN);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, channel);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_ADD_PLAYER_CHAN);
            return;
        }
        db.endTransaction(con);
    }

    public void updatePlayerChannel(Player player, String channel) {
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_UPDATE_PLAYER_CHAN);
            stmt.setString(1, channel);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_UPDATE_PLAYER_CHAN);
            return;
        }
        db.endTransaction(con);
    }

    public boolean addPlayerChannelBan(Player player, String channel) {
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_ADD_PLAYER_BAN);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, channel);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_ADD_PLAYER_BAN);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public boolean removePlayerChannelBan(Player player, String channel) {
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_REMOVE_PLAYER_BAN);
            stmt.setString(1, channel);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_REMOVE_PLAYER_BAN);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    public String getRecordedPlayerChannel(UUID uuid) {
        String res = null;
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_GET_PLAYER_CHAN);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getString(1);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_GET_PLAYER_CHAN);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public ChatPlayer loadPlayerInfos(UUID uuid) {
        String pChan = getRecordedPlayerChannel(uuid);
        if (pChan==null) return null;
        ChatPlayer res;
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_GET_PLAYER_BANS);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            res = new ChatPlayer(pChan);
            while (rs.next()) {
                res.banFromChannel(rs.getString(1));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_GET_PLAYER_BANS);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    public AquilonChatEvent[] getChatHistory() {
        return chatStack.toArray(new AquilonChatEvent[0]);
    }

    public Set<String> getChannelNames() {
        return Collections.unmodifiableSet(channelList.keySet());
    }

    public List<ChatChannel> getChannelList() {
        return Collections.unmodifiableList(new ArrayList<>(channelList.values()));
    }

    public ChatChannel getChannel(String channel) {
        return channelList.get(channel);
    }
}
