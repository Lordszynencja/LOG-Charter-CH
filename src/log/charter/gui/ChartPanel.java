package log.charter.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import log.charter.gui.ChartData.IdOrPos;
import log.charter.song.Event;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Note;
import log.charter.song.Section;
import log.charter.song.Tempo;

public class ChartPanel extends JPanel {
	private static class DrawList {
		private int[] positions = new int[1024];
		private int id = 0;

		public void addPositions(final int x, final int y, final int w, final int h) {
			if (id >= positions.length) {
				positions = Arrays.copyOf(positions, positions.length * 2);
			}
			positions[id++] = x;
			positions[id++] = y;
			positions[id++] = w;
			positions[id++] = h;
		}

		public void draw(final Graphics g, final Color c) {
			g.setColor(c);
			int i = 0;
			while (i < id) {
				g.fillRect(positions[i++], positions[i++], positions[i++], positions[i++]);
			}
		}
	}

	private static final long serialVersionUID = -3439446235287039031L;

	public static final int sectionNamesY = 10;

	public static final int spY = 30;
	public static final int tapY = 35;
	public static final int lane0Y = 100;

	public static final int laneDistY = 50;
	public static final int tempoMarkerY1 = 30;

	public static final int tempoMarkerY2 = lane0Y + (4 * laneDistY) + (laneDistY / 2);
	public static final int noteH = 30;

	public static final int noteW = 15;
	public static final int tailSize = 15;
	private static final Color BG_COLOR = new Color(160, 160, 160);

	private static final Color SP_COLOR = new Color(180, 200, 255);
	private static final Color TAP_COLOR = new Color(200, 0, 200);
	private static final Color SOLO_COLOR = new Color(50, 50, 180);
	private static final Color LANE_COLOR = new Color(0, 0, 0);
	private static final Color MAIN_BEAT_COLOR = new Color(255, 255, 255);
	private static final Color SECONDARY_BEAT_COLOR = new Color(200, 200, 200);
	private static final Color MARKER_COLOR = new Color(255, 0, 0);
	private static final Color OPEN_NOTE_COLOR = new Color(230, 20, 230);

	private static final Color OPEN_NOTE_TAIL_COLOR = new Color(200, 20, 200);
	private static final Color[] NOTE_COLORS = { new Color(20, 230, 20), new Color(230, 20, 20),
			new Color(230, 230, 20), new Color(20, 20, 230), new Color(230, 115, 20), new Color(155, 20, 155) };
	private static final Color[] NOTE_TAIL_COLORS = { new Color(20, 200, 20), new Color(200, 20, 20),
			new Color(200, 200, 20), new Color(20, 20, 200), new Color(200, 100, 20), new Color(155, 20, 155) };

	private static int colorToY(final int i) {
		return (lane0Y + (laneDistY * i));
	}

	public static int yToLane(final int y) {
		return (((y - ChartPanel.lane0Y) + (ChartPanel.laneDistY / 2)) / ChartPanel.laneDistY);
	}

	private final ChartData data;

	public ChartPanel(final ChartEventsHandler handler) {
		super();
		data = handler.data;
		addMouseListener(handler);
		addMouseMotionListener(handler);
	}

	private void drawAudio(final Graphics g) {
		if (data.drawAudio) {
			final int zero = (int) ((data.xToTime(0) * data.music.outFormat.getFrameRate()) / 1000);
			int start = zero;
			int end = (int) ((data.xToTime(getWidth()) * data.music.outFormat.getFrameRate()) / 1000);
			final double multiplier = ((double) getWidth()) / (end - start);
			if (start < 0) {
				start = 0;
			}
			final int[] musicValues = data.music.data[0];
			if (end > musicValues.length) {
				end = musicValues.length;
			}
			final int midY = colorToY(2);

			int step = 1;
			double xStep = multiplier;
			while (xStep < 0.1) {
				step++;
				xStep += multiplier;
			}
			start -= start % step;
			double x0 = 0;
			double x1 = -xStep + ((start - zero) * multiplier);
			int y0 = 0;
			int y1 = 0;
			for (int i = start; i < end; i += step) {
				x0 = x1;
				x1 += xStep;
				y0 = y1;
				y1 = musicValues[i] / 320;
				g.drawLine((int) x0, midY + y0, (int) x1, midY + y1);
			}
		}
	}

	private void drawBeats(final Graphics g) {
		final List<Tempo> tempos = data.s.tempoMap.tempos;
		int lastKBPM = 120000;
		int beatInMeasure = 0;
		int beatsPerMeasure = 9999;

		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);
			final Tempo nextTempo = tempos.get(i + 1);
			if (beatsPerMeasure != tmp.beats) {
				beatInMeasure = 0;
			}
			lastKBPM = tmp.kbpm;
			beatsPerMeasure = tmp.beats;

			for (int id = tmp.id; id < nextTempo.id; id++) {
				final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
				if (x > getWidth()) {
					return;
				}
				if (x >= 0) {
					g.setColor(beatInMeasure == 0 ? MAIN_BEAT_COLOR : SECONDARY_BEAT_COLOR);
					g.drawLine(x, tempoMarkerY1, x, tempoMarkerY2);
				}
				beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		lastKBPM = tmp.kbpm;
		beatsPerMeasure = tmp.beats;
		beatInMeasure = 0;

		int id = tmp.id;
		while (id < 9999) {
			final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
			if (x > getWidth()) {
				return;
			}
			if (x >= 0) {
				g.setColor(beatInMeasure == 0 ? MAIN_BEAT_COLOR : SECONDARY_BEAT_COLOR);
				g.drawLine(x, tempoMarkerY1, x, tempoMarkerY2);
			}
			beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			id++;
		}
	}

	private void drawGuitarNotes(final Graphics g) {
		if (data.s != null) {
			final DrawList openNoteTails = new DrawList();
			final DrawList openNotes = new DrawList();
			final DrawList[] noteTails = new DrawList[6];
			final DrawList[] notes = new DrawList[6];
			for (int i = 0; i < 6; i++) {
				noteTails[i] = new DrawList();
				notes[i] = new DrawList();
			}
			final DrawList hopos = new DrawList();

			for (final Note n : data.currentInstrument.notes.get(data.currentDiff)) {
				final int x = data.timeToX(n.pos);
				final int length = (int) (n.length * data.zoom);
				if (x > (getWidth() + (noteW / 2))) {
					break;
				}
				if ((x + length) > 0) {
					if (n.notes == 0) {
						openNoteTails.addPositions(x, colorToY(0) - (tailSize / 2), length, tailSize);
						openNoteTails.addPositions(x, colorToY(4) - (tailSize / 2), length, tailSize);
						openNotes.addPositions(x - (noteW / 2), lane0Y - (noteH / 2), noteW, (4 * laneDistY)
								+ noteH);
					} else {
						for (int c = 0; c < 5; c++) {
							if ((n.notes & (1 << c)) > 0) {
								final int y = colorToY(c);
								if (n.tap) {
									notes[5].addPositions(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
									noteTails[5].addPositions(x, y - (tailSize / 2), length, tailSize);
								} else {
									notes[c].addPositions(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
									noteTails[c].addPositions(x, y - (tailSize / 2), length, tailSize);
									if (n.hopo) {
										hopos.addPositions(x - (noteW / 4), y - (noteH / 4), noteW / 2, noteH / 2);
									}
								}
							}
						}
					}
				}
			}
			openNoteTails.draw(g, OPEN_NOTE_TAIL_COLOR);
			openNotes.draw(g, OPEN_NOTE_COLOR);
			for (int i = 0; i < 6; i++) {
				noteTails[i].draw(g, NOTE_TAIL_COLORS[i]);
				notes[i].draw(g, NOTE_COLORS[i]);
			}

			hopos.draw(g, Color.WHITE);
		}
	}

	private void drawInstrument(final Graphics g) {
		if (data.currentInstrument.type == InstrumentType.KEYS) {
			// TODO draw keys
		} else if (data.currentInstrument.type == InstrumentType.VOCALS) {
			// TODO draw vocals
		} else {
			drawGuitarNotes(g);
		}
	}

	private void drawLanes(final Graphics g) {
		g.setColor(LANE_COLOR);
		for (int i = 0; i < 5; i++) {
			g.drawLine(0, colorToY(i), getWidth(), colorToY(i));
		}
	}

	private void drawSections(final Graphics g) {
		g.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
		g.setColor(Color.WHITE);
		for (final Section s : data.s.sections) {
			final int x = data.timeToX(s.pos);
			if (x < -g.getFontMetrics().stringWidth(s.name)) {
				continue;
			}
			if (x >= getWidth()) {
				break;
			}
			g.drawString(s.name, x, sectionNamesY + 10);
		}

		final DrawList sp = new DrawList();
		for (final Event e : data.currentInstrument.sp) {
			final int x = data.timeToX(e.pos);
			final int l = data.timeToXLength(e.length);
			if ((x + l) < 0) {
				continue;
			}
			if (x >= getWidth()) {
				break;
			}
			sp.addPositions(x, spY, l, 5);
		}
		sp.draw(g, SP_COLOR);

		final DrawList tap = new DrawList();
		for (final Event e : data.currentInstrument.tap) {
			final int x = data.timeToX(e.pos);
			final int l = data.timeToXLength(e.length);
			if ((x + l) < 0) {
				continue;
			}
			if (x >= getWidth()) {
				break;
			}
			tap.addPositions(x, tapY, l, 5);
		}
		tap.draw(g, TAP_COLOR);

		final DrawList solos = new DrawList();
		for (final Event e : data.currentInstrument.solo) {
			final int x = data.timeToX(e.pos);
			final int l = data.timeToXLength(e.length);
			if ((x + l) < 0) {
				continue;
			}
			if (x >= getWidth()) {
				break;
			}
			solos.addPositions(x, lane0Y - (ChartPanel.laneDistY / 2), l, ChartPanel.laneDistY * 5);
		}
		solos.draw(g, SOLO_COLOR);
	}

	private void drawSpecial(final Graphics g) {
		if (data.my < (lane0Y - (laneDistY / 2))) {
			return;
		} else if (data.my < (lane0Y + ((laneDistY * 9) / 2))) {
			final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx);
			final int y = colorToY(yToLane(data.my));
			final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
					: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

			if (x >= 0) {
				g.setColor(Color.RED);
				g.drawRect(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
			}
		} else {
			return;
		}
	}

	@Override
	public void paintComponent(final Graphics g) {
		data.t = (int) data.nextT;
		g.setColor(BG_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (data.isEmpty) {
			return;
		}
		drawSections(g);
		drawAudio(g);
		drawLanes(g);
		drawBeats(g);
		drawInstrument(g);
		drawSpecial(g);

		g.setColor(MARKER_COLOR);
		g.drawLine(data.markerOffset, lane0Y - 50, data.markerOffset, lane0Y + (laneDistY * 4) + 50);
	}

	private int tempoX(final double lastPos, final int id, final int lastId, final int lastKBPM) {
		return data.timeToX(lastPos + (((id - lastId) * 60000000.0) / lastKBPM));
	}
}
