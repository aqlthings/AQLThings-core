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
