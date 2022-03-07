package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.database.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.events.EventRegistrar;
import fr.aquilon.minecraft.aquilonthings.utils.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * The AQLThings representation of a module.
 * <p>
 *     Manages module lifecycle and provides access to IOs and metadata.
 * </p>
 * @param <T> The class that actually contains the module logic.
 */
public class Module<T extends IModule> {
	public final ModuleDescription meta;
	private final T data;
	private final ModuleInputOutputs io;

	public Module(T data, ModuleDescription desc) {
		this.meta = desc;
		this.data = data;
		this.io = new ModuleInputOutputs();
	}

	/**
	 * @return The unique ID of this module.
	 */
	public String getID() {
		return meta.getID();
	}

	/**
	 * @return The user-friendly name of this module
	 */
	public String getName() {
		return meta.getShortName();
	}

	/**
	 * @return The instance of the implementing class
	 */
	public T data() {
		return data;
	}

	/**
	 * @return A read-only list of registered inputs and outputs for this module.
	 */
	public ModuleInputOutputs getInputOutputs() {
		return io.readonly();
	}

	/**
	 * @return The logger for this module
	 */
	public ModuleLogger getLogger() {
		return ModuleLogger.get(data.getClass());
	}

	/**
	 * @return A connector to the configured database
	 */
	public DatabaseConnector getDatabaseConnector() {
		return AquilonThings.instance.getNewDatabaseConnector();
	}

	/**
	 * Get the YAML config for this module.
	 * <p>
	 *     This method only calls {@link Module#getModuleConfig(String, String)} with no default config.
	 * </p>
	 * @param filename The name of the config file
	 * @return The loaded configuration
	 */
	public YamlConfiguration getModuleConfig(String filename) {
		return getModuleConfig(filename, null);
	}

	/**
	 * Get the YAML config for this module.
	 * <p>
	 *     The config for this module is placed in the AQLThings plugin folder. Each module has it's own subfolder.
	 *     The name of the config file is chosen by the module (often config.yml)
	 * </p>
	 * @param filename The name of the config file in the module config folder.
	 * @param defaultConfig The path of the default config file in the module jar, can be <code>null</code>.
	 * @return The loaded configuration (never <code>null</code>)
	 */
	public YamlConfiguration getModuleConfig(String filename, String defaultConfig) {
		File folder = new File(AquilonThings.instance.getDataFolder(), meta.shortName);
		File file = new File(folder, filename);
		return ConfigHelper.loadConfig(file, defaultConfig, getLogger());
	}

	private void registerCommands() {
		for(ModuleDescription.Command cmd : meta.getCommands()) {
			registerCommand(cmd.cmd, cmd.description, cmd.usage, cmd.aliases);
		}
		if (io.commands.size()>0)
			getLogger().mInfo("Commands: "+io.commands.stream()
					.map(ModuleCommand::getName)
					.collect(Collectors.joining(", ")));
	}

	/**
	 * Register a command to be handled by this module.
	 * <p>
	 *     Commands specified in the module description are already registered.
	 * </p>
	 * @param cmd The command name
	 * @param description A description of the command
	 * @param usageMessage A message to explain usage of the command
	 * @param aliases Other names the command can take
	 */
	public void registerCommand(String cmd, String description, String usageMessage, String... aliases) {
		ModuleCommand command = new ModuleCommand(this, cmd, description, usageMessage, Arrays.asList(aliases));
		try{
			ModuleCommand.registerCommand(command);
		} catch (Exception e) {
			getLogger().mWarning("Error while registering command: "+cmd);
			return;
		}
		io.commands.add(command);
	}

	// TODO: unregister command

	private void registerPackets() {
		for (String channel : meta.getInPackets()) {
			try {
				io.incomingPackets.add(
						Bukkit.getServer().getMessenger().registerIncomingPluginChannel(
								AquilonThings.instance,
								AquilonThings.CHANNEL_PREFIX + ':' + channel,
								data()
						)
				);
			} catch (IllegalArgumentException e) {
				getLogger().log(Level.WARNING, "", "Error while registering incomming channel: "+channel, e);
			}
		}
		if (io.incomingPackets.size()>0)
			getLogger().mInfo("InPackets: "+io.incomingPackets.stream()
					.map(PluginMessageListenerRegistration::getChannel)
					.collect(Collectors.joining(", ")));

		for (String channel : meta.getOutPackets()) {
			try {
				Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+channel);
				io.outgoingPackets.add(AquilonThings.CHANNEL_PREFIX+':'+channel);
			} catch (IllegalArgumentException e) {
				getLogger().log(Level.WARNING, "", "Error while registering outgoing channel: "+channel, e);
			}
		}
		if (io.outgoingPackets.size()>0)
			getLogger().mInfo("OutPackets: "+String.join(", ", io.outgoingPackets));
	}

	/**
	 * Register a Bukkit event listener to handle events.
	 * @param listener The class that contains event handlers.
	 */
	public void registerEventListener(Listener listener) {
		Map<Class<? extends Event>, Set<RegisteredListener>> listeners = AquilonThings.instance.getPluginLoader()
				.createRegisteredListeners(listener, AquilonThings.instance);
		for (Class<? extends Event> evt : listeners.keySet()) {
			Set<RegisteredListener> list = listeners.get(evt);
			EventRegistrar.getEventHandlerList(evt).registerAll(list);
			io.eventListeners.computeIfAbsent(evt, e -> new HashSet<>()).addAll(list);
		}
	}

	public void unregisterIO() {
		if (io.eventListeners != null) for (Class<? extends Event> event : io.eventListeners.keySet()) {
			for (RegisteredListener listener : io.eventListeners.get(event))
				EventRegistrar.getEventHandlerList(event).unregister(listener);
		}
		if (io.incomingPackets != null) for (PluginMessageListenerRegistration inPacket : io.incomingPackets) {
			Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(AquilonThings.instance, inPacket.getChannel());
		}
		if (io.outgoingPackets != null) for (String outPacket : io.outgoingPackets) {
			Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(AquilonThings.instance, outPacket);
		}
		if (io.commands != null) for (ModuleCommand cmd : io.commands) {
			try {
				ModuleCommand.unregisterCommand(cmd);
			} catch (ModuleCommand.CommandRegistrationException e) {
				getLogger().log(Level.INFO, "", "Error while unregistering command: "+cmd.getName(), e);
			}
		}
	}

	/**
	 * Called automatically, registers all module IOs.
	 * @param module The module whose IOs should be registered.
	 */
	public static void registerIO(Module<?> module) {
		module.registerEventListener(module.data());
		module.registerPackets();
		module.registerCommands();
	}

	/**
	 * Called automatically, triggers the start callback of the module.
	 * @param module The module whose start callback should be called.
	 * @throws StartException If an error occurs during module start.
	 */
	public static void start(Module<?> module) throws StartException {
		try {
			if (!module.data().onStartUp(module))
				throw new StartException("Error while starting module: "+module.getName());
		} catch (StartException e) {
			throw e;
		} catch (Throwable e) {
			throw new StartException("Error while starting module: "+module.getName(), e);
		}
	}

	/**
	 * Called automatically, triggers the stop callback of the module.
	 * @param module The module whose stop callback should be called.
	 * @throws StopException If an error occurs during module stop.
	 */
	public static void stop(Module<?> module) throws StopException {
		try {
			if (!module.data().onStop())
				throw new StopException("Error while stopping module: "+module.getName());
		} catch (StopException e) {
			throw e;
		} catch (Throwable e) {
			throw new StopException("Error while stopping module: "+module.getName());
		}
	}

	/**
	 * Build a module instance from a class and a description.
	 * @param klass The class to instantiate.
	 * @param desc The description of the module.
	 * @param <T> The type of the module implementing class.
	 * @return The build module
	 * @throws InitException In case an error occurs during instantiation.
	 */
	public static <T extends IModule> Module<T> loadFromClass(Class<T> klass, ModuleDescription desc) throws InitException {
		T data;
		try {
			data = klass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new InitException("Unable to create a new module instance of "+ klass.getName(), e);
		}
		return new Module<>(data, desc);
	}

	/**
	 * Thrown when an error occurs during module initialization.
	 */
	public static class InitException extends Exception {
		public InitException(String message) {
			super(message);
		}

		public InitException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * To be thrown by modules in the start callback in case an error occurs.
	 */
	public static class StartException extends Exception {
		public StartException(String message) {
			super(message);
		}

		public StartException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * To be thrown by modules in the stop callback in case an error occurs.
	 */
	public static class StopException extends Exception {
		public StopException(String message) {
			super(message);
		}

		public StopException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
