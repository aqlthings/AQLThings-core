package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * A command from an AquilonThings module
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class ModuleCommand extends BukkitCommand implements PluginIdentifiableCommand {
    private final Module module;

    public ModuleCommand(Module module, String name) {
        this(module, name, null, null, null);
    }

    public ModuleCommand(Module module, String name, String description, String usageMessage, List<String> aliases) {
        super(
                name,
                description != null ? description : "AquilonThings command",
                usageMessage != null ? usageMessage : "/"+name+" [args ...]",
                aliases
        );
        this.module = Objects.requireNonNull(module);
    }

    @Override
    public Plugin getPlugin() {
        return AquilonThings.instance;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (AquilonThings.instance.getModule(module.getName()) == null) {
            sender.sendMessage("Cannot execute command '/"+commandLabel+"' in disabled AquilonThings module "+module.getName());
            return true;
        }

        if (!testPermission(sender)) return true;

        boolean ok;
        try {
            ok = module.data().onCommand(sender, this, commandLabel, args);
        } catch (Throwable err) {
            throw new CommandException("Error executing command '" + commandLabel + "' from AquilonThings module" + module.getName(), err);
        }

        if (!ok && usageMessage.length() > 0) {
            for (String line : usageMessage.replace("<command>", commandLabel).split("\n")) {
                sender.sendMessage(line);
            }
        }

        return ok;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        List<String> options;
        try {
            options = module.data().onTabComplete(sender, this, alias, args);
        } catch (Throwable err) {
            StringBuilder message = new StringBuilder();
            message.append("Error during tab completion for command '/").append(alias).append(' ');
            for (String arg : args) {
                message.append(arg).append(' ');
            }
            message.deleteCharAt(message.length() - 1).append("' from AquilonThings module ").append(module.getName());
            throw new CommandException(message.toString(), err);
        }
        if (options == null) return super.tabComplete(sender, alias, args);
        return options;
    }

    @Override
    public String toString() {
        return "AquilonThings command ("+getName()+", module: "+module.getName()+")";
    }

    /**
     * As of now Bukkit doesn't provide an API to register commands at runtime, so we have no choice but to use reflection.
     * @see <a href="https://bukkit.org/threads/tutorial-registering-commands-at-runtime.158461/">Bukkit topic</a>
     * @param command The command to register
     * @throws CommandRegistrationException When unable to register the command
     */
    public static void registerCommand(ModuleCommand command) throws CommandRegistrationException {
        Objects.requireNonNull(command);
        getCommandMap().register(command.getName(), "aqlthings", command);
    }

    public static void unregisterCommand(ModuleCommand command) throws CommandRegistrationException {
        Objects.requireNonNull(command);
        command.unregister(getCommandMap());
    }

    private static CommandMap getCommandMap() throws CommandRegistrationException {
        PluginManager pluginManager = Bukkit.getPluginManager();
        try {
            Field f = pluginManager.getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(pluginManager);
        } catch (NoSuchFieldException e) {
            throw new CommandRegistrationException("Unable to find server command list", e);
        } catch (IllegalAccessException e) {
            throw new CommandRegistrationException("Couldn't access server command list", e);
        } catch (SecurityException e) {
            throw new CommandRegistrationException("Forbidden access to server command list", e);
        } catch (ClassCastException e) {
            throw new CommandRegistrationException("Unexpected server command list type", e);
        }
    }

    public static class CommandRegistrationException extends Exception {
        public CommandRegistrationException(String message) {
            super(message);
        }

        public CommandRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
