package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ModuleDescriptionBuilder {
    public final String id;
    public String shortName;
    public String longName;
    public String version;
    public String mainClass;
    public String link;
    public String description;
    public List<String> authors;
    public Set<String> dependencies;
    public Set<String> provides;
    public Set<String> inPackets;
    public Set<String> outPackets;
    public List<ModuleDescription.Command> commands;

    public ModuleDescriptionBuilder(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        authors = new ArrayList<>();
        dependencies = new HashSet<>();
        provides = new HashSet<>();
        inPackets = new HashSet<>();
        outPackets = new HashSet<>();
        commands = new ArrayList<>();
    }

    public ModuleDescriptionBuilder(String id, String name, String version, String mainClass) {
        this(id);
        this.shortName = Objects.requireNonNull(name, "name cannot be null");
        this.longName = name;
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.mainClass = Objects.requireNonNull(mainClass, "mainClass cannot be null");
    }

    public ModuleDescriptionBuilder shortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public ModuleDescriptionBuilder longName(String longName) {
        this.longName = longName;
        return this;
    }

    public ModuleDescriptionBuilder version(String version) {
        this.version = version;
        return this;
    }

    public ModuleDescriptionBuilder mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public ModuleDescriptionBuilder link(String link) {
        this.link = link;
        return this;
    }

    public ModuleDescriptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ModuleDescriptionBuilder author(String author) {
        this.authors.add(author);
        return this;
    }

    public ModuleDescriptionBuilder dependency(String dependency) {
        this.dependencies.add(dependency);
        return this;
    }

    public ModuleDescriptionBuilder clearDependencies() {
        this.dependencies.clear();
        return this;
    }

    public ModuleDescriptionBuilder provide(String provide) {
        this.provides.add(provide);
        return this;
    }

    public ModuleDescriptionBuilder clearProvides() {
        this.provides.clear();
        return this;
    }

    public ModuleDescriptionBuilder inPacket(String inPacket) {
        this.inPackets.add(inPacket);
        return this;
    }

    public ModuleDescriptionBuilder clearInPackets() {
        this.inPackets.clear();
        return this;
    }

    public ModuleDescriptionBuilder outPacket(String outPacket) {
        this.outPackets.add(outPacket);
        return this;
    }

    public ModuleDescriptionBuilder clearOutPackets() {
        this.outPackets.clear();
        return this;
    }

    public ModuleDescriptionBuilder clearCommands() {
        this.commands.clear();
        return this;
    }

    public ModuleDescriptionBuilder command(String cmd) {
        this.commands.add(new ModuleDescription.Command(cmd, null, null));
        return this;
    }

    public ModuleDescriptionBuilder command(String cmd, String desc) {
        this.commands.add(new ModuleDescription.Command(cmd, desc, null));
        return this;
    }

    public ModuleDescriptionBuilder command(String cmd, String desc, String usage, String... aliases) {
        this.commands.add(new ModuleDescription.Command(cmd, desc, usage, aliases));
        return this;
    }

    public ModuleDescription build() {
        if (longName == null) longName = shortName;
        return new ModuleDescription(
                id, shortName, longName, version,
                mainClass, link, description, authors,
                dependencies, provides,
                inPackets, outPackets, commands
        );
    }

    public static ModuleDescriptionBuilder fromClassAnnotation(Class<? extends IModule> klass, AQLThingsModule annotation) {
        ModuleDescriptionBuilder b =  new ModuleDescriptionBuilder(
                klass.getName(), annotation.name(), annotation.version(), klass.getName()
        );
        if (!annotation.link().isEmpty()) b.link(annotation.link());
        if (!annotation.description().isEmpty()) b.description(annotation.description());
        b.authors.addAll(Arrays.asList(annotation.authors()));
        b.dependencies.addAll(Arrays.asList(annotation.dependencies()));
        b.provides.addAll(Arrays.asList(annotation.provides()));
        for (Cmd cmd : annotation.cmds()) {
            b.command(
                cmd.value(),
                cmd.desc().isEmpty() ? null : cmd.desc(),
                cmd.usage().isEmpty() ? null : cmd.usage(),
                cmd.aliases()
            );
        }
        for (InPacket pkt : annotation.inPackets()) {
            b.inPacket(pkt.value());
        }
        for (OutPacket pkt : annotation.outPackets()) {
            b.outPacket(pkt.value());
        }
        return b;
    }
}
