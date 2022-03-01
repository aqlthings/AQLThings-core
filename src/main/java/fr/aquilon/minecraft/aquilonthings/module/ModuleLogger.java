package fr.aquilon.minecraft.aquilonthings.module;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ModuleLogger {
	private static final Map<String, ModuleLogger> modulesLoggers = new HashMap<>();

	private final String moduleName;
	private boolean enabled;
	public boolean debug;

	/**
	 * Cannot be used in static context
	 * @return The module logger for the calling module
	 */
	public static ModuleLogger get() {
		ClassLoader loader = AquilonThings.instance.getModuleClassLoader();
		if (loader == null) loader = Thread.currentThread().getContextClassLoader();
		String klassName = Thread.currentThread().getStackTrace()[2].getClassName();
		Class<?> rawKlass;
		try {
			rawKlass = loader.loadClass(klassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to automatically determine calling class", e);
		}
		Class<? extends IModule> klass;
		try {
			klass = rawKlass.asSubclass(IModule.class);
		} catch (ClassCastException e) {
			throw new RuntimeException("Calling class is not an AquilonThings module", e);
		}
		return get(klass);
	}

	public static ModuleLogger get(Class<? extends IModule> module) {
		return get(module, true);
	}

	public static ModuleLogger get(Class<? extends IModule> module, boolean debug) {
		ModuleLogger logger = modulesLoggers.get(module.getName());
		if (logger == null) {
			logger = get(module.getAnnotation(AQLThingsModule.class).name(), module.getName(), debug);
			modulesLoggers.put(module.getName(), logger);
		}
		return logger;
	}

	private static ModuleLogger get(String moduleName, String moduleClass, boolean debug) {
		ModuleLogger logger = modulesLoggers.get(moduleClass);
		if (logger == null) {
			logger = new ModuleLogger(moduleName, debug);
			modulesLoggers.put(moduleClass, logger);
		}
		return logger;
	}
	
	private ModuleLogger(String moduleName, boolean debug){
		this.enabled = true;
		this.moduleName = moduleName;
		this.debug = debug;
	}
	
	/**
	 * Send debug level log entry
	 * @param log Log message
	 */
	public void mDebug(String log){
		if (!this.enabled || !this.debug) return;
		AquilonThings.LOGGER.info(getPrefix(null) + log);
	}
	
	/**
	 * Send information level log entry
	 * @param log Log message
	 */
	public void mInfo(String log){
		if (!this.enabled) return;
		AquilonThings.LOGGER.info(getPrefix(null) + log);
	}
	
	/**
	 * Send warning level log entry
	 * @param log Log message
	 */
	public void mWarning(String log){
		if (!this.enabled) return;
		AquilonThings.LOGGER.warning(getPrefix(null) + log);
	}
	
	/**
	 * Send severe level log entry
	 * @param log Log message
	 */
	public void mSevere(String log){
		if (!this.enabled) return;
		AquilonThings.LOGGER.severe(getPrefix(null) + log);
	}

	public void log(Level lvl, String prefix, String msg, Object[] data) {
		if (!this.enabled) return;
		AquilonThings.LOGGER.log(lvl, getPrefix(prefix) + msg);
		for (Object d: data) {
			AquilonThings.LOGGER.log(lvl, "   ", d);
		}
	}

	public void log(Level lvl, String prefix, String msg, Object data) {
		if (!this.enabled) return;
		AquilonThings.LOGGER.log(lvl, getPrefix(prefix) + msg, data);
	}

	public void log(Level lvl, String prefix, String msg, Throwable data) {
		if (!this.enabled) return;
		AquilonThings.LOGGER.log(lvl, getPrefix(prefix) + msg, data);
	}

	public String getPrefix(String prefix) {
		return AquilonThings.LOG_PREFIX+"["+moduleName+"]" + (prefix != null ? prefix : "") + " ";
	}

	/**
	 * Enable Verbose
	 */
	public void enable(){
		this.enabled = true;
	}
	
	/**
	 * Disable Verbose
	 */
	public void disable(){
		this.enabled = false;
	}
	
	/**
	 * Enable all moduleLoggers
	 */
	public static void enableAll(){
		for (ModuleLogger mLog : modulesLoggers.values()){
			mLog.enable();
		}
	}
	
	/**
	 * Disable all moduleLoggers
	 */
	public static void disableAll(){
		for (ModuleLogger mLog : modulesLoggers.values()){
			mLog.disable();
		}
	}

	public static void logStatic(Class<? extends IModule> c, Level lvl, String prefix, String msg, Object data) {
		get(c).log(lvl, prefix, msg,data);
	}
	
}
