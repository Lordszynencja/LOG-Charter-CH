package log.io;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class Logger {
	private static PrintStream out = System.out;

	public static boolean debug = true;

	static {
		try {
			out = new PrintStream(new FileOutputStream("D://log.txt"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void debug(final String msg) {
		if (debug) {
			out.println("[DEBUG] " + msg);
		}
	}

	public static void error(final String msg) {
		out.println("[ERROR] " + msg);
	}

	public static void error(final String msg, final Exception e) {
		out.println("[ERROR] " + msg);
		e.printStackTrace(out);
	}

	public static void info(final String msg) {
		out.println("[INFO] " + msg);
	}

	public static void setOutput(final PrintStream output) {
		out = output;
	}
}
