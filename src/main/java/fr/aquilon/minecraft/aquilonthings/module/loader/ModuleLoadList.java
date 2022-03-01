package fr.aquilon.minecraft.aquilonthings.module.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;

public class ModuleLoadList {
    public final List<JarFile> modules;
    public final List<ModuleLoadException> errors;
    public final ClassLoader classLoader;

    public ModuleLoadList(ClassLoader classLoader, List<JarFile> modules, List<ModuleLoadException> errors) {
        this.classLoader = classLoader;
        this.modules = Collections.unmodifiableList(modules);
        this.errors = Collections.unmodifiableList(errors);
    }

    public static ModuleLoadList fromFolder(File folder, ClassLoader parentLoader) throws NoSuchFileException {
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

        List<URL> urls = new ArrayList<>();
        Iterator<JarFile> moduleIt = res.iterator();
        while (moduleIt.hasNext()) {
            JarFile jar = moduleIt.next();
            try {
                urls.add(new File(jar.getName()).toURI().toURL());
            } catch (MalformedURLException e) {
                errs.add(new ModuleLoadException(jar.getName(), "Invalid module name", e));
                moduleIt.remove();
            }
        }

        ClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), parentLoader);
        return new ModuleLoadList(loader, res, errs);
    }

    public List<JarFile> getModules() {
        return modules;
    }

    public List<ModuleLoadException> getErrors() {
        return errors;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
