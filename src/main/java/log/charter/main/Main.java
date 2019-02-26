package log.charter.main;

import static log.charter.io.Logger.info;

import java.io.IOException;

import log.charter.gui.CharterFrame;
import log.charter.io.Logger;
import log.charter.io.midi.reader.MidiReader;
import log.charter.io.midi.writer.MidiWriter;
import log.charter.song.Song;
import log.charter.sound.MusicData;
import log.charter.sound.MusicLoader;
import log.charter.sound.SoundPlayer;
import log.charter.sound.SoundPlayer.Player;

public class Main {
	private static final String path = "C:/Users/szymon/Desktop/";

	public static void main(final String[] args) throws InterruptedException, IOException {
		final Song s = MidiReader.readMidi(
				"G:/G/Phase Shift/Songs/RichaadEB ft. Jonathan Young, FamilyJules - Deja Vu/notes.mid");
		final CharterFrame frame = new CharterFrame();
		frame.s = s;
	}

	public static void midiTest() {
		Logger.debug = false;
		final String path = "G:/G/Phase Shift/Songs/RichaadEB ft. Jonathan Young, FamilyJules - Deja Vu/notes.mid";
		final String savePath = "G:/G/Phase Shift/Songs/RichaadEB ft. Jonathan Young, FamilyJules - Deja Vu/notes2.mid";

		info("--- READING ---");
		Song s = MidiReader.readMidi(path);

		info("--- WRITING ---");
		MidiWriter.writeMidi(savePath, s);

		info("--- RESAVING TEST ---");
		for (int i = 0; i < 100; i++) {
			s = MidiReader.readMidi(savePath);
			MidiWriter.writeMidi(savePath, s);
		}

		s = MidiReader.readMidi(savePath);
		info(s.toString());
	}

	public static void musicTest() throws IOException {
		final MusicData data = MusicLoader.load(path + "test.mp3");
		info("Music loaded");

		if (data == null) {
			System.err.println("Couldn't load data");
		} else {
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
