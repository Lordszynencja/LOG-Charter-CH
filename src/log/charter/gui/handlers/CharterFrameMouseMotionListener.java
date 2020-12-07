package log.charter.gui.handlers;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import log.charter.data.ChartData;
import log.charter.gui.ChartPanel;
import log.charter.song.TempoMap;

public class CharterFrameMouseMotionListener implements MouseMotionListener {
	private final ChartData data;

	public CharterFrameMouseMotionListener(final ChartData data) {
		this.data = data;
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		mouseMoved(e);

		if (data.draggedTempo != null) {
			data.draggedTempo.pos = data.xToTime(data.mx);
			if (data.draggedTempo.pos < (data.draggedTempoPrev.pos + 1)) {
				data.draggedTempo.pos = data.draggedTempoPrev.pos + 1;
			}
			TempoMap.calcBPM(data.draggedTempoPrev, data.draggedTempo);
			if (data.draggedTempoNext != null) {
				if (data.draggedTempo.pos > (data.draggedTempoNext.pos - 1)) {
					data.draggedTempo.pos = data.draggedTempoNext.pos - 1;
				}
				TempoMap.calcBPM(data.draggedTempo, data.draggedTempoNext);
			} else {
				data.draggedTempo.kbpm = data.draggedTempoPrev.kbpm;
			}
		}

		if (ChartPanel.isInLanes(data.my) && (Math.abs(data.mx - data.mousePressX) > 20)) {
			data.isNoteDrag = true;
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		data.mx = e.getX();
		data.my = e.getY();
	}

}
