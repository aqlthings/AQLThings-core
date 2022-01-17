package fr.aquilon.minecraft.aquilonthings;

import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ModuleLogger {
	private static Map<String, ModuleLogger> modulesLoggers = new HashMap<>();
	
	private boolean enabled;
	public boolean debug;
	private String moduleName;

	@SuppressWarnings("unchecked")
	public static ModuleLogger get() {
		String klassName = Thread.currentThread().getStackTrace()[2].getClassName();
		try {
			Class<?> klass = ModuleLogger.class.getClassLoader().loadClass(klassName);
			if (!IModule.class.isAssignableFrom(klass))
				throw new IllegalAccessException("Calling class is not an AquilonThings module");
			return get((Class<? extends IModule>) klass);
		} catch (ClassNotFoundException | IllegalAccessException e) {
			throw new RuntimeException("Unable to build ModuleLogger automaticaly", e);
		}
	}

	public static ModuleLogger get(Class<? extends IModule> module) {
		return get(module, true);
	}

	public static ModuleLogger get(Class<? extends IModule> module, boolean debug) {
		ModuleLogger logger = modulesLoggers.get(module.getName());
		if (logger == null) {
			logger = new ModuleLogger(module, debug);
			modulesLoggers.put(module.getName(), logger);
		}
		return logger;
	}
	
	private ModuleLogger(Class<? extends IModule> module, boolean debug){
		this.enabled = true;
		this.moduleName = module.getAnnotation(AQLThingsModule.class).name();
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
