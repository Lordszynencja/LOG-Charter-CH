package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;

public class NoteAdd implements UndoEvent {
	private final int id;

	public NoteAdd(final int id) {
		this.id = id;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		return new NoteRemove(id, data.currentNotes.remove(id));
	}

}
