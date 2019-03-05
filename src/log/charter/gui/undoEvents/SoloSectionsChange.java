package log.charter.gui.undoEvents;

import java.util.ArrayList;
import java.util.List;

import log.charter.gui.ChartData;
import log.charter.song.Event;

public class SoloSectionsChange implements UndoEvent {
	private final List<Event> soloSections;

	public SoloSectionsChange(final List<Event> soloSections) {
		this.soloSections = new ArrayList<>(soloSections.size());
		for (final Event e : soloSections) {
			this.soloSections.add(new Event(e));
		}
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final UndoEvent soloSectionsChange = new SoloSectionsChange(data.currentInstrument.solo);
		data.currentInstrument.solo.clear();
		data.currentInstrument.solo.addAll(soloSections);
		return soloSectionsChange;
	}

}
