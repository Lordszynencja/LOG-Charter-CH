package log.charter.song;

import java.util.ArrayList;
import java.util.List;

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

	public List<Section> sections = new ArrayList<>();
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
		for (final Section sec : s.sections) {
			sections.add(new Section(sec));
		}

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
		for (final Section s : sections) {
			sb.append(first ? "" : ",\n\t\t").append(s);
			first = false;
		}
		sb.append("],\n\ttempoMap: ").append(tempoMap.toString().replaceAll("\n\t", "\n\t\t\t")).append("}");

		return sb.toString();
	}

}
