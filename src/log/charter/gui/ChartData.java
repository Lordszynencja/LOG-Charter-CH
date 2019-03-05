package log.charter.gui;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	private final LinkedList<UndoEvent> undo = new LinkedList<>();

	private final LinkedList<UndoEvent> redo = new LinkedList<>();

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

	public void addZoom(final int change) {
		setZoomLevel(Config.zoomLvl + change);
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
		selectedNotes.sort(null);
		final double start = currentNotes.get(selectedNotes.get(0)).pos;
		final double end = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1)).pos;

		addUndo(new SoloSectionsChange(currentInstrument.solo));

		int id = 0;
		while (id < currentInstrument.solo.size()) {
			final Event solo = currentInstrument.solo.get(id);
			if ((solo.pos + solo.length) < start) {
				id++;
				continue;
			}
			if (solo.pos > end) {
				break;
			}
			currentInstrument.solo.remove(id);
		}

		currentInstrument.solo.add(new Event(start, end - start));
	}

	public void changeSPSections() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		selectedNotes.sort(null);
		final double start = currentNotes.get(selectedNotes.get(0)).pos;
		final double end = currentNotes.get(selectedNotes.get(selectedNotes.size() - 1)).pos;

		addUndo(new SPSectionsChange(currentInstrument.sp));

		int id = 0;
		while (id < currentInstrument.sp.size()) {
			final Event sp = currentInstrument.sp.get(id);
			if ((sp.pos + sp.length) < start) {
				id++;
				continue;
			}
			if (sp.pos > end) {
				break;
			}
			currentInstrument.sp.remove(id);
		}

		currentInstrument.sp.add(new Event(start, end - start));
	}

	public void changeTapSections() {
		if (selectedNotes.isEmpty()) {
			return;
		}
		selectedNotes.sort(null);
		final int startId = selectedNotes.get(0);
		final int endId = selectedNotes.get(selectedNotes.size() - 1);

		final double start = currentNotes.get(startId).pos;
		final double end = currentNotes.get(endId).pos;
		final List<UndoEvent> undoEvents = new ArrayList<>((endId - startId) + 2);
		undoEvents.add(new TapSectionsChange(currentInstrument.tap));

		int id = 0;
		int firstChangedNote = -1;
		int lastChangedNote = -1;
		while (id < currentInstrument.tap.size()) {
			final Event tap = currentInstrument.tap.get(id);
			if ((tap.pos + tap.length) < start) {
				id++;
				continue;
			}
			if (tap.pos > end) {
				break;
			}
			if (firstChangedNote < 0) {
				firstChangedNote = findFirstNoteAfterTime(tap.pos);
			}
			if ((tap.pos + tap.length) > end) {
				lastChangedNote = findLastNoteBeforeTime(tap.pos + tap.length);
			}
			currentInstrument.tap.remove(id);
		}

		if (firstChangedNote >= 0) {
			for (int i = firstChangedNote; i < startId; i++) {
				final Note n = currentNotes.get(i);
				undoEvents.add(new NoteChange(i, n));
				n.tap = false;
			}
		}
		for (int i = startId; i <= endId; i++) {
			final Note n = currentNotes.get(i);
			undoEvents.add(new NoteChange(i, n));
			n.tap = true;
		}
		if (lastChangedNote >= 0) {
			for (int i = endId + 1; i <= lastChangedNote; i++) {
				final Note n = currentNotes.get(i);
				undoEvents.add(new NoteChange(i, n));
				n.tap = false;
			}
		}

		currentInstrument.tap.add(new Event(start, end - start));
		addUndo(new UndoGroup(undoEvents));
	}

	public void clear() {
		path = "C:/";
		s = new Song();
		ini = new IniData();
		music = new MusicData(new byte[0], 44100);
		currentInstrument = s.g;
		currentDiff = 3;
		currentNotes = currentInstrument.notes.get(currentDiff);

		selectedNotes.clear();
		lastSelectedNote = null;
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

	public void deselect() {
		selectedNotes.clear();
		lastSelectedNote = null;
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

	public void redo() {
		deselect();
		if (!redo.isEmpty()) {
			undo.add(redo.removeLast().go(this));
		}
	}

	public void resetZoom() {
		zoom = Math.pow(0.99, Config.zoomLvl);
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

		for (int i = insertPos - 1; (i >= 0) && (i > (insertPos - 100)); i--) {
			final Note prevNote = currentNotes.get(i);
			fixNoteLength(prevNote, i, n, undoEvents);
		}
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
		} else {
			undoEvents.add(new NoteChange(id, n));
			n.notes ^= color;
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
