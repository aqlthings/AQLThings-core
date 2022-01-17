package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

/**
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class RouteRegistrationException extends RuntimeException {
    public final String module;
    public final String routeName;

    public RouteRegistrationException(String module, String routeName, String message) {
        this(module, routeName, message, null);
    }

    public RouteRegistrationException(String module, String routeName, String message, Throwable cause) {
        super(message, cause);
        this.module = module;
        this.routeName = routeName;
    }

    @Override
    public String getMessage() {
        return "Couldn't register route "+(module != null ? module : "")+"."+routeName+(
                super.getMessage()!=null ? ": "+super.getMessage() : "");
    }
}
