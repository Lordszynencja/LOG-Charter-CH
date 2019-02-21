package log.main;

import java.io.IOException;

import log.io.Logger;
import log.sound.MusicData;
import log.sound.MusicLoader;
import log.sound.SoundPlayer;
import log.sound.SoundPlayer.Player;

public class Main {

	public static void main(final String[] args) throws InterruptedException, IOException {
		Logger.setOutput(System.out);
		final MusicData data = MusicLoader.load("C:/Users/szymon/Desktop/test.mp3");
		Logger.info("Loaded");

		if (data == null)
			System.err.println("Couldn't load data");
		else {
			int start = 1000;
			while (true) {
				data.setSlow(1);
				start += 1000;
				final Player p = SoundPlayer.play(data, start);
				System.out.println("Played");
				System.in.read();
				p.stop();
				System.out.println("Stopped");
				System.in.read();
			}
		}
	}

}
