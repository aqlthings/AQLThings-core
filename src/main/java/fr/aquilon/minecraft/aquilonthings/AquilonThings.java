package fr.aquilon.minecraft.aquilonthings;

import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.Module;
import fr.aquilon.minecraft.aquilonthings.module.loader.ModuleLoadList;
import fr.aquilon.minecraft.aquilonthings.module.loader.ModuleLoadException;
import fr.aquilon.minecraft.aquilonthings.module.loader.ModuleLoadResult;
import fr.aquilon.minecraft.aquilonthings.module.loader.ModuleLoader;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AquilonThings extends JavaPlugin implements Listener {
	public static final String VERSION = Context.VERSION;

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

	public static AquilonThings instance;

	private ClassLoader moduleClassLoader;
	private Map<String, Module<?>> moduleList;

	public AquilonThings(){
		AquilonThings.instance = this;
	}

	/**
	 * Loads modules to be registered with the framework
	 */
	private void registerModules() {
		moduleList = new HashMap<>();
		File moduleFolder = new File(getDataFolder(), "modules");
		if (!moduleFolder.exists() && !moduleFolder.mkdir()) {
			LOGGER.severe(LOG_PREFIX+" could not create module folder");
			return;
		}
		List<Module<?>> loadedModules;
		List<ModuleLoadException> loadErrors;
		try {
			ModuleLoadList toLoad = ModuleLoadList.fromFolder(moduleFolder, getClassLoader());
			moduleClassLoader = toLoad.classLoader;
			loadErrors = new ArrayList<>(toLoad.errors);
			ModuleLoadResult loaded = ModuleLoader.loadModules(toLoad);
			loadedModules = loaded.modules;
			loadErrors.addAll(loaded.errors);
		} catch (NoSuchFileException e) {
			LOGGER.severe(LOG_PREFIX+" module folder not found");
			return;
		} catch (Exception ex) {
			LOGGER.severe(LOG_PREFIX+" Error while loading modules");
			LOGGER.log(Level.INFO, LOG_PREFIX+"Error:", ex);
			return;
		}
		moduleList.putAll(loadedModules.stream().collect(Collectors.toMap(Module::getName, Function.identity())));
		if (loadErrors.size() > 0) {
			for (ModuleLoadException err : loadErrors) {
				LOGGER.warning(LOG_PREFIX+" Could not load module \""+err.file+"\": "+err.getMessage());
				LOGGER.log(Level.FINE, LOG_PREFIX+" Error details:", err);
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

		// Enable modules
		Iterator<Module<?>> it = moduleList.values().iterator();
		while(it.hasNext()){
			Module<?> module = it.next();
			try {
				Module.start(module);
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

	public void disableModule(Module<?> module) {
		disableModule(module, true);
	}

	private void disableModule(Module<?> module, boolean remove) {
		try {
			Module.stop(module);
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
		if (remove) moduleList.remove(module.getName());
	}
	
	/**
	 * Enregistre les canaux d'entrées/sorties des modules
	 * Events, PacketChannels, Commands
	 */
	private void registerModulesIO() {
		Iterator<Module<?>> it = moduleList.values().iterator();
		while (it.hasNext()) {
			Module<?> module = it.next();
			try {
				Module.registerIO(module);
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

	public ClassLoader getModuleClassLoader() {
		return moduleClassLoader;
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
				Module<?> m = getModule(moduleName);
				if (m == null) {
					sender.sendMessage(ChatColor.YELLOW+"Module non trouvé !");
					return true;
				}
				Utils.warnStaff(
						m.data().getClass(),
						ChatColor.GOLD+"Warning: "+ChatColor.YELLOW+"Disabling module "+m.getName()+" ..."
				);
				disableModule(m);
			}
		} else {
			sender.sendMessage(globalUsage);
			return true;
		}
		return true;
	}

	private Method getServerMessageDispatcher() {
		String serverDispatchMethod = getConfig().getString(
				"server.messageDispatchMethod",
				"fr.aquilon.minecraft.Network:onPluginMessageReceived");
		if (serverDispatchMethod == null) return null;
		String[] parts = serverDispatchMethod.split(":", 2);
		try {
			Class<?> c = getClassLoader().loadClass(parts[0]);
			return c.getMethod(parts[1], String.class, byte[].class);
		} catch (Exception e) {
			LOGGER.warning(LOG_PREFIX+" Unable to setup Plugin-Server communication");
			LOGGER.log(Level.FINE, LOG_PREFIX+" Reflection error:",e);
			return null;
		}
	}

	/**
	 * Dispatch a plugin message to the target
	 * @param target A player to send the message to, or <code>null</code> to send it to the server
	 * @param channel The channel name (without the prefix, it will be added automatically)
	 * @param data The data to send
	 */
	public void dispatchPluginMessage(PluginMessageRecipient target, String channel, byte[] data) {
		String chan = AquilonThings.CHANNEL_PREFIX+':'+ Objects.requireNonNull(channel);
		byte[] payload = new byte[data.length+1];
		payload[0] = 0;
		System.arraycopy(data, 0, payload, 1, data.length);
		if (target == null) {
			try {
				Method serverMessageDispatcher = getServerMessageDispatcher();
				if (serverMessageDispatcher==null) return;
				serverMessageDispatcher.invoke(null, chan, payload);
			} catch (Exception e) {
				LOGGER.warning(LOG_PREFIX+" Unable to send server message");
				LOGGER.log(Level.FINE, LOG_PREFIX+" Reflection error:",e);
			}
		} else {
			target.sendPluginMessage(AquilonThings.instance, chan, payload);
		}
	}

	/**
	 * Dispatch a plugin message to the target
	 * @param target A player to send the message to, or <code>null</code> to send it to the server
	 * @param channel The channel name (without the prefix, it will be added automatically)
	 * @param data The data to send
	 */
	public static void sendPluginMessage(PluginMessageRecipient target, String channel, byte[] data) {
		AquilonThings.instance.dispatchPluginMessage(target, channel, data);
	}

	/**
	 * Alias for {@link #sendPluginMessage(PluginMessageRecipient, String, byte[])} with <code>target = null</code>
	 * @param channel The channel
	 * @param data The message
	 */
	public static void sendServerMessage(String channel, byte[] data) {
		sendPluginMessage(null, channel, data);
	}
}
