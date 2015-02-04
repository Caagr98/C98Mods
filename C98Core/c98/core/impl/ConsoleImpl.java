package c98.core.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import c98.core.C98Log;
import c98.core.impl.C98Formatter.Target;
import c98.core.impl.launch.C98Tweaker;

public class ConsoleImpl {
	public static void init(File logFile) {
		if(C98Tweaker.forge) C98Log.log.setParent(Logger.getLogger("ForgeModLoader"));
		else {
			Handler h = new StreamHandler(System.err, new C98Formatter(Target.OUT)) {
				@Override public synchronized void publish(LogRecord record) {
					super.publish(record);
					flush();
				}
			};
			h.setLevel(Level.INFO);
			C98Log.log.addHandler(h);
			C98Log.log.setUseParentHandlers(false);
		}
		C98Log.log.setLevel(Level.ALL);
		try {
			logFile.getParentFile().mkdirs();
			String path = logFile.getAbsolutePath();
			Handler handler = new FileHandler(path);
			handler.setLevel(Level.FINER);
			handler.setFormatter(new C98Formatter(Target.FILE));
			C98Log.log.addHandler(handler);
		} catch(SecurityException | IOException e) {
			C98Log.error(e);
		}
	}
}
