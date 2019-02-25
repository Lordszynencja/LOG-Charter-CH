package log.io.midi.reader;

import static log.io.Logger.debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import log.io.Logger;
import log.io.TickMsConverter;
import log.io.midi.MidTrack;
import log.song.Instrument.InstrumentType;
import log.song.Song;

public final class MidiReader {

	public static Song readMidi(final String path) {
		try {
			return new MidiReader(path).read();
		} catch (final Exception e) {
			Logger.error("Couln't load midi file", e);
			return null;
		}
	}

	private final List<MidTrack> tracks;

	private MidiReader(final String path) throws InvalidMidiDataException, IOException {
		final Sequence seq = MidiSystem.getSequence(new File(path));
		final double scaler = ((double) TickMsConverter.ticksPerBeat) / seq.getResolution();

		tracks = new ArrayList<>(seq.getTracks().length);
		boolean isTempo = true;
		for (final Track t : seq.getTracks()) {
			tracks.add(new MidTrack(t, isTempo, scaler));
			isTempo = false;
		}
		debug("Sequence loaded from " + path);
	}

	private Song read() {
		final Song s = new Song();

		for (final MidTrack t : tracks) {
			switch (t.type) {
			case TEMPO:
				s.tempos = TempoReader.read(t);
				break;
			case GUITAR:
				s.g = InstrumentReader.read(t, InstrumentType.GUITAR);
				break;
			case BASS:
				s.b = InstrumentReader.read(t, InstrumentType.BASS);
				break;
			case KEYS:
				s.k = InstrumentReader.read(t, InstrumentType.KEYS);
				break;
			case EVENTS:
				s.sections = SectionsReader.read(t);
				break;
			default:
				break;
			}
		}

		return TickMsConverter.convertToMs(s);
	}
}
