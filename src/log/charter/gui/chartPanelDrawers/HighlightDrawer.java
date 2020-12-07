package log.charter.gui.chartPanelDrawers;

import static log.charter.gui.ChartPanel.clamp;
import static log.charter.gui.ChartPanel.getLaneY;
import static log.charter.gui.ChartPanel.isInLanes;
import static log.charter.gui.ChartPanel.noteH5;
import static log.charter.gui.ChartPanel.noteH6;
import static log.charter.gui.ChartPanel.noteW;

import java.awt.Graphics;
import java.util.List;

import log.charter.data.ChartData;
import log.charter.data.ChartData.IdOrPos;
import log.charter.gui.ChartPanel;
import log.charter.gui.chartPanelDrawers.lists.DrawList;
import log.charter.song.Note;

public class HighlightDrawer implements Drawer {
	private void drawGuitarHighlightNoteAdd(final Graphics g, final ChartData data) {
		final DrawList highlighted = new DrawList();
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

		final double firstNotePos = startIdOrPos.isId() ? data.currentNotes.get(startIdOrPos.id).pos : startIdOrPos.pos;
		final double lastNotePos = idOrPos.isId() ? data.currentNotes.get(idOrPos.id).pos : idOrPos.pos;

		if (firstNotePos != lastNotePos) {
			final double length = lastNotePos - firstNotePos;

			final List<Note> notes = data.findNotesFromTo(
					startIdOrPos.isId() ? startIdOrPos.id : data.findFirstNoteAfterTime(firstNotePos), lastNotePos);

			for (final Note note : notes) {
				final double part = (note.pos - firstNotePos) / length;
				final int y = clamp((y0 * (1 - part)) + (part * y1), 6);
				if (isInLanes(y)) {
					highlighted.addPositions(data.timeToX(note.pos) - (noteW / 2), y - (noteH6 / 2), noteW - 1,
							noteH6 - 1);
				}
			}

			final List<Double> allGridPositions = data.s.tempoMap.getGridPositionsFromTo(data.gridSize,
					data.xToTime(x0), data.xToTime(x1));
			final List<Double> gridPositions = ChartData.removePostionsCloseToNotes(allGridPositions, notes);
			for (final Double pos : gridPositions) {
				final double part = (pos - firstNotePos) / length;
				final double y = (y0 * (1 - part)) + (part * y1);
				highlighted.addPositions(data.timeToX(pos) - (noteW / 2), clamp(y, 6) - (noteH6 / 2), noteW - 1,
						noteH6 - 1);
			}
		} else {
			final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
					: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

			if (x >= 0) {
				highlighted.addPositions(x - (noteW / 2), clamp(y0, 6) - (noteH6 / 2), noteW - 1, noteH6 - 1);
			}
		}

		highlighted.draw(g, ChartPanel.colors.get("HIGHLIGHT"));
	}

	private void drawGuitarHighlightNoteDrag(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx, data.handler.isCtrl());
		final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
				: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

		g.drawRect(x - (noteW / 2), clamp(data.my, 6) - (noteH6 / 2), noteW - 1, noteH6 - 1);
	}

	private void drawGuitarDefaultHighlight(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx);
		final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
				: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

		g.drawRect(x - (noteW / 2), clamp(data.my, 6) - (noteH6 / 2), noteW - 1, noteH6 - 1);
	}

	private void drawGuitarHighlight(final Graphics g, final ChartData data) {
		if (isInLanes(data.my)) {
			if (data.isNoteAdd) {
				drawGuitarHighlightNoteAdd(g, data);
			} else if (data.isNoteDrag) {
				drawGuitarHighlightNoteDrag(g, data);
			} else {
				drawGuitarDefaultHighlight(g, data);
			}
		}
	}

	private void drawDrumsKeysHighlightNoteAdd(final Graphics g, final ChartData data) {
		final DrawList highlighted = new DrawList();
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

		final double firstNotePos = startIdOrPos.isId() ? data.currentNotes.get(startIdOrPos.id).pos : startIdOrPos.pos;
		final double lastNotePos = idOrPos.isId() ? data.currentNotes.get(idOrPos.id).pos : idOrPos.pos;

		if (firstNotePos != lastNotePos) {
			final double length = lastNotePos - firstNotePos;

			final List<Note> notes = data.findNotesFromTo(
					startIdOrPos.isId() ? startIdOrPos.id : data.findFirstNoteAfterTime(firstNotePos), lastNotePos);

			for (final Note note : notes) {
				final double part = (note.pos - firstNotePos) / length;
				final int y = clamp((y0 * (1 - part)) + (part * y1), 5);
				if (isInLanes(y)) {
					highlighted.addPositions(data.timeToX(note.pos) - (noteW / 2), y - (noteH5 / 2), noteW - 1,
							noteH5 - 1);
				}
			}

			final List<Double> allGridPositions = data.s.tempoMap.getGridPositionsFromTo(data.gridSize,
					data.xToTime(x0), data.xToTime(x1));
			final List<Double> gridPositions = ChartData.removePostionsCloseToNotes(allGridPositions, notes);
			for (final Double pos : gridPositions) {
				final double part = (pos - firstNotePos) / length;
				final double y = (y0 * (1 - part)) + (part * y1);
				highlighted.addPositions(data.timeToX(pos) - (noteW / 2), clamp(y, 5) - (noteH5 / 2), noteW - 1,
						noteH5 - 1);
			}
		} else {
			final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
					: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

			if (x >= 0) {
				highlighted.addPositions(x - (noteW / 2), clamp(y0, 5) - (noteH5 / 2), noteW - 1, noteH5 - 1);
			}
		}

		highlighted.draw(g, ChartPanel.colors.get("HIGHLIGHT"));
	}

	private void drawDrumsKeysHighlightNoteDrag(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx, data.handler.isCtrl());
		final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
				: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

		g.drawRect(x - (noteW / 2), clamp(data.my, 5) - (noteH5 / 2), noteW - 1, noteH5 - 1);
	}

	private void drawDrumsKeysDefaultHighlight(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestIdOrPosForX(data.mx);
		final int x = idOrPos.isId() ? data.timeToX(data.currentNotes.get(idOrPos.id).pos)//
				: idOrPos.isPos() ? data.timeToX(idOrPos.pos) : -1;

		g.drawRect(x - (noteW / 2), clamp(data.my, 5) - (noteH5 / 2), noteW - 1, noteH5 - 1);
	}

	private void drawDrumsKeysHighlight(final Graphics g, final ChartData data) {
		if (isInLanes(data.my)) {
			if (data.isNoteAdd) {
				drawDrumsKeysHighlightNoteAdd(g, data);
			} else if (data.isNoteDrag) {
				drawDrumsKeysHighlightNoteDrag(g, data);
			} else {
				drawDrumsKeysDefaultHighlight(g, data);
			}
		}
	}

	private void drawVocalsHiglightNoteDrag(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestVocalIdOrPosForX(data.mx, data.handler.isCtrl());
		final int x = data.timeToX(idOrPos.isId() ? data.s.v.lyrics.get(idOrPos.id).pos : idOrPos.pos);
		int xLength = idOrPos.isId() ? data.timeToXLength(data.s.v.lyrics.get(idOrPos.id).getLength()) - 1 : 10;
		if (xLength < 1) {
			xLength = 1;
		}

		final int y = getLaneY(0, 1) - 3;
		g.drawRect(x, y, xLength, 7);
	}

	private void drawVocalsHiglightDefault(final Graphics g, final ChartData data) {
		final IdOrPos idOrPos = data.findClosestVocalIdOrPosForX(data.mx);
		final int x = data.timeToX(idOrPos.isId() ? data.s.v.lyrics.get(idOrPos.id).pos : idOrPos.pos);
		int xLength = idOrPos.isId() ? data.timeToXLength(data.s.v.lyrics.get(idOrPos.id).getLength()) - 1 : 10;
		if (xLength < 1) {
			xLength = 1;
		}

		final int y = getLaneY(0, 1) - 3;
		g.drawRect(x, y, xLength, 7);
	}

	private void drawVocalsHiglight(final Graphics g, final ChartData data) {
		if (data.isNoteDrag) {
			drawVocalsHiglightNoteDrag(g, data);
		} else {
			drawVocalsHiglightDefault(g, data);
		}
	}

	@Override
	public void draw(final Graphics g, final ChartPanel panel, final ChartData data) {
		g.setColor(ChartPanel.colors.get("HIGHLIGHT"));
		if (data.currentInstrument.type.isGuitarType()) {
			drawGuitarHighlight(g, data);
		} else if (data.currentInstrument.type.isDrumsType() || data.currentInstrument.type.isKeysType()) {
			drawDrumsKeysHighlight(g, data);
		} else if (data.currentInstrument.type.isVocalsType()) {
			drawVocalsHiglight(g, data);
		}
	}
}
