package log.charter.io.midi.writer;

import static log.charter.io.Logger.debug;

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
		final ShortMessage msgStart = new ShortMessage();
		msgStart.setMessage(-112, note, 100);
		track.add(new MidiEvent(msgStart, Math.round(pos)));
		final ShortMessage msgEnd = new ShortMessage();
		msgEnd.setMessage(-112, note, 0);
		track.add(new MidiEvent(msgEnd, Math.round(end)));
	}

	public static void write(final Instrument instr, final InstrumentType type, final Track track)
			throws InvalidMidiDataException {
		debug("Writing " + type);

		final byte[] bytes = TrackType.fromInstrumentType(type).partName.getBytes();
		final MetaMessage msg = new MetaMessage();
		msg.setMessage(3, bytes, bytes.length);
		track.add(new MidiEvent(msg, 0));

		writeSP(instr.sp, track);
		writeTap(instr.tap, track);
		writeSolo(instr.solo, track);
		writeNotes(instr.notes, track);

		debug("Writing " + type + " finished");
	}

	private static void writeNotes(final List<List<Note>> notes, final Track track) throws InvalidMidiDataException {
		for (int diffId = 0; diffId < 4; diffId++) {
			final List<Note> diff = notes.get(diffId);
			for (final Note n : diff) {
				if (n.notes == 0) {
					final SysexMessage msgStart = new SysexMessage();
					msgStart.setMessage(240, new byte[] { 80, 83, 0, 0, 3, 1, 1, -9 }, 8);
					track.add(new MidiEvent(msgStart, Math.round(n.pos)));
					final SysexMessage msgEnd = new SysexMessage();
					msgEnd.setMessage(240, new byte[] { 80, 83, 0, 0, 3, 1, 0, -9 }, 8);
					track.add(new MidiEvent(msgEnd, Math.round(n.pos + n.getLength())));
				} else {
					for (int i = 0; i < 5; i++) {
						if ((n.notes & (1 << i)) > 0) {
							addNote(60 + (diffId * 12) + i, n.pos, n.pos + n.getLength(), track);
						}
					}
				}
				if (n.hopo) {
					addNote(65 + (diffId * 12), n.pos, n.pos + n.getLength(), track);
				} else {
					addNote(66 + (diffId * 12), n.pos, n.pos + n.getLength(), track);
				}
			}
		}
	}

	private static void writeSolo(final List<Event> solo, final Track track) throws InvalidMidiDataException {
		for (final Event e : solo) {
			addNote(103, e.pos, e.pos + e.getLength(), track);
		}
	}

	private static void writeSP(final List<Event> sp, final Track track) throws InvalidMidiDataException {
		for (final Event e : sp) {
			addNote(116, e.pos, e.pos + e.getLength(), track);
		}
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
