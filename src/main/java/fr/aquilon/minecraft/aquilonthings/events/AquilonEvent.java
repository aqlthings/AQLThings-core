package fr.aquilon.minecraft.aquilonthings.events;

import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.utils.JSONExportable;

/**
 * Created on 14/07/2017.
 *
 * @author Billi
 */
public interface AquilonEvent<T extends IModule> extends JSONExportable {
    void call(T m);
}
