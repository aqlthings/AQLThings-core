package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.Module;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * AquilonThings module interface
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public interface IModule extends Listener, CommandExecutor, TabCompleter {
    /**
     * Module initialization method
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    boolean onStartUp(DatabaseConnector db) throws Module.ModuleStartException;

    /**
     * Module stop method
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    boolean onStop() throws Module.ModuleStopException;

    @Override
    default boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    default List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    default void onPluginMessageReceived(String channel, Player player, byte[] data) {}
}
