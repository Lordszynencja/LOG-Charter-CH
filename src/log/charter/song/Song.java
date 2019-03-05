package log.charter.song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import log.charter.song.Instrument.InstrumentType;

public class Song {

	public static void addNote(final List<List<Note>> list, final Note n, final int diff) {
		if ((diff < 0) || (diff > 255)) {
			return;
		}

		while (list.size() < diff) {
			list.add(new ArrayList<>());
		}

		list.get(diff).add(n);
	}

	public Instrument g;
	public Instrument b;
	public Instrument k;

	public Map<Integer, String> sections = new HashMap<>();
	public TempoMap tempoMap;

	public Song() {
		g = new Instrument(InstrumentType.GUITAR);
		b = new Instrument(InstrumentType.BASS);
		k = new Instrument(InstrumentType.KEYS);
		tempoMap = new TempoMap(new ArrayList<>());
	}

	public Song(final Song s) {
		g = new Instrument(s.g);
		b = new Instrument(s.b);
		k = new Instrument(s.k);
		s.sections.forEach((id, sec) -> sections.put(id, sec));

		tempoMap = new TempoMap(s.tempoMap);
	}

	public Instrument[] instruments() {
		return new Instrument[] { g, b, k };
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Song{g: ").append(g)//
				.append(",\n\tb: ").append(b)//
				.append(",\n\tk: ").append(k)//
				.append(",\n\tsections: [");

		boolean first = true;
		for (final Entry<Integer, String> pair : sections.entrySet()) {
			sb.append(first ? "" : ",\n\t\t").append("(" + pair.getKey() + "," + pair.getValue() + ")");
			first = false;
		}
		sb.append("],\n\ttempoMap: ").append(tempoMap.toString().replaceAll("\n\t", "\n\t\t\t")).append("}");

		return sb.toString();
	}

}
