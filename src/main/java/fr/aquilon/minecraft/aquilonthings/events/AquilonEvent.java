package fr.aquilon.minecraft.aquilonthings.events;

import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.utils.JSONExportable;

/**
 * A parent interface for all AquilonThings events.
 * @author Billi
 */
public interface AquilonEvent<T extends IModule> extends JSONExportable {
    void call(T m);
}
