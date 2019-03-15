package log.charter.gui.undoEvents;

import java.util.ArrayList;
import java.util.List;

import log.charter.gui.ChartData;
import log.charter.song.Event;

public class LyricLinesChange implements UndoEvent {
	private final List<Event> lyricLines;

	public LyricLinesChange(final List<Event> lyricLines) {
		this.lyricLines = new ArrayList<>(lyricLines.size());
		for (final Event e : lyricLines) {
			this.lyricLines.add(new Event(e));
		}
	}

	@Override
	public UndoEvent go(final ChartData data) {
		final UndoEvent spSectionsChange = new LyricLinesChange(data.s.v.lyricLines);
		data.s.v.lyricLines.clear();
		data.s.v.lyricLines.addAll(lyricLines);
		return spSectionsChange;
	}

}
