package log.charter.song;

import java.util.ArrayList;
import java.util.List;

public class Instrument {
	public static enum InstrumentType {
		GUITAR, GUITAR_COOP, GUITAR_RHYTM, BASS, KEYS, VOCALS;
	}

	private static final String[] diffNames = { "Easy", "Medium", "Hard", "Expert" };

	public final InstrumentType type;
	public final List<List<Note>> notes;
	public final List<Event> sp = new ArrayList<>();
	public final List<Event> tap = new ArrayList<>();
	public final List<Event> solo = new ArrayList<>();

	public Instrument(final Instrument instr) {
		this(instr.type);
		for (int i = 0; i < 4; i++) {
			final List<Note> diff = notes.get(i);
			for (final Note n : instr.notes.get(i)) {
				diff.add(new Note(n));
			}
		}
		for (final Event e : instr.sp) {
			sp.add(new Event(e));
		}
		for (final Event e : instr.tap) {
			tap.add(new Event(e));
		}
		for (final Event e : instr.solo) {
			solo.add(new Event(e));
		}
	}

	public Instrument(final InstrumentType type) {
		this.type = type;
		notes = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			notes.add(new ArrayList<>());
		}
	}

	public void fixNotes() {
		sort();

		for (int i = 0; i < 4; i++) {
			final List<Note> diffNotes = notes.get(i);
			final int size = diffNotes.size();

			if (size > 1) {
				if ((diffNotes.get(0).pos + diffNotes.get(0).length) >= diffNotes.get(1).pos) {
					diffNotes.get(0).crazy = true;
				}
				if (!diffNotes.get(size - 1).forced && (diffNotes.get(size - 1).notes != diffNotes.get(size - 2).notes)
						&& (diffNotes.get(size - 1).pos <= (diffNotes.get(size - 2).pos + 150))) {
					diffNotes.get(size - 1).hopo = true;
				}
			}

			for (int j = 1; j < (size - 1); j++) {
				final Note nm1 = diffNotes.get(j - 1);
				final Note n0 = diffNotes.get(j);
				final Note n1 = diffNotes.get(j + 1);
				if ((n0.pos + n0.length) >= n1.pos) {
					n0.crazy = true;
				}
				if (!n0.forced && (n0.notes != nm1.notes) && (n0.pos <= (nm1.pos + 150))) {
					n0.hopo = true;
				}
			}

			if (tap.size() > 0) {
				int nextTapId = 1;
				Event tapSection = tap.get(0);
				for (int j = 0; j < size; j++) {
					final Note n = diffNotes.get(j);
					if (tapSection.pos > n.pos) {
						continue;
					}
					while ((nextTapId < tap.size()) && ((tapSection.pos + tapSection.length) < n.pos)) {
						tapSection = tap.get(nextTapId);
						nextTapId++;
					}
					if ((nextTapId == tap.size()) && ((tapSection.pos + tapSection.length) < n.pos)) {
						break;
					} else {
						n.tap = true;
						n.hopo = true;
					}
				}
			}
		}
	}

	public boolean hasNotes() {
		for (final List<Note> diff : notes) {
			if (!diff.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public void sort() {
		notes.forEach(diff -> diff.sort(null));
		sp.sort(null);
		tap.sort(null);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Instrument{type: ").append(type.name())//
				.append(",\n\t\tnotes: {");

		boolean first;
		for (int i = 0; i < 4; i++) {
			sb.append(i == 0 ? "" : "],\n\t\t\t").append(diffNames[i]).append(": [");
			first = true;
			for (final Note n : notes.get(i)) {
				sb.append(first ? "" : ",\n\t\t\t\t").append(n);
				first = false;
			}
		}

		sb.append("]},\n\t\tsp: [");

		first = true;
		for (final Event e : sp) {
			sb.append(first ? "" : ",\n\t\t").append(e);
			first = false;
		}
		sb.append("],\n\t\ttap: [");

		first = true;
		for (final Event e : tap) {
			sb.append(first ? "" : ",\n\t\t").append(e);
			first = false;
		}
		sb.append("],\n\t\tsolo: [");

		first = true;
		for (final Event e : solo) {
			sb.append(first ? "" : ",\n\t\t").append(e);
			first = false;
		}
		sb.append("]}");

		return sb.toString();
	}

}
