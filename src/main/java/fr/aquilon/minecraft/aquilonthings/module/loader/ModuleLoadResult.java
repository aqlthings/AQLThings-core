package fr.aquilon.minecraft.aquilonthings.module.loader;

import fr.aquilon.minecraft.aquilonthings.module.Module;

import java.util.Collections;
import java.util.List;

public class ModuleLoadResult {
    public final List<Module<?>> modules;
    public final List<ModuleLoadException> errors;

    public ModuleLoadResult(List<Module<?>> modules, List<ModuleLoadException> errors) {
        this.modules = Collections.unmodifiableList(modules);
        this.errors = Collections.unmodifiableList(errors);
    }
}
