package log.io.midi.writer;

import static log.io.TickMsConverter.convertToTick;

import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import log.io.Logger;
import log.io.TickMsConverter;
import log.song.Instrument.InstrumentType;
import log.song.Song;

public final class MidiWriter {
	public static final byte[] TRACK_END = { -1, 47, 0 };

	public static void writeMidi(final String path, final Song s) {
		try {
			final Song conv = convertToTick(s);

			final Sequence seq = new Sequence(Sequence.PPQ, TickMsConverter.ticksPerBeat);

			TempoWriter.write(conv.tempos, seq.createTrack());
			SectionsWriter.write(conv.sections, seq.createTrack());
			if (conv.g.hasNotes()) {
				InstrumentWriter.write(conv.g, InstrumentType.GUITAR, seq.createTrack());
			}
			if (conv.b.hasNotes()) {
				InstrumentWriter.write(conv.b, InstrumentType.BASS, seq.createTrack());
			}
			if (conv.k.hasNotes()) {
				InstrumentWriter.write(conv.k, InstrumentType.KEYS, seq.createTrack());
			}

			MidiSystem.write(seq, 1, new File(path));
		} catch (final Exception e) {
			Logger.error("Couln't save midi file", e);
		}

	}
}
