package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

/**
 * AquilonThings module interface
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public interface IModule extends Listener, CommandExecutor, TabCompleter, PluginMessageListener {
    /**
     * Module initialization method
     * @param db A connector to access the database
     * @throws Module.StartException When an error occurs during start-up
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    boolean onStartUp(DatabaseConnector db) throws Module.StartException;

    /**
     * Module stop method
     * @throws Module.StopException When an error occurs during shut-down
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    boolean onStop() throws Module.StopException;

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
