package log.charter.gui.undoEvents;

import java.util.ArrayList;
import java.util.List;

import log.charter.gui.ChartData;
import log.charter.song.Event;

public class TapSectionsChange implements UndoEvent {
	private final List<Event> tapSections;

	public TapSectionsChange(final List<Event> tapSections) {
		this.tapSections = new ArrayList<>(tapSections.size());
		for (final Event e : tapSections) {
			this.tapSections.add(new Event(e));
		}
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final UndoEvent tapSectionsChange = new TapSectionsChange(data.currentInstrument.tap);
		data.currentInstrument.tap.clear();
		data.currentInstrument.tap.addAll(tapSections);
		return tapSectionsChange;
	}

}
