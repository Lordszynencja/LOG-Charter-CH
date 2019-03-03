package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;
import log.charter.song.Tempo;

public class TempoChange implements UndoEvent {
	private final Tempo t;
	private final double pos;
	private final int kbpm;
	private final int beats;

	public TempoChange(final Tempo t) {
		this.t = t;
		pos = t.pos;
		kbpm = t.kbpm;
		beats = t.beats;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		t.pos = pos;
		t.kbpm = kbpm;
		t.beats = beats;
		return new TempoChange(t);
	}

}
