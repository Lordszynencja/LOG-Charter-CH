package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;

public class LyricAdd implements UndoEvent {
	private final int id;

	public LyricAdd(final int id) {
		this.id = id;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		return new LyricRemove(id, data.s.v.lyrics.remove(id));
	}

}
