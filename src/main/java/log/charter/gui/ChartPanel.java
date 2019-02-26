package log.charter.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import log.charter.song.Note;

public class ChartPanel extends JPanel {
	private static final long serialVersionUID = -3439446235287039031L;

	public static final int lane0Y = 100;
	public static final int laneDistY = 50;

	private static final int noteSize = 30;
	private static final int tailSize = 15;

	private static final Color BG_COLOR = new Color(120, 120, 120);
	private static final Color LANE_COLOR = new Color(0, 0, 0);
	private static final Color[] NOTE_COLORS = { new Color(230, 20, 230), new Color(20, 230, 20), new Color(230, 20, 20),
			new Color(230, 230, 20), new Color(20, 20, 230), new Color(230, 115, 20) };
	private static final Color[] TAIL_COLORS = { new Color(200, 20, 200), new Color(20, 200, 20), new Color(200, 20, 20),
			new Color(200, 200, 20), new Color(20, 20, 200), new Color(200, 100, 20) };
	private static final Color MARKER_COLOR = new Color(255, 0, 0);

	private final CharterFrame frame;

	public ChartPanel(final CharterFrame frame) {
		super();
		this.frame = frame;
		final Dimension d = new Dimension(800, 500);
		setMinimumSize(d);
		this.setSize(d);
		setMaximumSize(d);
		setPreferredSize(d);
	}

	private void drawNotes(final Graphics g) {
		if (frame.s != null) {
			for (final Note n : frame.s.g.notes.get(3)) {
				final int x = (int) (((n.pos - frame.t)) * frame.zoom) + frame.offset;
				final int length = (int) (n.length * frame.zoom);

				if (n.notes == 0) {
					g.setColor(TAIL_COLORS[0]);
					g.fillRect(x, lane0Y - (tailSize / 2), length, tailSize);
					g.fillRect(x, (lane0Y + (laneDistY * 4)) - (tailSize / 2), length, tailSize);
					g.setColor(NOTE_COLORS[0]);
					g.fillRect(x - (noteSize / 2), lane0Y - (noteSize / 2), noteSize, (4 * laneDistY) + noteSize);
				} else {
					for (int c = 0; c < 5; c++) {
						if ((n.notes & (1 << c)) > 0) {
							final int y = lane0Y + (c * laneDistY);
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

	@Override
	public void paintComponent(final Graphics g) {
		g.setColor(BG_COLOR);
		g.fillRect(0, 0, 800, 500);

		g.setColor(LANE_COLOR);
		for (int i = 0; i < 5; i++) {
			g.drawLine(0, lane0Y + (laneDistY * i), 800, lane0Y + (laneDistY * i));
		}

		drawNotes(g);

		g.setColor(MARKER_COLOR);
		g.drawLine(frame.offset, lane0Y - 50, frame.offset, lane0Y + (laneDistY * 4) + 50);
	}
}
