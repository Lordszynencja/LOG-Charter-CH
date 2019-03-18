package log.charter.main;

import java.io.IOException;
import java.io.InputStream;

import log.charter.gui.CharterFrame;
import log.charter.gui.Config;
import log.charter.util.RW;

public class Main {
	private static void createHelpFile() throws IOException {
		final InputStream input = ClassLoader.getSystemResourceAsStream("log/charter/main/help.txt");
		final byte[] bytes = new byte[input.available()];
		input.read(bytes);
		RW.writeB("help.txt", bytes);
	}

	public static void main(final String[] args) throws InterruptedException, IOException {
		createHelpFile();
		new CharterFrame();
		new Thread(() -> {
			try {
				while (true) {
					Config.save();
					Thread.sleep(10000);
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

}
