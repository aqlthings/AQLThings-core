package fr.aquilon.minecraft.aquilonthings.module.loader;

public class ModuleLoadException extends Exception {
    public final String file;

    public ModuleLoadException(String file) {
        this.file = file;
    }

    public ModuleLoadException(String file, String message) {
        super(message);
        this.file = file;
    }

    public ModuleLoadException(String file, String message, Throwable cause) {
        super(message, cause);
        this.file = file;
    }
}
