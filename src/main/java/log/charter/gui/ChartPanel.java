package log.charter.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JPanel;

import log.charter.song.Note;
import log.charter.song.Tempo;

public class ChartPanel extends JPanel {
	private static final long serialVersionUID = -3439446235287039031L;

	public static final int lane0Y = 100;
	public static final int laneDistY = 50;

	private static final int noteSize = 30;
	private static final int tailSize = 15;

	private static final Color BG_COLOR = new Color(120, 120, 120);
	private static final Color LANE_COLOR = new Color(0, 0, 0);
	private static final Color MAIN_BEAT_COLOR = new Color(255, 255, 255);
	private static final Color SECONDARY_BEAT_COLOR = new Color(200, 200, 200);
	private static final Color[] NOTE_COLORS = { new Color(230, 20, 230), new Color(20, 230, 20), new Color(230, 20, 20),
			new Color(230, 230, 20), new Color(20, 20, 230), new Color(230, 115, 20) };
	private static final Color[] TAIL_COLORS = { new Color(200, 20, 200), new Color(20, 200, 20), new Color(200, 20, 20),
			new Color(200, 200, 20), new Color(20, 20, 200), new Color(200, 100, 20) };
	private static final Color MARKER_COLOR = new Color(255, 0, 0);

	private static int noteToY(final int i) {
		return (lane0Y + (laneDistY * i));
	}

	private static int yToNote(final int y) {
		return ((y - lane0Y) + (laneDistY / 2)) / laneDistY;
	}

	private final ChartData data;

	public ChartPanel(final ChartEventsHandler handler) {
		super();
		data = handler.data;
		addMouseListener(handler);
		addMouseMotionListener(handler);
	}

	private void drawAudio(final Graphics g) {
		// TODO
	}

	private void drawBeats(final Graphics g) {
		final List<Tempo> tempos = data.s.tempos;
		int lastKBPM = 120000;
		int beatInMeasure = 0;
		int beatsPerMeasure = 9999;

		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);
			final Tempo nextTempo = tempos.get(i + 1);
			if (tmp.sync) {
				lastKBPM = tmp.kbpm;
			} else {
				beatsPerMeasure = tmp.kbpm;
				beatInMeasure = 0;
			}

			for (int id = tmp.id; id < nextTempo.id; id++) {
				final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
				if (x > getWidth()) {
					return;
				}
				if (x >= 0) {
					g.setColor(beatInMeasure == 0 ? MAIN_BEAT_COLOR : SECONDARY_BEAT_COLOR);
					g.drawLine(x, lane0Y - 50, x, lane0Y + (laneDistY * 4) + 50);
				}
				beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		if (tmp.sync) {
			lastKBPM = tmp.kbpm;
		} else {
			beatsPerMeasure = tmp.kbpm;
			beatInMeasure = 0;
		}

		int id = tmp.id;
		while (id < 9999) {
			final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
			if (x > getWidth()) {
				return;
			}
			if (x >= 0) {
				g.setColor(beatInMeasure == 0 ? MAIN_BEAT_COLOR : SECONDARY_BEAT_COLOR);
				g.drawLine(x, lane0Y - 50, x, lane0Y + (laneDistY * 4) + 50);
			}
			beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			id++;
		}
	}

	private void drawNotes(final Graphics g) {
		if (data.s != null) {
			for (final Note n : data.s.g.notes.get(3)) {
				final int x = data.noteToX(n.pos);
				final int length = (int) (n.length * data.zoom);
				if (x > (getWidth() + (noteSize / 2))) {
					break;
				}
				if (((x + length) > 0)) {
					if (n.notes == 0) {
						g.setColor(TAIL_COLORS[0]);
						g.fillRect(x, noteToY(0) - (tailSize / 2), length, tailSize);
						g.fillRect(x, noteToY(4) - (tailSize / 2), length, tailSize);
						g.setColor(NOTE_COLORS[0]);
						g.fillRect(x - (noteSize / 2), lane0Y - (noteSize / 2), noteSize, (4 * laneDistY) + noteSize);
					} else {
						for (int c = 0; c < 5; c++) {
							if ((n.notes & (1 << c)) > 0) {
								final int y = noteToY(c);
								g.setColor(TAIL_COLORS[c + 1]);
								g.fillRect(x, y - (tailSize / 2), length, tailSize);
								g.setColor(NOTE_COLORS[c + 1]);
								g.fillRect(x - (noteSize / 2), y - (noteSize / 2), noteSize, noteSize);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void paintComponent(final Graphics g) {
		g.setColor(BG_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (data.drawAudio) {
			drawAudio(g);
		}

		g.setColor(LANE_COLOR);
		for (int i = 0; i < 5; i++) {
			g.drawLine(0, noteToY(i), getWidth(), noteToY(i));
		}

		drawBeats(g);
		drawNotes(g);

		g.setColor(MARKER_COLOR);
		g.drawLine(data.markerOffset, lane0Y - 50, data.markerOffset, lane0Y + (laneDistY * 4) + 50);
	}

	@SuppressWarnings("unused")
	private int stickX(final int x) {
		return data.noteToX(xToNote(x));
	}

	@SuppressWarnings("unused")
	private int stickY(final int y) {
		return noteToY(yToNote(y));
	}

	private int tempoX(final double lastPos, final int id, final int lastId, final int lastKBPM) {
		return data.noteToX(lastPos + (((id - lastId) * 60000000) / lastKBPM));
	}

	private int xToNote(final int x) {
		return 0;
		// return (((x + data.markerOffset + data.t) + (td / 2)) - t0) / td;
	}
}
