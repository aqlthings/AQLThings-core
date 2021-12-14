package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

@AQLThingsModule(
		name = "AQLTest",
		cmds = @Cmd(value = "aql", desc = "Commande d'exemple"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY)
)
public class ModuleTemplate implements IModule {
	private static final ModuleLogger LOGGER = ModuleLogger.get();

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		LOGGER.mInfo("Lancement du module de test.");
		return true;
	}

	@Override
	public boolean onStop() {
		LOGGER.mInfo( "Arrêt du module de test.");
		return true;
	}

	@Override
	public void onPluginMessageReceived(String arg0, Player arg1, byte[] arg2) {
		LOGGER.mInfo( "Paquet " + arg0 + " reçu en provenance de " + arg1.getName());
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		LOGGER.mInfo("Evenement " + event.getEventName() + " routé vers le module.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg1, String[] args) {
		try {
			LOGGER.mInfo( "Commande reçue: " + args[0] + " de :" + sender.getName());
		} catch (Exception e){
			LOGGER.mInfo("Commande sans argument reçue de :" + sender.getName());
		}
			
		return true;
	}
	
}
