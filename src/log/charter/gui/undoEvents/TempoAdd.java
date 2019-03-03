package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Tempo;

public class TempoAdd implements UndoEvent {
	private final Tempo tmp;

	public TempoAdd(final Tempo tmp) {
		this.tmp = tmp;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		data.s.tempoMap.tempos.remove(tmp);
		return new TempoRemove(tmp);
	}

}
