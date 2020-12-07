package log.charter.gui;

import static log.charter.gui.ChartPanel.isInLanes;
import static log.charter.gui.ChartPanel.isInTempos;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JOptionPane;

import log.charter.data.ChartData;
import log.charter.data.ChartData.IdOrPos;
import log.charter.data.Config;
import log.charter.gui.handlers.SongFileHandler;
import log.charter.io.Logger;
import log.charter.main.LogCharterMain;
import log.charter.song.Event;
import log.charter.song.Instrument;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Tempo;
import log.charter.sound.MusicData;
import log.charter.sound.RepeatingPlayer;
import log.charter.sound.SoundPlayer;
import log.charter.sound.SoundPlayer.Player;

public class ChartEventsHandler implements KeyListener, MouseListener {
	public static final int FL = 10;

	private static final MusicData tick = MusicData.generateSound(4000, 0.01, 1);
	private static final MusicData note = MusicData.generateSound(1000, 0.02, 0.8);

	private final RepeatingPlayer tickPlayer = new RepeatingPlayer(tick);
	private final RepeatingPlayer notePlayer = new RepeatingPlayer(note);

	public final ChartData data;
	public final CharterFrame frame;

	private int currentFrame = 0;
	private int framesDone = 0;
	private Player player = null;
	private int playStartT = 0;
	private int nextNoteId = -1;
	private boolean claps = false;
	private double nextTempoTime = -1;
	private boolean metronome = false;

	private boolean ctrl = false;
	private boolean alt = false;
	private boolean shift = false;
	private boolean left = false;
	private boolean right = false;
	private boolean gPressed = false;

	public final SongFileHandler songFileHandler;

	public ChartEventsHandler(final CharterFrame frame) {
		this.frame = frame;
		data = new ChartData();
		data.handler = this;
		songFileHandler = new SongFileHandler(this);

		new Thread(() -> {
			try {
				while (true) {
					currentFrame++;
					Thread.sleep(FL);
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}).start();

		new Thread(() -> {
			try {
				while (true) {
					while (currentFrame > framesDone) {
						frame();
						frame.repaint();
						framesDone++;
					}
					Thread.sleep(1);
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void cancelAllActions() {
		data.softClearWithoutDeselect();
		stopMusic();
	}

	public boolean checkChanged() {
		if (data.changed) {
			final int result = JOptionPane.showConfirmDialog(frame, "You have unsaved changes. Do you want to save?",
					"Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				songFileHandler.save();
				return true;
			} else if (result == JOptionPane.NO_OPTION) {
				return true;
			}
			return false;
		}
		return true;
	}

	public void clearKeys() {
		ctrl = false;
		alt = false;
		shift = false;
		left = false;
		right = false;
		gPressed = false;
	}

	public void copyFrom(final InstrumentType instrumentType, final int diff) {
		data.copyFrom(instrumentType, diff);
		setChanged();
	}

	private void editSection(final int x) {
		final int id = data.s.tempoMap.findBeatId(data.xToTime(x + 10));
		final String newSectionName = JOptionPane.showInputDialog(frame, "Section name:", data.s.sections.get(id));
		if (newSectionName == null) {
			return;
		}

		if (newSectionName.trim().equals("")) {
			data.s.sections.remove(id);
		} else {
			data.s.sections.put(id, newSectionName);
		}
	}

	private void editVocalNote(final IdOrPos idOrPos) {
		new LyricPane(frame, idOrPos);
	}

	public void exit() {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit?", "Exit",
				JOptionPane.YES_NO_OPTION)) {
			if (!checkChanged()) {
				return;
			}
			frame.dispose();
			System.exit(0);
		}
	}

	private void frame() {
		if ((player != null) && (player.startTime > 0)) {
			setNextTime(
					(playStartT + (((System.nanoTime() - player.startTime) * data.music.slowMultiplier()) / 1000000))
							- Config.delay);
			final double soundTime = data.nextT + Config.delay;

			final List<? extends Event> notes = data.currentInstrument.type.isVocalsType() ? data.s.v.lyrics
					: data.currentNotes;

			while ((nextNoteId != -1) && (notes.get(nextNoteId).pos < soundTime)) {
				nextNoteId++;
				if (nextNoteId >= notes.size()) {
					nextNoteId = -1;
				}
				if (claps) {
					notePlayer.queuePlaying();
				}
			}

			while ((nextTempoTime >= 0) && (nextTempoTime < soundTime)) {
				nextTempoTime = data.s.tempoMap.findNextBeatTime((int) soundTime);
				if (metronome) {
					tickPlayer.queuePlaying();
				}
			}

			if ((player != null) && player.isStopped()) {
				stopMusic();
			}
		} else {
			final int speed = (FL * (shift ? 10 : 2)) / (ctrl ? 10 : 1);
			setNextTime((data.nextT - (left ? speed : 0)) + (right ? speed : 0));
		}

		final String title = LogCharterMain.TITLE + " : " + data.ini.artist + " - " + data.ini.name + " : "//
				+ (data.currentInstrument.type.isVocalsType() ? "Vocals"
						: data.currentInstrument.type.name + " " + Instrument.diffNames[data.currentDiff])//
				+ (data.changed ? "*" : "");
		frame.setTitle(title);
	}

	public boolean isAlt() {
		return alt;
	}

	public boolean isCtrl() {
		return ctrl;
	}

	public boolean isGPressed() {
		return gPressed;
	}

	public boolean isLeft() {
		return left;
	}

	public boolean isRight() {
		return right;
	}

	public boolean isShift() {
		return shift;
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		final int keyCode = e.getKeyCode();
		if ((keyCode != KeyEvent.VK_CONTROL) && (keyCode != KeyEvent.VK_ALT) && (keyCode != KeyEvent.VK_SHIFT)
				&& (keyCode != KeyEvent.VK_F5) && (keyCode != KeyEvent.VK_C) && (keyCode != KeyEvent.VK_M)
				&& (keyCode != KeyEvent.VK_SPACE)) {
			stopMusic();
		}

		switch (keyCode) {
		case KeyEvent.VK_SPACE:
			if (!data.isEmpty && (player == null) && !left && !right) {
				if (data.currentInstrument.type.isVocalsType()) {
					nextNoteId = data.findClosestVocalForTime(data.nextT);
					if ((nextNoteId > 0) && (nextNoteId < data.s.v.lyrics.size()) //
							&& (data.s.v.lyrics.get(nextNoteId).pos < data.nextT)) {
						nextNoteId++;
					}
					if (nextNoteId >= data.s.v.lyrics.size()) {
						nextNoteId = -1;
					}
				} else {
					nextNoteId = data.findClosestNoteForTime(data.nextT);
					if ((nextNoteId > 0) && (nextNoteId < data.currentNotes.size()) //
							&& (data.currentNotes.get(nextNoteId).pos < data.nextT)) {
						nextNoteId++;
					}
					if (nextNoteId >= data.currentNotes.size()) {
						nextNoteId = -1;
					}
				}

				nextTempoTime = data.s.tempoMap.findNextBeatTime((int) (data.nextT - Config.delay));

				if (ctrl) {
					data.music.setSlow(2);
				} else {
					data.music.setSlow(1);
				}

				playMusic();
			} else {
				stopMusic();
			}
			break;
		case KeyEvent.VK_HOME:
			if (data.currentInstrument.type.isVocalsType()) {
				setNextTime(ctrl ? (int) (data.s.v.lyrics.isEmpty() ? 0 : data.s.v.lyrics.get(0).pos) : 0);
			} else {
				setNextTime(ctrl ? (int) (data.currentNotes.isEmpty() ? 0 : data.currentNotes.get(0).pos) : 0);
			}
			break;
		case KeyEvent.VK_END:
			if (data.currentInstrument.type.isVocalsType()) {
				setNextTime(ctrl
						? (int) (data.s.v.lyrics.isEmpty() ? 0 : data.s.v.lyrics.get(data.s.v.lyrics.size() - 1).pos)
						: data.music.msLength());
			} else {
				setNextTime(ctrl ? (int) (data.currentNotes.isEmpty() ? 0
						: data.currentNotes.get(data.currentNotes.size() - 1).pos) : data.music.msLength());
			}
			break;
		case KeyEvent.VK_DELETE:
			data.deleteSelected();
			setChanged();
			break;
		case KeyEvent.VK_UP:
			if (!data.currentInstrument.type.isVocalsType()) {
				data.moveSelectedDown();
				setChanged();
			}
			break;
		case KeyEvent.VK_DOWN:
			if (!data.currentInstrument.type.isVocalsType()) {
				data.moveSelectedUp();
				setChanged();
			}
			break;
		case KeyEvent.VK_LEFT:
			if (alt) {
				setNextTime(data.s.tempoMap.findBeatTime(data.nextT - 1));
			} else {
				left = true;
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (alt) {
				setNextTime(data.s.tempoMap.findNextBeatTime(data.nextT));
			} else {
				right = true;
			}
			break;
		case KeyEvent.VK_CONTROL:
			ctrl = true;
			break;
		case KeyEvent.VK_ALT:
			alt = true;
			break;
		case KeyEvent.VK_SHIFT:
			shift = true;
			break;
		case KeyEvent.VK_ESCAPE:
			stopMusic();
			exit();
			break;
		case KeyEvent.VK_F5:
			data.drawAudio = !data.drawAudio;
			break;
		case KeyEvent.VK_0:
			numberPressed(0);
			break;
		case KeyEvent.VK_1:
			numberPressed(1);
			break;
		case KeyEvent.VK_2:
			numberPressed(2);
			break;
		case KeyEvent.VK_3:
			numberPressed(3);
			break;
		case KeyEvent.VK_4:
			numberPressed(4);
			break;
		case KeyEvent.VK_5:
			numberPressed(5);
			break;
		case KeyEvent.VK_6:
			numberPressed(6);
			break;
		case KeyEvent.VK_7:
			numberPressed(7);
			break;
		case KeyEvent.VK_8:
			numberPressed(8);
			break;
		case KeyEvent.VK_9:
			numberPressed(9);
			break;
		case KeyEvent.VK_A:
			if (ctrl) {
				data.selectAll();
			}
			break;
		case KeyEvent.VK_B:
			if (ctrl && data.currentInstrument.type.isDrumsType()) {
				data.toggleSelectedNotesBlueTom();
			}
			break;
		case KeyEvent.VK_C:
			if (ctrl) {
				data.copy();
			} else {
				claps = !claps;
			}
			break;
		case KeyEvent.VK_E:
			if (ctrl && data.currentInstrument.type.isDrumsType() && data.currentDiff == 3) {
				data.toggleSelectedNotesExpertPlus();
			}
			break;
		case KeyEvent.VK_F:
			if (ctrl) {
				if (data.currentInstrument.type.isVocalsType()) {
					data.snapSelectedVocals();
				} else {
					data.snapSelectedNotes();
				}
				setChanged();
			}
			break;
		case KeyEvent.VK_G:
			if (ctrl && data.currentInstrument.type.isDrumsType()) {
				data.toggleSelectedNotesGreenTom();
			} else {
				data.useGrid = !data.useGrid;
				gPressed = true;
			}
			break;
		case KeyEvent.VK_H:
			if (!data.currentInstrument.type.isVocalsType() && (data.currentInstrument.type != InstrumentType.KEYS)) {
				if (ctrl) {
					double maxHOPODist = -1;
					while ((maxHOPODist < 0) || (maxHOPODist > 10000)) {
						try {
							final String value = JOptionPane.showInputDialog("Max distance between notes to make HOPO",
									"" + Config.lastMaxHOPODist);
							if (value == null) {
								return;
							}
							maxHOPODist = Double.parseDouble(value);
						} catch (final Exception exception) {
						}
					}
					Config.lastMaxHOPODist = maxHOPODist;
					data.toggleSelectedHopo(true, maxHOPODist);
				} else {
					data.toggleSelectedHopo(false, -1);
				}
				setChanged();
			}
			break;
		case KeyEvent.VK_K:
			if (ctrl && data.currentInstrument.type.isDrumsType()) {
				data.changeDrumRollSections();
			}
			break;
		case KeyEvent.VK_L:
			if (data.currentInstrument.type.isVocalsType()) {
				if (ctrl) {
					data.changeLyricLines();
				} else {
					if (data.selectedNotes.size() == 1) {
						editVocalNote(new IdOrPos(data.selectedNotes.get(0), -1));
					}
				}
				setChanged();
			} else if (ctrl && data.currentInstrument.type.isDrumsType()) {
				data.changeSpecialDrumRollSections();
			}
			break;
		case KeyEvent.VK_M:
			metronome = !metronome;
			break;
		case KeyEvent.VK_N:
			if (e.isControlDown()) {
				songFileHandler.newSong();
			}
			break;
		case KeyEvent.VK_O:
			if (e.isControlDown()) {
				songFileHandler.open();
			}
			break;
		case KeyEvent.VK_P:
			if (!data.currentInstrument.type.isVocalsType() && ctrl) {
				data.changeSoloSections();
				setChanged();
			}
			break;
		case KeyEvent.VK_Q:
			if (data.currentInstrument.type.isVocalsType()) {
				data.toggleSelectedLyricConnected();
				setChanged();
			}
			break;
		case KeyEvent.VK_R:
			if (ctrl) {
				data.redo();
				setChanged();
			}
			break;
		case KeyEvent.VK_S:
			if (ctrl) {
				songFileHandler.save();
			}
			break;
		case KeyEvent.VK_T:
			if (data.currentInstrument.type.isVocalsType()) {
				data.toggleSelectedLyricToneless();
				setChanged();
			} else if (data.currentInstrument.type.isGuitarType()) {
				if (ctrl) {
					data.changeTapSections();
				}
				setChanged();
			}
			break;
		case KeyEvent.VK_U:
			if (!data.currentInstrument.type.isVocalsType()) {
				data.toggleSelectedCrazy();
			}
			break;
		case KeyEvent.VK_V:
			if (ctrl) {
				try {
					data.paste();
					setChanged();
				} catch (final Exception exception) {
					Logger.error("Couldn't paste notes", exception);
				}
			}
			break;
		case KeyEvent.VK_W:
			if (!data.currentInstrument.type.isVocalsType() && ctrl) {
				data.changeSPSections();
				setChanged();
			} else if (data.currentInstrument.type.isVocalsType()) {
				data.toggleSelectedVocalsWordPart();
				setChanged();
			}
			break;
		case KeyEvent.VK_Y:
			if (ctrl && data.currentInstrument.type.isDrumsType()) {
				data.toggleSelectedNotesYellowTom();
			}
			break;
		case KeyEvent.VK_Z:
			if (ctrl) {
				data.undo();
				setChanged();
			}
			break;
		case KeyEvent.VK_COMMA:
			if (((data.gridSize / 2) * 2) == data.gridSize) {
				data.gridSize /= 2;
			}
			break;
		case KeyEvent.VK_PERIOD:
			data.gridSize *= 2;
			break;
		default:
			break;
		}
		if (e.getKeyCode() != KeyEvent.VK_G) {
			gPressed = false;
		}
	}

	@Override
	public void keyReleased(final KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			left = false;
			break;
		case KeyEvent.VK_RIGHT:
			right = false;
			break;
		case KeyEvent.VK_CONTROL:
			ctrl = false;
			break;
		case KeyEvent.VK_ALT:
			alt = false;
			break;
		case KeyEvent.VK_SHIFT:
			shift = false;
			break;
		case KeyEvent.VK_G:
			gPressed = true;
			break;
		default:
			break;
		}
	}

	@Override
	public void keyTyped(final KeyEvent e) {
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		cancelAllActions();
		data.mx = e.getX();
		data.my = e.getY();

		final int x = e.getX();
		final int y = e.getY();
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (isInTempos(y)) {
				final Object[] tempoData = data.s.tempoMap.findOrCreateClosestTempo(data.xToTime(x));
				if (tempoData != null) {
					data.startTempoDrag((Tempo) tempoData[0], (Tempo) tempoData[1], (Tempo) tempoData[2],
							(boolean) tempoData[3]);
				}
				return;
			} else if (isInLanes(y)) {
				data.mousePressX = data.mx;
				data.mousePressY = data.my;
			}
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if (isInLanes(y)) {
				data.selectedNotes.clear();
				data.lastSelectedNote = null;
				data.startNoteAdding(x, y);
			}
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		data.mx = e.getX();
		data.my = e.getY();

		switch (e.getButton()) {
		case MouseEvent.BUTTON1:
			if (data.draggedTempo != null) {
				data.stopTempoDrag();
				setChanged();
			} else if (data.isNoteDrag) {
				data.endNoteDrag();
			} else if ((data.my > (ChartPanel.sectionNamesY - 5)) && (data.my < ChartPanel.spY)) {
				editSection(data.mx);
			} else if (ChartPanel.isInLanes(data.my)) {
				selectNotes(data.mx);
			}
			break;
		case MouseEvent.BUTTON3:
			if (data.isNoteAdd) {
				data.endNoteAdding();
				setChanged();
			}
			break;
		default:
			break;
		}

		cancelAllActions();
	}

	private void numberPressed(final int num) {
		if (gPressed) {
			if (num != 0) {
				data.gridSize = num;
				data.useGrid = true;
			}
		} else if (isInTempos(data.my)) {
			if (num != 0) {
				final Object[] tempoData = data.s.tempoMap.findOrCreateClosestTempo(data.xToTime(data.mx));
				if (tempoData != null) {
					data.changeTempoBeatsInMeasure((Tempo) tempoData[1], (boolean) tempoData[3], num);
					setChanged();
				}
			}
		} else if (shift && (num >= 0) && (num <= data.currentInstrument.type.lanes)) {
			data.toggleSelectedNotes(num);
		}
	}

	private void playMusic() {
		player = SoundPlayer.play(data.music, data.t);
		playStartT = data.t;
	}

	private void selectNotes(final int x) {
		final IdOrPos idOrPos = data.currentInstrument.type.isVocalsType() ? data.findClosestVocalIdOrPosForX(x)
				: data.findClosestIdOrPosForX(x);

		final int[] newSelectedNotes;
		final Integer last;
		if (shift) {
			if (idOrPos.isId() && (data.lastSelectedNote != null)) {
				last = idOrPos.id;
				final int start;
				final int n;
				if (data.lastSelectedNote < idOrPos.id) {
					start = data.lastSelectedNote;
					n = (idOrPos.id - data.lastSelectedNote) + 1;
				} else {
					start = idOrPos.id;
					n = (data.lastSelectedNote - idOrPos.id) + 1;
				}
				newSelectedNotes = new int[n];
				for (int i = 0; i < n; i++) {
					newSelectedNotes[i] = start + i;
				}
			} else {
				last = null;
				newSelectedNotes = new int[0];
			}
		} else {
			if (idOrPos.isId()) {
				last = idOrPos.id;
				newSelectedNotes = new int[] { idOrPos.id };
			} else {
				last = null;
				newSelectedNotes = new int[0];
			}
		}
		if (!ctrl) {
			data.deselect();
		}
		data.lastSelectedNote = last;
		for (final Integer id : newSelectedNotes) {
			if (!data.selectedNotes.remove(id)) {
				data.selectedNotes.add(id);
			}
		}
		data.selectedNotes.sort(null);
	}

	public void setChanged() {
		if (!data.isEmpty) {
			data.changed = true;
		}
	}

	public void setNextTime(final double t) {
		if ((frame != null) && (frame.scrollBar != null)) {
			final int songLength = data.music.msLength();
			final double songPart = songLength == 0 ? 0 : t / songLength;
			frame.scrollBar.setValue((int) (songPart * frame.scrollBar.getMaximum()));
		}
		setNextTimeWithoutScrolling(t);
	}

	public void setNextTimeWithoutScrolling(final double t) {
		data.nextT = t;
		if (data.nextT < 0) {
			data.nextT = 0;
		}
	}

	public void showPopup(final String msg) {
		JOptionPane.showMessageDialog(frame, msg);
	}

	public void stopMusic() {
		if (player != null) {
			final Player p = player;
			player = null;
			p.stop();
		}
	}

}
