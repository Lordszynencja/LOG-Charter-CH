package log.charter.song;

import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.List;

import log.charter.io.TickMsConverter;

public class TempoMap {
	public static double btt(final double t, final int kbpm) {
		return (t * 60000000.0) / kbpm;
	}

	public static void calcBPM(final Tempo t0, final Tempo t1) {
		final int idDiff = t1.id - t0.id;
		final double timeDiff = t1.pos - t0.pos;
		t0.kbpm = (int) ((idDiff * 60000000.0) / timeDiff);
	}

	public static double ttb(final double t, final int kbpm) {
		return (t * kbpm) / 60000000.0;
	}

	public final List<Tempo> tempos = new ArrayList<>();
	public boolean isMs = false;

	public TempoMap(final List<Tempo> tempos) {
		for (final Tempo tmp : tempos) {
			this.tempos.add(new Tempo(tmp));
		}
		if (this.tempos.isEmpty()) {
			this.tempos.add(new Tempo(0, 0, 120000, true));
		}
		sort();
	}

	public TempoMap(final TempoMap tempoMap) {
		for (final Tempo tmp : tempoMap.tempos) {
			tempos.add(new Tempo(tmp));
		}
		isMs = tempoMap.isMs;
	}

	public void addTempo(final Tempo tmp, final boolean withSorting) {
		tempos.add(tmp);
		if (withSorting) {
			sort();
		}
	}

	public void addTempo(final Tempo tmp, final int id) {
		tempos.add(id, tmp);
	}

	public double closestGridTime(final double time, final int tmpId, final int gridSize) {
		final Tempo tmp = tempos.get(tmpId);
		final long gridPoint = round(ttb(time - tmp.pos, tmp.kbpm) * gridSize);

		if (tmpId < (tempos.size() - 1)) {
			final Tempo nextTmp = tempos.get(tmpId + 1);
			if (gridPoint == ((nextTmp.id - tmp.id) * gridSize)) {
				return nextTmp.pos;
			}
		}
		return tmp.pos + (btt(gridPoint, tmp.kbpm) / gridSize);
	}

	public void convertToMs() {
		if (isMs) {
			return;
		}
		if (tempos.get(0).pos != 0) {
			System.err.println("first beat is not on zero: " + tempos.get(0));
		}

		Tempo prev = tempos.get(0);
		double lastPos = tempos.get(0).pos;
		for (int i = 1; i < tempos.size(); i++) {
			final Tempo t0 = tempos.get(i);
			double newLastPos = lastPos;
			newLastPos = t0.pos;
			t0.pos = prev.pos + (((t0.pos - lastPos) * 60000000.0) / prev.kbpm / TickMsConverter.ticksPerBeat);

			prev = t0;
			lastPos = newLastPos;
		}
		isMs = true;
	}

	/*
	 * public Tempo findOrCreateClosestBeatForTime(final double time) {//TODO if
	 * (s.tempos.isEmpty()) { throw new IllegalArgumentException(); }
	 *
	 * int l = 0; int r = currentNotes.size() - 1;
	 *
	 * while ((r - l) > 1) { final int mid = (l + r) / 2;
	 *
	 * if (currentNotes.get(mid).pos > time) { r = mid; } else { l = mid; } }
	 *
	 * return (abs(currentNotes.get(l).pos - time) > abs(currentNotes.get(r).pos
	 * - time)) ? r : l; }
	 */
	/*
	 * public Tempo findOrCreateTempoCloseTo(final double time) {// TODO binary
	 * // search final double tDiff = xToTimeLength(10); int lastKbpm = 120000;
	 *
	 * Tempo tmp = s.tempos.get(s.tempos.size()-1); if (time > tmp.pos-tDiff ) {
	 * if (time<tmp.pos + tDiff)return tmp; double closestBeatTime = tmp.pos +
	 * btt(Math.floor(ttb(time - tmp.pos, lastKbpm)), lastKbpm); }
	 *
	 * for (int i = 0; i < s.tempos.size()-1; i++) { tmp = s.tempos.get(i); if
	 * (tmp.sync) { lastKbpm = tmp.kbpm; } if ((tmp.pos + tDiff) < time) {
	 * continue; }
	 *
	 *
	 *
	 * if ((tmp.pos - tDiff) > time) {//TODO this tempo is after tempo to make
	 * final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
	 * if (s.tempos.get(i + 1).id <= id) { return s.tempos.get(i + 1).pos; }
	 * return tmp.pos + btt(id - tmp.id, lastKbpm); final Tempo new Tempo = new
	 * Tempo(); s.sections.add(i, newSection); return newSection; } return tmp; }
	 * throw new IllegalArgumentException(); }
	 */

	public void convertToTick() {
		if (!isMs) {
			return;
		}
		if (tempos.get(0).pos != 0) {
			System.err.println("first beat is not on zero: " + tempos.get(0));
		}

		Tempo prev = tempos.get(0);
		for (int i = 1; i < tempos.size(); i++) {
			final Tempo tmp = tempos.get(i);
			tmp.pos = prev.pos + (((tmp.pos - tmp.pos) * prev.kbpm * TickMsConverter.ticksPerBeat) / 60000000.0);

			prev = tmp;
		}
		isMs = false;
	}

	public int findBeatId(final int time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);

			if ((long) tempos.get(i + 1).pos > time) {
				return (int) (tmp.id + ttb(time - tmp.pos, tmp.kbpm));
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		return (int) (tmp.id + ttb(time - tmp.pos, tmp.kbpm));
	}

	public double findBeatTime(final double time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);

			if ((long) tempos.get(i + 1).pos > time) {
				return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, tmp.kbpm)), tmp.kbpm);
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, tmp.kbpm)), tmp.kbpm);
	}

	public double findBeatTimeById(final int id) {// TODO binary search
		if (id < 0) {
			return 0;
		}
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);

			if (tempos.get(i + 1).id > id) {
				return tmp.pos + btt(id - tmp.id, tmp.kbpm);
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		return tmp.pos + btt(id - tmp.id, tmp.kbpm);
	}

	public double findClosestGridPositionForTime(final double time, final boolean useGrid, final int gridSize) {
		if (useGrid) {
			final int tmpId = findTempoId(time);
			final double closestGrid = closestGridTime(time, tmpId, gridSize);
			return closestGrid;
		}
		return time;
	}

	public double findNextBeatTime(final double time) {// TODO binary search
		if (time < 0) {
			return 0;
		}
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);

			if (tempos.get(i + 1).pos > time) {
				final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, tmp.kbpm) + 1);
				if (tempos.get(i + 1).id <= id) {
					return tempos.get(i + 1).pos;
				}
				return tmp.pos + btt(id - tmp.id, tmp.kbpm);
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);

		final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, tmp.kbpm) + 1);
		return tmp.pos + btt(id - tmp.id, tmp.kbpm);
	}

	public double findNextBeatTime(final int time) {// TODO binary search
		if (time < 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);
			lastKbpm = tmp.kbpm;

			if ((long) tempos.get(i + 1).pos > time) {
				final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
				if (tempos.get(i + 1).id <= id) {
					return tempos.get(i + 1).pos;
				}
				return tmp.pos + btt(id - tmp.id, lastKbpm);
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		lastKbpm = tmp.kbpm;
		final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
		return tmp.pos + btt(id - tmp.id, lastKbpm);
	}

	public Object[] findOrCreateClosestTempo(final double time) {// TODO
		final int tmpId = findTempoId(time);
		final Tempo tmp = tempos.get(tmpId);
		final long gridPoint = round(ttb(time - tmp.pos, tmp.kbpm));
		if (gridPoint == 0) {
			if (tmpId == 0) {
				return null;
			}
			return new Object[] { tempos.get(tmpId - 1), tmp, tempos.size() > (tmpId + 1) ? tempos.get(tmpId + 1) : null,
					false };
		}

		final Tempo nextTmp = tempos.size() > (tmpId + 1) ? tempos.get(tmpId + 1) : null;
		if ((nextTmp != null) && (gridPoint == (nextTmp.id - tmp.id))) {
			return new Object[] { tmp, nextTmp, tempos.size() > (tmpId + 2) ? tempos.get(tmpId + 2) : null, false };
		}

		final Tempo newTempo = new Tempo(tmp);
		newTempo.id = (int) (tmp.id + gridPoint);
		newTempo.pos = tmp.pos + btt(gridPoint, tmp.kbpm);

		this.addTempo(newTempo, true);
		return new Object[] { tmp, newTempo, nextTmp, true };
	}

	public Tempo findTempo(final double time) {
		return tempos.get(findTempoId(time));
	}

	public int findTempoId(final double time) {// TODO binary search
		if (time <= 0) {
			return 0;
		}

		int result = 0;
		for (int i = 0; i < tempos.size(); i++) {
			final Tempo tmp = tempos.get(i);

			if (tmp.pos > time) {
				return result;
			}
			result = i;
		}

		return result;
	}

	public List<Double> getGridPositionsFromTo(final int gridSize, final double start, final double end) {// TODO
		final List<Double> grids = new ArrayList<>();
		return grids;
	}

	public Tempo getTempoById(final int id) {
		return tempos.get(id);
	}

	public void join() {
		final List<Tempo> newTempos = new ArrayList<>(tempos.size());
		int i;
		for (i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);
			final Tempo nextTmp = tempos.get(i + 1);
			if (tmp.pos == nextTmp.pos) {
				tmp.kbpm = tmp.kbpm > 0 ? tmp.kbpm : nextTmp.kbpm;
				tmp.beats = tmp.beats > 0 ? tmp.beats : nextTmp.beats;
				newTempos.add(tmp);
				i++;
			} else {
				newTempos.add(tmp);
			}
		}
		if (i == (tempos.size() - 1)) {
			newTempos.add(tempos.get(tempos.size() - 1));
		}

		int lastKbpm = 120000;
		int lastBeats = 4;
		for (i = 0; i < newTempos.size(); i++) {
			final Tempo tmp = newTempos.get(i);
			if (tmp.kbpm <= 0) {
				tmp.kbpm = lastKbpm;
			}
			if (tmp.beats <= 0) {
				tmp.beats = lastBeats;
			}

			lastKbpm = tmp.kbpm;
			lastBeats = tmp.beats;
		}

		tempos.clear();
		tempos.addAll(newTempos);
	}

	public void sort() {
		tempos.sort(null);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder("TempoMap{tempos:[");
		boolean first = true;
		for (final Tempo t : tempos) {
			b.append(first ? "" : ",\n\t").append(t);
			first = false;
		}
		b.append("]}");

		return b.toString();
	}
}
