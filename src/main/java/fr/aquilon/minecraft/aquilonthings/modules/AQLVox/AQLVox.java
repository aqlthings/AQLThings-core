package fr.aquilon.minecraft.aquilonthings.modules.AQLVox;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.logging.APILogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIDatabaseUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIStaticUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.UserProviderRegistry;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * The HTTP API module
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
@AQLThingsModule(
		name = "AQLVox",
		cmds = @Cmd(value = "api", desc = "Gestion de l'API HTTP")
)
public class AQLVox implements IModule {
	/* TODO:
	 * 	- Players > kick/ban/heal/kill
	 * 	- load base URL from config (ex: aquilon-mc.fr/api/)
	 * 	- auth method registry
	 */
	public static final String VERSION = "2.4.0";

	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String CONFIG_FILE = "AQLVox.yml";
	public static final String PERM_API_CMD = AquilonThings.PERM_ROOT+".aqlvox.command";

	public static final int DEFAULT_PORT = 20088;

	public static AQLVox instance;

	private DatabaseConnector db;
	private APILogger apiLogger;
	private APIServer server = null;
	private FileConfiguration config;
	private final HashMap<String, APIStaticUser> staticUsers;
	private APIStaticUser defaultUser;

	public AQLVox() {
		instance = this;
		this.staticUsers = new HashMap<>();
		try {
			File serverFolder = AquilonThings.instance.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
			File apiLogsFolder = new File(serverFolder, "logs/AquilonThings");
			apiLogger = new APILogger(apiLogsFolder, "AQLVox",5);
		} catch (Throwable ex) {
			LOGGER.log(Level.WARNING, null, "Unable to create logger.", ex);
		}
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		if (!init()) return false;
		this.db = db;
		int port = config.getInt("port", DEFAULT_PORT);
		try {
			this.server = new APIServer(port, this);
		} catch (Throwable e) {
			LOGGER.log(Level.WARNING, null, "Error when creating server :", e);
			return false;
		}
		this.server.start();
		return true;
	}

	@Override
	public boolean onStop() {
		if (this.server!=null) this.server.stop();
		this.server = null;
		return true;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg1, String[] args) {
		if (cmd.getName().equalsIgnoreCase("api")) {
			if (!sender.hasPermission(PERM_API_CMD)) {
				sender.sendMessage(ChatColor.RED+"Qu'est-ce que tu veux à nous prendre le tête là pov'con ?!");
				return true;
			}

			if (args.length>=1 && args[0].equalsIgnoreCase("start")) {
				sender.sendMessage(ChatColor.YELLOW+"Démarrage de l'API.");
				LOGGER.mInfo("Démarrage de l'API ...");
				if (server==null) {
					int port = config.getInt("port", DEFAULT_PORT);
					try {
						this.server = new APIServer(port, this);
					} catch (Throwable e) {
						LOGGER.log(Level.WARNING, null, "Error when creating server :", e);
						sender.sendMessage(ChatColor.RED+"Erreur lors de l'initialisation du serveur.");
					}
					this.server.start();
				} else if (this.server.isAlive()) {
					sender.sendMessage(ChatColor.RED+"L'API est dejà active.");
				} else this.server.start();
				return true;
			} else if (args.length>=1 && args[0].equalsIgnoreCase("stop")) {
				if (server==null || !this.server.isAlive()) {
					sender.sendMessage(ChatColor.RED+"L'API est dejà arrétée.");
					return true;
				}
				LOGGER.mInfo("Arrêt de l'API ...");
				sender.sendMessage(ChatColor.YELLOW+"Arrêt de l'API.");
				this.server.stop();
				return true;
			} else if (args.length>=1 && args[0].equalsIgnoreCase("restart")) {
				LOGGER.mInfo("Redémarrage de l'API ...");
				sender.sendMessage(ChatColor.YELLOW+"Redémarrage de l'API.");
				try {
					loadStaticUsers();
				} catch (InvalidConfigurationException e) {
					sender.sendMessage(ChatColor.RED+"Erreur ! Fichier de configuration invalide, arrêt de l'API.");
					LOGGER.mSevere("Chargement des utilisateurs : Fichier de configuration invalide !");
					LOGGER.log(Level.INFO, null, "Exception: ",e);
					this.server.stop();
					return true;
				}
				this.server.restart();
				return true;
			} else if (args.length>=1 && args[0].equalsIgnoreCase("logs")) {
				File logsFolder = new File(AquilonThings.instance.getDataFolder()+"/logs");
				File[] logs = logsFolder.listFiles();
				sender.sendMessage(ChatColor.YELLOW+"API Logs: ("+logs.length+")");
				String msg = "";
				for (File l: logs) {
					msg += ChatColor.GRAY + l.getName() + ChatColor.YELLOW + ", ";
				}
                if (!msg.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "> " + msg.substring(0, msg.length()-2));
				return true;
			} else if (args.length>=1 && args[0].equalsIgnoreCase("staticUsers")) {
				sender.sendMessage(ChatColor.YELLOW+"API Static Users: ("+ staticUsers.size()+")");
				String msg = "";
				for (String u: staticUsers.keySet()) {
					msg += ChatColor.BLUE + u + ChatColor.YELLOW + ", ";
				}
                if (!msg.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "> " + msg.substring(0, msg.length()-2));
				return true;
			} else if (args.length>=2 && args[0].equalsIgnoreCase("perms")) {
				APIUser usr = staticUsers.get(args[1]);
				if (args[1].equals("default")) usr = defaultUser;
				if (usr==null) {
					sender.sendMessage(ChatColor.RED+"Aucun utilisateur API à ce nom.");
					return true;
				}
				sender.sendMessage(ChatColor.YELLOW+"API User Permissions: ("+ChatColor.BLUE+args[1]+ChatColor.YELLOW+")");
				String msg = "";
				for (String u: usr.getPermissions().getPermList()) {
					msg += ChatColor.WHITE + u + ChatColor.YELLOW + ", ";
				}
				if (!msg.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "> " + msg.substring(0, msg.length()-2));
				return true;
			} else if (args.length>=1 && args[0].equalsIgnoreCase("reload")) {
                FileConfiguration newConf = Utils.loadConfig(CONFIG_FILE);
                if (newConf==null) {
                    sender.sendMessage(ChatColor.RED+"Error while reloading AQLVox config !");
					LOGGER.mWarning("Error while reloading config !");
                    return true;
                }
                config = newConf;
                try {
                	loadStaticUsers();
				} catch (InvalidConfigurationException e) {
					sender.sendMessage(ChatColor.RED+"Errors while reloading AQLVox config !");
					LOGGER.log(Level.WARNING, null, "Error found during config reload:", e);
                	return true;
				}
                sender.sendMessage(ChatColor.YELLOW+"AQLVox config reloaded !");
                LOGGER.mInfo("Config reloaded !");
				return true;
			}
			sender.sendMessage("Usage: /api <start|stop|restart|staticUsers|logs|perms|reload> ...");
			return true;
		}
		return false;
	}

	private void loadStaticUsers() throws InvalidConfigurationException {
		staticUsers.clear();
		ConfigurationSection usersConfig = config.getConfigurationSection("auth.static");
		if (usersConfig==null) throw new InvalidConfigurationException("Config has static users section: auth.static");
		for (String user: usersConfig.getKeys(false)) {
			staticUsers.put(user, APIStaticUser.fromConfig(usersConfig.getConfigurationSection(user)));
		}
		LOGGER.mDebug("Static users: "+String.join(", ", staticUsers.keySet()));
		defaultUser = APIStaticUser.fromConfig(config.getConfigurationSection("auth.default"));
	}

	private void loadUserProviders() {
		UserProviderRegistry registry = UserProviderRegistry.getInstance();
		// TODO: Dispatch an event to allow other modules/plugins to register providers
		//registry.register(APIForumUser.class.getSimpleName(),
		//		(id) -> APIForumUser.fromUID(Integer.parseInt(id), null));
		registry.register(APIDatabaseUser.class.getSimpleName(), APIDatabaseUser::findUser);
	}

	public APIUser getUser(String userType, String userID) throws APIError {
		if (Objects.equals(userType, APIStaticUser.class.getSimpleName()))
			return getStaticUser(userID);
		return UserProviderRegistry.getInstance().getUser(userType, userID);
	}

	/**
	 * Initialisation des fichiers de configuration
	 * et des utilisateurs API
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

		try {
			config = Utils.loadConfig(CONFIG_FILE);
			if (config==null) throw new IOException("Config cannot be null");
		} catch (IOException ex) {
			LOGGER.mSevere("Lecture du fichier de configuration impossible ! Désactivation du plugin.");
			LOGGER.log(Level.INFO, null, "Exception: ",ex);
			return false;
		}

		loadUserProviders();

		try {
			loadStaticUsers();
		} catch (InvalidConfigurationException ex) {
			LOGGER.mSevere("Fichier de configuration invalide ! Désactivation du plugin.");
			LOGGER.log(Level.INFO, null, "Exception: ",ex);
			return false;
		}
		return true;
	}

	public APIServer getServer() {
		return server;
	}

	public APIStaticUser getDefaultUser() {
		return defaultUser;
	}

	public APIStaticUser getStaticUser(String name) {
		return staticUsers.get(name);
	}

	public APILogger getApiLogger() {
		return apiLogger;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public String getConfig(String s) {
		return config.getString(s, null);
	}

	public List<String> getConfigArray(String s) {
		return config.getStringList(s);
	}

	public DatabaseConnector getDatabaseConnector() {
		return db;
	}
}
