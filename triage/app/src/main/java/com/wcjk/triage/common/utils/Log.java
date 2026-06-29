package com.wcjk.triage.common.utils;

import android.content.Context;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Log {
	private final static LogConfigurator _logConfigurator = new LogConfigurator();

	public static void Configure(String logfileName) {
		String logFilePath = Utils.getLogPath(logfileName + ".log");
		String filePattern = "%d - [%c] - %p : %m%n";
		int maxBackupSize = 10;
		long maxFileSize = 1024 * 1024 *5;
		//DpsInits.CfgInit();
		// set the name of the log file
		_logConfigurator.setFileName(logFilePath);
		// set output format of the log line
		_logConfigurator.setFilePattern(filePattern);
		// Maximum number of backed up log files
		_logConfigurator.setMaxBackupSize(maxBackupSize);
		// Maximum size of log file until rolling
		_logConfigurator.setMaxFileSize(maxFileSize);
//		_logConfigurator.setRootLevel(str2LogLevel(DpsCfgGlobal.getIntance().getLogLevel()));
		_logConfigurator.setRootLevel(Level.DEBUG);
		//_logConfigurator.setRootLevel(Level.ERROR);
		// _logConfigurator.setRootLevel(Level.INFO);
		//Level.toLevel(DpsCfgGlobal.getIntance().getLogLevel());
		// configure
		_logConfigurator.setLevel("org.apache.mina.*", Level.ERROR);

		_logConfigurator.configure();

		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("org.apache.mina");
		logger.setLevel(Level.WARN);
	}

	public static void setLogLevel(String logLevel){
		_logConfigurator.setRootLevel(str2LogLevel(logLevel));
		_logConfigurator.configure();
	}

	static String className;
	static String methodName;
	static int lineNumber;
	private static String createLog( String log ) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append(methodName);
		buffer.append(":");
		buffer.append(lineNumber);
		buffer.append("]");
		buffer.append(log);
		return buffer.toString();
	}
	private static void getMethodNames(StackTraceElement[] sElements){
		className = sElements[1].getFileName();
		methodName = sElements[1].getMethodName();
		lineNumber = sElements[1].getLineNumber();
	}

	public static void ConfigureDatadir(Context cxt,String logfileName) {

		String logFilePath =cxt.getFilesDir()+ File.separator+ logfileName + ".log";
		String filePattern = "%d - [%c] - %p : %m%n";
		int maxBackupSize = 5;
		long maxFileSize = 1024 * 1024 *1;
//		DpsInits.CfgInit();
		// set the name of the log file
		_logConfigurator.setFileName(logFilePath);
		// set output format of the log line
		_logConfigurator.setFilePattern(filePattern);
		// Maximum number of backed up log files
		_logConfigurator.setMaxBackupSize(maxBackupSize);
		// Maximum size of log file until rolling
		_logConfigurator.setMaxFileSize(maxFileSize);
		_logConfigurator.setRootLevel(Level.WARN);
		//_logConfigurator.setRootLevel(Level.ERROR);
		// _logConfigurator.setRootLevel(Level.INFO);
		//Level.toLevel(DpsCfgGlobal.getIntance().getLogLevel());
		// configure
		_logConfigurator.configure();
	}

	/*
	 * if(s.equals("ALL")) return Level.ALL; 163 if(s.equals("DEBUG")) return
	 * Level.DEBUG; 164 if(s.equals("INFO")) return Level.INFO; 165
	 * if(s.equals("WARN")) return Level.WARN; 166 if(s.equals("ERROR")) return
	 * Level.ERROR; 167 if(s.equals("FATAL")) return Level.FATAL; 168
	 * if(s.equals("OFF")) return Level.OFF; 169 if(s.equals("TRACE")) return
	 * Level.TRACE;
	 */
	public static void Configure(String fileName, String filePattern, int maxBackupSize, long maxFileSize) {
		// set the name of the log file
		_logConfigurator.setFileName(fileName);
		// set output format of the log line
		_logConfigurator.setFilePattern(filePattern);
		// Maximum number of backed up log files
		_logConfigurator.setMaxBackupSize(maxBackupSize);
		// Maximum size of log file until rolling
		_logConfigurator.setMaxFileSize(maxFileSize);

		// configure
		_logConfigurator.configure();

	}

	public void e(String msg) {
		// Log.e(tag, msg);
		getMethodNames(new Throwable().getStackTrace());
		msg=createLog(msg);
		logger.error(msg);
	}

	public void i(String msg) {
		// Log.i(tag, msg);
		getMethodNames(new Throwable().getStackTrace());
		msg=createLog(msg);
		logger.info(msg);
	}

	public void w(String msg) {
		// Log.w(tag, msg);
		getMethodNames(new Throwable().getStackTrace());
		msg=createLog(msg);
		logger.warn(msg);
	}

	public void e(Throwable e) {
		getMethodNames(new Throwable().getStackTrace());
		logger.error(tag, e);
	}

	public void d(String msg) {
		// Log.d(tag, msg);
		getMethodNames(new Throwable().getStackTrace());
		msg=createLog(msg);
		logger.debug(msg);

	}
	public   void d(java.lang.String arg0, java.lang.Object... arg1){
		getMethodNames(new Throwable().getStackTrace());
		arg0=createLog(arg0);
		logger.debug(arg0,arg1);
	}

	private Logger logger = null;
	private String tag;

//	private DpsLog(String name) {
//		tag = "DMB:" + name;
//		logger = Logger.getLogger(name);
//	}

	private Log(Class<?> name) {
		tag = name.getSimpleName();
//		logger = Logger.getLogger(name);
		logger=LoggerFactory.getLogger(name);
	}

	public static Log getLogger(Class<?> name) {
		return new Log(name);
	}

	public static Level str2LogLevel(String str) {
		Level level = Level.WARN;
		try{
			if(str != null && !str.equals("")) {
				level=Level.toLevel(str.trim().toUpperCase());
			}

		}catch(Exception ex){
			ex.printStackTrace();
		}
		return level;

	}
}
