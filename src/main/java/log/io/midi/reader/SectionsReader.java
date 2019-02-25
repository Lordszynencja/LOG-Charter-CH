package log.io.midi.reader;

import static log.io.Logger.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import log.io.midi.MidTrack;
import log.io.midi.MidTrack.MidEvent;
import log.song.Section;

public class SectionsReader {
	public static List<Section> read(final MidTrack t) {
		debug("Reading sections");
		final List<Section> sections = new ArrayList<>(t.events.size());

		for (final MidEvent e : t.events) {
			final String name = new String(Arrays.copyOfRange(e.msg, 3, e.msg.length));
			if (name.startsWith("[section ")) {
				sections.add(new Section(name.substring(9, name.length() - 1), e.t));
			}
		}

		sections.sort(null);
		debug("Reading sections finished");
		return sections;
	}
}
