package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class ModuleDescription {
    public final String key;
    public final String id;
    public final String name;
    public final String displayName;
    public final List<String> authors;
    public final String version;
    public final String mainClass;
    public final String link;
    public final String description;
    public final Set<String> dependencies;
    public final Set<String> inPackets;
    public final Set<String> outPackets;
    public final List<Command> commands;

    public ModuleDescription(
            String key, String id, String name, String displayName,
            List<String> authors, String version, String mainClass,
            String link, String description, Set<String> dependencies,
            Set<String> inPackets, Set<String> outPackets, List<Command> commands
    ) {
        this.key = key;
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.authors = authors;
        this.version = version;
        this.mainClass = mainClass;
        this.link = link;
        this.description = description;
        this.dependencies = dependencies;
        this.inPackets = inPackets;
        this.outPackets = outPackets;
        this.commands = commands;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getVersion() {
        return version;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getLink() {
        return link;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getInPackets() {
        return inPackets;
    }

    public Set<String> getOutPackets() {
        return outPackets;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public static ModuleDescription.Builder builder() {
        return new Builder(); // FIXME
    }

    public static ModuleDescription fromClassAnnotation(Class<? extends IModule> klass, AQLThingsModule annotation) {
        return builder().build(); // FIXME
    }

    public static ModuleDescription fromYamlStream(InputStream stream) {
        return builder().build(); // FIXME
    }

    public static class Builder {
        public ModuleDescription build() {
            // FIXME
            return new ModuleDescription(null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    public static class Command {
        public final String cmd;
        public final String description;
        public final String usage;
        public final String[] aliases;

        public Command(String cmd, String description, String usage, String... aliases) {
            this.cmd = cmd;
            this.description = description;
            this.usage = usage;
            this.aliases = aliases;
        }
    }
}
