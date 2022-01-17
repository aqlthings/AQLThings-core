package fr.aquilon.minecraft.aquilonthings.annotation;

/**
 * Annotation to register a command
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public @interface Cmd {
    String value();
    String desc() default "";
    String usage() default "";
    String[] aliases() default {};
}
