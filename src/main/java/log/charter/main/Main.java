package log.charter.main;

import java.io.IOException;

import log.charter.gui.CharterFrame;
import log.charter.gui.Config;

public class Main {
	public static void main(final String[] args) throws InterruptedException, IOException {
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
