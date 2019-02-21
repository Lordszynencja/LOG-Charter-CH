package log.io;

import static java.util.Arrays.copyOfRange;
import static log.io.Logger.error;
import static log.io.Logger.info;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import log.io.MidTrack.MidEvent;
import log.song.Event;
import log.song.Note;
import log.song.Section;
import log.song.Song;
import log.song.Tempo;

public final class MidiReader {
	private static enum EventType {
		NOTE, TAP_SECTION, SP_SECTION;

		public static EventType from(final MidEvent e) {
			if (e.msg.length == 9 && (e.msg[0] & 0xFF) == 0xF0 && (e.msg[5] & 0xFF) == 0xFF)
				return TAP_SECTION;
			if ((e.msg[1] & 0xFF) == 116)
				return SP_SECTION;
			return NOTE;
		}
	}

	public static Song readMidi(final String path) {
		try {
			return new MidiReader(path).read();
		} catch (final Exception e) {
			Logger.error("Couln't load midi file", e);
			return null;
		}
	}

	private final List<MidTrack> tracks;
	private final Song s = new Song();

	int trackId = 0;
	int eventId = 0;

	private List<List<Note>> notes = new ArrayList<>();
	private List<Event> sps = new ArrayList<>();
	private Event sp = null;
	private List<Event> tapSections = new ArrayList<>();
	private Event tapSection = null;

	private MidiReader(final String path) throws InvalidMidiDataException, IOException {
		final Sequence seq = MidiSystem.getSequence(new File(path));
		final double scaler = 192.0D / seq.getResolution();

		tracks = new ArrayList<>(seq.getTracks().length);
		boolean isTempo = true;
		for (final Track t : seq.getTracks()) {
			tracks.add(new MidTrack(t, isTempo, scaler));
			isTempo = false;
		}
		info("Sequence loaded from " + path);
	}

	private void handleSP(final MidEvent e) {
		if (sp == null) {
			info("SP started at track " + trackId + ", event " + eventId);
			sp = new Event(e.t);
		} else {
			info("SP ended at track " + trackId + ", event " + eventId);

			sp.length = e.t - sp.pos;
			sps.add(sp);
			sp = null;
		}
	}

	private void handleTap(final MidEvent e) {
		if ((e.msg[7] & 0xFF) == 1) {// start1, end0
			if (tapSection != null) {
				error("Slider already started, restart at track " + trackId + ", event " + eventId);
				return;
			}
			info("Slider started at track " + trackId + ", event " + eventId);
			tapSection = new Event(e.t);
		} else if ((e.msg[7] & 0xFF) == 0) {
			if (tapSection == null) {
				error("No slider started, restart at track " + trackId + ", event " + eventId);
				return;
			}
			info("Slider ended at track " + trackId + ", event " + eventId);

			tapSection.length = e.t - tapSection.pos;
			tapSections.add(tapSection);
			tapSection = null;
		}
	}

	private Song read() {
		try {
			for (final MidTrack t : tracks) {
				switch (t.type) {
				case TEMPO:
					readTempo(t);
					break;
				case GUITAR:
				case BASS:
				case KEYS:
					readNotes(t);
					break;
				case EVENTS:
					readSections(t);
					break;
				default:
					break;

				}
				trackId++;
			}
		} catch (final Exception e) {
			System.err.println("Error occured when reading midi");
			e.printStackTrace();
		}

		return s;
	}

	private void readNotes(final MidTrack t) {
		switch (t.type) {
		case GUITAR:
			notes = s.g;
			sps = s.gsp;
			tapSections = s.gt;
			break;
		case BASS:
			notes = s.b;
			sps = s.bsp;
			tapSections = s.bt;
			break;
		case KEYS:
			notes = s.k;
			sps = s.ksp;
			tapSections = new ArrayList<>();
			break;
		default:
			break;
		}

		notes.clear();
		notes.add(new ArrayList<>());
		notes.add(new ArrayList<>());
		notes.add(new ArrayList<>());
		notes.add(new ArrayList<>());
		sps.clear();
		tapSections.clear();

		for (eventId = 0; eventId < t.events.length; eventId++) {
			final MidEvent e = t.events[eventId];
			switch (EventType.from(e)) {
			case SP_SECTION:
				handleSP(e);
				break;
			case TAP_SECTION:
				handleTap(e);
				break;
			case NOTE:// TODO
				break;
			}
		}
	}

	private void readSections(final MidTrack t) {
		info("Reading sections");
		s.sections.clear();
		for (eventId = 0; eventId < t.events.length; eventId++) {
			final MidEvent e = t.events[eventId];

			final String name = new String(e.msg);
			if (name.startsWith("[section "))
				s.sections.add(new Section(name.substring(12, name.length() - 1), e.t));
		}
		info("Reading sections finished");
	}

	private void readTempo(final MidTrack t) {
		info("Reading tempo");
		s.tempos.clear();
		for (eventId = 0; eventId < t.events.length; eventId++) {
			final MidEvent e = t.events[eventId];

			final int type = e.msg[1];
			if (type == 3) {// song name
				final String text = new String(copyOfRange(e.msg, 3, e.msg.length));
				info("Song name: " + text);
			} else if (type == 81) {// bpm change
				final int mpq = (e.msg[3] & 0xFF) << 16 | (e.msg[4] & 0xFF) << 8 | e.msg[5] & 0xFF;

				final int bpm = (int) Math.floor(6.0E7D / mpq * 1000.0D);
				s.tempos.add(new Tempo(e.t, bpm, true));
			} else if (type == 88) {// sync
				final int num = e.msg[3];
				s.tempos.add(new Tempo(e.t, num, false));
			}
		}
		info("Reading tempo finished");
	}
}
