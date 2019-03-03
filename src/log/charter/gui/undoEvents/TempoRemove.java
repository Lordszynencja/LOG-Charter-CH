package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Tempo;

public class TempoRemove implements UndoEvent {
	private final Tempo tmp;

	public TempoRemove(final Tempo tmp) {
		this.tmp = tmp;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		data.s.tempoMap.addTempo(tmp, true);
		return new TempoAdd(tmp);
	}

}
