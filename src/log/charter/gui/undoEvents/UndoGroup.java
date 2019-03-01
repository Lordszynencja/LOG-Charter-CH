package log.charter.gui.undoEvents;

import java.util.ArrayList;
import java.util.List;

import log.charter.gui.ChartData;

public class UndoGroup implements UndoEvent {
	private final List<UndoEvent> events;

	public UndoGroup(final List<UndoEvent> events) {
		this.events = events;
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final List<UndoEvent> undidEvents = new ArrayList<>(events.size());
		for (int i = events.size() - 1; i >= 0; i--) {
			final UndoEvent e = events.get(i);
			undidEvents.add(e.go(data));
		}

		return new UndoGroup(undidEvents);
	}

}
