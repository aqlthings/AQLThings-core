package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;

/**
 * @author Billi
 */
@FunctionalInterface
public interface GenericRouteHandler<T> {
    T apply(APIRequest req) throws APIException;
}
