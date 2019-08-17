package log.charter.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;

import log.charter.data.ChartData;
import log.charter.data.ChartData.IdOrPos;
import log.charter.data.Config;
import log.charter.gui.handlers.CharterFrameMouseMotionListener;
import log.charter.io.Logger;
import log.charter.song.Event;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Lyric;
import log.charter.song.Note;
import log.charter.song.Tempo;
import log.charter.util.RW;

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
				g.drawRect(positions[i++], positions[i++], positions[i++], positions[i++]);
			}
		}
	}

	private static class FillList {
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

	private static class TextDrawList {
		private String[] strings = new String[16];
		private int[] positions = new int[32];
		private int id = 0;

		public void addString(final String s, final int x, final int y) {
			if (s == null) {
				return;
			}
			if (id >= positions.length) {
				strings = Arrays.copyOf(strings, strings.length * 2);
				positions = Arrays.copyOf(positions, positions.length * 2);
			}
			strings[id / 2] = s;
			positions[id++] = x;
			positions[id++] = y;
		}

		public void draw(final Graphics g, final Color c) {
			g.setColor(c);
			int i = 0;
			while (i < id) {
				g.drawString(strings[i / 2], positions[i++], positions[i++]);
			}
		}
	}

	private static final long serialVersionUID = -3439446235287039031L;

	public static final int noteH = 30;
	public static final int noteW = 15;
	public static final int tailSize = 15;

	public static final int sectionNamesY = 10;
	public static final int lyricLinesY = sectionNamesY + 20;
	public static final int textY = lyricLinesY + 10;
	public static final int tempoMarkerY1 = textY + 15;
	public static final int beatTextY = tempoMarkerY1;
	public static final int beatSizeTextY = beatTextY + 15;
	public static final int spY = beatSizeTextY + 20;
	public static final int tapY = spY + 5;
	public static final int lane0Y = tapY + 35;
	public static final int laneDistY = noteH + 20;
	public static final int tempoMarkerY2 = lane0Y + (4 * laneDistY) + (laneDistY / 2);
	public static final int HEIGHT = tempoMarkerY2;

	private static Map<String, Color> colors = new HashMap<>();

	private static String[] hexes = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

	static {
		// bg colors
		colors.put("BG", new Color(160, 160, 160));
		colors.put("NOTE_BG", new Color(16, 16, 16));
		colors.put("NOTE_ADD_LINE", new Color(0, 255, 0));
		colors.put("LANE", new Color(128, 128, 128));
		colors.put("MAIN_BEAT", new Color(255, 255, 255));
		colors.put("SECONDARY_BEAT", new Color(200, 200, 200));
		colors.put("SECTION_TEXT", new Color(255, 255, 255));
		colors.put("MARKER", new Color(255, 0, 0));
		colors.put("HIGHLIGHT", new Color(255, 0, 0));
		colors.put("SELECT", new Color(0, 255, 255));

		// notes colors
		colors.put("SP_SECTION", new Color(180, 200, 255));
		colors.put("TAP_SECTION", new Color(200, 0, 200));
		colors.put("SOLO_SECTION", new Color(100, 100, 210));
		colors.put("OPEN_NOTE", new Color(230, 20, 230));
		colors.put("OPEN_NOTE_TAIL", new Color(200, 20, 200));
		colors.put("CRAZY", new Color(0, 0, 0));
		colors.put("HOPO", new Color(255, 255, 255));

		colors.put("NOTE_0", new Color(20, 230, 20));
		colors.put("NOTE_1", new Color(230, 20, 20));
		colors.put("NOTE_2", new Color(230, 230, 20));
		colors.put("NOTE_3", new Color(20, 20, 230));
		colors.put("NOTE_4", new Color(230, 115, 20));

		colors.put("NOTE_TAIL_0", new Color(20, 200, 20));
		colors.put("NOTE_TAIL_1", new Color(200, 20, 20));
		colors.put("NOTE_TAIL_2", new Color(200, 200, 20));
		colors.put("NOTE_TAIL_3", new Color(20, 20, 200));
		colors.put("NOTE_TAIL_4", new Color(200, 100, 20));

		colors.put("TAP_NOTE", new Color(155, 20, 155));
		colors.put("TAP_NOTE_TAIL", new Color(155, 20, 155));

		// lyrics colors
		colors.put("LYRIC_LINE", new Color(100, 200, 200));
		colors.put("LYRIC_LINE_TEXT", new Color(0, 0, 128));
		colors.put("LYRIC_TEXT", new Color(255, 255, 255));
		colors.put("LYRIC", new Color(255, 0, 255));
		colors.put("LYRIC_NO_TONE", new Color(128, 128, 0));
		colors.put("LYRIC_WORD_PART", new Color(0, 0, 255));
		colors.put("LYRIC_CONNECTION", new Color(255, 128, 255));

		final Map<String, String> config = RW.readConfig("colors.txt");
		for (final Entry<String, String> configEntry : config.entrySet()) {
			try {
				final String[] rgb = configEntry.getValue().split(" ");
				colors.put(configEntry.getKey(), new Color(Integer.valueOf(rgb[0], 16), Integer.valueOf(rgb[1], 16), Integer.valueOf(rgb[2], 16)));
			} catch (final Exception e) {
				Logger.error("Couldn't load color " + configEntry.getKey() + "=" + configEntry.getValue(), e);
			}
		}

		for (final Entry<String, Color> colorEntry : colors.entrySet()) {
			final Color c = colorEntry.getValue();
			config.put(colorEntry.getKey(), hex2(c.getRed()) + " " + hex2(c.getGreen()) + " " + hex2(c.getBlue()));
		}
		RW.writeConfig("colors.txt", config);
	}

	private static int clamp(final double y) {
		return colorToY(yToLane(y));
	}

	private static int colorToY(final int i) {
		return (lane0Y + (laneDistY * i));
	}

	private static String hex2(final int v) {
		return hexes[(v / 16) % 16] + hexes[v % 16];
	}

	public static boolean isInNotes(final int y) {
		return (y >= (lane0Y - (laneDistY / 2))) && (y <= (lane0Y + ((laneDistY * 9) / 2)));
	}

	public static boolean isInTempos(final int y) {
		return (y >= tempoMarkerY1) && (y < (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2)));
	}

	public static int yToLane(final double y) {
		return (int) (((y - ChartPanel.lane0Y) + (ChartPanel.laneDistY / 2)) / ChartPanel.laneDistY);
	}

	private final ChartData data;

	public ChartPanel(final ChartEventsHandler handler) {
		super();
		data = handler.data;
		addMouseListener(handler);
		addMouseMotionListener(new CharterFrameMouseMotionListener(data));
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
		int beatsPerMeasure = 1;

		g.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
		for (int i = 0; i < (tempos.size() - 1); i++) {
			final Tempo tmp = tempos.get(i);
			final Tempo nextTempo = tempos.get(i + 1);
			if (beatsPerMeasure != tmp.beats) {
				beatInMeasure = 0;
				beatsPerMeasure = tmp.beats;
			}
			lastKBPM = tmp.kbpm;
			final boolean drawing = data.timeToX(nextTempo.pos) >= -20;

			for (int id = tmp.id; id < nextTempo.id; id++) {
				if (drawing) {
					final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
					if (x > getWidth()) {
						return;
					}
					if (x >= -100) {
						g.setColor(beatInMeasure == 0 ? colors.get("MAIN_BEAT") : colors.get("SECONDARY_BEAT"));
						g.drawLine(x, tempoMarkerY1, x, tempoMarkerY2);
						g.drawString("" + id, x + 3, beatTextY + 11);
						if (id == tmp.id) {
							g.drawString("" + tmp.beats, x + 3, beatSizeTextY + 11);
						}
						final String sectionName = data.s.sections.get(id);
						if (sectionName != null) {
							g.setColor(colors.get("SECTION_TEXT"));
							g.drawString("[" + sectionName + "]", x, sectionNamesY + 11);
						}
					}
				}
				beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			}
		}
		final Tempo tmp = tempos.get(tempos.size() - 1);
		lastKBPM = tmp.kbpm;
		beatsPerMeasure = tmp.beats;
		if (beatsPerMeasure != tmp.beats) {
			beatInMeasure = 0;
		}

		int id = tmp.id;
		while (id < 9999) {
			final int x = tempoX(tmp.pos, id, tmp.id, lastKBPM);
			if (x > getWidth()) {
				return;
			}
			if (x >= -100) {
				g.setColor(beatInMeasure == 0 ? colors.get("MAIN_BEAT") : colors.get("SECONDARY_BEAT"));
				g.drawLine(x, tempoMarkerY1, x, tempoMarkerY2);
				g.drawString("" + id, x + 3, beatTextY + 11);
				if (id == tmp.id) {
					g.drawString("" + tmp.beats, x + 3, beatSizeTextY + 11);
				}
				final String sectionName = data.s.sections.get(id);
				if (sectionName != null) {
					g.setColor(colors.get("SECTION_TEXT"));
					g.drawString("[" + sectionName + "]", x, sectionNamesY + 11);
				}
			}
			beatInMeasure = (beatInMeasure + 1) % beatsPerMeasure;
			id++;
		}
	}

	private void drawGuitarNotes(final Graphics g) {
		if (data.s != null) {
			final FillList openNotes = new FillList();
			final FillList openNoteTails = new FillList();
			final FillList[] notes = new FillList[6];
			final FillList[] noteTails = new FillList[6];
			final FillList tapNotes = new FillList();
			final FillList tapNoteTails = new FillList();
			for (int i = 0; i < 5; i++) {
				notes[i] = new FillList();
				noteTails[i] = new FillList();
			}
			final FillList crazy = new FillList();
			final FillList hopos = new FillList();

			for (final Note n : data.currentNotes) {
				final int x = data.timeToX(n.pos);
				final int length = data.timeToXLength(n.getLength());
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
									tapNotes.addPositions(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
									tapNoteTails.addPositions(x, y - (tailSize / 2), length, tailSize);
								} else {
									notes[c].addPositions(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
									noteTails[c].addPositions(x, y - (tailSize / 2), length, tailSize);
									if (n.hopo) {
										hopos.addPositions(x - (noteW / 4), y - (noteH / 4), noteW / 2, noteH / 2);
									}
									if (n.crazy) {
										crazy.addPositions(x - (noteW / 4), y - (noteH / 3) - 1, noteW / 2, ((3 * noteH) / 4)
												+ 1);
									}
								}
							}
						}
					}
				}
			}
			openNoteTails.draw(g, colors.get("OPEN_NOTE_TAIL"));
			openNotes.draw(g, colors.get("OPEN_NOTE"));
			for (int i = 0; i < 5; i++) {
				noteTails[i].draw(g, colors.get("NOTE_TAIL_" + i));
				notes[i].draw(g, colors.get("NOTE_" + i));
			}
			tapNoteTails.draw(g, colors.get("TAP_NOTE_TAIL"));
			tapNotes.draw(g, colors.get("TAP_NOTE"));
			crazy.draw(g, colors.get("CRAZY"));
			hopos.draw(g, colors.get("HOPO"));
		}
	}

	private void drawInstrument(final Graphics g) {
		if (data.vocalsEditing) {
			drawLyrics(g);
		} else if (data.currentInstrument.type == InstrumentType.KEYS) {
			drawKeysNotes(g);
		} else {
			drawGuitarNotes(g);
		}
	}

	private void drawKeysNotes(final Graphics g) {
		if (data.s != null) {
			final FillList[] noteTails = new FillList[5];
			final FillList[] notes = new FillList[5];
			for (int i = 0; i < 5; i++) {
				noteTails[i] = new FillList();
				notes[i] = new FillList();
			}

			for (final Note n : data.currentNotes) {
				final int x = data.timeToX(n.pos);
				final int length = data.timeToXLength(n.getLength());
				if (x > (getWidth() + (noteW / 2))) {
					break;
				}
				if ((x + length) > 0) {
					for (int c = 0; c < 5; c++) {
						if ((n.notes & (1 << c)) > 0) {
							final int y = colorToY(c);
							notes[c].addPositions(x - (noteW / 2), y - (noteH / 2), noteW, noteH);
							noteTails[c].addPositions(x, y - (tailSize / 2), length, tailSize);
						}
					}
				}
			}
			for (int i = 0; i < 5; i++) {
				noteTails[i].draw(g, colors.get("NOTE_TAIL_" + i));
				notes[i].draw(g, colors.get("NOTE_" + i));
			}
		}
	}

	private void drawLanes(final Graphics g) {
		g.setColor(colors.get("LANE"));
		for (int i = 0; i < 5; i++) {
			g.drawLine(0, colorToY(i), getWidth(), colorToY(i));
		}
	}

	private void drawLyrics(final Graphics g) {
		if (data.s != null) {
			final TextDrawList texts = new TextDrawList();
			final FillList notes = new FillList();
			final FillList tonelessNotes = new FillList();
			final FillList connections = new FillList();
			final FillList wordConnections = new FillList();

			final List<Lyric> lyrics = data.s.v.lyrics;
			final int y = colorToY(2) - 4;

			for (int i = 0; i < lyrics.size(); i++) {
				final Lyric l = lyrics.get(i);
				final int x = data.timeToX(l.pos);
				int length = data.timeToXLength(l.getLength());
				if (length < 1) {
					length = 1;
				}
				if (((x > getWidth()) && !l.connected) //
						|| ((i > 0) && (data.timeToX(lyrics.get(i - 1).pos) > getWidth()))) {
					break;
				}
				if ((x + length) > 0) {
					(l.toneless ? tonelessNotes : notes).addPositions(x, y, length, 8);
				}
				if (l.connected && (i > 0)) {
					final Lyric prev = lyrics.get(i - 1);
					final int prevEnd = data.timeToX(prev.pos + prev.getLength());
					connections.addPositions(prevEnd, y, x - prevEnd, 8);
				}
				if ((x + g.getFontMetrics().stringWidth(l.lyric)) > 0) {
					texts.addString(l.lyric + (l.wordPart ? "-" : ""), x, textY + 11);
				}
				if (l.wordPart && (i < (lyrics.size() - 1))) {
					final Lyric next = lyrics.get(i + 1);
					final int nextStart = data.timeToX(next.pos);
					wordConnections.addPositions(x + length, y + 2, nextStart - x - length, 4);
				}
			}
			texts.draw(g, colors.get("LYRIC_TEXT"));
			notes.draw(g, colors.get("LYRIC"));
			tonelessNotes.draw(g, colors.get("LYRIC_NO_TONE"));
			connections.draw(g, colors.get("LYRIC_CONNECTION"));
			wordConnections.draw(g, colors.get("LYRIC_WORD_PART"));
		}
	}

	private void drawSections(final Graphics g) {
		final TextDrawList lyricLines = new TextDrawList();
		final FillList lines = new FillList();
		for (final Event e : data.s.v.lyricLines) {
			final int x = data.timeToX(e.pos);
			final int l = data.timeToXLength(e.getLength());
			if ((x + l) < 0) {
				continue;
			}
			if (x >= getWidth()) {
				break;
			}
			lines.addPositions(x, lyricLinesY, l, 13);

			final List<Lyric> lyricsInLine = new ArrayList<>(20);
			for (final Lyric lyric : data.s.v.lyrics) {
				if (lyric.pos < e.pos) {
					continue;
				}
				if (lyric.pos > (e.pos + e.getLength())) {
					break;
				}
				lyricsInLine.add(lyric);
			}
			lyricLines.addString(Lyric.joinLyrics(lyricsInLine), x, lyricLinesY + 11);
		}
		lines.draw(g, colors.get("LYRIC_LINE"));
		lyricLines.draw(g, colors.get("LYRIC_LINE_TEXT"));

		if (!data.vocalsEditing) {
			final FillList sp = new FillList();
			for (final Event e : data.currentInstrument.sp) {
				final int x = data.timeToX(e.pos);
				final int l = data.timeToXLength(e.getLength());
				if ((x + l) < 0) {
					continue;
				}
				if (x >= getWidth()) {
					break;
				}
				sp.addPositions(x, spY, l, 5);
			}
			sp.draw(g, colors.get("SP_SECTION"));

			final FillList tap = new FillList();
			for (final Event e : data.currentInstrument.tap) {
				final int x = data.timeToX(e.pos);
				final int l = data.timeToXLength(e.getLength());
				if ((x + l) < 0) {
					continue;
				}
				if (x >= getWidth()) {
					break;
				}
				tap.addPositions(x, tapY, l, 5);
			}
			tap.draw(g, colors.get("TAP_SECTION"));

			final FillList solos = new FillList();
			for (final Event e : data.currentInstrument.solo) {
				final int x = data.timeToX(e.pos);
				final int l = data.timeToXLength(e.getLength());
				if ((x + l) < 0) {
					continue;
				}
				if (x >= getWidth()) {
					break;
				}
				solos.addPositions(x, lane0Y - (ChartPanel.laneDistY / 2), l, ChartPanel.laneDistY * 5);
			}
			solos.draw(g, colors.get("SOLO_SECTION"));
		}
	}

	private void drawSelectedNotes(final Graphics g) {
		if (data.vocalsEditing) {
			final DrawList selects = new DrawList();

			for (final int id : data.selectedNotes) {
				final Lyric l = data.s.v.lyrics.get(id);
				final int x = data.timeToX(l.pos);
				final int y = colorToY(2) - 5;
				int length = data.timeToXLength(l.getLength()) + 1;
				if (length < 3) {
					length = 3;
				}
				if (x > (getWidth() + (noteW / 2))) {
					break;
				}
				if ((x + length) > 0) {
					for (int c = 0; c < 5; c++) {
						selects.addPositions(x - 1, y, length, 9);

					}
				}
			}

			selects.draw(g, colors.get("SELECT"));
		} else {
			final DrawList selects = new DrawList();

			for (final int id : data.selectedNotes) {
				final Note n = data.currentNotes.get(id);
				final int x = data.timeToX(n.pos);
				final int length = data.timeToXLength(n.getLength());
				if (x > (getWidth() + (noteW / 2))) {
					break;
				}
				if ((x + length) > 0) {
					if (n.notes == 0) {
						selects.addPositions(x - (noteW / 2) - 1, lane0Y - (noteH / 2) - 1, noteW + 1, (4 * laneDistY)
								+ noteH + 1);
					} else {
						for (int c = 0; c < 5; c++) {
							if ((n.notes & (1 << c)) > 0) {
								selects.addPositions(x - (noteW / 2) - 1, colorToY(c) - (noteH / 2) - 1, noteW + 1, noteH + 1);
							}
						}
					}
				}
			}

			selects.draw(g, colors.get("SELECT"));
		}
	}

	private void drawSpecial(final Graphics g) {
		if (data.vocalsEditing) {
			if (data.isNoteDrag) {
				final IdOrPos idOrPos = data.findClosestVocalIdOrPosForX(data.mx, data.handler.isCtrl());
				final int x = data.timeToX(idOrPos.isId() ? data.s.v.lyrics.get(idOrPos.id).pos : idOrPos.pos);
				int xLength = idOrPos.isId() ? data.timeToXLength(data.s.v.lyrics.get(idOrPos.id).getLength()) - 1 : 10;
				if (xLength < 1) {
					xLength = 1;
				}
				final int y = colorToY(2) - 4;

				g.setColor(colors.get("HIGHLIGHT"));
				g.drawRect(x, y, xLength, 7);
			} else {
				final IdOrPos idOrPos = data.findClosestVocalIdOrPosForX(data.mx);
				final int x = data.timeToX(idOrPos.isId() ? data.s.v.lyrics.get(idOrPos.id).pos : idOrPos.pos);
				int xLength = idOrPos.isId() ? data.timeToXLength(data.s.v.lyrics.get(idOrPos.id).getLength()) - 1 : 10;
				if (xLength < 1) {
					xLength = 1;
				}
				final int y = colorToY(2) - 4;

				g.setColor(colors.get("HIGHLIGHT"));
				g.drawRect(x, y, xLength, 7);
			}
		} else if (ChartPanel.isInNotes(data.my)) {
			final DrawList highlighted = new DrawList();
			if (data.isNoteAdd) {
				int x0, x1;
				int y0, y1;
				if (data.mx < data.mousePressX) {
					x0 = data.mx;
					y0 = data.my;
					x1 = data.mousePressX;
					y1 = data.mousePressY;
				} else {
					x0 = data.mousePressX;
					y0 = data.mousePressY;
					x1 = data.mx;
					y1 = data.my;
				}
				final IdOrPos idOrPos = data.findClosestIdOrPosForX(x1);
				final IdOrPos startIdOrPos = data.findClosestIdOrPosForX(x0);

				final double firstNotePos = startIdOrPos.isId() ? data.currentNotes.get(startIdOrPos.id).pos
						: startIdOrPos.pos;
				final double lastNotePos = idOrPos.isId() ? data.currentNotes.get(idOrPos.id).pos : idOrPos.pos;

				if (firstNotePos != lastNotePos) {
					final double length = lastNotePos - firstNotePos;

					final List<Note> notes = data.findNotesFromTo(startIdOrPos.isId() ? startIdOrPos.id
							: data.findFirstNoteAfterTime(firstNotePos), lastNotePos);

					for (final Note note : notes) {
						final double part = (note.pos - firstNotePos) / length;
						final int y = clamp((y0 * (1 - part)) + (part * y1));
						if ((y >= lane0Y) && (y <= (lane0Y + (laneDistY * 4)))) {
							highlighted.addPositions(data.timeToX(note.pos) - (noteW / 2), y - (noteH / 2), noteW - 1,
									noteH - 1);
						}
					}

					final List<Double> allGridPositions = data.s.tempoMap.getGridPositionsFromTo(data.gridSize, data.xToTime(
							x0), data.xToTime(x1));
					final List<Double> gridPositions = ChartData.removePostionsCloseToNotes(allGridPositions, notes);
					for (final Double pos : gridPositions) {
						final double part = (pos - firstNotePos) / length;
						final double y = (y0 * (1 - part)) + (part * y1);
						highlighted.addPositions(data.timeToX(pos) - (noteW / 2), clamp(y) - (noteH / 2), noteW - 1, noteH
								- 1);
					}
				} else {
					final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
							: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

					if (x >= 0) {
						highlighted.addPositions(x - (noteW / 2), clamp(y0) - (noteH / 2), noteW - 1, noteH - 1);
					}
				}
			} else if (data.isNoteDrag) {
				final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx, data.handler.isCtrl());
				final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
						: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

				if (x >= 0) {
					highlighted.addPositions(x - (noteW / 2), clamp(data.my) - (noteH / 2), noteW - 1, noteH - 1);
				}
			} else {
				final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx);
				final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
						: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

				if (x >= 0) {
					highlighted.addPositions(x - (noteW / 2), clamp(data.my) - (noteH / 2), noteW - 1, noteH - 1);
				}
			}

			highlighted.draw(g, colors.get("HIGHLIGHT"));
		}
	}

	@Override
	public void paintComponent(final Graphics g) {
		data.t = (int) data.nextT;
		g.setColor(colors.get("BG"));
		g.fillRect(0, 0, getWidth(), getHeight());
		if (data.isEmpty) {
			return;
		}

		g.setColor(colors.get("NOTE_BG"));
		g.fillRect(0, lane0Y - (laneDistY / 2), getWidth(), laneDistY * 5);

		drawSections(g);
		drawAudio(g);
		drawLanes(g);
		drawBeats(g);
		drawInstrument(g);
		drawSelectedNotes(g);
		drawSpecial(g);

		g.setColor(colors.get("MARKER"));
		g.drawLine(Config.markerOffset, spY - 5, Config.markerOffset, lane0Y + (laneDistY * 4) + 50);
		if (!data.vocalsEditing && data.isNoteAdd) {
			g.setColor(colors.get("NOTE_ADD_LINE"));
			g.drawLine(data.mousePressX, data.mousePressY, data.mx, data.my);
		}
	}

	private int tempoX(final double lastPos, final int id, final int lastId, final int lastKBPM) {
		return data.timeToX(lastPos + (((id - lastId) * 60000000.0) / lastKBPM));
	}
}
