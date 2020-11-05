package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.DelayedPlayerAction;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Module de gestion des blessures
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
@AQLThingsModule(
        name = "AQLBlessures",
        cmds = @Cmd(value = AQLBlessures.COMMAND, desc = "Gestion des compteurs de blessure"),
        inPackets = @InPacket(AquilonThings.CHANNEL_READY)
)
public class AQLBlessures implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();
    public static final String COMMAND = "blessure";
    public static final String COMMAND_PREFIX = "/"+COMMAND;
    public static final String CONFIG_FILE = "AQLBlessures.yml";
    public static final Pattern REGEX_COUNTER_NAME = Pattern.compile("[\\w-_]+");
    public static final String PERM_COUNTER = AquilonThings.PERM_ROOT+".blessures.counter";

    private FileConfiguration config;
    private HashMap<String, Location> frozenPlayers;
    private Map<String, String> injuryMessages;
    private Map<String, InjuryCounter> counters;
    private Map<String, String> playersCounter;
    private InjuryConfig context;
    private Map<Class<? extends PlayerEvent>, List<DelayedPlayerAction>> delayedActions;

    public AQLBlessures() {
        this.frozenPlayers = new HashMap<>();
        this.counters = new HashMap<>();
        this.playersCounter = new HashMap<>();
        this.context = null;
        this.delayedActions = new HashMap<>();
    }

    /*
    TODO: Enregistrer les blessure d'un compteur dans un fichier quand on l'arrête. Permettre de lire les fichier avec info.
    FIXME: Si le joueur se déconnecte en étant freeze avant le stop et que le serveur rédémarre, il ne sera pas défreeze à sa reconnexion.
    */

    @Override
    public boolean onStartUp(DatabaseConnector db) {
        return init();
    }

    @Override
    public boolean onStop() {
        return true;
    }

    /**
     * Initialisation des fichiers de configuration
     * et des messages de blessure.
     */
    private boolean init() {
        File file = new File(AquilonThings.instance.getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            LOGGER.mInfo("Config introuvable. Géneration d'un nouveau fichier.");
            try {
                Utils.saveResource(CONFIG_FILE, false, LOGGER);
            } catch (Exception e) {
                LOGGER.mSevere("Erreur lors de la création du fichier de configuration.");
            }
        }

        config = null;
        try {
            config = Utils.loadConfig(CONFIG_FILE);
            if (config==null) throw new IOException("Config cannot be null");
        } catch (IOException ex) {
            LOGGER.mSevere("Lecture des fichiers de configuration impossible ! Désactivation du plugin.");
            LOGGER.log(Level.INFO, null, "Exception: ",ex);
            return false;
        }

        String[] injuryLevels = {"graves", "legeres"};
        injuryMessages = new HashMap<>();
        for (String lvl: injuryLevels) {
            for (Injury.BodyPart p: Injury.BodyPart.values()) {
                for (int i=1; i<=5; i++) {
                    String key = lvl+"."+p.id+"."+i;
                    injuryMessages.put(key, config.getString(key));
                }
                if (lvl.equals("graves")) { // Échec critique
                    String key = lvl+"."+p.id+".critique";
                    injuryMessages.put(key, config.getString(key));
                }
            }
        }

        context = new InjuryConfig(
            this,
            config.getInt("config.scoreBlessureLegere", InjuryConfig.DEF_SCORE_MINOR_INJURY),
            config.getInt("config.scoreBlessureGrave", InjuryConfig.DEF_SCORE_MAJOR_INJURY),
            config.getInt("config.scoreMort", InjuryConfig.DEF_SCORE_DEAD),
            config.getInt("config.scoreIncrementMax", InjuryConfig.DEF_SCORE_INCREMENT),
            config.getBoolean("config.freezeJoueurMort", InjuryConfig.DEF_FREEZE_ON_DEATH)
        );
        return true;
    }

    public List<InjuryCounter> getCounters() {
        return Collections.unmodifiableList(new ArrayList<>(counters.values()));
    }

    public InjuryCounter getCounter(String name) {
        return counters.get(name);
    }

    /**
     * Liste des commandes pour déclencher le compteur de morts
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
        if(args.length == 0){
            if (!(sender instanceof Player)) return false;
            sender.sendMessage(ChatColor.RED + "Argument obligatoire.");

            String msg = ChatColor.WHITE+COMMAND_PREFIX+" (légère | grave | info)";
            if (sender.hasPermission(PERM_COUNTER))
                msg += "\n"+ChatColor.WHITE+COMMAND_PREFIX+" (start | pause | stop) "+ChatColor.YELLOW+"-- Gestion du compteur\n" +
                        ChatColor.WHITE+COMMAND_PREFIX+" (info | add | del | score) "+ChatColor.YELLOW+"-- Statut du compteur\n" +
                        ChatColor.WHITE+COMMAND_PREFIX+" (freeze | unfreeze) "+ChatColor.YELLOW+"-- Freeze les joueurs\n" +
                        ChatColor.WHITE+COMMAND_PREFIX+" (update | opts) "+ChatColor.YELLOW+"-- Options du compteur";

            sender.sendMessage(msg);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("blessure")) {
            if (args[0].equalsIgnoreCase("info")) {
                return infoCommand(sender, args);
            } else if(args[0].contentEquals("légère") || args[0].contentEquals("legere")) {
                return rollInjury(sender, false);
            } else if(args[0].contentEquals("grave")) {
                return rollInjury(sender, true);
            }

            if (!sender.hasPermission(PERM_COUNTER)) {
                sender.sendMessage(ChatColor.YELLOW + "Tu t'es cru où ? Touche pas à ça !");
                return true;
            }

            if(args[0].contentEquals("start")) {
                return startCounter(sender, args);
            } else if(args[0].contentEquals("pause")) {
                return pauseCounter(sender, args);
            } else if(args[0].contentEquals("stop")) {
                return stopCounter(sender, args);
            } else if(args[0].contentEquals("add")) {
                return addCounterTarget(sender, args);
            } else if(args[0].contentEquals("del")) {
                return delCounterTarget(sender, args);
            } else if(args[0].contentEquals("score")) {
                return counterSetPlayerScore(sender, args);
            } else if(args[0].contentEquals("freeze")) {
                return freezeTarget(sender, args, true);
            } else if(args[0].contentEquals("unfreeze")) {
                return freezeTarget(sender, args, false);
            } else if(args[0].contentEquals("update")) {
                return updatePlayerList(sender);
            } else if(args[0].contentEquals("opts")) {
                return counterOpts(sender, args);
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equals(COMMAND)) return null;
        if (args.length == 1) {
            List<String> res = Arrays.asList("info", "légère", "grave");
            if (sender.hasPermission(PERM_COUNTER)) res.addAll(Arrays.asList(
                    "start", "stop", "add", "del", "score", "freeze", "unfreeze", "update", "opts"));
            return res;
        }
        if (!sender.hasPermission(PERM_COUNTER)) return null;
        List<String> counterCompletes = Arrays.asList("info", "start", "pause", "stop", "add", "del", "score", "opts");
        if (args.length == 2 && counterCompletes.contains(args[0]))
            return counters.keySet().stream()
                .filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
        List<String> playerCompletes = Arrays.asList("info", "add", "del", "score");
        if (args.length == 3 && playerCompletes.contains(args[0]))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(s -> args[2].length() < 1 || s.startsWith(args[2])).collect(Collectors.toList());
        if (args.length == 3 && args[0].equals("opts"))
            return Stream.of("list", "legere", "grave", "mort", "freeze", "random")
                .filter(s -> args[2].length() < 1 || s.startsWith(args[2])).collect(Collectors.toList());
        return null;
    }

    public boolean updatePlayerList(CommandSender sender) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName(Utils.decoratePlayerName(p));
        }
        int count = 0;
        for (InjuryCounter counter: counters.values()) {
            counter.update();
            count++;
        }
        sender.sendMessage(count+""+ChatColor.YELLOW+" compteur"+(count!=1?"s":"")+" mis à jour.");
        return true;
    }

    public String getInjuryMessage(boolean severe, String bodyPart, int level) {
        if (level==0)
            if (severe) return injuryMessages.get("graves."+bodyPart+".critique");
            else return "Lancer une blessure grave";
        return injuryMessages.get((severe?"graves":"legeres")+"."+bodyPart+"."+level);
    }

    public boolean rollInjury(CommandSender sender, boolean severe) {
        showInjuryRoll(sender, Injury.rollInjury(context, severe));
        return true;
    }

    public void showInjuryRoll(CommandSender sender, Injury injury) {
        Player p = null;
        boolean console = true;
        String prefix = ChatColor.RED+"Console"+ChatColor.YELLOW+": ";
        if (sender instanceof Player) {
            console = false;
            p = (Player) sender;
            InjuryEvent evt = new InjuryEvent(p, injury);
            evt.call(this);
            prefix = Utils.decoratePlayerName(p)+ChatColor.YELLOW+
                    " ("+ChatColor.GRAY+p.getDisplayName()+ChatColor.YELLOW+"): ";
        }
        sender.sendMessage(injury.toString());
        if (!console) {
            for (Entity e: p.getNearbyEntities(50, 50 ,50)) {
                if (e instanceof Player) {
                    Player a = (Player) e;
                    // On skip le staff pour eviter d'envoyer en double
                    if (a.hasPermission(PERM_COUNTER)) continue;
                    a.sendMessage(prefix+injury.toString());
                }
            }
        }
        Utils.warnStaff(AQLBlessures.class, prefix+injury.toString(), new String[] {sender.getName()});
    }

    /**
     * Démarrage d'un compteur de morts
     * @param sender
     * @param args
     * @return Success
     */
    private boolean startCounter(CommandSender sender, String[] args) {
        final String commandUsage = COMMAND_PREFIX+" start <compteur> <rayon> (<difficulté> <freeze>)";
        final String advancedSettingsSyntax = ChatColor.RED+"Erreur, syntaxe de la difficulté invalide : "+
                ChatColor.WHITE+"<limite légére>,<limite grave>,<limite mort>,<random> <freeze>";
        if (args.length<2) {
            sender.sendMessage(ChatColor.RED+"Merci de preciser le compteur séléctionné.");
            return true;
        }
        String counterName = args[1];
        if (counters.containsKey(counterName)) {
            InjuryCounter counter = counters.get(counterName);
            if (counter.isPaused()) {
                counter.start();
                sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                        " à repris.");
            } else if (counter.isActive()) {
                sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                        " est déjà actif.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                        " à été arrété.");
            }
        } else if (counterName.equals("default")) {
            sender.sendMessage(ChatColor.RED+"Nom de compteur interdit.");
            return true;
        } else {
            if (args.length<3) {
                sender.sendMessage(commandUsage);
                return true;
            }
            if (!REGEX_COUNTER_NAME.matcher(counterName).matches()) {
                sender.sendMessage(ChatColor.RED+"Nom de compteur invalide !");
                return true;
            }
            int radius;
            try {
                radius = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Un rayon, c'est un chiffre crétin !");
                return true;
            }
            InjuryConfig localContext = context;
            if (args.length==5) {
                // On tente de parser les infos de difficulté
                String[] diffParts = args[3].split(",");
                if (diffParts.length!=4) {
                    sender.sendMessage(advancedSettingsSyntax);
                    return true;
                }
                int[] diffNums = new int[4];
                for (int i=0; i<4; i++) {
                    try {
                        diffNums[i] = Integer.parseInt(diffParts[i]);
                        if (diffNums[i]==0) throw new Exception("Nombre invalide");
                    } catch (Exception numEx) {
                        sender.sendMessage(advancedSettingsSyntax);
                        return true;
                    }
                }
                if (!(args[4].equalsIgnoreCase("oui") || args[4].equalsIgnoreCase("non"))) {
                    sender.sendMessage(advancedSettingsSyntax);
                    return true;
                }
                localContext = new InjuryConfig(context)
                        .setScoreMinor(diffNums[0])
                        .setScoreSevere(diffNums[1])
                        .setScoreDeath(diffNums[2])
                        .setScoreIncrementLimit(diffNums[3])
                        .setFreezePlayersOnDeath(args[4].equalsIgnoreCase("oui"));
            }
            InjuryCounter counter = new InjuryCounter(
                    counterName,
                    this,
                    localContext
            );
            counters.put(counterName, counter);
            if (sender instanceof Player) {
                Player p = (Player) sender;
                for (Entity e: p.getNearbyEntities(radius, radius, radius)) {
                    if (!(e instanceof Player)) continue;
                    Player a = (Player) e;
                    if (a.getGameMode()==GameMode.CREATIVE) continue;
                    if (a.getGameMode()==GameMode.SPECTATOR) continue;
                    String sUUID = a.getUniqueId().toString().replace("-","");
                    if (playersCounter.containsKey(sUUID)) continue;

                    playersCounter.put(sUUID, counterName);
                    new PlayerState(counter, a);
                }
            }
            counter.start();
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " à commencé.");
            sendTargetList(sender, counter);
        }
        return true;
    }

    /**
     * Ajout manuel d'un joueur au compteur de mort
     * @param sender
     * @param args
     * @return Success
     */
    private boolean addCounterTarget(CommandSender sender, String[] args) {
        if (args.length<3) {
            sender.sendMessage(COMMAND_PREFIX + " add <compteur> <joueur>");
            return true;
        }
        String counterName = args[1];
        if (!counters.containsKey(counterName)) {
            sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
            return true;
        }
        InjuryCounter counter = counters.get(counterName);
        if (counter.isStarted()) {
            Player p = Bukkit.getOnlinePlayers().stream()
                    .filter(t -> t.getName().equals(args[2]))
                    .findFirst().orElse(null);
            if (p==null) {
                sender.sendMessage(ChatColor.RED + "Joueur inconnu !");
                return true;
            }
            String sUUID = p.getUniqueId().toString().replace("-","");
            if (playersCounter.containsKey(sUUID)) {
                sender.sendMessage(ChatColor.RED + "Joueur déjà enregistré.");
                return true;
            }
            playersCounter.put(sUUID, counterName);
            new PlayerState(counter, p);
            sendTargetList(sender, counter);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " n'a pas débuté. ("+ChatColor.WHITE+"/blessure start"+ChatColor.YELLOW+")");
        }
        return true;
    }

    /**
     * Retrait d'un joueur du compteur de mort
     * @param sender
     * @param args
     * @return Success
     */
    private boolean delCounterTarget(CommandSender sender, String[] args) {
        if (args.length<3) {
            sender.sendMessage(COMMAND_PREFIX + " del <compteur> <joueur>");
            return true;
        }
        String counterName = args[1];
        if (!counters.containsKey(counterName)) {
            sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
            return true;
        }
        InjuryCounter counter = counters.get(counterName);
        if (counter.isStarted()) {
            Player p = Bukkit.getOnlinePlayers().stream()
                    .filter(t -> t.getName().equals(args[2]))
                    .findFirst().orElse(null);
            if (p==null) {
                sender.sendMessage(ChatColor.RED + "Joueur inconnu !");
                return true;
            }
            String uuid = p.getUniqueId().toString().replace("-","");
            PlayerState pState = counter.getPlayerState(uuid);
            if (pState == null) {
                sender.sendMessage(ChatColor.RED + "Le joueur n'est pas enregistré.");
                return true;
            }
            if (pState.reset(counter)) counter.removePlayerState(uuid); // If reset was successful remove the entry
            playersCounter.remove(uuid);
            sendTargetList(sender, counter);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " n'a pas débuté. ("+ChatColor.WHITE+"/blessure start"+ChatColor.YELLOW+")");
        }
        return true;
    }

    /**
     * Ajout de points au score d'un joueur du compteur de mort
     * @param sender
     * @param args
     * @return Success
     */
    private boolean counterSetPlayerScore(CommandSender sender, String[] args) {
        if (args.length<3) {
            sender.sendMessage(COMMAND_PREFIX + " score <joueur> <valeur>");
            return true;
        }
        Player p = Bukkit.getOnlinePlayers().stream()
                .filter(t -> t.getName().equals(args[1]))
                .findFirst().orElse(null);
        if (p==null) {
            sender.sendMessage(ChatColor.RED + "Joueur inconnu !");
            return true;
        }
        String uuid = p.getUniqueId().toString().replace("-","");
        String counterName = playersCounter.get(uuid);
        if (counterName==null) {
            sender.sendMessage(ChatColor.RED + "Le joueur n'est enregistré dans aucun compteur.");
            return true;
        }

        char symbol = args[2].charAt(0);
        String valueStr = args[2];
        if (Character.isDigit(symbol))
            symbol = 0;
        else
            valueStr = valueStr.substring(1);

        int value;
        try {
            value = Integer.parseInt(valueStr);
            if (value<0) throw new NumberFormatException();
        } catch (NumberFormatException numEx) {
            sender.sendMessage(ChatColor.RED + "Valeur invalide ! (nombre positif attendu)");
            return true;
        }

        InjuryCounter counter = counters.get(counterName);
        if (!counter.isStarted()) {
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " n'a pas débuté. ("+ChatColor.WHITE+"/blessure start"+ChatColor.YELLOW+")");
            return true;
        }
        PlayerState pState = counter.getPlayerState(uuid);

        if (symbol == 0 || symbol == '=') {
            pState.setScore(counter, value, false);
        } else if (symbol == '+') {
            pState.addScore(counter, value, false);
        } else if (symbol == '-') {
            if (value > pState.getScore()) {
                sender.sendMessage(ChatColor.YELLOW+"Valeur interdite, maximum "+pState.getScore()+" !");
                return true;
            }
            pState.setScore(counter, pState.getScore()-value, false);
        } else if (symbol == '%') {
            pState.setIncrement(value);
        } else {
            sender.sendMessage(ChatColor.RED + "Format invalide.\n"+ChatColor.YELLOW+"Le score peut être " +
                    "directement un nombre pour définir le score du joueur, il peut commencer par un + ou un - " +
                    "pour augmenter ou diminuer le score de la valeur choisie, ou par un % pour définir le random " +
                    "à lancer à chaque mort.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW+"Score modifié :\n  "+pState.stateString(false));
        return true;
    }

    /**
     * Mise en pause compteur de morts
     * @param sender
     * @param args
     * @return Success
     */
    private boolean pauseCounter(CommandSender sender, String[] args) {
        if (args.length<2) {
            sender.sendMessage(COMMAND_PREFIX + " pause <compteur>");
            return true;
        }
        String counterName = args[1];
        if (!counters.containsKey(counterName)) {
            sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
            return true;
        }
        InjuryCounter counter = counters.get(counterName);
        if (counter.isStarted()) {
            if (counter.isPaused()) {
                sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                        " est déjà en pause.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                        " est mis en pause.");
                counter.pause();
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " n'a pas débuté. ("+ChatColor.WHITE+"/blessure start"+ChatColor.YELLOW+")");
        }
        return true;
    }

    /**
     * Arrêt du compteur de morts
     * @param sender
     * @param args
     * @return Success
     */
    private boolean stopCounter(CommandSender sender, String[] args) {
        if (args.length<2) {
            sender.sendMessage(COMMAND_PREFIX + " stop <compteur>");
            return true;
        }
        String counterName = args[1];
        if (!counters.containsKey(counterName)) {
            sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
            return true;
        }
        InjuryCounter counter = counters.get(counterName);
        if (!counter.isStarted()) {
            sender.sendMessage(ChatColor.YELLOW + "Le compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                    " n'a pas débuté. ("+ChatColor.WHITE+"/blessure start"+ChatColor.YELLOW+")");
            return true;
        }
        counter.stop();
        sender.sendMessage(ChatColor.YELLOW + "Arrêt du compteur "+ChatColor.WHITE+counterName+ChatColor.YELLOW+
                "\nRappel des blessures :");
        Iterator<PlayerState> it = counter.getPlayerStateIterator();
        while (it.hasNext()) {
            PlayerState p = it.next();
            String sUUID = p.getPlayer().getUniqueId().toString().replace("-","");
            sender.sendMessage(ChatColor.YELLOW + "  - " + p.stateString(true));
            if (p.reset(counter)) it.remove(); // If reset was successful remove the entry
            playersCounter.remove(sUUID);
        }
        //TODO: sender.sendMessage(ChatColor.YELLOW+"Les joueurs suivants ne sont plus connectés :");
        return true;
    }

    /**
     * Affiche la liste des compteurs ou la liste des joueurs, le nombre de morts, le score et leur blessures
     * @param sender
     * @param args
     * @return Success
     */
    private boolean infoCommand(CommandSender sender, String[] args) {
        // Info sur le joueur actuel
        if (!sender.hasPermission(PERM_COUNTER)) {
            Player p = (Player) sender;
            String pUUID = p.getUniqueId().toString().replace("-","");
            String counterName = playersCounter.get(pUUID);
            if (counterName==null) {
                sender.sendMessage(ChatColor.YELLOW + "Vous n'êtes enregistré dans aucun compteur.");
                return true;
            }
            PlayerState pState = counters.get(counterName).getPlayerState(pUUID);
            sender.sendMessage(pState.stateString(true));
            return true;
        }
        // Staff
        if (args.length<2) {
            StringBuilder str = new StringBuilder(ChatColor.YELLOW + "Liste des compteurs :");
            for (String cName: counters.keySet()) {
                InjuryCounter counter = counters.get(cName);
                if (!counter.isStarted()) continue; // N'afficher que les compteurs actifs ou en pause.
                str.append("\n" + ChatColor.YELLOW + "  - ")
                        .append(ChatColor.WHITE).append(cName)
                        .append(
                            counter.isActive() ? ChatColor.DARK_GREEN + "●" :
                            (counter.isPaused() ? ChatColor.GOLD + "○" : ChatColor.RED + "#")
                        );
                if (counter.isStarted())
                    str.append(ChatColor.YELLOW + " (" + ChatColor.GRAY)
                            .append(counter.getPlayerCount())
                            .append(" joueur")
                            .append(counter.getPlayerCount() > 1 ? "s" : "")
                            .append(ChatColor.YELLOW).append(")");
            }
            if (counters.size()==0)
                str.append("\n  " + ChatColor.GRAY + ChatColor.ITALIC + "Aucun compteur");
            sender.sendMessage(str.toString());
            return true;
        }
        String counterName = args[1];
        if (!counters.containsKey(counterName)) {
            sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
            return true;
        }
        InjuryCounter counter = counters.get(counterName);
        if (args.length==3) {
            Player p = Bukkit.getOnlinePlayers().stream()
                    .filter(t -> t.getName().equals(args[2]))
                    .findFirst().orElse(null);
            if (p==null) {
                sender.sendMessage(ChatColor.RED + "Joueur inconnu !");
                return true;
            }
            PlayerState s = counter.getPlayerState(p.getUniqueId().toString().replaceAll("-",""));
            if (s==null) {
                sender.sendMessage(ChatColor.YELLOW + "Le joueur n'est pas présent dans le compteur ("+ChatColor.WHITE+"/blessure add"+ChatColor.YELLOW+")");
                return true;
            }
            sender.sendMessage(s.stateString(true));
            int inc = s.getIncrement()>0 ? s.getIncrement() : counter.getConfig().getScoreIncrementLimit();
            sender.sendMessage(ChatColor.YELLOW+"  Random increment : "+ChatColor.WHITE+inc);
        } else {
            StringBuilder str = new StringBuilder(ChatColor.YELLOW + "Compteur de morts (");
            if (counter.isActive()) str.append(ChatColor.DARK_GREEN.toString()).append("actif");
            else if (counter.isStarted()) str.append(ChatColor.GOLD.toString()).append("en pause");
            else str.append(ChatColor.RED.toString()).append("arrété");
            str.append(ChatColor.YELLOW).append("):");
            // Envoi de la liste des joueurs et du nombre de leur mort

            for (PlayerState p: counter.getPlayerStates()) {
                str.append("\n").append(ChatColor.YELLOW.toString()).append("  - ")
                        .append(p.stateString(false));
            }
            if (counter.getPlayerCount()==0) str.append("\n  ").append(ChatColor.GRAY.toString())
                    .append(ChatColor.ITALIC.toString()).append("Aucun joueur");
            sender.sendMessage(str.toString());
            // TODO: sender.sendMessage(ChatColor.YELLOW+"[ Déconnectés ]");
        }
        return true;
    }

    private boolean counterOpts(CommandSender sender, String[] args) {
        if (args.length<3 || args.length>4) {
            sender.sendMessage(ChatColor.RED+"Paramètres invalides, "+ChatColor.WHITE+
                    "/blessure opts <compteur> <paramètre> (<valeur>)");
            return true;
        } else {
            String counterName = args[1];
            if (!counters.containsKey(counterName) && !counterName.equals("default")) {
                sender.sendMessage(ChatColor.RED+"Compteur inconnu !");
                return true;
            }
            InjuryCounter counter = counters.get(counterName);
            InjuryConfig localConfig = counter==null ? context : counter.getConfig();
            try {
                switch (args[2]) {
                    case "list":
                        sender.sendMessage(
                                ChatColor.YELLOW+"Liste des paramètres :\n" +
                                "  - "+ChatColor.WHITE+"limite_legere"+ChatColor.YELLOW+": " +
                                "Niveau de blessure légère\n" +
                                "  - "+ChatColor.WHITE+"limite_grave"+ChatColor.YELLOW+": " +
                                "Niveau de blessure grave\n" +
                                "  - "+ChatColor.WHITE+"limite_mort"+ChatColor.YELLOW+": " +
                                "Niveau de mort\n" +
                                "  - "+ChatColor.WHITE+"random"+ChatColor.YELLOW+": " +
                                "Max. de points par mort\n" +
                                "  - "+ChatColor.WHITE+"freeze"+ChatColor.YELLOW+": " +
                                "Freeze ou non les joueurs morts"
                        );
                        break;
                    case "limiteLegere":
                    case "limite_legere":
                    case "legere":
                    case "l":
                    case "L":
                        if (args.length==4 && counter!=null) {
                            int val = Integer.parseInt(args[3]);
                            if (val<=0) throw new NumberFormatException();
                            counter.setConfig(new InjuryConfig(counter.getConfig()).setScoreMinor(val));
                            localConfig = counter.getConfig();
                        }
                        sender.sendMessage(
                                ChatColor.YELLOW+"Paramètre "+ChatColor.WHITE+counterName+".scoreLimiteLegere"+
                                ChatColor.YELLOW+" = "+ChatColor.WHITE+localConfig.getScoreMinor()
                        );
                        break;
                    case "limiteGrave":
                    case "limite_grave":
                    case "grave":
                    case "g":
                    case "G":
                        if (args.length==4 && counter!=null) {
                            int val = Integer.parseInt(args[3]);
                            if (val<=0) throw new NumberFormatException();
                            counter.setConfig(new InjuryConfig(counter.getConfig()).setScoreSevere(val));
                            localConfig = counter.getConfig();
                        }
                        sender.sendMessage(
                                ChatColor.YELLOW+"Paramètre "+ChatColor.WHITE+counterName+".scoreLimiteGrave"+
                                ChatColor.YELLOW+" = "+ChatColor.WHITE+localConfig.getScoreSevere()
                        );
                        break;
                    case "limiteMort":
                    case "limite_mort":
                    case "mort":
                    case "m":
                    case "M":
                        if (args.length==4 && counter!=null) {
                            int val = Integer.parseInt(args[3]);
                            if (val<=0) throw new NumberFormatException();
                            counter.setConfig(new InjuryConfig(counter.getConfig()).setScoreDeath(val));
                            localConfig = counter.getConfig();
                        }
                        sender.sendMessage(
                                ChatColor.YELLOW+"Paramètre "+ChatColor.WHITE+counterName+".scoreLimiteMort"+
                                ChatColor.YELLOW+" = "+ChatColor.WHITE+localConfig.getScoreDeath()
                        );
                        break;
                    case "randomIncrement":
                    case "random_increment":
                    case "random":
                    case "r":
                    case "R":
                        if (args.length==4 && counter!=null) {
                            int val = Integer.parseInt(args[3]);
                            if (val<=0) throw new NumberFormatException();
                            counter.setConfig(new InjuryConfig(counter.getConfig()).setScoreIncrementLimit(val));
                            localConfig = counter.getConfig();
                        }
                        sender.sendMessage(
                                ChatColor.YELLOW+"Paramètre "+ChatColor.WHITE+counterName+".randomIncrement"+
                                ChatColor.YELLOW+" = "+ChatColor.WHITE+localConfig.getScoreIncrementLimit()
                        );
                        break;
                    case "freezeMort":
                    case "freeze_mort":
                    case "freeze":
                    case "f":
                    case "F":
                        if (args.length==4 && counter!=null) {
                            if (!args[3].equalsIgnoreCase("oui") && !args[2].equalsIgnoreCase("non"))
                                throw new IllegalArgumentException();
                            counter.setConfig(new InjuryConfig(counter.getConfig()).setFreezePlayersOnDeath(args[2].equalsIgnoreCase("oui")));
                            localConfig = counter.getConfig();
                        }
                        sender.sendMessage(
                                ChatColor.YELLOW+"Paramètre "+ChatColor.WHITE+counterName+".freezeMort"+
                                ChatColor.YELLOW+" = "+ChatColor.WHITE+(localConfig.freezePlayersOnDeath()?"oui":"non")
                        );
                        break;
                    default:
                        if (args[2].toLowerCase().matches("^[lg]\\.(tete|torse|bras|jambe)\\.[0-5]$")) {
                            String[] parts = args[2].split("\\.");
                            int lvl = Integer.parseInt(parts[2]);
                            boolean severe = args[2].charAt(0) == 'g';
                            sender.sendMessage(
                                    ChatColor.YELLOW+"Blessure "+(severe?"grave":"légère") +
                                            " ("+parts[1]+", niveau "+ lvl+"):\n" +
                                            "    "+ChatColor.GRAY+getInjuryMessage(severe, parts[1], lvl)
                            );
                        } else {
                            sender.sendMessage(ChatColor.RED+"Paramètre inconnu.");
                        }
                }
            } catch (NumberFormatException numEx) {
                sender.sendMessage(ChatColor.RED+"Nombre positif attendu !");
            } catch (IllegalArgumentException textEx) {
                sender.sendMessage(ChatColor.RED+"Valeur incorrecte !");
            }
        }
        return true;
    }

    /**
     * Envoie la liste des joueurs enregistré.
     * @param sender
     */
    private void sendTargetList(CommandSender sender, InjuryCounter counter) {
        String list = "";
        for (PlayerState p: counter.getPlayerStates()) {
            list += p.getColoredPlayerName()+ ChatColor.YELLOW + ", ";
        }
        if (list.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "Aucun joueur dans le compteur. ("+
                ChatColor.WHITE+"/blessure add"+ChatColor.YELLOW+")");
        else sender.sendMessage(ChatColor.YELLOW + "> " + list.substring(0, list.length()-2));
    }

    /**
     * Pour freeze ou defreeze un joueur spécifique.
     * @param sender
     * @param args
     * @param freeze
     * @return success
     */
    private boolean freezeTarget(CommandSender sender, String[] args, boolean freeze) {
        if (args.length<2) {
            sender.sendMessage(ChatColor.RED + "Faut ptêtre dire de qui on parle ?");
            return true;
        }
        Player p = Bukkit.getOnlinePlayers().stream()
                .filter(t -> t.getName().equals(args[1]))
                .findFirst().orElse(null);
        if (p==null) {
            sender.sendMessage(ChatColor.RED + "Joueur inconnu !");
            return true;
        }
        if (freeze) freezePlayer(p);
        else unfreezePlayer(p);
        sender.sendMessage(ChatColor.YELLOW+"Le joueur "+Utils.decoratePlayerName(p)+ChatColor.YELLOW+
                "("+ChatColor.GRAY+p.getDisplayName()+ChatColor.YELLOW+") à bien été "+(freeze?"freeze":"défreeze")+".");
        return true;
    }

    public void freezePlayer(Player p) {
        Location loc = p.getLocation();
        while (!loc.getBlock().getRelative(0,-1,0).getType().isSolid()) {
            loc.add(0,-1,0);
        }
        p.teleport(loc);
        frozenPlayers.put(p.getUniqueId().toString().replace("-",""), loc);
        p.setWalkSpeed(0);
        p.setInvulnerable(true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "emotex "+p.getName()+" sleep");
    }

    public boolean isFrozen(String uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public void unfreezePlayer(Player p) {
        p.setWalkSpeed(0.2F); // Rendre la vitesse par defaut.
        p.setInvulnerable(false); // Permettre de reprendre des dégats.
        frozenPlayers.remove(p.getUniqueId().toString().replace("-",""));
    }

    public void delayActionAfterEvent(UUID uuid, Class<? extends PlayerEvent> event, Runnable callback, int delay) {
        List<DelayedPlayerAction> eventActions = delayedActions.computeIfAbsent(event, k -> new ArrayList<>());
        eventActions.add(new DelayedPlayerAction(event, uuid, delay, callback));
    }

    private void runDelayedActions(PlayerEvent e) {
        if (!delayedActions.containsKey(e.getClass())) return;
        Iterator<DelayedPlayerAction> it = delayedActions.get(e.getClass()).iterator();
        String pUUID = e.getPlayer().getUniqueId().toString().replaceAll("-","");
        while (it.hasNext()) {
            DelayedPlayerAction act = it.next();
            if (act.getPlayerUUID().equals(pUUID)) {
                act.run();
                it.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        String uuid = player.getUniqueId().toString().replace("-","");
        if (frozenPlayers.containsKey(uuid)) return;
        if (playersCounter.containsKey(uuid)) {
            InjuryCounter counter = counters.get(playersCounter.get(uuid));
            if (!counter.isActive()) return;
            PlayerState pS = counter.getPlayerState(uuid);
            if (pS==null) return;
            pS.addDeath(counter, player.getLocation());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent evt) {
        runDelayedActions(evt);
        final Player p = evt.getPlayer();
        String uuid = p.getUniqueId().toString().replace("-","");
        if (frozenPlayers.containsKey(uuid)) {
            evt.setRespawnLocation(frozenPlayers.get(uuid));
            // On attends 20 ticks que le joueur ai vraiment respawn
            Bukkit.getScheduler().runTaskLater(AquilonThings.instance, () -> {
                // emote allongé
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "emotex "+p.getName()+" sleep");
            }, 20);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        runDelayedActions(evt);
        Player p = evt.getPlayer();
        String uuid = p.getUniqueId().toString().replace("-","");
        boolean focused = playersCounter.containsKey(uuid);
        if (focused) {
            InjuryCounter counter = counters.get(playersCounter.get(uuid));
            counter.getPlayerState(uuid).updatePlayer(counter, p);
        } else if (frozenPlayers.containsKey(uuid)) { // Si il est mort est qu'il n'est plus focus par un compteur
            unfreezePlayer(p);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent evt) {
        Player p = evt.getPlayer();
        if (frozenPlayers.containsKey(p.getUniqueId().toString().replace("-","")) && !Utils.isSameLocation(evt.getFrom(), evt.getTo())) {
            evt.setCancelled(true);
        }
    }
}