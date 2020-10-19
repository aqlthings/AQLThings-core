package fr.aquilon.minecraft.aquilonthings.modules.AQLBabel;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.AQLChat.AquilonChatEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLChat.ChatChannel;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AQLThingsModule(
        name = "AQLBabel",
        cmds = @Cmd(value = AQLBabel.COMMAND_BABEL, desc = "Gestion des langues")
)
public class AQLBabel implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();

    public static final String COMMAND_BABEL = "babel";

    public static final String PERM_INFO_SELF = AquilonThings.PERM_ROOT + ".babel.info.self";
    public static final String PERM_INFO_OTHERS = AquilonThings.PERM_ROOT + ".babel.info.others";
    public static final String PERM_EDIT_LANG = AquilonThings.PERM_ROOT + ".babel.edit.lang";
    public static final String PERM_EDIT_LEVEL = AquilonThings.PERM_ROOT + ".babel.edit.level";
    public static final String PERM_LANG = AquilonThings.PERM_ROOT + ".babel.lang";
    public static final String PERM_LANG_SPEAK = PERM_LANG + ".speak.";
    public static final String PERM_LANG_READ = PERM_LANG + ".read.";

    private static final String SQL_FIND_ALL_LANGS = "SELECT * FROM aqlbabel_lang";
    private static final String SQL_FIND_LANG_PLAYERS = "SELECT pl.*, p.player_name " +
            "FROM aqlbabel_player p, aqlbabel_player_lang pl " +
            "WHERE pl.lang = ? AND p.player = pl.player AND pl.lvl > 0";
    private static final String SQL_FIND_PLAYER_INFO = "SELECT * FROM aqlbabel_player WHERE player = ? ";
    private static final String SQL_FIND_PLAYER_LANGS = "SELECT * FROM aqlbabel_player_lang WHERE player = ? ";
    private static final String SQL_UPDATE_PLAYER_INFO = "INSERT INTO aqlbabel_player VALUES " +
            "(?, ?, ?) ON DUPLICATE KEY UPDATE player_name = ?, selected_lang = ?";
    private static final String SQL_UPDATE_PLAYER_LANGS = "INSERT INTO aqlbabel_player_lang VALUES " +
            "(?, ?, ?, ?) ON DUPLICATE KEY UPDATE lvl = ?, comment = ?";
    private static final String SQL_UPDATE_LANGUAGE = "INSERT INTO aqlbabel_lang VALUES " +
            "(?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = ?, alphabet = ?, description = ?";
    private static final String SQL_RESET_PLAYER_LANG = "UPDATE aqlbabel_player " +
            "SET selected_lang = null WHERE selected_lang = ?";
    private static final String SQL_DELETE_PLAYER_LANG = "DELETE FROM aqlbabel_player_lang WHERE lang = ?";
    private static final String SQL_DELETE_LANG = "DELETE FROM aqlbabel_lang WHERE lang = ?";

    private DatabaseConnector db;
    private Map<String, Language> languages;
    private Map<String, BabelPlayer> playerInfos;

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        this.db = db;
        languages = retrieveLanguages();
        playerInfos = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) getPlayerInfo(p);
        return true;
    }

    @Override
    public boolean onStop() {
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!alias.equalsIgnoreCase(COMMAND_BABEL)) return false;
        final String USAGE = ChatColor.YELLOW+"Usage: "+ ChatColor.WHITE+
                "/babel (info [<player>])|(select <lang>)|(set <player> <lang> <level> [<comment>])(lang ...)";
        if (args.length < 1) {
            sender.sendMessage(USAGE);
            return true;
        }
        if (args[0].equals("info")) {
            Player target = null;
            if (sender instanceof Player) {
                target = (Player) sender;
            }
            if (args.length > 1) {
                target = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getName().equals(args[1]))
                        .findFirst().orElse(null);
            }
            if (target == null) {
                sender.sendMessage(ChatColor.YELLOW+"Missing target player");
                sender.sendMessage(USAGE);
                return true;
            }
            boolean self = sender == target;
            if (!sender.hasPermission(self ? PERM_INFO_SELF : PERM_INFO_OTHERS)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            BabelPlayer targetInfo = getPlayerInfo(target);
            Set<BabelPlayer.PlayerLanguage> pLangs = targetInfo.getLanguages().stream()
                    .filter(l -> l.getLevel() > 0).collect(Collectors.toSet());
            sender.sendMessage(ChatColor.YELLOW+"Known languages"+(self ? "" : " for "+Utils.decoratePlayerName(target))
                    +ChatColor.YELLOW+" ("+ChatColor.GRAY+pLangs.size()+ChatColor.YELLOW+"):"+
                    (pLangs.size()==0 ? ChatColor.GRAY+""+ChatColor.ITALIC+" None" : ""));
            for (BabelPlayer.PlayerLanguage pLang : pLangs) {
                if (pLang.getLevel() < 1) continue;
                Language lang = getLanguage(pLang.getLanguage());
                sender.sendMessage("  "+ChatColor.WHITE+lang.getName()+ChatColor.YELLOW+
                        " (level "+ChatColor.WHITE+pLang.getLevel()+ChatColor.YELLOW+")"+
                        (pLang.getComment() != null ?": "+ChatColor.GRAY+pLang.getComment() : ""));
            }
            Language selectedLanguage = targetInfo.getSelectedLanguage();
            sender.sendMessage(ChatColor.YELLOW+"Selected language: "+ChatColor.WHITE+
                    (selectedLanguage == null ? "Common tongue" : selectedLanguage.getName()));
        } else if (args[0].equals("select")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Player command only !");
                return true;
            }
            final String USAGE_SELECT = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/babel select <lang>";
            if (args.length < 2) {
                sender.sendMessage(USAGE_SELECT);
                return true;
            }
            Language lang = languages.values().stream()
                    .filter(l -> l.getName().equals(args[1])).findFirst().orElse(null);
            if (!args[1].equals("none") && lang == null) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid language name");
                sender.sendMessage(USAGE_SELECT);
                return true;
            }
            if (lang != null && !sender.hasPermission(PERM_LANG_SPEAK.concat(lang.getKey()))) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to speak in this language");
                return true;
            }
            Player target = (Player) sender;
            BabelPlayer pInfo = getPlayerInfo(target);
            if (lang != null && !pInfo.speaks(lang)) {
                sender.sendMessage(ChatColor.YELLOW+"You cannot select a language you do not speak");
                return true;
            }
            pInfo.selectLanguage(lang);
            updatePlayerInfo(pInfo);
            sender.sendMessage(ChatColor.YELLOW+"You are now speaking "+
                    (lang != null ? ChatColor.WHITE+lang.getName() : "in common tongue"));
        } else if (args[0].equals("set")) {
            if (!sender.hasPermission(PERM_EDIT_LEVEL)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            final String USAGE_SET = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/babel set <player> <lang> <level> [<comment>]";
            if (args.length < 4) {
                sender.sendMessage(USAGE_SET);
                return true;
            }
            Player target = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().equals(args[1]))
                    .findFirst().orElse(null);
            if (target == null) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid player name");
                sender.sendMessage(USAGE_SET);
                return true;
            }
            Language lang = languages.values().stream()
                    .filter(l -> l.getName().equals(args[2])).findFirst().orElse(null);
            if (lang == null) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid language name");
                sender.sendMessage(USAGE_SET);
                return true;
            }
            int level;
            try {
                level = Integer.parseUnsignedInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid understanding level");
                sender.sendMessage(USAGE_SET);
                return true;
            }
            String comment = args.length > 4 ? Utils.joinStrings(args, " ", 4) : null;
            BabelPlayer pInfo = getPlayerInfo(target);
            pInfo.setLanguage(lang, level, comment);
            updatePlayerLanguage(pInfo, lang.getKey());
            sender.sendMessage(ChatColor.YELLOW+"Saved player level in "+ChatColor.WHITE+lang.getName()+
                    ChatColor.YELLOW+": "+ChatColor.WHITE+level+
                    (comment != null ? ChatColor.YELLOW+", "+ChatColor.GRAY+comment : ""));
        } else if (args[0].equals("lang")) {
            if (!sender.hasPermission(PERM_EDIT_LANG)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            final String USAGE_LANG=ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/babel lang " +
                    "list/info/new/name/desc/alphabet/delete [<key>] [<value>]";
            if (args.length == 2 && args[1].equals("list")) {
                sender.sendMessage(ChatColor.YELLOW+"Language list ("+ChatColor.GRAY+languages.size()+ChatColor.YELLOW+
                        "):"+(languages.size()==0 ? ChatColor.GRAY+""+ChatColor.ITALIC+" No language" : ""));
                for (Language lang : languages.values()) {
                    sender.sendMessage("  "+ChatColor.WHITE+lang.getKey()+ChatColor.YELLOW+" ("+
                            ChatColor.WHITE+lang.getName()+ChatColor.YELLOW+"): "+ChatColor.GRAY+
                            (lang.getDescription() != null ? lang.getDescription() : ChatColor.ITALIC+"No description"));
                }
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(USAGE_LANG);
                return true;
            }
            String key = args[2];
            Language lang = getLanguage(key);
            if (args[1].equals("info")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW+"Language info: "+ChatColor.WHITE+lang.getName());
                sender.sendMessage(ChatColor.YELLOW+"  Key: "+ChatColor.WHITE+lang.getKey());
                sender.sendMessage(ChatColor.YELLOW+"  Alphabet: "+ChatColor.WHITE+lang.getAlphabet());
                sender.sendMessage(ChatColor.YELLOW+"  Description: "+
                        (lang.getDescription() == null ? ChatColor.GRAY+""+ChatColor.ITALIC+"No description" : ""));
                if (lang.getDescription() != null) sender.sendMessage("    "+ChatColor.GRAY+lang.getDescription());
                Language language = lang;
                Set<BabelPlayer> players = retrieveLanguagePlayers(language);
                if (players == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Error: Unable to retrieve reader/speaker list");
                    return true;
                }
                List<String> readers = players.stream().filter(p -> p.reads(language))
                        .map(i -> Utils.decoratePlayerName(i.getPlayerId(), i.getPlayerName())).collect(Collectors.toList());
                List<String> speakers = players.stream().filter(p -> p.speaks(language))
                        .map(i -> Utils.decoratePlayerName(i.getPlayerId(), i.getPlayerName())).collect(Collectors.toList());
                if (readers.size() > 0) {
                    sender.sendMessage(ChatColor.YELLOW+"  Readers ("+ChatColor.GRAY+readers.size()+ChatColor.YELLOW+"): ");
                    sender.sendMessage("    "+String.join(ChatColor.YELLOW+", ", readers));
                } else {
                    sender.sendMessage(ChatColor.YELLOW+"  Readers: "+ChatColor.GRAY+""+ChatColor.ITALIC+"None");
                }
                if (speakers.size() > 0) {
                    sender.sendMessage(ChatColor.YELLOW+"  Speakers ("+ChatColor.GRAY+speakers.size()+ChatColor.YELLOW+"): ");
                    sender.sendMessage("    "+String.join(ChatColor.YELLOW+", ", speakers));
                } else {
                    sender.sendMessage(ChatColor.YELLOW+"  Speakers: "+ChatColor.GRAY+""+ChatColor.ITALIC+"None");
                }
            } else if (args[1].equals("new")) {
                if (lang != null) {
                    sender.sendMessage(ChatColor.YELLOW+"A language already exists with this key");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW+"Missing alphabet");
                    return true;
                }
                lang = new Language(key, key, args[3]);
                languages.put(key, lang);
                saveLanguage(lang);
                sender.sendMessage(ChatColor.YELLOW+"Created language: "+ChatColor.WHITE+key);
            } else if (args[1].equals("name")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW+"Missing new name");
                    return true;
                }
                lang.setName(args[3]);
                saveLanguage(lang);
                sender.sendMessage(ChatColor.YELLOW+"Saved new name for "+ChatColor.WHITE+key+ChatColor.YELLOW+": "+
                        ChatColor.WHITE+lang.getName());
            } else if (args[1].equals("desc")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW+"Missing new description");
                    return true;
                }
                lang.setDescription(Utils.joinStrings(args, " ", 3));
                saveLanguage(lang);
                sender.sendMessage(ChatColor.YELLOW+"Saved description for "+ChatColor.WHITE+key+ChatColor.YELLOW+": "+
                        ChatColor.GRAY+lang.getDescription());
            } else if (args[1].equals("alphabet")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW+"Missing new alphabet");
                    return true;
                }
                lang.setAlphabet(args[3]);
                saveLanguage(lang);
                sender.sendMessage(ChatColor.YELLOW+"Saved alphabet for "+ChatColor.WHITE+key+ChatColor.YELLOW+": "+
                        ChatColor.WHITE+lang.getAlphabet());
            } else if (args[1].equals("delete")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                languages.remove(key);
                deleteLanguage(key);
                playerInfos.clear();
                for (Player p : Bukkit.getOnlinePlayers()) getPlayerInfo(p);
                sender.sendMessage(ChatColor.YELLOW+"Deleted language "+ChatColor.WHITE+key+ChatColor.YELLOW+" ("+
                        ChatColor.WHITE+lang.getName()+ChatColor.YELLOW+"): "+ChatColor.GRAY+
                        (lang.getDescription() != null ? lang.getDescription() : ChatColor.ITALIC+"No description"));
            } else {
                sender.sendMessage(USAGE_LANG);
                return true;
            }
        } else {
            sender.sendMessage(USAGE);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> res = new ArrayList<>();
            res.add("select");
            if (sender.hasPermission(PERM_EDIT_LEVEL)) res.add("set");
            if (sender.hasPermission(PERM_EDIT_LANG)) res.add("lang");
            if (sender.hasPermission(PERM_INFO_SELF) || sender.hasPermission(PERM_INFO_OTHERS)) res.add("info");
            return res.stream().filter(s -> args[0].length() < 1 || s.startsWith(args[0])).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equals("select") && sender instanceof Player) {
            List<String> res = getPlayerInfo((Player) sender).getLanguages().stream().filter(l -> l.getLevel() >= 2)
                    .map(BabelPlayer.PlayerLanguage::getLanguage).map(this::getLanguage).map(Language::getName)
                    .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
            res.add("none");
            return res;
        }
        if (args.length == 2 && args[0].equals("info")) return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        if (args.length == 2 && args[0].equals("set") && sender.hasPermission(PERM_EDIT_LEVEL))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        if (args.length == 3 && args[0].equals("set") && sender.hasPermission(PERM_EDIT_LEVEL))
            return languages.values().stream().map(Language::getName)
                .filter(s -> args[2].length() < 1 || s.startsWith(args[2])).collect(Collectors.toList());
        if (args.length == 4 && args[0].equals("set") && sender.hasPermission(PERM_EDIT_LEVEL))
            return Stream.of("0", "1", "2", "3", "4", "5")
                .filter(s -> args[3].length() < 1 || s.startsWith(args[3])).collect(Collectors.toList());
        if (args.length == 5 && args[0].equals("set") && sender.hasPermission(PERM_EDIT_LEVEL))
            return Collections.singletonList("<comment>");
        if (args.length == 2 && args[0].equals("lang") && sender.hasPermission(PERM_EDIT_LANG))
            return Stream.of("list", "info", "new", "name", "alphabet", "desc", "delete")
                    .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        if (args.length == 3 && args[0].equals("lang") && !args[1].equals("new") && !args[1].equals("list")
                && sender.hasPermission(PERM_EDIT_LANG)) return languages.keySet().stream()
                    .filter(s -> args[2].length() < 1 || s.startsWith(args[2])).collect(Collectors.toList());
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onChat(AquilonChatEvent evt) {
        if (evt.getSender() == null) return; // Do not handle machine messages
        ChatChannel chan = evt.getChannel();
        if (chan.getDistance() == 0) return; // Do not handle global messages

        // Retrieve message context
        Player sender = evt.getSender();
        BabelPlayer senderInfo = getPlayerInfo(sender);
        Language senderLang = senderInfo.getSelectedLanguage();
        if (senderLang == null) return; // Do not garble common tongue

        // Translate message
        String garble = senderLang.translate(evt.getMessage());
        String garbledMessage = chan.computeMessage(garble, evt.getSenderName(), evt.getSenderDisplayName(),
                evt.getSenderPrefix(), evt.getSenderSuffix(), evt.getSenderColor(), evt.isMessageColored());

        // Check all message recipients
        Iterator<Player> targets = evt.getRecipients().iterator();
        while (targets.hasNext()) {
            Player target = targets.next();
            if (getPlayerInfo(target).reads(senderLang)) continue;
            if (target.hasPermission(AQLBabel.PERM_LANG_READ.concat(senderLang.getKey()))) continue;
            // Target doesn't speak language, send garbled message
            targets.remove();
            target.sendMessage(garbledMessage);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getPlayerInfo(event.getPlayer());
    }

    public BabelPlayer getPlayerInfo(OfflinePlayer target) {
        return getPlayerInfo(target.getUniqueId(), target.getName());
    }

    public BabelPlayer getPlayerInfo(UUID playerId, String playerName) {
        return playerInfos.computeIfAbsent(playerId.toString().replaceAll("-",""), id -> {
            BabelPlayer res = retrieveBabelPlayer(playerId);
            if (res == null) res = new BabelPlayer(playerId, playerName);
            else res.setPlayerName(playerName);
            return res;
        });
    }

    public Language getLanguage(String lang) {
        return languages.get(lang);
    }

    public Set<Language> getLanguages() {
        return new HashSet<>(languages.values());
    }

    /**
     * Retrieve BabelPlayer from database from it's UUID
     * @param playerId The player UUID
     * @return The player info, or <code>null</code> if not found or if there was an error
     */
    private BabelPlayer retrieveBabelPlayer(UUID playerId) {
        String uuid = Objects.requireNonNull(playerId).toString().replaceAll("-","");
        Connection con = db.startTransaction();
        BabelPlayer res = null;
        String selectedLang = null;
        try {
            PreparedStatement stmt = db.prepare(con, SQL_FIND_PLAYER_INFO);
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = new BabelPlayer(playerId, rs.getString("player_name"));
                selectedLang = rs.getString("selected_lang");
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_FIND_PLAYER_INFO);
            return null;
        }
        if (res == null) {
            db.endTransaction(con);
            return null;
        }
        try {
            PreparedStatement stmt = db.prepare(con, SQL_FIND_PLAYER_LANGS);
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                res.setLanguage(
                        getLanguage(rs.getString("lang")),
                        rs.getInt("lvl"),
                        rs.getString("comment"));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_FIND_PLAYER_LANGS);
            return null;
        }
        db.endTransaction(con);
        if (selectedLang != null) res.selectLanguage(getLanguage(selectedLang));
        return res;
    }

    /**
     * Retrieve all languages from database
     */
    private Map<String, Language> retrieveLanguages() {
        Connection con = db.startTransaction();
        Map<String, Language> res;
        try {
            PreparedStatement stmt = db.prepare(con, SQL_FIND_ALL_LANGS);
            ResultSet rs = stmt.executeQuery();
            res = new HashMap<>();
            while (rs.next()) {
                String langKey = rs.getString("lang");
                res.put(langKey, new Language(langKey,
                        rs.getString("name"),
                        rs.getString("alphabet"),
                        rs.getString("description")));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_FIND_ALL_LANGS);
            return null;
        }
        db.endTransaction(con);
        return res;
    }

    /**
     * Save the player information in database
     * @param info The player info
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public boolean updatePlayerInfo(BabelPlayer info) {
        String uuid = Objects.requireNonNull(info).getPlayerId().toString().replaceAll("-","");
        Connection con = db.startTransaction();
        String selectedLanguage = info.getSelectedLanguage() != null ? info.getSelectedLanguage().getKey() : null;
        try {
            PreparedStatement stmt = db.prepare(con, SQL_UPDATE_PLAYER_INFO);
            stmt.setString(1, uuid);
            stmt.setString(2, info.getPlayerName());
            stmt.setString(4, info.getPlayerName());
            stmt.setString(3, selectedLanguage);
            stmt.setString(5, selectedLanguage);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_UPDATE_PLAYER_INFO);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    /**
     * Save a player language in database
     * @param info The player info
     * @param langKey The key of language to save
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public boolean updatePlayerLanguage(BabelPlayer info, String langKey) {
        if (!updatePlayerInfo(info)) return false;
        BabelPlayer.PlayerLanguage lang = Objects.requireNonNull(info).getLanguage(Objects.requireNonNull(langKey));
        if (lang == null) throw new IllegalArgumentException("Unknown language for player: "+info.getPlayerId());
        String uuid = info.getPlayerId().toString().replaceAll("-","");
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_UPDATE_PLAYER_LANGS);
            stmt.setString(1, uuid);
            stmt.setString(2, lang.getLanguage());
            stmt.setInt(3, lang.getLevel());
            stmt.setInt(5, lang.getLevel());
            stmt.setString(4, lang.getComment());
            stmt.setString(6, lang.getComment());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_UPDATE_PLAYER_LANGS);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    /**
     * Save the language in database
     * @param lang The language to save
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public boolean saveLanguage(Language lang) {
        Objects.requireNonNull(lang);
        Connection con = db.startTransaction();
        try {
            PreparedStatement stmt = db.prepare(con, SQL_UPDATE_LANGUAGE);
            stmt.setString(1, lang.getKey());
            stmt.setString(2, lang.getName());
            stmt.setString(5, lang.getName());
            stmt.setString(3, lang.getAlphabet());
            stmt.setString(6, lang.getAlphabet());
            stmt.setString(4, lang.getDescription());
            stmt.setString(7, lang.getDescription());
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_UPDATE_LANGUAGE);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    private boolean deleteLanguage(String langKey) {
        Objects.requireNonNull(langKey);
        Connection con = db.startTransaction(false);
        for (String sql : Arrays.asList(SQL_RESET_PLAYER_LANG, SQL_DELETE_PLAYER_LANG, SQL_DELETE_LANG)) try {
            PreparedStatement stmt = db.prepare(con, sql);
            stmt.setString(1, langKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        return true;
    }

    /**
     * Retrieve all BabelPlayer that know the language
     * @param lang The language
     * @return A set of <b>incomplete</b> BabelPlayer
     */
    public Set<BabelPlayer> retrieveLanguagePlayers(Language lang) {
        Objects.requireNonNull(lang);
        Connection con = db.startTransaction();
        Set<BabelPlayer> res;
        try {
            PreparedStatement stmt = db.prepare(con, SQL_FIND_LANG_PLAYERS);
            stmt.setString(1, lang.getKey());
            ResultSet rs = stmt.executeQuery();
            res = new HashSet<>();
            while (rs.next()) {
                BabelPlayer p = new BabelPlayer(Utils.getUUID(rs.getString("player")),
                        rs.getString("player_name"));
                p.setLanguage(lang, rs.getInt("lvl"), rs.getString("comment"));
                res.add(p);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_FIND_LANG_PLAYERS);
            return null;
        }
        db.endTransaction(con);
        return res;
    }
}
