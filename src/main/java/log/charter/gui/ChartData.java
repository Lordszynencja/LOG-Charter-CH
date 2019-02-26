package log.charter.gui;

import java.util.ArrayList;
import java.util.List;

import log.charter.song.IniData;
import log.charter.song.Instrument;
import log.charter.song.Song;
import log.charter.song.Tempo;
import log.charter.sound.MusicData;

public class ChartData {
	public static double btt(final double t, final int kbpm) {
		return (t * 60000000) / kbpm;
	}

	public static double ttb(final double t, final int kbpm) {
		return (t * kbpm) / 60000000;
	}

	public String path = Config.lastPath;
	public Song s = new Song();
	public IniData ini = new IniData();

	public MusicData music = new MusicData(new byte[0], 44100);
	public Instrument currentInstrument = s.g;
	public int currentDiff = 3;
	public List<Integer> selectedNotes = new ArrayList<>();
	public Integer lastSelectedNote = null;
	public int dragStartX = -1;
	public int dragStartY = -1;
	public int mx = -1;

	public int my = -1;
	public int t = 0;
	public double zoom = 1;
	public int markerOffset = 300;
	public boolean drawAudio = false;

	public boolean changed = false;

	public ChartData() {
		resetZoom();
	}

	public void addZoom(final int change) {
		setZoomLevel(Config.zoomLvl + change);
	}

	public int findBeatId(final int time) {
		if (time <= 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				return (int) (tmp.id + ttb(time - tmp.pos, lastKbpm));
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return (int) (tmp.id + ttb(time - tmp.pos, lastKbpm));
	}

	public double findBeatTime(final int time) {
		if (time <= 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, lastKbpm)), lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return tmp.pos + btt(Math.floor(ttb(time - tmp.pos, lastKbpm)), lastKbpm);
	}

	public double findBeatTimeById(final int id) {
		if (id < 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if (s.tempos.get(i + 1).id > id) {
				return tmp.pos + btt(id - tmp.id, lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		return tmp.pos + btt(id - tmp.id, lastKbpm);
	}

	public double findNextBeatTime(final int time) {
		if (time < 0) {
			return 0;
		}
		int lastKbpm = 120000;
		for (int i = 0; i < (s.tempos.size() - 1); i++) {
			final Tempo tmp = s.tempos.get(i);
			if (tmp.sync) {
				lastKbpm = tmp.kbpm;
			}

			if ((long) s.tempos.get(i + 1).pos > time) {
				final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
				if (s.tempos.get(i + 1).id <= id) {
					return s.tempos.get(i + 1).pos;
				}
				return tmp.pos + btt(id - tmp.id, lastKbpm);
			}
		}
		final Tempo tmp = s.tempos.get(s.tempos.size() - 1);
		if (tmp.sync) {
			lastKbpm = tmp.kbpm;
		}
		final int id = (int) (tmp.id + ttb((time - tmp.pos) + 1, lastKbpm) + 1);
		return tmp.pos + btt(id - tmp.id, lastKbpm);
	}

	public int noteToX(final double pos) {
		return (int) ((pos - t) * zoom) + markerOffset;
	}

	public void resetZoom() {
		zoom = Math.pow(0.99, Config.zoomLvl);
	}

	public void setZoomLevel(final int newZoomLevel) {
		Config.zoomLvl = newZoomLevel;
		resetZoom();
	}
}
