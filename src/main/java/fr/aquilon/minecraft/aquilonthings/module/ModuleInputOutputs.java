package fr.aquilon.minecraft.aquilonthings.module;

import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleInputOutputs {
    public final Map<Class<? extends Event>, Set<RegisteredListener>> eventListeners;
    public final Set<PluginMessageListenerRegistration> incomingPackets;
    public final Set<String> outgoingPackets;
    public final Set<ModuleCommand> commands;

    public ModuleInputOutputs(
            Map<Class<? extends Event>, Set<RegisteredListener>> eventListeners,
            Set<PluginMessageListenerRegistration> incomingPackets,
            Set<String> outgoingPackets,
            Set<ModuleCommand> commands
    ) {
        this.eventListeners = eventListeners;
        this.incomingPackets = incomingPackets;
        this.outgoingPackets = outgoingPackets;
        this.commands = commands;
    }

    ModuleInputOutputs() {
        this(
                new HashMap<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>()
        );
    }

    public ModuleInputOutputs readonly() {
        return new ModuleInputOutputs(
                Collections.unmodifiableMap(eventListeners),
                Collections.unmodifiableSet(incomingPackets),
                Collections.unmodifiableSet(outgoingPackets),
                Collections.unmodifiableSet(commands)
        );
    }

    public Map<Class<? extends Event>, Set<RegisteredListener>> getEventListeners() {
        return eventListeners;
    }

    public Set<PluginMessageListenerRegistration> getIncomingPackets() {
        return incomingPackets;
    }

    public Set<String> getOutgoingPackets() {
        return outgoingPackets;
    }

    public Set<ModuleCommand> getCommands() {
        return commands;
    }
}
