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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    public static final String PERM_LIST_SELF = AquilonThings.PERM_ROOT + ".babel.list.self";
    public static final String PERM_LIST_OTHERS = AquilonThings.PERM_ROOT + ".babel.list.others";
    public static final String PERM_INFO = AquilonThings.PERM_ROOT + ".babel.info";
    public static final String PERM_EDIT_LANG = AquilonThings.PERM_ROOT + ".babel.edit.lang";
    public static final String PERM_EDIT_LEVEL = AquilonThings.PERM_ROOT + ".babel.edit.level";

    private static final String SQL_FIND_PLAYER_INFO = "SELECT * FROM aqlbabel_player_lang WHERE player = ? ";
    private static final String SQL_UPDATE_PLAYER_INFO = "INSERT INTO aqlbabel_player_lang VALUES " +
            "(?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = ?, comment = ?";

    private DatabaseConnector db;
    private HashMap<String, Language> languages;
    private HashMap<String, BabelPlayer> playerInfos;

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        this.db = db;
        languages = new HashMap<>();
        languages.put("ru", new Language("ru", "Russian", "абвгдеёжзийлмнорстуфхцчшщъыьэюя"));
        languages.put("gr", new Language("gr", "Greek", "αβγδεζηθικλμνξοπρσςτυφχψω"));
        // TODO: Load languages
        playerInfos = new HashMap<>();
        // TODO: Load connected player infos
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
                "/babel (info <lang>)|(select <lang>)|(list [<player>])|(set <player> <lang> <level> [<comment>])(lang ...)";
        if (args.length < 1) {
            sender.sendMessage(USAGE);
            return true;
        }
        if (args[0].equals("list")) {
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
            if (!sender.hasPermission(self ? PERM_LIST_SELF : PERM_LIST_OTHERS)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            BabelPlayer targetInfo = getPlayerInfo(target);
            sender.sendMessage(ChatColor.YELLOW+"Known languages"+
                    (self ? "" : " for "+Utils.decoratePlayerName(target))+ChatColor.YELLOW+":");
            for (BabelPlayer.PlayerLanguage pLang : targetInfo.getLanguages()) {
                if (pLang.getLevel() < 1) continue;
                Language lang = getLanguage(pLang.getLanguage());
                sender.sendMessage("  "+ChatColor.WHITE+lang.getName()+ChatColor.YELLOW+
                        " (level "+ChatColor.WHITE+pLang.getLevel()+ChatColor.YELLOW+")"+
                        (pLang.getComment() != null ?": "+ChatColor.GRAY+pLang.getComment() : ""));
            }
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
            Player target = (Player) sender;
            BabelPlayer pInfo = getPlayerInfo(target);
            if (lang != null && !pInfo.speaks(lang)) {
                sender.sendMessage(ChatColor.YELLOW+"You cannot select a language you do not speak");
                return true;
            }
            pInfo.selectLanguage(lang);
            sender.sendMessage(ChatColor.YELLOW+"You are now speaking "+
                    (lang != null ? ChatColor.WHITE+lang.getName() : "in common tongue"));
        } else if (args[0].equals("info")) {
            if (!sender.hasPermission(PERM_INFO)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            final String USAGE_SELECT = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/babel info <lang>";
            if (args.length < 2) {
                sender.sendMessage(USAGE_SELECT);
                return true;
            }
            Language lang = languages.values().stream()
                    .filter(l -> l.getName().equals(args[1])).findFirst().orElse(null);
            if (lang == null) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid language name");
                sender.sendMessage(USAGE_SELECT);
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW+"Language info: "+ChatColor.WHITE+lang.getName());
            sender.sendMessage(ChatColor.YELLOW+"  Key: "+ChatColor.WHITE+lang.getKey());
            sender.sendMessage(ChatColor.YELLOW+"  Alphabet: "+ChatColor.WHITE+lang.getAlphabet());
            sender.sendMessage(ChatColor.YELLOW+"  Description: "+
                    (lang.getDescription() == null ? ChatColor.GRAY+""+ChatColor.ITALIC+"No description" : ""));
            if (lang.getDescription() != null) sender.sendMessage("    "+ChatColor.GRAY+lang.getDescription());
            // TODO: Retrieve readers/speakers from DB
            List<String> readers = playerInfos.values().stream().filter(p -> p.reads(lang))
                    .map(i -> Utils.decoratePlayerName(i.getPlayerId(), i.getPlayerName())).collect(Collectors.toList());
            List<String> speakers = playerInfos.values().stream().filter(p -> p.speaks(lang))
                    .map(i -> Utils.decoratePlayerName(i.getPlayerId(), i.getPlayerName())).collect(Collectors.toList());
            if (readers.size() > 0) {
                sender.sendMessage(ChatColor.YELLOW+"  Readers ("+ChatColor.GRAY+readers.size()+ChatColor.YELLOW+"): ");
                sender.sendMessage("    "+String.join(ChatColor.YELLOW+", ", readers));
            } else {
                sender.sendMessage(ChatColor.YELLOW+"  Readers: "+ChatColor.ITALIC+"None");
            }
            if (speakers.size() > 0) {
                sender.sendMessage(ChatColor.YELLOW+"  Speakers ("+ChatColor.GRAY+speakers.size()+ChatColor.YELLOW+"): ");
                sender.sendMessage("    "+String.join(ChatColor.YELLOW+", ", speakers));
            } else {
                sender.sendMessage(ChatColor.YELLOW+"  Speakers: "+ChatColor.ITALIC+"None");
            }
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
            // TODO: save player info
            sender.sendMessage(ChatColor.YELLOW+"Saved player level in "+ChatColor.WHITE+lang.getName()+
                    ChatColor.YELLOW+": "+ChatColor.WHITE+level+
                    (comment != null ? ChatColor.YELLOW+", "+ChatColor.GRAY+comment : ""));
        } else if (args[0].equals("lang")) {
            if (!sender.hasPermission(PERM_EDIT_LANG)) {
                sender.sendMessage(ChatColor.YELLOW+"You are not allowed to do that !");
                return true;
            }
            final String USAGE_LANG=ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/babel lang " +
                    "(list)|(new <key> <alphabet>)|(name <key> <name>)|(desc <key> <desc>)|" +
                    "(alphabet <key> <alphabet>)|(delete <key>)";
            if (args.length == 2 && args[1].equals("list")) {
                sender.sendMessage(ChatColor.YELLOW+"Language list:");
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
            Language lang = languages.get(key);
            if (args[1].equals("new")) {
                if (lang != null) {
                    sender.sendMessage(ChatColor.YELLOW+"A language already exists with this key");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW+"Missing alphabet");
                    return true;
                }
                lang = new Language(key, key, args[3]);
                // TODO: save language
                languages.put(key, lang);
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
                // TODO: save language
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
                // TODO: save language
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
                // TODO: save language
                sender.sendMessage(ChatColor.YELLOW+"Saved alphabet for "+ChatColor.WHITE+key+ChatColor.YELLOW+": "+
                        ChatColor.WHITE+lang.getAlphabet());
            } else if (args[1].equals("delete")) {
                if (lang == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown language");
                    return true;
                }
                languages.remove(key);
                // TODO: delete player language references, delete language
                // TODO: update player cache to remove references to deleted language
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
        if (args.length == 1) return Stream.of("info", "select", "list", "set", "lang") // TODO: filter based on permissions
                .filter(s -> args[0].length() < 1 || s.startsWith(args[0])).collect(Collectors.toList());
        if (args.length == 2 && args[0].equals("info") && sender.hasPermission(PERM_INFO))
            return languages.values().stream().map(Language::getName)
                .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        if (args.length == 2 && args[0].equals("select") && sender instanceof Player) {
            List<String> res = getPlayerInfo((Player) sender).getLanguages().stream().filter(l -> l.getLevel() > 0)
                    .map(BabelPlayer.PlayerLanguage::getLanguage).map(l -> languages.get(l)).map(Language::getName)
                    .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
            res.add("none");
            return res;
        }
        if (args.length == 2 && args[0].equals("list")) return Bukkit.getOnlinePlayers().stream().map(Player::getName)
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
            return Stream.of("list", "new", "name", "alphabet", "desc", "delete")
                    .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        if (args.length == 3 && args[0].equals("lang") && !args[1].equals("new") && sender.hasPermission(PERM_EDIT_LANG))
            return languages.keySet().stream()
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
            // Target doesn't speak language, send garbled message
            targets.remove();
            target.sendMessage(garbledMessage);
        }
        sender.sendMessage("[Babel]"+garbledMessage); // FIXME: Remove me ! Debug only !
    }

    public BabelPlayer getPlayerInfo(Player target) {
        return playerInfos.computeIfAbsent(target.getUniqueId().toString().replaceAll("-",""), id -> {
            // TODO: load from DB;
            BabelPlayer res = new BabelPlayer(target);
            Language lang = getLanguage("ru");
            res.setLanguage(lang, 2, "Basic spoken level");
            res.selectLanguage(lang);
            return res;
        });
    }

    private Language getLanguage(String lang) {
        return languages.get(lang);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // TODO: Load player languages
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // TODO: Drop player languages
    }

    public void loadPlayerInfo(UUID uuid) {
        // TODO: Retrieve player info from database
    }
}
