package fr.aquilon.minecraft.aquilonthings;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.EventRegistrar;
import fr.aquilon.minecraft.aquilonthings.utils.ModuleCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Module<T extends IModule> {
	public final AQLThingsModule meta;
	public final Class<T> klass;
	private T data;
	private RegisteredInputOutputs io;

	public Module(AQLThingsModule meta, Class<T> klass) throws ModuleInitException {
		this.meta = meta;
		this.klass = klass;
		this.io = new RegisteredInputOutputs();
		init();
	}

	public String getName() {
		return meta.name();
	}

	public InPacket[] getIncommingPacketList() {
		return meta.inPackets();
	}

	public OutPacket[] getOutgoingPacketList() {
		return meta.outPackets();
	}

	public Cmd[] getCommandList() {
		return meta.cmds();
	}

	public T data() {
		return data;
	}

	public void registerIO() {
		registerEventListener(data());
		registerPackets();
		registerCommands();
	}

	private void registerCommands() {
		for(Cmd cmd : getCommandList()){
			ModuleCommand command = ModuleCommand.build(this, cmd);
			try{
				ModuleCommand.registerCommand(command);
			} catch (Exception e) {
				getLogger().mWarning("Error while registering command: "+cmd.value());
				continue;
			}
			io.commands.add(command);
		}
		if (io.commands.size()>0)
			getLogger().mInfo("Commands: "+io.commands.stream()
					.map(ModuleCommand::getName)
					.collect(Collectors.joining(", ")));
	}

	private void registerPackets() {
		for (InPacket channel : getIncommingPacketList()) {
			try {
				io.incomingPackets.add(
						Bukkit.getServer().getMessenger().registerIncomingPluginChannel(
								AquilonThings.instance,
								AquilonThings.CHANNEL_PREFIX + ':' + channel.value(),
								(chan, p, payload) -> {
									if (!chan.equals(AquilonThings.CHANNEL_PREFIX+':'+channel.value())) {
										getLogger().mWarning("Unexpected plugin message channel: registered='"+
												channel.value()+"', actual='"+chan+"'");
										return;
									}
									data().onPluginMessageReceived(channel.value(), p,
											Arrays.copyOfRange(payload, 1, payload.length));
								}
						)
				);
			} catch (IllegalArgumentException e) {
				getLogger().log(Level.WARNING, "", "Error while registering incomming channel: "+channel.value(), e);
			}
		}
		if (io.incomingPackets.size()>0)
			getLogger().mInfo("InPackets: "+io.incomingPackets.stream()
					.map(PluginMessageListenerRegistration::getChannel)
					.collect(Collectors.joining(", ")));

		for(OutPacket channel : getOutgoingPacketList()){
			try {
				Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+channel.value());
				io.outgoingPackets.add(AquilonThings.CHANNEL_PREFIX+':'+channel.value());
			} catch (IllegalArgumentException e) {
				getLogger().log(Level.WARNING, "", "Error while registering outgoing channel: "+channel.value(), e);
			}
		}
		if (io.outgoingPackets.size()>0)
			getLogger().mInfo("OutPackets: "+String.join(", ", io.outgoingPackets));
	}

	public void registerEventListener(Listener listener) {
		Map<Class<? extends Event>, Set<RegisteredListener>> listeners = AquilonThings.instance.getPluginLoader().createRegisteredListeners(listener, AquilonThings.instance);
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

	public ModuleLogger getLogger() {
		return ModuleLogger.get(klass);
	}

	private void init() throws ModuleInitException {
		if (!isValidModule(klass))
			throw new ModuleInitException("Invalid module class: "+ klass.getName());
		try {
			this.data = klass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ModuleInitException("Unable to create a new module instance of "+ klass.getName(), e);
		}
	}

	public void start() throws ModuleStartException {
		try {
			if (!data().onStartUp(AquilonThings.instance.getNewDatabaseConnector()))
				throw new ModuleStartException("Error while starting module: "+getName());
		} catch (Throwable e) {
			throw new ModuleStartException("Error while starting module: "+getName(), e);
		}
	}

	public void stop() throws ModuleStopException{
		try {
			if (!data().onStop())
				throw new ModuleStopException("Error while stopping module: "+getName());
		} catch (Throwable e) {
			throw new ModuleStopException("Error while stopping module: "+getName());
		}
	}

	public static boolean isValidModule(Class<?> klass) {
		if (!IModule.class.isAssignableFrom(klass)) return false;
		AQLThingsModule meta = klass.getAnnotation(AQLThingsModule.class);
		if (meta == null) return false;
		if (!meta.enabled()) return false;
		return AquilonThings.instance.getConfig().getBoolean("modules." + meta.name() + ".enabled", true);
	}

	@SuppressWarnings("unchecked")
	public static Module loadFromClass(Class<?> klass) throws ModuleInitException {
		if (!isValidModule(klass)) throw new ModuleInitException("Invalid module class");
		AQLThingsModule meta = klass.getAnnotation(AQLThingsModule.class);
		return new Module(meta, klass);
	}

	public static class ModuleInitException extends Exception {
		public ModuleInitException(String message) {
			super(message);
		}
		public ModuleInitException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class ModuleStartException extends Exception {
		public ModuleStartException(String message) {
			super(message);
		}

		public ModuleStartException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class ModuleStopException extends Exception {
		public ModuleStopException(String message) {
			super(message);
		}

		public ModuleStopException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class RegisteredInputOutputs {
		private Map<Class<? extends Event>, Set<RegisteredListener>> eventListeners;
		private Set<PluginMessageListenerRegistration> incomingPackets;
		private Set<String> outgoingPackets;
		private Set<ModuleCommand> commands;

		private RegisteredInputOutputs() {
			eventListeners = new HashMap<>();
			incomingPackets = new HashSet<>();
			outgoingPackets = new HashSet<>();
			commands = new HashSet<>();
		}

		public Map<Class<? extends Event>, Set<RegisteredListener>> getEventListeners() {
			return Collections.unmodifiableMap(eventListeners);
		}

		public Set<PluginMessageListenerRegistration> getIncomingPackets() {
			return Collections.unmodifiableSet(incomingPackets);
		}

		public Set<String> getOutgoingPackets() {
			return Collections.unmodifiableSet(outgoingPackets);
		}

		public Set<ModuleCommand> getCommands() {
			return Collections.unmodifiableSet(commands);
		}
	}
}
