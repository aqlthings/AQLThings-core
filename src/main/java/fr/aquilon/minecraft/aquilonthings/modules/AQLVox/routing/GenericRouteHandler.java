package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;

/**
 * @author Billi
 */
@FunctionalInterface
public interface GenericRouteHandler<T> {
    T apply(APIRequest req) throws APIException;
}
