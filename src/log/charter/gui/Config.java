package log.charter.gui;

import static log.charter.io.Logger.error;

import java.util.function.BiConsumer;

import log.charter.util.RW;

public class Config {
	private static final String configName = "config.ini";

	public static String lastPath = "C:/";
	public static int zoomLvl = 0;
	public static int gridSize = 4;
	public static String musicPath = System.getProperty("user.home") + "/Music";
	public static String songsPath = System.getProperty("user.home") + "/Documents";

	static {
		read();
		save();
	}

	public static void read() {
		for (final String line : RW.read(configName).split("\r\n")) {
			try {
				final int split = line.indexOf('=');
				if (split != -1) {
					final String val = line.substring(split + 1);
					switch (line.substring(0, split)) {
					case "lastPath":
						lastPath = val;
						break;
					case "zoomLvl":
						zoomLvl = Integer.valueOf(val);
						break;
					case "gridSize":
						gridSize = Integer.valueOf(val);
						break;
					default:
						error("Unknown config line " + line);
						break;
					}
				}
			} catch (final Exception e) {
				error("wrong config line " + line, e);
			}
		}
	}

	public static void save() {
		final StringBuilder b = new StringBuilder();

		final BiConsumer<String, String> adder = (name, val) -> b.append(name + "=" + val + "\r\n");

		adder.accept("lastPath", lastPath);
		adder.accept("zoomLvl", zoomLvl + "");
		adder.accept("gridSize", gridSize + "");

		RW.write(configName, b.toString());
	}
}
