package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Note;

public class NoteRemove implements UndoEvent {
	private final int id;
	private final Note n;

	public NoteRemove(final int id, final Note n) {
		this.id = id;
		this.n = n;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		data.currentNotes.add(id, n);
		return new NoteAdd(id);
	}

}
