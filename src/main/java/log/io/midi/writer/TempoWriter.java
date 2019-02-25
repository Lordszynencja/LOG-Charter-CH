package log.io.midi.writer;

import static log.io.Logger.debug;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

import log.song.Tempo;

public class TempoWriter {
	public static void write(final List<Tempo> tempos, final Track track) throws InvalidMidiDataException {
		debug("Writing tempo");

		for (final Tempo t : tempos) {
			final MetaMessage msg = new MetaMessage();
			if (t.sync) {
				final int mpq = (int) Math.floor(6.0E10D / t.kbpm);
				msg.setMessage(81, new byte[] { (byte) ((mpq >> 16) & 0xFF), (byte) ((mpq >> 8)
						& 0xFF), (byte) (mpq & 0xFF) }, 3);
			} else {
				msg.setMessage(88, new byte[] { (byte) t.kbpm, 2, 24, 8 }, 4);
			}
			track.add(new MidiEvent(msg, Math.round(t.pos)));
		}

		debug("Writing tempo finished");
	}
}
