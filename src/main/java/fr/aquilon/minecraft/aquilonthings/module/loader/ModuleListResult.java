package fr.aquilon.minecraft.aquilonthings.module.loader;

import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

public class ModuleListResult {
    public final List<JarFile> modules;
    public final List<ModuleLoadException> errors;

    public ModuleListResult(List<JarFile> modules, List<ModuleLoadException> errors) {
        this.modules = Collections.unmodifiableList(modules);
        this.errors = Collections.unmodifiableList(errors);
    }
}
