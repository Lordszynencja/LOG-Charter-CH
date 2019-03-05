package log.charter.gui.undoEvents;

import java.util.ArrayList;
import java.util.List;

import log.charter.gui.ChartData;
import log.charter.song.Event;

public class SPSectionsChange implements UndoEvent {
	private final List<Event> spSections;

	public SPSectionsChange(final List<Event> spSections) {
		this.spSections = new ArrayList<>(spSections.size());
		for (final Event e : spSections) {
			this.spSections.add(new Event(e));
		}
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final UndoEvent spSectionsChange = new SPSectionsChange(data.currentInstrument.sp);
		data.currentInstrument.sp.clear();
		data.currentInstrument.sp.addAll(spSections);
		return spSectionsChange;
	}

}
