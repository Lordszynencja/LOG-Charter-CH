package log.charter.data;

import static java.lang.Math.abs;
import static log.charter.gui.ChartPanel.isInLanes;
import static log.charter.gui.ChartPanel.yToLane;

import java.awt.HeadlessException;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import log.charter.gui.ChartEventsHandler;
import log.charter.gui.ChartPanel;
import log.charter.gui.LyricPane;
import log.charter.io.ClipboardHandler;
import log.charter.io.Logger;
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

	private static byte[] joinList(final List<byte[]> list) {
		int length = 0;
		for (final byte[] b : list) {
			length += b.length + 2;
		}

		final byte[] bytes = new byte[length];
		int a = 0;
		for (final byte[] b : list) {
			bytes[a] = (byte) (b.length & 255);
			bytes[a + 1] = (byte) ((b.length >> 8) & 255);

			System.arraycopy(b, 0, bytes, a + 2, b.length);
			a += b.length + 2;
		}

		return bytes;
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

	private static List<byte[]> splitToList(final byte[] bytes) {
		int a = 0;
		final List<byte[]> list = new ArrayList<>(bytes.length / 20);
		while (a < bytes.length) {
			final int length = (bytes[a] & 255) + ((bytes[a + 1] & 255) << 8);
			a += 2;

			list.add(Arrays.copyOfRange(bytes, a, a + length));
			a += length;
		}

		return list;
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
	public int mousePressX = -1;
	public int mousePressY = -1;
	public int mx = -1;
	public int my = -1;
	public int t = 0;
	public double nextT = 0;
	public double zoom = 1;
	public boolean drawAudio = false;

	public int gridSize = 1;
	public boolean useGrid = true;
	public boolean isNoteAdd = false;
	public boolean isNoteDrag = false;

	public boolean changed = false;

	public final UndoSystem undoSystem;
	public ChartEventsHandler handler;

	public ChartData() {
		resetZoom();
		undoSystem = new UndoSystem(this);
	}

	public void addVocalNote(final double pos, final int tone, final String text, final boolean toneless,
			final boolean wordPart, final boolean connected) {
		undoSystem.addUndo();

		deselect();
		final Lyric l = new Lyric(pos, tone, text, toneless, wordPart, connected);
		int insertPos = findFirstLyricAfterTime(pos);

		if (insertPos == -1) {
			insertPos = s.v.lyrics.size();
			s.v.lyrics.add(l);
		} else {
			s.v.lyrics.add(insertPos, l);
		}
		l.setLength(Config.minTailLength);
		if (insertPos > 0) {
			fixLyricLength(s.v.lyrics.get(insertPos - 1), insertPos - 1, l);
		}

		selectedNotes.add(insertPos);
		lastSelectedNote = insertPos;
	}

	public void addZoom(final int change) {
		setZoomLevel(Config.zoomLvl + change);
	}

	public void changeDifficulty(final int newDiff) {
		if (!currentInstrument.type.isVocalsType()) {
			currentDiff = newDiff;
			currentNotes = currentInstrument.notes.get(newDiff);
			softClear();
		}
	}

	private void changeEventList(final List<Event> events, final double start, final double end) {
		int id = 0;
		while (id < events.size()) {
			final Event e = events.get(id);
			if ((e.pos + e.getLength()) < start) {
				id++;
				continue;
			}
			if (e.pos > end) {
				break;
			}
			if ((e.pos == start) && ((e.pos + e.getLength()) == end)) {
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
		handler.frame.menuBar.changeInstrument(type);
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
		case DRUMS:
			currentInstrument = s.d;
			break;
		case KEYS:
			currentInstrument = s.k;
			break;
		case VOCALS:
			currentInstrument = s.vInstrument;
			break;
		default:
			break;
		}
		changeDifficulty(3);
		softClear();
		undoSystem.clear();
	}

	public void changeLyricLength(final int grids) {
		undoSystem.addUndo();
		for (final int id : selectedNotes) {
			final Lyric l = s.v.lyrics.get(id);
			if (useGrid) {
				if (grids < 0) {
					l.setLength(s.tempoMap.findNextGridPositionForTime(l.pos + l.getLength(), gridSize) - l.pos);
				} else {
					l.setLength(s.tempoMap.findPreviousGridPositionForTime(l.pos + l.getLength(), gridSize) - l.pos);
				}
			} else {
				l.setLength(l.getLength() - (100 * grids));
			}
			if ((id + 1) < s.v.lyrics.size()) {
				fixLyricLength(l, id, s.v.lyrics.get(id + 1));
			}
		}
	}

	public void changeLyricLines() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		undoSystem.addUndo();

		final Lyric first = s.v.lyrics.get(selectedNotes.get(0));
		final Lyric last = s.v.lyrics.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(s.v.lyricLines, first.pos, last.pos + last.getLength());
	}

	public void changeNoteLength(final int grids) {
		undoSystem.addUndo();
		for (final int id : selectedNotes) {
			final Note note = currentNotes.get(id);
			if (useGrid) {
				if (grids < 0) {
					note.setLength(
							s.tempoMap.findNextGridPositionForTime(note.pos + note.getLength(), gridSize) - note.pos);
				} else {
					note.setLength(s.tempoMap.findPreviousGridPositionForTime(note.pos + note.getLength(), gridSize)
							- note.pos);
				}
			} else {
				note.setLength(note.getLength() - (100 * grids));
			}
			fixNextNotesLength(note, id);
		}
	}

	private void changeSections(final List<Event> events) {
		if (selectedNotes.isEmpty()) {
			return;
		}
		undoSystem.addUndo();

		final Note first = currentNotes.get(selectedNotes.get(0));
		final Note last = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1));

		changeEventList(events, first.pos, last.pos + last.getLength());
	}

	public void changeSoloSections() {
		changeSections(currentInstrument.solo);
	}

	public void changeSPSections() {
		changeSections(currentInstrument.sp);
	}

	public void changeDrumRollSections() {
		changeSections(currentInstrument.drumRoll);
	}

	public void changeSpecialDrumRollSections() {
		changeSections(currentInstrument.specialDrumRoll);
	}

	public void changeTapSections() {
		changeSections(currentInstrument.tap);
		fixTapSections();
	}

	public void changeTempoBeatsInMeasure(final Tempo tmp, final boolean isNew, final int beats) {
		undoSystem.addUndo();
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
		mx = -1;
		my = -1;
		isNoteAdd = false;
		isNoteDrag = false;

		t = 0;
		nextT = 0;
		drawAudio = false;
		gridSize = 4;
		useGrid = true;
		undoSystem.clear();
	}

	public void copy() {
		if (isEmpty) {
			return;
		}

		final List<byte[]> list = new ArrayList<>(selectedNotes.size() + 1);

		if (currentInstrument.type.isVocalsType()) {
			final double firstLyricPos = s.v.lyrics.get(selectedNotes.get(0)).pos;
			list.add("lyrics".getBytes());

			for (final int id : selectedNotes) {
				list.add(s.v.lyrics.get(id).toBytes(firstLyricPos));
			}
		} else {
			final double firstNotePos = currentNotes.get(selectedNotes.get(0)).pos;
			list.add("notes".getBytes());

			for (final int id : selectedNotes) {
				list.add(currentNotes.get(id).toBytes(firstNotePos));
			}
		}

		ClipboardHandler.setClipboardBytes(joinList(list));
	}

	public void copyFrom(final InstrumentType instrumentType, final int diff) {
		if ((!isEmpty && (instrumentType != currentInstrument.type)) || (diff != currentDiff)) {
			final List<Note> from = s.getInstrument(instrumentType).notes.get(diff);

			undoSystem.addUndo();

			currentNotes.clear();
			for (int i = 0; i < from.size(); i++) {
				currentNotes.add(new Note(from.get(i)));
			}
		}
	}

	public void deleteSelected() {
		undoSystem.addUndo();

		for (int i = selectedNotes.size() - 1; i >= 0; i--) {
			final int id = selectedNotes.get(i);
			if (currentInstrument.type.isVocalsType()) {
				s.v.lyrics.remove(id);
			} else {
				currentNotes.remove(id);
			}
		}

		deselect();
	}

	public void deselect() {
		selectedNotes.clear();
		lastSelectedNote = null;
	}

	public void endNoteAdding() {
		isNoteAdd = false;
		deselect();

		if (isInLanes(my)) {
			if (currentInstrument.type.isVocalsType()) {
				final IdOrPos idOrPos = findClosestVocalIdOrPosForX(mousePressX);
				if (idOrPos.isId()) {
					removeVocalNote(idOrPos.id);
				} else {
					new LyricPane(handler.frame, idOrPos);
				}
				handler.setChanged();
			} else {
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
					if (currentInstrument.type.isVocalsType()) {
						return;
					}
					undoSystem.addUndo();
					final double length = lastNotePos - firstNotePos;
					int id = startIdOrPos.isId() ? startIdOrPos.id : findFirstNoteAfterTime(firstNotePos);

					final List<Note> notes = findNotesFromTo(id, lastNotePos);
					final List<Double> allGridPositions = s.tempoMap.getGridPositionsFromTo(gridSize, xToTime(x0),
							xToTime(x1));
					final List<Double> gridPositions = ChartData.removePostionsCloseToNotes(allGridPositions, notes);

					for (final Note note : notes) {
						final double part = (note.pos - firstNotePos) / length;
						final int colorBit = yToLane((y0 * (1 - part)) + (y1 * part), currentInstrument.type.lanes);
						if ((colorBit >= 0) && (colorBit <= currentInstrument.type.lanes)) {
							toggleNote(id, colorBit);
						}
						if ((currentNotes.size() > id) && (currentNotes.get(id) == note)) {
							id++;
						}
					}

					for (final Double pos : gridPositions) {
						final double part = (pos - firstNotePos) / length;
						final int colorBit = yToLane((y0 * (1 - part)) + (y1 * part), currentInstrument.type.lanes);
						if ((colorBit >= 0) && (colorBit <= currentInstrument.type.lanes)) {
							addNote(pos, colorBit);
						}
					}
				} else {
					toggleNote(startIdOrPos, yToLane(y0, currentInstrument.type.lanes));
				}
			}
		}
	}

	public void endNoteDrag() {
		isNoteDrag = false;
		if (selectedNotes.isEmpty()) {
			return;
		}

		handler.setChanged();

		if (currentInstrument.type.isVocalsType()) {
			endNoteDragLyrics();
		} else {
			endNoteDragNotes();
		}
	}

	private void endNoteDragLyrics() {
		undoSystem.addUndo();

		final List<Lyric> events = new ArrayList<>(selectedNotes.size());
		final List<Lyric> editedEvents = s.v.lyrics;
		for (int i = selectedNotes.size() - 1; i >= 0; i--) {
			final int id = selectedNotes.get(i);
			final Lyric l = editedEvents.remove(id);
			events.add(l);
		}

		final double dt = xToTime(mx) - events.get(events.size() - 1).pos;

		deselect();
		for (int i = events.size() - 1; i >= 0; i--) {
			final Lyric l = events.get(i);
			final IdOrPos noteMovedTo = findClosestVocalIdOrPosForTime(l.pos + dt, handler.isCtrl());
			if (noteMovedTo.isPos()) {
				l.pos = noteMovedTo.pos;
				int firstAfter = findFirstLyricAfterTime(l.pos);
				if (firstAfter == -1) {
					firstAfter = editedEvents.size();
				}
				editedEvents.add(firstAfter, l);
				selectedNotes.add(firstAfter);
				if ((firstAfter - 1) > 0) {
					fixLyricLength(editedEvents.get(firstAfter - 1), firstAfter - 1, l);
				}
				if ((firstAfter + 1) < events.size()) {
					fixLyricLength(l, firstAfter, editedEvents.get(firstAfter + 1));
				}
			}
		}
	}

	private void endNoteDragNotes() {
		undoSystem.addUndo();

		final List<Note> events = new ArrayList<>(selectedNotes.size());
		final List<Note> editedEvents = currentNotes;
		for (int i = selectedNotes.size() - 1; i >= 0; i--) {
			final int id = selectedNotes.get(i);
			final Note l = editedEvents.remove(id);
			events.add(l);
		}

		final double dt = xToTime(mx) - events.get(events.size() - 1).pos;

		deselect();
		for (int i = events.size() - 1; i >= 0; i--) {
			final Note n = events.get(i);
			final IdOrPos noteMovedTo = findClosestIdOrPosForTime(n.pos + dt, handler.isCtrl());
			if (noteMovedTo.isPos()) {
				final Note newNote = new Note(n);
				newNote.pos = noteMovedTo.pos;
				int firstAfter = findFirstNoteAfterTime(newNote.pos);
				if (firstAfter == -1) {
					firstAfter = editedEvents.size();
				}
				editedEvents.add(firstAfter, newNote);
				selectedNotes.add(firstAfter);
				fixNotesLength(newNote, firstAfter);
			} else {
				final int id = noteMovedTo.id;
				final Note existing = editedEvents.get(id);
				existing.notes |= n.notes;
			}
		}
		fixTapSections();
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

	public IdOrPos findClosestIdOrPosForTime(final double time, final boolean shouldUseGrid) {
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, shouldUseGrid, gridSize);
		final int closestNoteId = findClosestNoteForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - currentNotes.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time)) + (ChartPanel.noteW / 2)))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
	}

	public IdOrPos findClosestIdOrPosForX(final int x) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, useGrid, gridSize);
		final int closestNoteId = findClosestNoteForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - currentNotes.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time)) + (ChartPanel.noteW / 2)))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
	}

	public IdOrPos findClosestIdOrPosForX(final int x, final boolean shouldUseGrid) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, shouldUseGrid, gridSize);
		final int closestNoteId = findClosestNoteForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - currentNotes.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time)) + (ChartPanel.noteW / 2)))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
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

	public IdOrPos findClosestVocalIdOrPosForTime(final double time, final boolean shouldUseGrid) {
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, shouldUseGrid, gridSize);
		final int closestNoteId = findClosestVocalForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - s.v.lyrics.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time))))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
	}

	public IdOrPos findClosestVocalIdOrPosForX(final int x) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, useGrid, gridSize);
		final int closestNoteId = findClosestVocalForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - s.v.lyrics.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time))))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
	}

	public IdOrPos findClosestVocalIdOrPosForX(final int x, final boolean shouldUseGrid) {
		final double time = xToTime(x);
		final double closestGridPosition = s.tempoMap.findClosestGridPositionForTime(time, shouldUseGrid, gridSize);
		final int closestNoteId = findClosestVocalForTime(time);

		return (closestNoteId == -1) || (timeToXLength(abs(time - s.v.lyrics.get(closestNoteId).pos)) > //
				(timeToXLength(abs(closestGridPosition - time))))//
						? new IdOrPos(-1, closestGridPosition)
						: new IdOrPos(closestNoteId, -1);
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

	private void fixLyricLength(final Lyric l, final int id, final Lyric next) {
		if (next.pos < (Config.minLongNoteDistance + l.pos + l.getLength())) {
			l.setLength(next.pos - Config.minLongNoteDistance - l.pos);
		}
	}

	private void fixNextNotesLength(final Note n, final int id) {
		if (n.getLength() < Config.minTailLength) {
			n.setLength(1);
		}
		for (int i = id + 1; (i < currentNotes.size()) && (i < (id + 100)); i++) {
			final Note nextNote = currentNotes.get(i);
			if (fixNoteLength(n, id, nextNote)) {
				return;
			}
		}
	}

	private boolean fixNoteLength(final Note n, final int nId, final Note next) {
		if (n.getLength() < Config.minTailLength) {
			n.setLength(1);
			return true;
		}

		if (next == null) {
			return false;
		}

		if ((n.crazy ? notesColorsOverlap(n, next) : true) && notesOverlap(n, next)) {
			n.setLength(next.pos - Config.minLongNoteDistance - n.pos);
			return true;
		}

		return false;
	}

	private void fixNotesLength(final Note n, final int id) {
		fixNextNotesLength(n, id);
		fixPreviousNotesLength(n, id);
	}

	private void fixPreviousNotesLength(final Note n, final int id) {
		for (int i = id - 1; (i >= 0) && (i > (id - 100)); i--) {
			final Note prevNote = currentNotes.get(i);
			fixNoteLength(prevNote, i, n);
		}
	}

	private void fixTapSections() {
		if (currentInstrument.tap.size() == 0) {
			for (final List<Note> diffNotes : currentInstrument.notes) {
				for (final Note n : diffNotes) {
					n.tap = false;
				}
			}
			return;
		}

		for (int diff = 0; diff < 4; diff++) {
			final List<Note> diffNotes = currentInstrument.notes.get(diff);
			for (int i = 0; i < diffNotes.size(); i++) {
				final Note n = diffNotes.get(i);
				if (n.tap) {
					n.tap = false;
				}
				for (final Event e : currentInstrument.tap) {
					if ((n.pos >= e.pos) && (n.pos <= (e.pos + e.getLength()))) {
						n.tap = true;
					}
				}
			}
		}
	}

	private boolean isInSection(final Note n, final List<Event> sections) {
		for (final Event e : currentInstrument.tap) {
			if ((e.pos + e.getLength()) < n.pos) {
				continue;
			}
			if (e.pos > n.pos) {
				break;
			}
			return true;
		}
		return false;
	}

	public void moveSelectedDown() {
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

	public void moveSelectedUp() {
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

	private boolean notesColorsOverlap(final Note n, final Note next) {
		return ((next.notes & n.notes) > 0) || (next.notes == 0) || (n.notes == 0);
	}

	private boolean notesOverlap(final Note n, final Note next) {
		return next.pos < (Config.minLongNoteDistance + n.pos + n.getLength());
	}

	public void paste() throws HeadlessException, IOException, UnsupportedFlavorException {
		final byte[] notesData = ClipboardHandler.readClipboardBytes();

		try {
			final List<byte[]> list = splitToList(notesData);
			final String name = new String(list.get(0));
			final boolean notesPaste = "notes".equals(name);
			final boolean lyricsPaste = "lyrics".equals(name);
			if ((lyricsPaste && !currentInstrument.type.isVocalsType())
					|| (notesPaste && currentInstrument.type.isVocalsType())) {
				return;
			}

			deselect();
			undoSystem.addUndo();
			final int n = list.size();
			final double markerPos = nextT;

			if (notesPaste) {
				int noteId = findFirstNoteAfterTime(markerPos);
				if (noteId < 0) {
					noteId = currentNotes.size();
				}

				for (int i = 1; i < n; i++) {
					final Note note = Note.fromBytes(list.get(i), markerPos);

					while ((noteId < currentNotes.size()) && (currentNotes.get(noteId).pos < note.pos)) {
						noteId++;
					}

					if (noteId < currentNotes.size()) {// is inside
						if (currentNotes.get(noteId).pos != note.pos) {
							currentNotes.add(noteId, note);
							fixNotesLength(note, noteId);
						}
					} else {// is last
						currentNotes.add(note);
						fixNotesLength(note, noteId);
					}
					selectedNotes.add(noteId);
					noteId++;
				}
			} else if (lyricsPaste) {
				int lyricId = findFirstLyricAfterTime(markerPos);
				if (lyricId < 0) {
					lyricId = s.v.lyrics.size();
				}

				for (int i = 1; i < n; i++) {
					final Lyric l = Lyric.fromBytes(list.get(i), markerPos);

					while ((lyricId < s.v.lyrics.size()) && (s.v.lyrics.get(lyricId).pos < l.pos)) {
						lyricId++;
					}

					if (lyricId < s.v.lyrics.size()) {// is inside
						if (s.v.lyrics.get(lyricId).pos != l.pos) {
							s.v.lyrics.add(lyricId, l);
							fixLyricLength(l, lyricId, s.v.lyrics.get(lyricId + 1));
							if (lyricId > 0) {
								fixLyricLength(s.v.lyrics.get(lyricId), lyricId - 1, l);
							}
						}
					} else {// is last
						s.v.lyrics.add(l);
						if (lyricId > 0) {
							fixLyricLength(s.v.lyrics.get(lyricId - 1), lyricId - 1, l);
						}
					}
					selectedNotes.add(lyricId);
					lyricId++;
				}
			}
		} catch (final Exception e) {
			Logger.error("Couldn't paste", e);
		}
	}

	public void redo() {
		deselect();
		undoSystem.redo();
	}

	public void removeVocalNote(final int id) {
		undoSystem.addUndo();
		final List<? extends Event> events = currentInstrument.type.isVocalsType() ? s.v.lyrics : currentNotes;
		events.remove(id);
		selectedNotes.remove((Integer) id);
	}

	public void resetZoom() {
		zoom = Math.pow(0.99, Config.zoomLvl);
	}

	public void selectAll() {
		deselect();
		final List<? extends Event> events = currentInstrument.type.isVocalsType() ? s.v.lyrics : currentNotes;
		for (int i = 0; i < events.size(); i++) {
			selectedNotes.add(i);
		}
	}

	public void setSong(final String dir, final Song song, final IniData iniData, final MusicData musicData) {
		clear();
		isEmpty = false;
		s = song;
		changeInstrument(InstrumentType.GUITAR);
		path = dir;
		Config.lastPath = path;
		ini = iniData;
		music = musicData;
	}

	public void setZoomLevel(final int newZoomLevel) {
		Config.zoomLvl = newZoomLevel;
		resetZoom();
	}

	public void snapSelectedNotes() {
		undoSystem.addUndo();

		for (int i = 0; i < selectedNotes.size(); i++) {
			final int id = selectedNotes.get(i);
			final Note n = currentNotes.get(id);
			final double newPos = s.tempoMap.findClosestGridPositionForTime(n.pos, useGrid, gridSize);
			if (((id > 0) && (newPos <= currentNotes.get(id - 1).pos))//
					|| (((id + 1) < currentNotes.size()) && (newPos >= currentNotes.get(id + 1).pos))) {
				selectedNotes.remove(i);
				for (int j = i; j < selectedNotes.size(); j++) {
					selectedNotes.add(j, selectedNotes.remove(j) - 1);
				}
				currentNotes.remove(id);
			} else {
				final double newLength = s.tempoMap.findClosestGridPositionForTime(n.pos + n.getLength(), useGrid,
						gridSize) - newPos;
				n.pos = newPos;
				n.setLength(newLength);
			}
		}
		for (int i = 0; i < selectedNotes.size(); i++) {
			final int id = selectedNotes.get(i);
			fixNotesLength(currentNotes.get(id), id);
		}
	}

	public void snapSelectedVocals() {
		undoSystem.addUndo();

		for (int i = 0; i < selectedNotes.size(); i++) {
			final int id = selectedNotes.get(i);
			final Lyric l = s.v.lyrics.get(id);
			final double newPos = s.tempoMap.findClosestGridPositionForTime(l.pos, useGrid, gridSize);
			if (((id > 0) && (newPos <= s.v.lyrics.get(id - 1).pos))//
					|| (((id + 1) < s.v.lyrics.size()) && (newPos >= s.v.lyrics.get(id + 1).pos))) {
				selectedNotes.remove(i);
				for (int j = i; j < selectedNotes.size(); j++) {
					selectedNotes.add(j, selectedNotes.remove(j) - 1);
				}
				s.v.lyrics.remove(id);
			} else {
				final double newLength = s.tempoMap.findClosestGridPositionForTime(l.pos + l.getLength(), useGrid,
						gridSize) - newPos;
				l.pos = newPos;
				l.setLength(newLength);

				if (id > 0) {
					fixLyricLength(s.v.lyrics.get(id - 1), id - 1, l);
				}
				if ((id + 1) < s.v.lyrics.size()) {
					fixLyricLength(l, id, s.v.lyrics.get(id + 1));
				}
			}
		}
	}

	private void softClear() {
		deselect();
		softClearWithoutDeselect();
	}

	public void softClearWithoutDeselect() {
		draggedTempoPrev = null;
		draggedTempo = null;
		draggedTempoNext = null;
		mousePressX = -1;
		mousePressY = -1;
		isNoteAdd = false;
		isNoteDrag = false;
	}

	public void startNoteAdding(final int x, final int y) {
		isNoteAdd = true;
		mousePressX = x;
		mousePressY = y;
	}

	public void startTempoDrag(final Tempo prevTmp, final Tempo tmp, final Tempo nextTmp, final boolean isNew) {
		draggedTempoPrev = prevTmp;
		draggedTempo = tmp;
		draggedTempoNext = nextTmp;
		undoSystem.addUndo();
	}

	public void stopTempoDrag() {
		draggedTempoPrev = null;
		draggedTempo = null;
		draggedTempoNext = null;
	}

	public int timeToX(final double pos) {
		return (int) ((pos - t) * zoom) + Config.markerOffset;
	}

	public int timeToXLength(final double length) {
		return (int) (length * zoom);
	}

	private void addNote(final double pos, final int colorBit) {
		int color = 0;
		if (currentInstrument.type.isGuitarType()) {
			color = colorBit == 0 ? 0 : (1 << (colorBit - 1));
		} else {
			color = 1 << colorBit;
		}

		final Note n = new Note(pos, color);
		int insertPos = findFirstNoteAfterTime(pos);
		if (insertPos == -1) {
			insertPos = currentNotes.size();
			currentNotes.add(n);
		} else {
			currentNotes.add(insertPos, n);
		}
		n.tap = isInSection(n, currentInstrument.tap);

		fixNotesLength(n, insertPos);
		selectedNotes.add(insertPos);
		lastSelectedNote = insertPos;
	}

	public void toggleNote(final IdOrPos idOrPos, final int colorBit) {
		undoSystem.addUndo();

		if (idOrPos.isPos()) {
			addNote(idOrPos.pos, colorBit);
		} else if (idOrPos.isId()) {
			toggleNote(idOrPos.id, colorBit);
		}
	}

	private void toggleNote(final int id, final int colorBit) {
		final Note n = currentNotes.get(id);
		int color = 0;
		if (currentInstrument.type.isGuitarType()) {
			color = colorBit == 0 ? 0 : (1 << (colorBit - 1));
		} else {
			color = 1 << colorBit;
		}

		if (n.notes == color) {
			currentNotes.remove(id);
			selectedNotes.remove((Integer) id);
		} else if (color == 0) {
			n.notes = 0;
			fixNotesLength(n, id);
			if (!selectedNotes.contains(id)) {
				selectedNotes.add(id);
				selectedNotes.sort(null);
			}
		} else {
			n.notes ^= color;
			fixNotesLength(n, id);
			if (!selectedNotes.contains(id)) {
				selectedNotes.add(id);
				selectedNotes.sort(null);
			}
		}
	}

	public void toggleSelectedCrazy() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			n.crazy = !n.crazy;
			fixNotesLength(n, id);
		}
	}

	public void toggleSelectedHopo(final boolean ctrl, final double maxDistance) {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
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
			l.toneless = !l.toneless;
		}
	}

	public void toggleSelectedNotes(final int colorBit) {
		undoSystem.addUndo();

		for (int i = selectedNotes.size() - 1; i >= 0; i--) {
			final int id = selectedNotes.get(i);
			toggleNote(id, colorBit);
			if ((selectedNotes.size() > i) && (selectedNotes.get(i) != id)) {
				for (int j = i; j < selectedNotes.size(); j++) {
					selectedNotes.set(j, selectedNotes.get(j) - 1);
				}
			}
		}
	}

	public void toggleSelectedVocalsWordPart() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Lyric l = s.v.lyrics.get(id);
			l.wordPart = !l.wordPart;
		}
	}

	public void toggleSelectedNotesExpertPlus() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			n.expertPlus = !n.expertPlus;
		}
	}

	public void toggleSelectedNotesYellowTom() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			n.yellowTom = !n.yellowTom;
		}
	}

	public void toggleSelectedNotesBlueTom() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			n.blueTom = !n.blueTom;
		}
	}

	public void toggleSelectedNotesGreenTom() {
		undoSystem.addUndo();

		for (final int id : selectedNotes) {
			final Note n = currentNotes.get(id);
			n.greenTom = !n.greenTom;
		}
	}

	public void undo() {
		deselect();
		undoSystem.undo();
	}

	public double xToTime(final int x) {
		return ((x - Config.markerOffset) / zoom) + t;
	}

	public double xToTimeLength(final int x) {
		return x / zoom;
	}
}
