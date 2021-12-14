package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ModuleDescription {
    public final String id;
    public final String shortName;
    public final String longName;
    public final String version;
    public final String mainClass;
    public final String link;
    public final String description;
    public final List<String> authors;
    public final Set<String> dependencies;
    public final Set<String> provides;
    public final Set<String> inPackets;
    public final Set<String> outPackets;
    public final List<Command> commands;

    public ModuleDescription(
            String id, String shortName, String longName, String version,
            String mainClass, String link, String description, List<String> authors,
            Set<String> dependencies, Set<String> provides,
            Set<String> inPackets, Set<String> outPackets, List<Command> commands
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.shortName = Objects.requireNonNull(shortName, "shortName cannot be null");
        if (shortName.length() > 32) throw new IllegalArgumentException("shortName cannot be longer than 32 chars");
        this.longName = Objects.requireNonNull(longName, "longName cannot be null");
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.mainClass = Objects.requireNonNull(mainClass, "mainClass cannot be null");
        this.link = Objects.requireNonNull(link, "link cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.authors = Objects.requireNonNull(authors, "authors cannot be null");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null");
        this.provides = Objects.requireNonNull(provides, "provides cannot be null");
        this.inPackets = Objects.requireNonNull(inPackets, "inPackets cannot be null");
        this.outPackets = Objects.requireNonNull(outPackets, "outPackets cannot be null");
        this.commands = Objects.requireNonNull(commands, "commands cannot be null");
    }

    public String getID() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
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

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public Set<String> getProvides() {
        return Collections.unmodifiableSet(provides);
    }

    public Set<String> getInPackets() {
        return Collections.unmodifiableSet(inPackets);
    }

    public Set<String> getOutPackets() {
        return Collections.unmodifiableSet(outPackets);
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public static ModuleDescriptionBuilder builder(String id) {
        return new ModuleDescriptionBuilder(id);
    }

    public static ModuleDescriptionBuilder builder(String id, String name, String version, String mainClass) {
        return new ModuleDescriptionBuilder(id, name, version, mainClass);
    }

    public static ModuleDescription fromClassAnnotation(Class<? extends IModule> klass, AQLThingsModule annotation) {
        return ModuleDescriptionBuilder.fromClassAnnotation(klass, annotation).build();
    }

    public static ModuleDescription fromYamlStream(InputStream stream) {
        // FIXME: parse YAML, fill fields
        return builder(null).build();
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
