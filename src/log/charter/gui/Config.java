package log.charter.gui;

import static log.charter.io.Logger.error;

import java.util.function.BiConsumer;

import log.charter.util.RW;

public class Config {
	private static final String configName = "config.ini";

	public static String lastPath = "C:/";
	public static String musicPath = System.getProperty("user.home") + "/Music";
	public static String songsPath = System.getProperty("user.home") + "/Documents";
	public static String charter = "Jurgen Gunterschwarzhaffenstrasen";

	public static int windowPosX = 100;
	public static int windowPosY = 100;
	public static int windowWidth = 800;
	public static int windowHeight = 600;
	public static int zoomLvl = 0;

	public static int minNoteDistance = 5;
	public static int minLongNoteDistance = 30;
	public static int minTailLength = 30;
	public static int delay = 15;
	public static int markerOffset = 300;
	public static double lastMaxHOPODist = 100;

	public static boolean debugLogging = false;

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
					case "musicPath":
						musicPath = val;
						break;
					case "charter":
						charter = val;
						break;
					case "songsPath":
						songsPath = val;
						break;
					case "windowPosX":
						windowPosX = Integer.valueOf(val);
						break;
					case "windowPosY":
						windowPosY = Integer.valueOf(val);
						break;
					case "windowWidth":
						windowWidth = Integer.valueOf(val);
						break;
					case "windowHeight":
						windowHeight = Integer.valueOf(val);
						break;
					case "zoomLvl":
						zoomLvl = Integer.valueOf(val);
						break;
					case "minNoteDistance":
						minNoteDistance = Integer.valueOf(val);
						break;
					case "minLongNoteDistance":
						minLongNoteDistance = Integer.valueOf(val);
						break;
					case "minTailLength":
						minTailLength = Integer.valueOf(val);
						break;
					case "delay":
						delay = Integer.valueOf(val);
						break;
					case "markerOffset":
						markerOffset = Integer.valueOf(val);
						break;
					case "lastMaxHOPODist":
						lastMaxHOPODist = Double.valueOf(val);
						break;
					case "debugLogging":
						debugLogging = Boolean.valueOf(val);
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
		adder.accept("musicPath", musicPath);
		adder.accept("songsPath", songsPath);
		adder.accept("charter", charter);
		adder.accept("windowPosX", windowPosX + "");
		adder.accept("windowPosY", windowPosY + "");
		adder.accept("windowWidth", windowWidth + "");
		adder.accept("windowHeight", windowHeight + "");
		adder.accept("zoomLvl", zoomLvl + "");
		adder.accept("minNoteDistance", minNoteDistance + "");
		adder.accept("minLongNoteDistance", minLongNoteDistance + "");
		adder.accept("minTailLength", minTailLength + "");
		adder.accept("delay", delay + "");
		adder.accept("markerOffset", markerOffset + "");
		adder.accept("lastMaxHOPODist", lastMaxHOPODist + "");
		adder.accept("debugLogging", debugLogging + "");

		RW.write(configName, b.toString());
	}
}
