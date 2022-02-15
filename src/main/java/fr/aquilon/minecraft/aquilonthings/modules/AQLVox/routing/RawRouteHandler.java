package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author Billi
 */
@FunctionalInterface
public interface RawRouteHandler extends GenericRouteHandler<NanoHTTPD.Response> {}
