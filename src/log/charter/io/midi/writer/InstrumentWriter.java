package log.charter.io.midi.writer;

import static log.charter.io.Logger.debug;
import static log.charter.io.Logger.error;
import static log.charter.io.midi.NoteIds.EVENT_DRUM_ROLL;
import static log.charter.io.midi.NoteIds.EVENT_SOLO;
import static log.charter.io.midi.NoteIds.EVENT_SP;
import static log.charter.io.midi.NoteIds.EVENT_SPECIAL_DRUM_ROLL;
import static log.charter.io.midi.NoteIds.getNoteId;
import static log.charter.util.ByteUtils.getBit;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import log.charter.io.midi.MidTrack.TrackType;
import log.charter.song.Event;
import log.charter.song.Instrument;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Note;

public class InstrumentWriter {
	private static void addNote(final int note, final double pos, final double end, final Track track)
			throws InvalidMidiDataException {
		if (note == -1) {
			error("INVALID NOTE");
		}
		final ShortMessage msgStart = new ShortMessage();
		msgStart.setMessage(-112, note, 100);
		track.add(new MidiEvent(msgStart, Math.round(pos)));
		final ShortMessage msgEnd = new ShortMessage();
		msgEnd.setMessage(-112, note, 0);
		track.add(new MidiEvent(msgEnd, Math.round(end)));
	}

	private static void writeGuitar(final Instrument instrument, final Track track) throws InvalidMidiDataException {
		final byte[] bytes = TrackType.fromInstrumentType(instrument.type).partName.getBytes();
		final MetaMessage msg = new MetaMessage();
		msg.setMessage(3, bytes, bytes.length);
		track.add(new MidiEvent(msg, 0));

		writeSP(instrument.sp, track);
		writeTap(instrument.tap, track);
		writeSolo(instrument.solo, track);
		writeGuitarNotes(instrument.notes, track);
	}

	private static void writeDrums(final Instrument instrument, final Track track) throws InvalidMidiDataException {
		final byte[] bytes = TrackType.fromInstrumentType(instrument.type).partName.getBytes();
		final MetaMessage msg = new MetaMessage();
		msg.setMessage(3, bytes, bytes.length);
		track.add(new MidiEvent(msg, 0));

		writeSP(instrument.sp, track);
		writeSolo(instrument.solo, track);
		writeDrumRoll(instrument.drumRoll, track);
		writeSpecialDrumRoll(instrument.specialDrumRoll, track);
		writeDrumsNotes(instrument.notes, track);
	}

	private static void writeKeys(final Instrument instrument, final Track track) throws InvalidMidiDataException {
		final byte[] bytes = TrackType.fromInstrumentType(instrument.type).partName.getBytes();
		final MetaMessage msg = new MetaMessage();
		msg.setMessage(3, bytes, bytes.length);
		track.add(new MidiEvent(msg, 0));

		writeSP(instrument.sp, track);
		writeSolo(instrument.solo, track);
		writeKeysNotes(instrument.notes, track);
	}

	public static void write(final Instrument instrument, final Track track) throws InvalidMidiDataException {
		debug("Writing " + instrument.type);

		if (instrument.type.isGuitarType()) {
			writeGuitar(instrument, track);
		} else if (instrument.type.isDrumsType()) {
			writeDrums(instrument, track);
		} else if (instrument.type.isKeysType()) {
			writeKeys(instrument, track);
		}

		debug("Writing " + instrument.type + " finished");
	}

	private static void writeGuitarNotes(final List<List<Note>> notes, final Track track)
			throws InvalidMidiDataException {
		for (int diffId = 0; diffId < 4; diffId++) {
			final List<Note> diff = notes.get(diffId);
			for (final Note n : diff) {
				if (n.notes == 0) {
					addNote(getNoteId(InstrumentType.GUITAR, diffId, 0), n.pos, n.pos + n.getLength(), track);
					final SysexMessage msgStart = new SysexMessage();
					msgStart.setMessage(240, new byte[] { 80, 83, 0, 0, 3, 1, 1, -9 }, 8);
					track.add(new MidiEvent(msgStart, Math.round(n.pos)));
					final SysexMessage msgEnd = new SysexMessage();
					msgEnd.setMessage(240, new byte[] { 80, 83, 0, 0, 3, 1, 0, -9 }, 8);
					track.add(new MidiEvent(msgEnd, Math.round(n.pos + n.getLength())));
				} else {
					for (int i = 0; i < 5; i++) {
						if (getBit(n.notes, i)) {
							addNote(getNoteId(InstrumentType.GUITAR, diffId, i), n.pos, n.pos + n.getLength(), track);
						}
					}
				}

				addNote(getNoteId(InstrumentType.GUITAR, diffId, n.hopo ? 5 : 6), n.pos, n.pos + n.getLength(), track);
			}
		}
	}

	private static void writeDrumsNotes(final List<List<Note>> notes, final Track track)
			throws InvalidMidiDataException {
		for (int diffId = 0; diffId < 4; diffId++) {
			final List<Note> diff = notes.get(diffId);
			for (final Note n : diff) {
				for (int i = 1; i < 5; i++) {
					if (getBit(n.notes, i)) {
						addNote(getNoteId(InstrumentType.DRUMS, diffId, i), n.pos, n.pos + n.getLength(), track);
					}
				}
				if (getBit(n.notes, 0)) {
					addNote(getNoteId(InstrumentType.DRUMS, diffId, n.expertPlus ? 5 : 0), n.pos, n.pos + n.getLength(),
							track);
				}
				if (getBit(n.notes, 2) && n.yellowTom) {
					addNote(getNoteId(InstrumentType.DRUMS, diffId, 6), n.pos, n.pos + n.getLength(), track);
				}
				if (getBit(n.notes, 3) && n.blueTom) {
					addNote(getNoteId(InstrumentType.DRUMS, diffId, 7), n.pos, n.pos + n.getLength(), track);
				}
				if (getBit(n.notes, 4) && n.greenTom) {
					addNote(getNoteId(InstrumentType.DRUMS, diffId, 8), n.pos, n.pos + n.getLength(), track);
				}
			}
		}
	}

	private static void writeKeysNotes(final List<List<Note>> notes, final Track track)
			throws InvalidMidiDataException {
		for (int diffId = 0; diffId < 4; diffId++) {
			final List<Note> diff = notes.get(diffId);
			for (final Note n : diff) {
				for (int i = 0; i < 5; i++) {
					if ((n.notes & (1 << i)) > 0) {
						addNote(getNoteId(InstrumentType.KEYS, diffId, i), n.pos, n.pos + n.getLength(), track);
					}
				}
			}
		}
	}

	private static void writeEvents(final List<Event> events, final Track track, final int noteId)
			throws InvalidMidiDataException {
		for (final Event e : events) {
			addNote(noteId, e.pos, e.pos + e.getLength(), track);
		}
	}

	private static void writeSolo(final List<Event> solo, final Track track) throws InvalidMidiDataException {
		writeEvents(solo, track, EVENT_SOLO);
	}

	private static void writeSP(final List<Event> sp, final Track track) throws InvalidMidiDataException {
		writeEvents(sp, track, EVENT_SP);
	}

	private static void writeDrumRoll(final List<Event> drumRoll, final Track track) throws InvalidMidiDataException {
		writeEvents(drumRoll, track, EVENT_DRUM_ROLL);
	}

	private static void writeSpecialDrumRoll(final List<Event> specialDrumRoll, final Track track)
			throws InvalidMidiDataException {
		writeEvents(specialDrumRoll, track, EVENT_SPECIAL_DRUM_ROLL);
	}

	private static void writeTap(final List<Event> tap, final Track track) throws InvalidMidiDataException {
		for (final Event e : tap) {
			final SysexMessage msgStart = new SysexMessage();
			msgStart.setMessage(240, new byte[] { 80, 83, 0, 0, -1, 4, 1, -9 }, 8);
			track.add(new MidiEvent(msgStart, Math.round(e.pos)));
			final SysexMessage msgEnd = new SysexMessage();
			msgEnd.setMessage(240, new byte[] { 80, 83, 0, 0, -1, 4, 0, -9 }, 8);
			track.add(new MidiEvent(msgEnd, Math.round(e.pos + e.getLength())));
		}
	}
}
