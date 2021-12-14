package fr.aquilon.minecraft.aquilonthings.module.loader;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.Module;
import fr.aquilon.minecraft.aquilonthings.module.ModuleDescription;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * AQLThings module loader
 */
public class ModuleLoader {
    public static ModuleLoadResult loadModulesFromFolder(File folder, ClassLoader parentLoader) throws NoSuchFileException {
        List<Module<?>> res = new ArrayList<>();

        ModuleListResult list = listModulesInFolder(folder);
        List<ModuleLoadException> errs = new ArrayList<>(list.errors);

        List<URL> urls = new ArrayList<>();
        List<JarFile> moduleJARs = new ArrayList<>(list.modules);
        Iterator<JarFile> jarIterator = moduleJARs.iterator();
        while (jarIterator.hasNext()) {
            JarFile jar = jarIterator.next();
            try {
                urls.add(new File(jar.getName()).toURI().toURL());
            } catch (MalformedURLException e) {
                errs.add(new ModuleLoadException(jar.getName(), "Invalid module name", e));
                jarIterator.remove();
            }
        }

        URLClassLoader customClassLoader = new URLClassLoader(urls.toArray(new URL[0]), parentLoader);
        for (JarFile f : list.modules) {
            ModuleDescription desc;
            try {
                desc = loadModuleDescription(f, customClassLoader);
            } catch (ModuleLoadException e) {
                errs.add(e);
                continue;
            }
            // FIXME: checkModuleDependencies();
            Module<?> module;
            try {
                module = loadModule(f, desc, customClassLoader);
            } catch (ModuleLoadException e) {
                errs.add(e);
                continue;
            }
            res.add(module);
        }

        return new ModuleLoadResult(res, errs);
    }

    public static ModuleListResult listModulesInFolder(File folder) throws NoSuchFileException {
        File[] moduleFiles = folder.listFiles();
        if (moduleFiles == null) {
            throw new NoSuchFileException(folder.getName());
        }

        List<JarFile> res = new ArrayList<>();
        List<ModuleLoadException> errs = new ArrayList<>();
        for (File f : moduleFiles) {
            if (!f.getName().endsWith(".jar")) continue;
            try {
                res.add(new JarFile(f));
            } catch (IOException e) {
                errs.add(new ModuleLoadException(f.getName(), "Invalid module JAR", e));
            }
        }
        return new ModuleListResult(res, errs);
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
        } catch (IOException e) {
            throw new ModuleLoadException(jar.getName(), "Couldn't parse module.yml", e);
        }
    }

    public static ModuleDescription loadModuleDescriptionFromClass(String file, Class<? extends IModule> klass) throws ModuleLoadException {
        AQLThingsModule meta = klass.getAnnotation(AQLThingsModule.class);
        if (meta == null)
            throw new ModuleLoadException(file, "Class is not annotated");
        return ModuleDescription.fromClassAnnotation(klass, meta);
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

    public static <T extends IModule> Module<T> loadAnnotatedModule(String modName, T mod) throws ModuleLoadException {
        ModuleDescription desc = loadModuleDescriptionFromClass(modName, mod.getClass());
        return new Module<>(mod, desc);
    }
}
