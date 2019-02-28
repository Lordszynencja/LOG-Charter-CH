package log.charter.gui.undoEvents;

import log.charter.gui.ChartData;

public interface UndoEvent {
	public UndoEvent go(ChartData data);
}
