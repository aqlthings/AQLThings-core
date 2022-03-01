package fr.aquilon.minecraft.aquilonthings.module.loader;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.Module;
import fr.aquilon.minecraft.aquilonthings.module.ModuleDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * AQLThings module loader
 */
public class ModuleLoader {
    public static ModuleLoadResult loadModules(ModuleLoadList list) {
        List<Module<?>> res = new ArrayList<>();
        List<ModuleLoadException> errs = new ArrayList<>();
        ClassLoader classLoader = list.getClassLoader();
        for (JarFile f : list.modules) {
            ModuleDescription desc;
            try {
                desc = loadModuleDescription(f, classLoader);
            } catch (ModuleLoadException e) {
                errs.add(e);
                continue;
            } catch (Exception e) {
                errs.add(new ModuleLoadException(f.getName(), "Error loading module description", e));
                continue;
            }

            // FIXME: checkModuleDependencies();

            Module<?> module;
            try {
                module = loadModule(f, desc, classLoader);
            } catch (ModuleLoadException e) {
                errs.add(e);
                continue;
            } catch (Exception e) {
                errs.add(new ModuleLoadException(f.getName(), "Error during module instantiation", e));
                continue;
            }
            res.add(module);
        }

        return new ModuleLoadResult(res, errs);
    }

    public static ModuleDescription loadModuleDescription(JarFile jar, ClassLoader classLoader) throws ModuleLoadException {
        JarEntry moduleYaml = jar.getJarEntry("module.yml");
        if (moduleYaml == null) {
            Manifest manifest;
            try {
                manifest = jar.getManifest();
            } catch (IOException e) {
                throw new ModuleLoadException(jar.getName() ,"JAR has no module.yml and no manifest", e);
            }
            // FIXME: Main-Class is not the best attribute, we should use another attribute
            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            if (mainClass == null)
                throw new ModuleLoadException(jar.getName(), "JAR has no module.yml and no Main-Class");
            Class<?> rawKlass;
            try {
                rawKlass = classLoader.loadClass(mainClass);
            } catch (ClassNotFoundException e) {
                throw new ModuleLoadException(jar.getName(), "Main-Class not found", e);
            }
            Class<? extends IModule> klass;
            try {
                klass = rawKlass.asSubclass(IModule.class);
            } catch (ClassCastException e) {
                throw new ModuleLoadException(jar.getName(), "Main-Class does not implement IModule", e);
            }
            return loadModuleDescriptionFromClass(jar.getName(), klass);
        }
        try {
            return ModuleDescription.fromYamlStream(jar.getInputStream(moduleYaml));
        } catch (Exception e) {
            throw new ModuleLoadException(jar.getName(), "Couldn't parse module.yml", e);
        }
    }

    public static ModuleDescription loadModuleDescriptionFromClass(String file, Class<? extends IModule> klass) throws ModuleLoadException {
        AQLThingsModule meta = klass.getAnnotation(AQLThingsModule.class);
        if (meta == null)
            throw new ModuleLoadException(file, "Class is not annotated");
        try {
            return ModuleDescription.fromClassAnnotation(klass, meta);
        } catch (Exception e) {
            throw new ModuleLoadException(file, "Couldn't parse module annotation", e);
        }
    }

    public static Module<?> loadModule(JarFile jar, ModuleDescription desc, ClassLoader classLoader) throws ModuleLoadException {
        String mClass = desc.mainClass;
        Class<?> rawClass;
        try {
            rawClass = classLoader.loadClass(mClass);
        } catch (ClassNotFoundException e) {
            throw new ModuleLoadException(jar.getName(), "Could not find module main class", e);
        }
        Class<? extends IModule> klass;
        try {
            klass = rawClass.asSubclass(IModule.class);
        } catch (ClassCastException e) {
            throw new ModuleLoadException(jar.getName(), "Module main class does not implement IModule", e);
        }
        try {
            return Module.loadFromClass(klass, desc);
        } catch (Module.InitException ex) {
            throw new ModuleLoadException(jar.getName(), "Error during module instantiation", ex);
        }
    }

    public static <T extends IModule> Module<T> loadAnnotatedModule(T mod) throws ModuleLoadException {
        Class<? extends IModule> klass = mod.getClass();
        ModuleDescription desc = loadModuleDescriptionFromClass(klass.getSimpleName(), klass);
        return new Module<>(mod, desc);
    }
}
