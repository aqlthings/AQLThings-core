package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TriggerCommandTickControl {
	
	private static TriggerCommandTickControl instance;
	public ArrayList<TriggerCommandCyclic> triggerCommandList;
	
	public TriggerCommandTickControl() {
		TriggerCommandTickControl.instance = this;
		this.triggerCommandList = new ArrayList<>();
		wakeUpAndWork();
	}
	
	public static TriggerCommandTickControl getInstance(){
		if(TriggerCommandTickControl.instance == null)
			new TriggerCommandTickControl();
		return TriggerCommandTickControl.instance;
	}
	
	public void registerTriggerCommand(TriggerCommandCyclic trigger){
		this.triggerCommandList.add(trigger);
	}
	
	/**
	 * Initialisation de la tâche planifiée: 
	 * Vérification des triggers cycliques et exécution des commandes
	 */
	public void wakeUpAndWork(){
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(AquilonThings.instance, new Runnable() {
            @Override
            public void run() {
            	for (TriggerCommandCyclic trigger : triggerCommandList){
            		if (trigger.activePlayerlist.isEmpty()) continue;
            		
            		if (trigger.counter>0) {
						trigger.counter--;
						continue;
					}
					trigger.resetCounter();

					// Déclenchement du trigger cyclique
					if (trigger.discriminate) {
						for(Player player : trigger.activePlayerlist){
							try {
								trigger.runCommand(trigger.command, player);
							} catch (TriggerFailedException e) {
								e.printStackTrace(); // TODO: handle
							}
						}
					} else {
						try {
							Player p = trigger.activePlayerlist.get(ThreadLocalRandom.current().nextInt(0,trigger.activePlayerlist.size()));
							trigger.runCommand(trigger.command, p);
						} catch (TriggerFailedException e) {
							e.printStackTrace(); // TODO: handle
						}
					}
            	}
            }
        }, 0L, 20L);
	}
}
/*
try {
							trigger.runCommand(player);
						} catch (TriggerFailedException e) {
							e.printStackTrace();
						}
*/