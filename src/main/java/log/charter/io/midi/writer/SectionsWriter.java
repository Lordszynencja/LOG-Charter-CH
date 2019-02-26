package log.charter.io.midi.writer;

import static log.charter.io.Logger.debug;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

import log.charter.song.Section;

public class SectionsWriter {

	public static void write(final List<Section> sections, final Track track) throws InvalidMidiDataException {
		debug("Writing sections");

		MetaMessage msg = new MetaMessage();
		msg.setMessage(3, "EVENTS".getBytes(), "EVENTS".getBytes().length);
		track.add(new MidiEvent(msg, 0));

		for (final Section s : sections) {
			msg = new MetaMessage();
			final byte[] bytes = ("[section " + s.name + "]").getBytes();
			msg.setMessage(1, bytes, bytes.length);
			track.add(new MidiEvent(msg, (long) s.pos));
		}

		debug("Writing sections finished");
	}
}
