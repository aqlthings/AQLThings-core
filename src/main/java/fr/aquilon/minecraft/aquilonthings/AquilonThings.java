package fr.aquilon.minecraft.aquilonthings;

import fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures.AQLBlessures;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.AQLCharacters;
import fr.aquilon.minecraft.aquilonthings.modules.AQLChat.AQLChat;
import fr.aquilon.minecraft.aquilonthings.modules.AQLEmotes.AQLEmotes;
import fr.aquilon.minecraft.aquilonthings.modules.AQLFire.AQLFire;
import fr.aquilon.minecraft.aquilonthings.modules.AQLGroups;
import fr.aquilon.minecraft.aquilonthings.modules.AQLLooting.AQLLooting;
import fr.aquilon.minecraft.aquilonthings.modules.AQLMisc;
import fr.aquilon.minecraft.aquilonthings.modules.AQLNobles;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.modules.AQLRegionBorder;
import fr.aquilon.minecraft.aquilonthings.modules.AQLSeason.AQLSeason;
import fr.aquilon.minecraft.aquilonthings.modules.AQLStaff;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVanish;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AquilonThings extends JavaPlugin implements Listener {
	public static final String VERSION = "3.1.0";

	public static final String PERM_ROOT = "aqlthings";
	public static final String PERM_NOBLE = PERM_ROOT+".noble";
	public static final String PERM_VISITEUR = PERM_ROOT+".visiteur";
	public static final String PERM_WARNING = PERM_ROOT+".warning.";
	private static final String PERM_RELOAD_ALL= PERM_ROOT+".reload.all";
	private static final String PERM_RELOAD_CONFIG = PERM_ROOT+".reload.config";
	private static final String PERM_STOP_MODULE = PERM_ROOT+".stop.module.";
	private static final String PERM_STOP_ALL = PERM_ROOT+".stop.all";

	public static final String CHANNEL_PREFIX = "aqlthings";
	public static final String CHANNEL_READY = "ready";

    public static final Logger LOGGER = Logger.getLogger("Minecraft");
    public static final String LOG_PREFIX = "[AqlThings]";

	public static final Class[] MODULE_LIST = {
			AQLEmotes.class,
			AQLPlaces.class,
			AQLNobles.class,
			AQLVanish.class,
			AQLGroups.class,
			AQLChat.class,
			AQLFire.class,
			AQLBlessures.class,
			AQLMisc.class,
			AQLStaff.class,
			AQLCharacters.class,
			AQLSeason.class,
			AQLLooting.class,
            AQLRegionBorder.class
	};

	public static AquilonThings instance;

	private Map<String, Module<?>> moduleList;
	private Method serverMessageDispatcher;

	public AquilonThings(){
		AquilonThings.instance = this;
	}

	/**
	 * Loads modules to be registered with the framework
	 * @todo lecture automatique depuis le package modules (or load JARs from a "modules" folder)
	 */
	private void registerModules() {
		moduleList = new HashMap<>();
		ClassLoader loader = getClassLoader();
		List<Class> moduleClasses;
		if (getConfig().get("modules.enable") != null) {
			moduleClasses = new ArrayList<>();
			List<String> confModules = getConfig().getStringList("modules.enable");
			for (String confModule : confModules) {
				try {
					moduleClasses.add(loader.loadClass(confModule));
				} catch (ClassNotFoundException e) {
					LOGGER.warning(LOG_PREFIX+" No module class found matching \""+confModule+"\"");
				}
			}
		} else {
			moduleClasses = new ArrayList<>(Arrays.asList(MODULE_LIST));
		}
		if (getConfig().getBoolean("modules.aqlvox.enabled", true)) moduleClasses.add(AQLVox.class);

		for (Class<?> mClass : moduleClasses) {
			try {
				moduleList.put(mClass.getName(), Module.loadFromClass(mClass));
			} catch (Module.ModuleInitException ex) {
				LOGGER.severe(AquilonThings.LOG_PREFIX+" Error during module instantiation: "+mClass.getSimpleName());
				LOGGER.info(AquilonThings.LOG_PREFIX+" Exception: "+ex);
			}
		}
	}

	@Override
	public void onEnable() {
		//noinspection ConstantConditions
		getCommand("aqlthings").setExecutor(this);

		initConfig();
		initDatabase();
		registerModules();
		registerModulesIO();
		setupServerDispatcher();

		// Enable all modules
		Iterator<Module<?>> it = moduleList.values().iterator();
		while(it.hasNext()){
			Module<?> module = it.next();
			try {
				module.start();
			} catch (Throwable e) {
				LOGGER.severe(LOG_PREFIX+" Unable to start module: " + module.getName());
				LOGGER.log(Level.INFO, AquilonThings.LOG_PREFIX+" Exception: ",e);
				it.remove();
			}
		}
		LOGGER.info(LOG_PREFIX+" Enabled "+ moduleList.size()+" modules"+ (moduleList.size()<1 ? "." : ": "+
				moduleList.values().stream().map(Module::getName).collect(Collectors.joining(", "))));
	}

	@Override
	public void onDisable() {
		if (moduleList == null) return;
		Iterator<Module<?>> it = moduleList.values().iterator();
		while(it.hasNext()){
			disableModule(it.next(), false);
			it.remove();
		}
	}

	public void disableModule(Module module) {
		disableModule(module, true);
	}

	private void disableModule(Module module, boolean remove) {
		try {
			module.stop();
		} catch (Throwable e) {
			LOGGER.severe(AquilonThings.LOG_PREFIX+" Unable to stop module: " + module.getName());
			LOGGER.info(AquilonThings.LOG_PREFIX+" Exception: "+e);
		}
		try {
			module.unregisterIO();
		} catch (Throwable e) {
			LOGGER.warning(AquilonThings.LOG_PREFIX+" Unable to unregister module IO: "+module.getName());
			LOGGER.info(AquilonThings.LOG_PREFIX+" Exception: "+e);
		}
		if (remove) moduleList.remove(module.klass.getName());
	}
	
	/**
	 * Enregistre les canaux d'entrées/sorties des modules
	 * Events, PacketChannels, Commands
	 */
	private void registerModulesIO() {
		Iterator<Module<?>> it = moduleList.values().iterator();
		while (it.hasNext()) {
			Module module = it.next();
			try {
				module.registerIO();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, AquilonThings.LOG_PREFIX+"IO registration error: "+e.getMessage());
				it.remove();
				try {
					module.unregisterIO();
				} catch (Throwable t) {
					LOGGER.warning(AquilonThings.LOG_PREFIX+" Unable to unregister module IO: "+module.getName());
				}
			}
		}
	}

	/**
	 * Initialisation des fichiers de configuration
	 */
	private void initConfig() {
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			LOGGER.info(LOG_PREFIX+" config.yml not found. Creating a new one ...");
			try {
				saveDefaultConfig();
			} catch (Exception e) {
				LOGGER.severe(LOG_PREFIX+" Unable to create config.yml");
			}
		}
	}
	
	/**
	 * Initialisation du connecteur de la BDD
	 */
	private void initDatabase(){
		// Création du connecteur BDD.
		DatabaseConnector db = getNewDatabaseConnector();
		
		if (!db.validateConnection()) {
			LOGGER.severe(LOG_PREFIX+" Broken DB connection");
			throw new RuntimeException("Broken DB connection");
		}
	}

	public void setupServerDispatcher() {
		try {
			Class c = getClassLoader().loadClass("net.minecraft.aquilon.util.Network");
			serverMessageDispatcher = c.getMethod("onPluginMessageReceived", String.class, byte[].class);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			LOGGER.warning(LOG_PREFIX+" Unable to setup Plugin-Server communication");
			LOGGER.log(Level.FINE, LOG_PREFIX+" Reflection error:",e);
		}
	}

	public void sendServerMessage(String channel, byte[] payload) {
		if (serverMessageDispatcher==null) return;
		try {
			serverMessageDispatcher.invoke(null, channel, payload);
		} catch (InvocationTargetException | IllegalAccessException e) {
			LOGGER.warning(LOG_PREFIX+" Unable to send server message");
			LOGGER.log(Level.FINE, LOG_PREFIX+" Reflection error:",e);
		}
	}

	public DatabaseConnector getNewDatabaseConnector() {
		return new DatabaseConnector(
				getConfig().getString("database.address"),
				getConfig().getInt("database.port"),
				getConfig().getString("database.user"),
				getConfig().getString("database.password"),
				getConfig().getString("database.base"),
				getConfig().getBoolean("database.secure", true)
		);
	}

	@SuppressWarnings("unchecked")
	public <T extends IModule> Module<T> getModule(String className) {
		return (Module<T>) moduleList.get(className);
	}

	public <T extends IModule> Module<T> getModule(Class<T> mClass) {
		return getModule(mClass.getName());
	}

	public <T extends IModule> T getModuleData(String className) {
		Module<T> m = getModule(className);
		return m != null ? m.data() : null;
	}

	public <T extends IModule> T getModuleData(Class<T> mClass) {
		return getModuleData(mClass.getName());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!label.equals("aqlthings")) return false;
		final String globalUsage = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/aqlthings (reload (all | config) | stop [<module>])";
		if (args.length < 1) {
			sender.sendMessage(globalUsage);
			return true;
		}
		if (args[0].equals("reload")) {
			final String reloadUsage = "/aquilon reload (all | config)";
			if (args.length < 2) {
				sender.sendMessage(reloadUsage);
				return true;
			}
			if (args[1].equals("config")) {
				if (!sender.hasPermission(PERM_RELOAD_CONFIG)) {
					sender.sendMessage(ChatColor.YELLOW + "Dégage !");
					return true;
				}
				this.reloadConfig();
				sender.sendMessage(ChatColor.YELLOW + "Configuration rechargée !");
				getLogger().info(LOG_PREFIX + " Config reloaded !");
			} else if (args[1].equals("all")) {
				if (!sender.hasPermission(PERM_RELOAD_ALL)) {
					sender.sendMessage(ChatColor.YELLOW + "Dégage !");
					return true;
				}
				getLogger().warning(LOG_PREFIX + " Reloading whole plugin !");
				this.onDisable();
				this.reloadConfig();
				this.onEnable();
				sender.sendMessage(ChatColor.YELLOW + "AquilonThings rechargé !");
			} else {
				sender.sendMessage(reloadUsage);
				return true;
			}
		} else if (args[0].equals("stop")) {
			String moduleName = "all";
			if (args.length > 1) moduleName = args[1];
			if (moduleName.equals("all")) {
				if (!sender.hasPermission(PERM_STOP_ALL)) {
					sender.sendMessage(ChatColor.RED+"Alors ... NON !");
					return true;
				}
				Utils.warnStaff(null, ChatColor.GOLD+"Warning: "+ChatColor.RED+"Stopping AquilonThings ...");
				this.onDisable();
				getPluginLoader().disablePlugin(this);
			} else {
				if (!sender.hasPermission(PERM_STOP_MODULE+moduleName)) {
					sender.sendMessage(ChatColor.RED+"Alors ... NON !");
					return true;
				}
				Module m = getModule(moduleName);
				if (m == null) {
					sender.sendMessage(ChatColor.YELLOW+"Module non trouvé !");
					return true;
				}
				Utils.warnStaff(m.klass, ChatColor.GOLD+"Warning: "+ChatColor.YELLOW+"Disabling module "+m.getName()+" ...");
				disableModule(m);
			}
		} else {
			sender.sendMessage(globalUsage);
			return true;
		}
		return true;
	}
}
