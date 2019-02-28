package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Note;

public class NoteChange implements UndoEvent {
	private final int id;

	private final double pos;
	private final double length;
	private final int notes;

	private final boolean forced;
	private final boolean hopo;
	private final boolean tap;
	private final boolean crazy;

	public NoteChange(final int id, final Note n) {
		this.id = id;

		pos = n.pos;
		length = n.length;
		notes = n.notes;

		forced = n.forced;
		hopo = n.hopo;
		tap = n.tap;
		crazy = n.crazy;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final Note n = data.currentNotes.get(id);
		final UndoEvent undo = new NoteChange(id, n);

		n.pos = pos;
		n.length = length;
		n.notes = notes;

		n.forced = forced;
		n.hopo = hopo;
		n.tap = tap;
		n.crazy = crazy;

		return undo;
	}

}
