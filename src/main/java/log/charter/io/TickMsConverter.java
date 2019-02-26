package log.charter.io;

import java.util.List;

import log.charter.song.Event;
import log.charter.song.Instrument;
import log.charter.song.Position;
import log.charter.song.Song;
import log.charter.song.Tempo;

public class TickMsConverter {
	private static interface Converter {
		double convert(final Tempo tmp, Tempo oldTmp, final double t);
	}

	public static final int ticksPerBeat = 768;

	private static final Converter toMsConverter = (tmp, oldTmp, t) -> {
		return tmp.pos + (((t - oldTmp.pos) * 60000000.0) / tmp.kbpm / ticksPerBeat);
	};

	private static final Converter toTickConverter = (tmp, oldTmp, t) -> {
		return tmp.pos + (((t - oldTmp.pos) * tmp.kbpm * ticksPerBeat) / 60000000.0);
	};

	private static double[][] convert(final double[][] data, final List<Tempo> tempos, final List<Tempo> oldTempos,
			final Converter c) {
		final int n = data.length;
		final double[][] conv = new double[n][];
		final int[] ids = new int[n];
		for (int i = 0; i < n; i++) {
			conv[i] = new double[data[i].length];
			ids[i] = 0;
		}

		int tmpId = 0;
		int nextTmpId = findNextSyncId(oldTempos, 1);
		while (nextTmpId < tempos.size()) {
			final Tempo tmp = tempos.get(tmpId);
			final Tempo oldTmp = oldTempos.get(tmpId);
			final Tempo oldNextTmp = oldTempos.get(nextTmpId);

			if (tmp.sync) {
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < data[i].length; j++) {
						final double t = data[i][j];
						if ((t >= oldTmp.pos) && (t < oldNextTmp.pos)) {
							conv[i][j] = c.convert(tmp, oldTmp, t);
						}
					}
				}
			}
			tmpId = nextTmpId;
			nextTmpId = findNextSyncId(oldTempos, nextTmpId + 1);
		}

		final Tempo tmp = tempos.get(tmpId);
		final Tempo oldTmp = oldTempos.get(tmpId);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < data[i].length; j++) {
				final double t = data[i][j];
				if (t >= oldTmp.pos) {
					conv[i][j] = c.convert(tmp, oldTmp, t);
				}
			}
		}

		return conv;
	}

	private static Song convert(final Song copy, final List<Tempo> oldTempos, final Converter c) {
		final double[][] data = new double[22][];
		data[0] = positionListToArray(copy.sections);

		Instrument[] instruments = copy.instruments();
		for (int i = 0; i < 3; i++) {
			data[1 + (i * 7)] = eventListToArray(instruments[i].notes.get(0));
			data[2 + (i * 7)] = eventListToArray(instruments[i].notes.get(1));
			data[3 + (i * 7)] = eventListToArray(instruments[i].notes.get(2));
			data[4 + (i * 7)] = eventListToArray(instruments[i].notes.get(3));
			data[5 + (i * 7)] = eventListToArray(instruments[i].sp);
			data[6 + (i * 7)] = eventListToArray(instruments[i].tap);
			data[7 + (i * 7)] = eventListToArray(instruments[i].solo);
		}

		final double[][] convData = convert(data, copy.tempos, oldTempos, c);

		replacePositionsTime(copy.sections, convData[0]);

		instruments = copy.instruments();
		for (int i = 0; i < 3; i++) {
			replaceEventsTime(instruments[i].notes.get(0), convData[1 + (i * 7)]);
			replaceEventsTime(instruments[i].notes.get(1), convData[2 + (i * 7)]);
			replaceEventsTime(instruments[i].notes.get(2), convData[3 + (i * 7)]);
			replaceEventsTime(instruments[i].notes.get(3), convData[4 + (i * 7)]);
			replaceEventsTime(instruments[i].sp, convData[5 + (i * 7)]);
			replaceEventsTime(instruments[i].tap, convData[6 + (i * 7)]);
			replaceEventsTime(instruments[i].solo, convData[7 + (i * 7)]);
		}

		return copy;
	}

	private static void convertTemposToMs(final List<Tempo> tempos) {
		if (tempos.get(0).pos != 0) {
			System.err.println("first beat is not on zero: " + tempos.get(0));
		}

		Tempo prev = tempos.get(0);
		double lastPos = tempos.get(0).pos;
		for (int i = 1; i < tempos.size(); i++) {
			final Tempo t0 = tempos.get(i);
			double newLastPos = lastPos;
			if (t0.sync) {
				newLastPos = t0.pos;
			}
			t0.pos = prev.pos + (((t0.pos - lastPos) * 60000000.0) / prev.kbpm / ticksPerBeat);
			if (t0.sync) {
				prev = t0;
			}
			lastPos = newLastPos;
		}
	}

	private static void convertTemposToTick(final List<Tempo> tempos) {
		if (tempos.get(0).pos != 0) {
			System.err.println("first beat is not on zero: " + tempos.get(0));
		}

		Tempo prev = tempos.get(0);
		double lastPos = tempos.get(0).pos;
		for (int i = 1; i < tempos.size(); i++) {
			final Tempo t0 = tempos.get(i);
			double newLastPos = lastPos;
			if (t0.sync) {
				newLastPos = t0.pos;
			}
			t0.pos = prev.pos + (((t0.pos - lastPos) * prev.kbpm * ticksPerBeat) / 60000000.0);
			if (t0.sync) {
				prev = t0;
			}
			lastPos = newLastPos;
		}
	}

	public static Song convertToMs(final Song s) {
		final Song copy = new Song(s);
		convertTemposToMs(copy.tempos);
		return convert(copy, s.tempos, toMsConverter);
	}

	public static Song convertToTick(final Song s) {
		final Song copy = new Song(s);
		convertTemposToTick(copy.tempos);
		return convert(copy, s.tempos, toTickConverter);
	}

	private static double[] eventListToArray(final List<? extends Event> events) {
		final int n = events.size();
		final double[] data = new double[n * 2];

		for (int i = 0; i < n; i++) {
			final Event e = events.get(i);
			data[2 * i] = e.pos;
			data[(2 * i) + 1] = e.pos + e.length;
		}

		return data;
	}

	private static int findNextSyncId(final List<Tempo> tempos, final int start) {
		for (int i = start; i < tempos.size(); i++) {
			if (tempos.get(i).sync) {
				return i;
			}
		}
		return tempos.size();
	}

	private static double[] positionListToArray(final List<? extends Position> positions) {
		final int n = positions.size();
		final double[] data = new double[n];

		for (int i = 0; i < n; i++) {
			data[i] = positions.get(i).pos;
		}

		return data;
	}

	private static void replaceEventsTime(final List<? extends Event> events, final double[] data) {
		for (int i = 0; i < (data.length / 2); i++) {
			final Event e = events.get(i);
			e.pos = data[2 * i];
			e.length = data[(2 * i) + 1] - e.pos;
		}
	}

	private static void replacePositionsTime(final List<? extends Position> events, final double[] data) {
		for (int i = 0; i < data.length; i++) {
			events.get(i).pos = data[i];
		}
	}
}
