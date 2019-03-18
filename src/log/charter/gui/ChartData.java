package log.charter.gui;

import static java.lang.Math.abs;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;

import log.charter.gui.undoEvents.LyricAdd;
import log.charter.gui.undoEvents.LyricChange;
import log.charter.gui.undoEvents.LyricLinesChange;
import log.charter.gui.undoEvents.LyricRemove;
import log.charter.gui.undoEvents.NoteAdd;
import log.charter.gui.undoEvents.NoteChange;
import log.charter.gui.undoEvents.NoteRemove;
import log.charter.gui.undoEvents.SPSectionsChange;
import log.charter.gui.undoEvents.SoloSectionsChange;
import log.charter.gui.undoEvents.TapSectionsChange;
import log.charter.gui.undoEvents.TempoAdd;
import log.charter.gui.undoEvents.TempoChange;
import log.charter.gui.undoEvents.UndoEvent;
import log.charter.gui.undoEvents.UndoGroup;
import log.charter.song.Event;
import log.charter.song.IniData;
import log.charter.song.Instrument;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Lyric;
import log.charter.song.Note;
import log.charter.song.Song;
import log.charter.song.Tempo;
import log.charter.sound.MusicData;

public class ChartData {
	public static class IdOrPos {
		public final int id;
		public final double pos;

		public IdOrPos(final int id, final double pos) {
			this.id = id;
			this.pos = pos;
		}

		public boolean isId() {
			return id >= 0;
		}

		public boolean isPos() {
			return pos >= 0;
		}

		@Override
		public String toString() {
			return "IdOrPos{" + (id >= 0 ? "id:" + id + "}" : "pos:" + pos + "}");
		}
	}

	private static double bytesToDouble(final byte[] bytes) {
		final long l = (((long) bytes[0]) & 255)//
				| ((((long) bytes[1]) & 255) << 8) //
				| (((((long) bytes[2])) & 255) << 16)//
				| ((((long) bytes[3]) & 255) << 24)//
				| ((((long) bytes[4]) & 255) << 32)//
				| ((((long) bytes[5]) & 255) << 40)//
				| ((((long) bytes[6]) & 255) << 48)//
				| ((((long) bytes[7]) & 255) << 56);
		return Double.longBitsToDouble(l);
	}

	private static byte[] doubleToBytes(final double d) {
		final long l = Double.doubleToLongBits(d);
		return new byte[] {
				(byte) (l & 255), //
				(byte) ((l >> 8) & 255), //
				(byte) ((l >> 16) & 255), //
				(byte) ((l >> 24) & 255), //
				(byte) ((l >> 32) & 255), //
				(byte) ((l >> 40) & 255), //
				(byte) ((l >> 48) & 255), //
				(byte) ((l >> 56) & 255) };
	}

	public static List<Double> removePostionsCloseToNotes(final List<Double> positions, final List<Note> notes) {
		final int posSize = positions.size();
		final List<Double> newPositions = new ArrayList<>(posSize);
		if (posSize == 0) {
			return newPositions;
		}

		final int notesSize = notes.size();
		if (notesSize == 0) {
			return positions;
		}

		int posId = 0;
		int noteId = 0;
		double pos = positions.get(posId);
		double notePos = notes.get(noteId).pos;
		while (posId < posSize) {
			if (Math.abs(notePos - pos) < Config.minNoteDistance) {
				posId++;
				if (posId >= posSize) {
					break;
				}
				pos = positions.get(posId);
				continue;
			}
			if (notePos < pos) {
				noteId++;
				if (noteId >= notesSize) {
					break;
				}
				notePos = notes.get(noteId).pos;
				continue;
			}

			newPositions.add(pos);
			posId++;
			if (posId >= posSize) {
				break;
			}
			pos = positions.get(posId);
		}

		for (; posId < posSize; posId++) {
			newPositions.add(positions.get(posId));
		}

		return newPositions;
	}

	public String path = Config.lastPath;

	public boolean isEmpty = true;
	public Song s = new Song();
	public IniData ini = new IniData();
	public MusicData music = new MusicData(new byte[0], 44100);
	public Instrument currentInstrument = s.g;

	public int currentDiff = 3;
	public List<Note> currentNotes = s.g.notes.get(currentDiff);
	public List<Integer> selectedNotes = new ArrayList<>();

	public Integer lastSelectedNote = null;
	public Tempo draggedTempoPrev = null;
	public Tempo draggedTempo = null;
	public Tempo draggedTempoNext = null;
	public List<UndoEvent> draggedTempoUndo = null;
	public int mousePressX = -1;
	public int mousePressY = -1;
	public int mx = -1;
	public int my = -1;
	public int t = 0;

	public double nextT = 0;
	public double zoom = 1;
	public boolean drawAudio = false;
	public boolean changed = false;
	public int gridSize = 2;
	public boolean useGrid = true;
	public boolean vocalsEditing = false;
	private final LinkedList<UndoEvent> undo = new LinkedList<>();

	private final LinkedList<UndoEvent> redo = new LinkedList<>();
	public ChartEventsHandler handler;

	public ChartData() {
		resetZoom();
	}

	private void addUndo(final UndoEvent e) {
		undo.add(e);
		while (undo.size() > 100) {
			undo.removeFirst();
		}
		redo.clear();
	}

	public void addVocalNote(final double pos, final int tone, final String text, final boolean noTone,
			final boolean wordPart, final boolean connected) {
		final List<UndoEvent> undoEvents = new ArrayList<>();

		deselect();
		final Lyric l = new Lyric(pos, tone, text, noTone, wordPart, connected);
		int insertPos = findFirstLyricAfterTime(pos);

		if (insertPos == -1) {
			insertPos = s.v.lyrics.size();
			s.v.lyrics.add(l);
		} else {
			s.v.lyrics.add(insertPos, l);
		}
		l.length = Config.minTailLength;
		if (insertPos > 0) {
			fixLyricLength(s.v.lyrics.get(insertPos - 1), insertPos - 1, l, undoEvents);
		}

		undoEvents.add(new LyricAdd(insertPos));
		selectedNotes.add(insertPos);
		addUndo(new UndoGroup(undoEvents));
	}

	public void addZoom(final int change) {
		setZoomLevel(Config.zoomLvl + change);
	}

	public void changeDifficulty(final int newDiff) {
		if (!vocalsEditing) {
			currentDiff = newDiff;
			currentNotes = currentInstrument.notes.get(newDiff);
			softClear();
		}
	}

	private void changeEventList(final List<Event> events, final double start, final double end) {
		int id = 0;
		while (id < events.size()) {
			final Event e = events.get(id);
			if ((e.pos + e.length) < start) {
				id++;
				continue;
			}
			if (e.pos > end) {
				break;
			}
			if ((e.pos == start) && ((e.pos + e.length) == end)) {
				events.remove(id);
				return;
			}
			events.remove(id);
		}

		events.add(new Event(start, end - start));
		events.sort(null);
	}

	public void changeInstrument(final InstrumentType type) {
		handler.stopMusic();
		vocalsEditing = false;
		switch (type) {
		case GUITAR:
			currentInstrument = s.g;
			break;
		case GUITAR_COOP:
			currentInstrument = s.gc;
			break;
		case GUITAR_RHYTHM:
			currentInstrument = s.gr;
			break;
		case BASS:
			currentInstrument = s.b;
			break;
		case KEYS:
			currentInstrument = s.k;
			break;
		default:
			break;
		}
		changeDifficulty(3);
	}

	public void changeLyricLength(final int grids) {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Lyric l = s.v.lyrics.get(id);
			undoEvents.add(new LyricChange(id, l));
			if (useGrid) {
				if (grids < 0) {
					l.length = s.tempoMap.findNextGridPositionForTime(l.pos + l.length, gridSize) - l.pos;
				} else {
					l.length = s.tempoMap.findPreviousGridPositionForTime(l.pos + l.length, gridSize) - l.pos;
				}
			} else {
				l.length -= 100 * grids;
			}
			if ((id + 1) < s.v.lyrics.size()) {
				fixLyricLength(l, id, s.v.lyrics.get(id + 1), undoEvents);
			}
		}
	}

	public void changeLyricLines() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		addUndo(new LyricLinesChange(s.v.lyricLines));

		final Lyric first = s.v.lyrics.get(selectedNotes.get(0));
		final Lyric last = s.v.lyrics.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(s.v.lyricLines, first.pos, last.pos + last.length);
	}

	public void changeNoteLength(final int grids) {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Note note = currentNotes.get(id);
			undoEvents.add(new NoteChange(id, note));
			if (useGrid) {
				if (grids < 0) {
					note.length = s.tempoMap.findNextGridPositionForTime(note.pos + note.length, gridSize) - note.pos;
				} else {
					note.length = s.tempoMap.findPreviousGridPositionForTime(note.pos + note.length, gridSize) - note.pos;
				}
			} else {
				note.length -= 100 * grids;
			}
			if ((id + 1) < currentNotes.size()) {
				fixNoteLength(note, id, currentNotes.get(id + 1), undoEvents);
			} else {
				fixNoteLength(note, id, null, undoEvents);
			}
		}
	}

	public void changeSoloSections() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		addUndo(new SoloSectionsChange(currentInstrument.solo));

		final Note first = currentNotes.get(selectedNotes.get(0));
		final Note last = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(currentInstrument.solo, first.pos, last.pos + last.length);
	}

	public void changeSPSections() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		addUndo(new SPSectionsChange(currentInstrument.sp));

		final Note first = currentNotes.get(selectedNotes.get(0));
		final Note last = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(currentInstrument.sp, first.pos, last.pos + last.length);
	}

	public void changeTapSections() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		final List<UndoEvent> undoEvents = new ArrayList<>();
		undoEvents.add(new TapSectionsChange(currentInstrument.tap));

		final Note first = currentNotes.get(selectedNotes.get(0));
		final Note last = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(currentInstrument.tap, first.pos, last.pos + last.length);

		for (int diff = 0; diff < 4; diff++) {
			final List<Note> diffNotes = currentInstrument.notes.get(diff);
			for (int i = 0; i < diffNotes.size(); i++) {
				final Note n = diffNotes.get(i);
				if (currentInstrument.tap.size() == 0) {
					n.tap = false;
				} else {
					for (final Event e : currentInstrument.tap) {
						final boolean newTap = (n.pos >= e.pos) && (n.pos <= (e.pos + e.length));
						if (newTap != n.tap) {
							undoEvents.add(new NoteChange(i, n));
							n.tap = newTap;
						}
					}
				}
			}
		}
		addUndo(new UndoGroup(undoEvents));
	}

	public void changeTempoBeatsInMeasure(final Tempo tmp, final boolean isNew, final int beats) {
		if (isNew) {
			addUndo(new TempoAdd(tmp));
		} else {
			addUndo(new TempoChange(tmp));
		}
		tmp.beats = beats;
	}

	public void clear() {
		path = "C:/";
		s = new Song();
		ini = new IniData();
		music = new MusicData(new byte[0], 44100);
		currentInstrument = s.g;
		currentDiff = 3;
		currentNotes = currentInstrument.notes.get(currentDiff);

		deselect();
		draggedTempoPrev = null;
		draggedTempo = null;
		draggedTempoNext = null;
		draggedTempoUndo = null;
		mousePressX = -1;
		mousePressY = -1;
		mx = -1;
		my = -1;

		t = 0;
		nextT = 0;
		drawAudio = false;
		changed = false;
		gridSize = 2;
		useGrid = true;
	}

	public void copy() {// TODO vocals
		final double firstNotePos = currentNotes.get(selectedNotes.get(0)).pos;
		final byte[] copiedNotesData = new byte[(selectedNotes.size() * 18) + 5];
		copiedNotesData[0] = 'n';
		copiedNotesData[1] = 'o';
		copiedNotesData[2] = 't';
		copiedNotesData[3] = 'e';
		copiedNotesData[4] = 's';

		for (int i = 0; i < selectedNotes.size(); i++) {
			final Note n = currentNotes.get(selectedNotes.get(i));
			copiedNotesData[(18 * i) + 5] = (byte) n.notes;
			copiedNotesData[(18 * i) + 6] = (byte) ((n.crazy ? 4 : 0) + (n.hopo ? 2 : 0) + (n.tap ? 1 : 0));
			final double pos = n.pos - firstNotePos;

			System.arraycopy(doubleToBytes(pos), 0, copiedNotesData, (18 * i) + 7, 8);
			System.arraycopy(doubleToBytes(n.length), 0, copiedNotesData, (18 * i) + 15, 8);
		}
		final DataHandler dataHandler = new DataHandler(copiedNotesData, "application/octet-stream");
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(dataHandler, null);
	}

	public void copyFrom(final InstrumentType instrumentType, final int diff) {
		if ((!isEmpty && (instrumentType != currentInstrument.type)) || (diff != currentDiff)) {
			final List<Note> from = s.getInstrument(instrumentType).notes.get(diff);

			final List<UndoEvent> undoEvents = new ArrayList<>(from.size() + currentNotes.size());
			for (int i = currentNotes.size() - 1; i >= 0; i--) {
				undoEvents.add(new NoteRemove(i, currentNotes.get(i)));
			}
			currentNotes.clear();
			for (int i = 0; i < from.size(); i++) {
				undoEvents.add(new NoteAdd(i));
				currentNotes.add(new Note(from.get(i)));
			}
			addUndo(new UndoGroup(undoEvents));
		}
	}

	public void deleteSelected() {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());

		for (int i = selectedNotes.size() - 1; i >= 0; i--) {
			final int id = selectedNotes.get(i);
			if (vocalsEditing) {
				final Lyric l = s.v.lyrics.get(id);
				undoEvents.add(new LyricRemove(id, l));
				s.v.lyrics.remove(id);
			} else {
				final Note n = currentNotes.get(id);
				undoEvents.add(new NoteRemove(id, n));
				currentNotes.remove(id);
			}
		}

		addUndo(new UndoGroup(undoEvents));
		deselect();
	}

	public void deselect() {
		selectedNotes.clear();
		lastSelectedNote = null;
	}

	public void editVocals() {
		handler.stopMusic();
		vocalsEditing = true;
	}

	public void endNoteAdding() {
		deselect();
		if (ChartPanel.isInNotes(my) && ChartPanel.isInNotes(mousePressY)) {
			int x0, x1;
			int y0, y1;
			if (mx < mousePressX) {
				x0 = mx;
				y0 = my;
				x1 = mousePressX;
				y1 = mousePressY;
			} else {
				x0 = mousePressX;
				y0 = mousePressY;
				x1 = mx;
				y1 = my;
			}
			final IdOrPos startIdOrPos = findClosestIdOrPosForX(x0);
			final IdOrPos endIdOrPos = findClosestIdOrPosForX(x1);

			final double firstNotePos = startIdOrPos.isId() ? currentNotes.get(startIdOrPos.id).pos
					: startIdOrPos.pos;
			final double lastNotePos = endIdOrPos.isId() ? currentNotes.get(endIdOrPos.id).pos : endIdOrPos.pos;

			if (firstNotePos != lastNotePos) {
				final double length = lastNotePos - firstNotePos;
				int id = startIdOrPos.isId() ? startIdOrPos.id : findFirstNoteAfterTime(firstNotePos);

				final List<Note> notes = findNotesFromTo(id, lastNotePos);
				final List<Double> allGridPositions = s.tempoMap.getGridPositionsFromTo(gridSize, xToTime(x0), xToTime(x1));
				final List<Double> gridPositions = ChartData.removePostionsCloseToNotes(allGridPositions, notes);
				final List<UndoEvent> undoEvents = new ArrayList<>(notes.size() + gridPositions.size());

				for (final Note note : notes) {
					final double part = (note.pos - firstNotePos) / length;
					final int colorBit = ChartPanel.yToLane((y0 * (1 - part)) + (part * y1)) + 1;
					if ((colorBit >= 0) && (colorBit <= 5)) {
						this.toggleNote(id, colorBit, undoEvents);
					}
					if ((currentNotes.size() > id) && (currentNotes.get(id) == note)) {
						id++;
					}
				}

				for (final Double pos : gridPositions) {
					final double part = (pos - firstNotePos) / length;
					final int colorBit = ChartPanel.yToLane((y0 * (1 - part)) + (part * y1)) + 1;
					if ((colorBit >= 0) && (colorBit <= 5)) {
						this.toggleNote(pos, colorBit, undoEvents);
					}
				}

				addUndo(new UndoGroup(undoEvents));
			} else {
				this.toggleNote(startIdOrPos, ChartPanel.yToLane(y0) + 1);
			}
		}

		mousePressX = -1;
		mousePressY = -1;
	}

	public int findCloseNoteForTime(final double time) {
		final int closest = findClosestNoteForTime(time);
		if (closest == -1) {
			return -1;
		}
		final int noteX = timeToX(currentNotes.get(closest).pos);
		final int x = timeToX(time);

		return (noteX < (x + (ChartPanel.noteW / 2))) && (noteX > (x - (ChartPanel.noteW / 2))) ? closest : -1;
	}

	public IdOrPos findClosestIdOrPosForX(final int x) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, useGrid, gridSize);
		final int closestNoteId = findClosestNoteForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - currentNotes.get(closestNoteId).pos)) > //
		(timeToXLength(abs(closestGridPosition - time)) + (ChartPanel.noteW / 2)))//
				? new IdOrPos(-1, closestGridPosition) : new IdOrPos(closestNoteId, -1);
	}

	public int findClosestNoteForTime(final double time) {
		if (currentNotes.isEmpty()) {
			return -1;
		}

		int l = 0;
		int r = currentNotes.size() - 1;

		while ((r - l) > 1) {
			final int mid = (l + r) / 2;

			if (currentNotes.get(mid).pos > time) {
				r = mid;
			} else {
				l = mid;
			}
		}

		return (abs(currentNotes.get(l).pos - time) > abs(currentNotes.get(r).pos - time)) ? r : l;
	}

	public int findClosestVocalForTime(final double time) {
		if (s.v.lyrics.isEmpty()) {
			return -1;
		}

		int l = 0;
		int r = s.v.lyrics.size() - 1;

		while ((r - l) > 1) {
			final int mid = (l + r) / 2;

			if (s.v.lyrics.get(mid).pos > time) {
				r = mid;
			} else {
				l = mid;
			}
		}

		return (abs(s.v.lyrics.get(l).pos - time) > abs(s.v.lyrics.get(r).pos - time)) ? r : l;
	}

	public IdOrPos findClosestVocalIdOrPosForX(final int x) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, useGrid, gridSize);
		final int closestNoteId = findClosestVocalForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - s.v.lyrics.get(closestNoteId).pos)) > //
		(timeToXLength(abs(closestGridPosition - time))))//
				? new IdOrPos(-1, closestGridPosition) : new IdOrPos(closestNoteId, -1);
	}

	public int findFirstLyricAfterTime(final double time) {
		if (s.v.lyrics.isEmpty() || (s.v.lyrics.get(s.v.lyrics.size() - 1).pos < time)) {
			return -1;
		}

		for (int i = s.v.lyrics.size() - 1; i >= 0; i--) {
			if (s.v.lyrics.get(i).pos < time) {
				return i + 1;
			}
		}

		return 0;
	}

	public int findFirstNoteAfterTime(final double time) {
		if (currentNotes.isEmpty() || (currentNotes.get(currentNotes.size() - 1).pos < time)) {
			return -1;
		}

		for (int i = currentNotes.size() - 1; i >= 0; i--) {
			if (currentNotes.get(i).pos < time) {
				return i + 1;
			}
		}

		return 0;
	}

	public int findLastNoteBeforeTime(final double time) {
		if (currentNotes.isEmpty() || (currentNotes.get(0).pos > time)) {
			return -1;
		}

		for (int i = currentNotes.size() - 1; i >= 0; i--) {
			if (currentNotes.get(i).pos <= time) {
				return i;
			}
		}

		return 0;
	}

	public List<Note> findNotesFromTo(final int firstNoteId, final double end) {
		final List<Note> notes = new ArrayList<>();
		if (firstNoteId < 0) {
			return notes;
		}
		int nextId = firstNoteId;
		Note n = currentNotes.get(nextId);
		nextId++;
		if (nextId >= currentNotes.size()) {
			notes.add(n);
			return notes;
		}
		while (n.pos <= end) {
			notes.add(n);
			n = currentNotes.get(nextId);
			nextId++;
			if (nextId >= currentNotes.size()) {
				if (n.pos <= end) {
					notes.add(n);
				}
				break;
			}
		}

		return notes;
	}

	private void fixLyricLength(final Lyric l, final int id, final Lyric next, final List<UndoEvent> undoEvents) {
		if (next.pos < (Config.minLongNoteDistance + l.pos + l.length)) {
			undoEvents.add(new LyricChange(id, l));
			l.length = next.pos - Config.minLongNoteDistance - l.pos;
		}
		if (l.length < 0) {
			l.length = 0;
		}
	}

	private void fixNoteLength(final Note n, final int nId, final Note next, final List<UndoEvent> events) {
		if (n.length < Config.minTailLength) {
			n.length = 0;
		}
		if ((next != null) && (next.pos < (Config.minLongNoteDistance + n.pos + n.length))//
				&& (!next.crazy || ((next.notes & n.notes) > 0) || (next.notes == 0) || (n.notes == 0))) {
			events.add(new NoteChange(nId, n));
			n.length = next.pos - Config.minLongNoteDistance - n.pos;
		}
	}

	private void fixNotesLength(final Note n, final int id, final List<UndoEvent> undoEvents) {
		for (int i = id - 1; (i >= 0) && (i > (id - 100)); i--) {
			final Note prevNote = currentNotes.get(i);
			fixNoteLength(prevNote, i, n, undoEvents);
		}
		for (int i = id + 1; (i < currentNotes.size()) && (i < (id + 100)); i++) {
			final Note nextNote = currentNotes.get(i);
			fixNoteLength(n, id, nextNote, undoEvents);
		}
	}

	private boolean isInSection(final Note n, final List<Event> sections) {
		for (final Event e : currentInstrument.tap) {
			if ((e.pos + e.length) < n.pos) {
				continue;
			}
			if (e.pos > n.pos) {
				break;
			}
			return true;
		}
		return false;
	}

	public void moveSelectedDownWithOpen() {
		final List<Note> notes = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			if ((n.notes & 16) > 0) {
				return;
			}
			notes.add(n);
		}
		for (final Note n : notes) {
			n.notes *= 2;
			if (n.notes == 0) {
				n.notes = 1;
			}
		}
	}

	public void moveSelectedDownWithoutOpen() {
		final List<Note> notes = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			if ((n.notes == 0) || ((n.notes & 16) > 0)) {
				return;
			}
			notes.add(n);
		}
		for (final Note n : notes) {
			n.notes *= 2;
		}
	}

	public void moveSelectedUpWithOpen() {
		final List<Note> notes = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			if (n.notes == 0) {
				return;
			}
			notes.add(n);
		}
		for (final Note n : notes) {
			n.notes = (n.notes & 1) > 0 ? 0 : n.notes / 2;
		}
	}

	public void moveSelectedUpWithoutOpen() {
		final List<Note> notes = new ArrayList<>(selectedNotes.size());
		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			if ((n.notes == 0) || ((n.notes & 1) > 0)) {
				return;
			}
			notes.add(n);
		}
		for (final Note n : notes) {
			n.notes /= 2;
		}
	}

	public void paste() throws HeadlessException, IOException, UnsupportedFlavorException {// TODO
																														// vocals
		final Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

		byte[] notesData = null;
		for (final DataFlavor f : t.getTransferDataFlavors()) {
			final Object o = t.getTransferData(f);
			if (o instanceof byte[]) {
				notesData = (byte[]) o;
				break;
			}
		}
		if ((notesData == null) || (notesData.length < 5) || (((notesData.length - 5) % 18) != 0) || (notesData[0] != 'n')
				|| (notesData[1] != 'o') || (notesData[2] != 't') || (notesData[3] != 'e') || (notesData[4] != 's')) {
			return;
		}
		deselect();

		final int n = (notesData.length - 5) / 18;
		final List<UndoEvent> undoEvents = new ArrayList<>(n);
		final double markerPos = nextT;
		int noteId = findFirstNoteAfterTime(markerPos);
		if (noteId < 0) {
			noteId = currentNotes.size();
		}
		for (int i = 0; i < n; i++) {
			final double pos = bytesToDouble(Arrays.copyOfRange(notesData, (18 * i) + 7, (18 * i) + 15));
			final double length = bytesToDouble(Arrays.copyOfRange(notesData, (18 * i) + 15, (18 * i) + 23));
			final Note note = new Note(markerPos + pos, notesData[(18 * i) + 5]);
			note.length = length;
			note.crazy = (notesData[(18 * i) + 6] & 4) > 0;
			note.hopo = (notesData[(18 * i) + 6] & 2) > 0;
			note.tap = (notesData[(18 * i) + 6] & 1) > 0;

			while ((noteId < currentNotes.size()) && (currentNotes.get(noteId).pos < pos)) {
				noteId++;
			}

			if (noteId < currentNotes.size()) {// is inside
				if (currentNotes.get(noteId).pos != pos) {
					undoEvents.add(new NoteAdd(noteId));
					currentNotes.add(noteId, note);
					fixNotesLength(note, noteId, undoEvents);
				}
			} else {// is last
				undoEvents.add(new NoteAdd(noteId));
				currentNotes.add(note);
				fixNotesLength(note, noteId, undoEvents);
			}
			selectedNotes.add(noteId);
			noteId++;
		}

		addUndo(new UndoGroup(undoEvents));
	}

	public void redo() {
		deselect();
		if (!redo.isEmpty()) {
			undo.add(redo.removeLast().go(this));
		}
	}

	public void removeVocalNote(final int id) {
		deselect();
		addUndo(new LyricRemove(id, s.v.lyrics.remove(id)));
	}

	public void resetZoom() {
		zoom = Math.pow(0.99, Config.zoomLvl);
	}

	public void selectAll() {
		deselect();
		final List<? extends Event> events = vocalsEditing ? s.v.lyrics : currentNotes;
		for (int i = 0; i < events.size(); i++) {
			selectedNotes.add(i);
		}
	}

	public void setSong(final String dir, final Song song, final IniData iniData, final MusicData musicData) {
		clear();
		isEmpty = false;
		s = song;
		currentInstrument = s.g;
		currentNotes = currentInstrument.notes.get(currentDiff);
		path = dir;
		Config.lastPath = path;
		ini = iniData;
		music = musicData;
		undo.clear();
		redo.clear();
	}

	public void setZoomLevel(final int newZoomLevel) {
		Config.zoomLvl = newZoomLevel;
		resetZoom();
	}

	public void snapSelectedNotes() {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());

		for (int i = 0; i < selectedNotes.size(); i++) {
			final int id = selectedNotes.get(i);
			final Note n = currentNotes.get(id);
			final double newPos = s.tempoMap.findClosestGridPositionForTime(n.pos, useGrid, gridSize);
			if (((id > 0) && (newPos <= currentNotes.get(id - 1).pos))//
					|| (((id + 1) < currentNotes.size()) && (newPos >= currentNotes.get(id + 1).pos))) {
				undoEvents.add(new NoteRemove(id, n));
				selectedNotes.remove(i);
				for (int j = i; j < selectedNotes.size(); j++) {
					selectedNotes.add(j, selectedNotes.remove(j) - 1);
				}
				currentNotes.remove(id);
			} else {
				undoEvents.add(new NoteChange(id, n));
				final double newLength = s.tempoMap.findClosestGridPositionForTime(n.pos + n.length, useGrid, gridSize)
						- newPos;
				n.pos = newPos;
				n.length = newLength;

				if ((id + 1) < currentNotes.size()) {
					fixNoteLength(n, id, currentNotes.get(id + 1), undoEvents);
				}

				for (int j = id - 1; (j >= 0) && (j > (id - 100)); j--) {
					final Note prevNote = currentNotes.get(j);
					fixNoteLength(prevNote, j, n, undoEvents);
				}
			}
		}

		addUndo(new UndoGroup(undoEvents));
	}

	public void snapSelectedVocals() {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());

		for (int i = 0; i < selectedNotes.size(); i++) {
			final int id = selectedNotes.get(i);
			final Lyric l = s.v.lyrics.get(id);
			final double newPos = s.tempoMap.findClosestGridPositionForTime(l.pos, useGrid, gridSize);
			if (((id > 0) && (newPos <= s.v.lyrics.get(id - 1).pos))//
					|| (((id + 1) < s.v.lyrics.size()) && (newPos >= s.v.lyrics.get(id + 1).pos))) {
				undoEvents.add(new LyricRemove(id, l));
				selectedNotes.remove(i);
				for (int j = i; j < selectedNotes.size(); j++) {
					selectedNotes.add(j, selectedNotes.remove(j) - 1);
				}
				s.v.lyrics.remove(id);
			} else {
				undoEvents.add(new LyricChange(id, l));
				final double newLength = s.tempoMap.findClosestGridPositionForTime(l.pos + l.length, useGrid, gridSize)
						- newPos;
				l.pos = newPos;
				l.length = newLength;

				if (id > 0) {
					fixLyricLength(s.v.lyrics.get(id - 1), id - 1, l, undoEvents);
				}
				if ((id + 1) < s.v.lyrics.size()) {
					fixLyricLength(l, id, s.v.lyrics.get(id + 1), undoEvents);
				}
			}
		}

		addUndo(new UndoGroup(undoEvents));
	}

	private void softClear() {
		deselect();
		draggedTempoPrev = null;
		draggedTempo = null;
		draggedTempoNext = null;
		draggedTempoUndo = null;
		mousePressX = -1;
		mousePressY = -1;
		mx = -1;
		my = -1;
	}

	public void startNoteAdding(final int x, final int y) {
		mousePressX = x;
		mousePressY = y;
	}

	public void startTempoDrag(final Tempo prevTmp, final Tempo tmp, final Tempo nextTmp, final boolean isNew) {
		draggedTempoPrev = prevTmp;
		draggedTempo = tmp;
		draggedTempoNext = nextTmp;
		draggedTempoUndo = new ArrayList<>();
		if (isNew) {
			draggedTempoUndo.add(new TempoAdd(tmp));
		} else {
			draggedTempoUndo.add(new TempoChange(tmp));
		}
		draggedTempoUndo.add(new TempoChange(prevTmp));
		addUndo(new UndoGroup(draggedTempoUndo));
	}

	public void stopTempoDrag() {
		draggedTempoPrev = null;
		draggedTempo = null;
		draggedTempoNext = null;
		draggedTempoUndo = null;
	}

	public int timeToX(final double pos) {
		return (int) ((pos - t) * zoom) + Config.markerOffset;
	}

	public int timeToXLength(final double length) {
		return (int) (length * zoom);
	}

	private void toggleNote(final double pos, final int colorBit, final List<UndoEvent> undoEvents) {
		final int color = colorBit == 0 ? 0 : (1 << (colorBit - 1));

		final Note n = new Note(pos, color);
		int insertPos = findFirstNoteAfterTime(pos);
		if (insertPos == -1) {
			insertPos = currentNotes.size();
			currentNotes.add(n);
		} else {
			currentNotes.add(insertPos, n);
		}
		n.tap = isInSection(n, currentInstrument.tap);
		undoEvents.add(new NoteAdd(insertPos));

		fixNotesLength(n, insertPos, undoEvents);
		selectedNotes.add(insertPos);
	}

	public void toggleNote(final IdOrPos idOrPos, final int colorBit) {
		final List<UndoEvent> undoEvents = new ArrayList<>();

		if (idOrPos.isPos()) {
			toggleNote(idOrPos.pos, colorBit, undoEvents);
		} else if (idOrPos.isId()) {
			toggleNote(idOrPos.id, colorBit, undoEvents);
		}
		addUndo(new UndoGroup(undoEvents));
	}

	private void toggleNote(final int id, final int colorBit, final List<UndoEvent> undoEvents) {
		final Note n = currentNotes.get(id);
		final int color = colorBit == 0 ? 0 : (1 << (colorBit - 1));

		if (n.notes == color) {
			undoEvents.add(new NoteRemove(id, n));
			currentNotes.remove(id);
		} else if (color == 0) {
			n.notes = 0;
			undoEvents.add(new NoteChange(id, n));
			if (!selectedNotes.contains(id)) {
				selectedNotes.add(id);
				selectedNotes.sort(null);
			}
		} else {
			undoEvents.add(new NoteChange(id, n));
			n.notes ^= color;
			if (!selectedNotes.contains(id)) {
				selectedNotes.add(id);
				selectedNotes.sort(null);
			}
		}
	}

	public void toggleSelectedHopo(final boolean ctrl, final double maxDistance) {
		final List<UndoEvent> undoEvents = new ArrayList<>(selectedNotes.size());

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			undoEvents.add(new NoteChange(id, n));
			if (ctrl) {
				if (id == 0) {
					n.hopo = n.tap;
				} else {
					final Note prev = currentNotes.get(id - 1);
					final double distance = n.pos - prev.pos;
					n.hopo = n.tap || ((distance <= maxDistance) && (n.notes != prev.notes));
				}
			} else {
				n.hopo = n.tap || (n.hopo ^ true);
			}
		}

		addUndo(new UndoGroup(undoEvents));
	}

	public void toggleSelectedLyricConnected() {
		for (final int id : selectedNotes) {
			if (id != 0) {
				final Lyric l = s.v.lyrics.get(id);
				l.connected = !l.connected;
			}
		}
	}

	public void toggleSelectedLyricToneless() {
		for (final int id : selectedNotes) {
			final Lyric l = s.v.lyrics.get(id);
			l.noTone = !l.noTone;
		}
	}

	public void toggleSelectedVocalsWordPart() {
		for (final int id : selectedNotes) {
			if (id != 0) {
				final Lyric l = s.v.lyrics.get(id);
				l.wordPart = !l.wordPart;
			}
		}
	}

	public void undo() {
		deselect();
		if (!undo.isEmpty()) {
			redo.add(undo.removeLast().go(this));
		}
	}

	public double xToTime(final int x) {
		return ((x - Config.markerOffset) / zoom) + t;
	}

	public double xToTimeLength(final int x) {
		return x / zoom;
	}

}
