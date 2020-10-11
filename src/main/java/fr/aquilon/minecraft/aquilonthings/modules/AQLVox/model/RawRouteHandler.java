package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author Billi
 */
@FunctionalInterface
public interface RawRouteHandler extends GenericRouteHandler<NanoHTTPD.Response> {}
