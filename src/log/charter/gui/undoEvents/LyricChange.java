package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Lyric;

public class LyricChange implements UndoEvent {
	private final int id;

	private final double pos;
	private final double length;
	private final int tone;

	private final boolean toneless;
	private final boolean wordPart;
	private final boolean connected;

	public LyricChange(final int id, final Lyric l) {
		this.id = id;

		pos = l.pos;
		length = l.length;
		tone = l.tone;

		toneless = l.toneless;
		wordPart = l.wordPart;
		connected = l.connected;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final Lyric l = data.s.v.lyrics.get(id);
		final UndoEvent undo = new LyricChange(id, l);

		l.pos = pos;
		l.length = length;
		l.tone = tone;

		l.toneless = toneless;
		l.wordPart = wordPart;
		l.connected = connected;

		return undo;
	}

}
