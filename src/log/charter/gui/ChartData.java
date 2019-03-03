package log.charter.gui;

import static java.lang.Math.abs;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import log.charter.gui.undoEvents.NoteAdd;
import log.charter.gui.undoEvents.NoteChange;
import log.charter.gui.undoEvents.NoteRemove;
import log.charter.gui.undoEvents.UndoEvent;
import log.charter.gui.undoEvents.UndoGroup;
import log.charter.song.Event;
import log.charter.song.IniData;
import log.charter.song.Instrument;
import log.charter.song.Note;
import log.charter.song.Section;
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

	public static double btt(final double t, final int kbpm) {
		return (t * 60000000.0) / kbpm;
	}

	public static double ttb(final double t, final int kbpm) {
		return (t * kbpm) / 60000000.0;
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
	public int mousePressX = -1;
	public int mousePressY = -1;
	public int dragStartX = -1;
	public int dragStartY = -1;
	public int mx = -1;
	public int my = -1;

	public int t = 0;
	public double nextT = 0;
	public double zoom = 1;
	public int markerOffset = 300;
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
		dragStartX = -1;
		dragStartY = -1;
		mx = -1;
		my = -1;

		t = 0;
		drawAudio = false;
		changed = false;
	}

	public double closestGridTime(final double time, final int tmpId) {
		final Tempo tmp = s.tempos.get(tmpId);
		final long gridPoint = round(ttb(time - tmp.pos, tmp.kbpm) * gridSize);

		if (tmpId < (s.tempos.size() - 1)) {
			final Tempo nextTmp = s.tempos.get(tmpId + 1);
			if (gridPoint == ((nextTmp.id - tmp.id) * gridSize)) {
				return nextTmp.pos;
			}
		}
		return tmp.pos + (btt(gridPoint, tmp.kbpm) / gridSize);
	}

	public int findBeatId(final int time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				return (int) (tmp.id + ttb(time - tmp.pos, lastKbpm));
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return (int) (tmp.id + ttb(time - tmp.pos, lastKbpm));
	}

	public double findBeatTime(final double time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, lastKbpm)), lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, lastKbpm)), lastKbpm);
	}

	public double findBeatTimeById(final int id) {// TODO binary search
		if (id < 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if (s.tempos.get(i + 1).id > id) {
				return tmp.pos + btt(id - tmp.id, lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return tmp.pos + btt(id - tmp.id, lastKbpm);
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

	public double findClosestGridPositionForTime(final double time) {
		if (useGrid) {
			final int tmpId = findTempoId(time);
			final double closestGrid = closestGridTime(time, tmpId);
			return closestGrid;
		}
		return time;
	}

	public IdOrPos findClosestIdOrPosForX(final int x) {
		final double time = xToTime(x);
		final double closestGridPosition = findClosestGridPositionForTime(time);
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

	public double findNextBeatTime(final int time) {// TODO binary search
		if (time < 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
				if (s.tempos.get(i + 1).id <= id) {
					return s.tempos.get(i + 1).pos;
				}
				return tmp.pos + btt(id - tmp.id, lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
		return tmp.pos + btt(id - tmp.id, lastKbpm);
	}

	public Section findOrCreateSectionCloseTo(final double time) {// TODO binary
																					  // search
		for (int i = 0; i < s.sections.size(); i++) {
			final Section section = s.sections.get(i);
			if ((section.pos + 2) < time) {
				continue;
			}
			if ((section.pos - 2) > time) {
				final Section newSection = new Section("", time);
				s.sections.add(i, newSection);
				return newSection;
			}
			return section;
		}
		final Section newSection = new Section("", time);
		s.sections.add(newSection);
		return newSection;
	}

	public Tempo findTempo(final double time) {
		return s.tempos.get(findTempoId(time));
	}

	public int findTempoId(final double time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}

		int result = 0;
		for (int i = 0; i < s.tempos.size(); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (!tmp.sync) {
				continue;
			}

			if (tmp.pos > time) {
				return result;
			}
			result = i;
		}

		return result;
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

	public void redo() {
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

	public int timeToX(final double pos) {
		return (int) ((pos - t) * zoom) + markerOffset;
	}

	public int timeToXLength(final double pos) {
		return (int) (pos * zoom);
	}

	public void toggleNote(final IdOrPos idOrPos, final int colorByte) {
		final int color = colorByte == 0 ? 0 : (1 << (colorByte - 1));
		if (idOrPos.isPos()) {
			final List<UndoEvent> undoEvents = new ArrayList<>();
			final Note n = new Note(idOrPos.pos, color);
			int insertPos = findFirstNoteAfterTime(idOrPos.pos);
			if (insertPos == -1) {
				insertPos = currentNotes.size();
				currentNotes.add(n);
			} else {
				currentNotes.add(insertPos, n);
			}
			n.tap = isInSection(n, currentInstrument.tap);
			undoEvents.add(new NoteAdd(insertPos));

			for (int i = insertPos - 1; (i > 0) && (i > (insertPos - 100)); i--) {
				final Note prevNote = currentNotes.get(i);
				if (((n.pos - prevNote.pos - prevNote.length) < Config.minNoteDistance)//
						&& (!n.crazy || ((n.notes & prevNote.notes) > 0) || (n.notes == 0) || (prevNote.notes == 0))) {
					undoEvents.add(new NoteChange(i, prevNote));
					prevNote.length = n.pos - Config.minNoteDistance - prevNote.pos;
					if (prevNote.length < Config.minTailLength) {
						prevNote.length = 0;
					}
				}
			}

			addUndo(new UndoGroup(undoEvents));
		} else if (idOrPos.isId()) {
			final Note n = currentNotes.get(idOrPos.id);
			if (n.notes == color) {
				addUndo(new NoteRemove(idOrPos.id, n));
				currentNotes.remove(idOrPos.id);
			} else if (color == 0) {
				n.notes = 0;
				addUndo(new NoteChange(idOrPos.id, n));
			} else {
				addUndo(new NoteChange(idOrPos.id, n));
				n.notes ^= color;
			}
		}
	}

	public void undo() {
		if (!undo.isEmpty()) {
			redo.add(undo.removeLast().go(this));
		}
	}

	public double xToTime(final int x) {
		return ((x - markerOffset) / zoom) + t;
	}

}
