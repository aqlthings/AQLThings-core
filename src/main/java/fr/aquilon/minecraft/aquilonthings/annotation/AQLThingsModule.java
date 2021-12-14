package fr.aquilon.minecraft.aquilonthings.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to declare an AquilonThings module
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AQLThingsModule {
    /**
     * @return The module name
     */
    String name();

    /**
     * @return The version of the module
     */
    String version() default "unknown";

    /**
     * @return An HTTP link to a page with info about the module
     */
    String link() default "";

    /**
     * @return Description of the goal and content of the module
     */
    String description() default "";

    /**
     * @return A list of author names
     */
    String[] authors() default {};

    /**
     * @return A list of dependencies
     */
    String[] dependencies() default {};

    /**
     * @return A list of provided APIs
     */
    String[] provides() default {};

    /**
     * @return The list of commands to register
     */
    Cmd[] cmds() default {};

    /**
     * @return The list of incomming packets to register
     */
    InPacket[] inPackets() default {};

    /**
     * @return The list of outgoing packets to register
     */
    OutPacket[] outPackets() default {};
}
