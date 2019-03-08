package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Lyric;

public class LyricRemove implements UndoEvent {
	private final int id;
	private final Lyric l;

	public LyricRemove(final int id, final Lyric l) {
		this.id = id;
		this.l = l;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		data.s.v.lyrics.add(id, l);
		return new LyricAdd(id);
	}

}
