package log.charter.gui;

import static log.charter.gui.ChartPanel.isInTempos;
import static log.charter.io.Logger.error;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import log.charter.gui.ChartData.IdOrPos;
import log.charter.io.IniWriter;
import log.charter.io.Logger;
import log.charter.io.midi.reader.MidiReader;
import log.charter.io.midi.writer.MidiWriter;
import log.charter.song.Event;
import log.charter.song.IniData;
import log.charter.song.Instrument.InstrumentType;
import log.charter.song.Song;
import log.charter.song.Tempo;
import log.charter.song.TempoMap;
import log.charter.sound.MusicData;
import log.charter.sound.SoundPlayer;
import log.charter.sound.SoundPlayer.Player;
import log.charter.util.RW;

public class ChartEventsHandler implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener,
		WindowFocusListener, ComponentListener {
	public static final int FL = 10;

	private static final MusicData tick = MusicData.generateSound(4000, 0.01, 1);
	private static final MusicData note = MusicData.generateSound(1000, 0.02, 0.8);

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

	public ChartEventsHandler(final CharterFrame frame) {
		this.frame = frame;
		data = new ChartData();
		data.handler = this;

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

	private void changeWidth(final Component c, final int w) {
		final int y = c.getY();
		final int h = c.getHeight();
		final Dimension newScrollBarSize = new Dimension(Config.windowWidth, h);
		c.setMinimumSize(newScrollBarSize);
		c.setPreferredSize(newScrollBarSize);
		c.setMaximumSize(newScrollBarSize);
		c.setBounds(0, y, Config.windowWidth, h);
		c.validate();
		c.repaint();
	}

	@Override
	public void componentHidden(final ComponentEvent e) {
	}

	@Override
	public void componentMoved(final ComponentEvent e) {
		Config.windowPosX = e.getComponent().getX();
		Config.windowPosY = e.getComponent().getY();
	}

	@Override
	public void componentResized(final ComponentEvent e) {
		Config.windowHeight = e.getComponent().getHeight();
		Config.windowWidth = e.getComponent().getWidth();

		changeWidth(frame.scrollBar, Config.windowWidth);
		changeWidth(frame.chartPanel, Config.windowWidth);
	}

	@Override
	public void componentShown(final ComponentEvent e) {
	}

	public void copyFrom(final InstrumentType instrumentType, final int diff) {
		data.copyFrom(instrumentType, diff);
	}

	private void editVocalNote(final IdOrPos idOrPos) {// TODO
		new LyricPane(frame, idOrPos);
	}

	public void exit() {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit?", "Exit",
				JOptionPane.YES_NO_OPTION)) {
			frame.dispose();
			System.exit(0);
		}
	}

	private void frame() {
		if ((player != null) && (player.startTime > 0)) {
			setNextTime((playStartT + (((System.nanoTime() - player.startTime) * data.music.slowMultiplier()) / 1000000))
					- Config.delay);

			final List<? extends Event> notes = data.vocalsEditing ? data.s.v.lyrics : data.currentNotes;

			while ((nextNoteId != -1) && (notes.get(nextNoteId).pos < data.nextT)) {
				nextNoteId++;
				if (nextNoteId >= notes.size()) {
					nextNoteId = -1;
				}
				if (claps) {
					SoundPlayer.play(note, 0);
				}
			}

			while ((nextTempoTime >= 0) && (nextTempoTime < data.nextT)) {
				nextTempoTime = data.s.tempoMap.findNextBeatTime((int) data.nextT);
				if (metronome) {
					SoundPlayer.play(tick, 0);
				}
			}
		} else {
			final int speed = (FL * (shift ? 10 : 2)) / (ctrl ? 10 : 1);
			setNextTime((data.nextT - (left ? speed : 0)) + (right ? speed : 0));
		}
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
				if (data.vocalsEditing) {
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
			if (data.vocalsEditing) {
				setNextTime(ctrl ? (int) (data.s.v.lyrics.isEmpty() ? 0 : data.s.v.lyrics.get(0).pos) : 0);
			} else {
				setNextTime(ctrl ? (int) (data.currentNotes.isEmpty() ? 0 : data.currentNotes.get(0).pos) : 0);
			}
			break;
		case KeyEvent.VK_END:
			if (data.vocalsEditing) {
				setNextTime(ctrl ? (int) (data.s.v.lyrics.isEmpty() ? 0
						: data.s.v.lyrics.get(data.s.v.lyrics.size() - 1).pos) : data.music.msLength());
			} else {
				setNextTime(ctrl ? (int) (data.currentNotes.isEmpty() ? 0
						: data.currentNotes.get(data.currentNotes.size() - 1).pos) : data.music.msLength());
			}
			break;
		case KeyEvent.VK_DELETE:
			data.deleteSelected();
			break;
		case KeyEvent.VK_UP:
			if (!data.vocalsEditing) {
				if (ctrl) {
					data.moveSelectedDownWithOpen();
				} else {
					data.moveSelectedDownWithoutOpen();
				}
			}
			break;
		case KeyEvent.VK_DOWN:
			if (!data.vocalsEditing) {
				if (ctrl) {
					data.moveSelectedUpWithOpen();
				} else {
					data.moveSelectedUpWithoutOpen();
				}
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
		case KeyEvent.VK_C:
			if (ctrl) {
				data.copy();
			} else {
				claps = !claps;
			}
			break;
		case KeyEvent.VK_F:
			if (ctrl) {
				if (data.vocalsEditing) {
					data.snapSelectedVocals();
				} else {
					data.snapSelectedNotes();
				}
			}
			break;
		case KeyEvent.VK_G:
			data.useGrid = !data.useGrid;
			gPressed = true;
			break;
		case KeyEvent.VK_H:
			if (!data.vocalsEditing && (data.currentInstrument.type != InstrumentType.KEYS)) {
				if (ctrl) {
					double maxHOPODist = -1;
					while ((maxHOPODist < 0) || (maxHOPODist > 10000)) {
						try {
							maxHOPODist = Double.parseDouble(JOptionPane.showInputDialog(
									"Max distance between notes to make HOPO", "" + Config.lastMaxHOPODist));
						} catch (final Exception exception) {
						}
					}
					Config.lastMaxHOPODist = maxHOPODist;
					data.toggleSelectedHopo(true, maxHOPODist);
				} else {
					data.toggleSelectedHopo(false, -1);
				}
			}
			break;
		case KeyEvent.VK_L:
			if (data.vocalsEditing) {
				if (ctrl) {
					data.changeLyricLines();
				} else {
					if (data.selectedNotes.size() == 1) {
						editVocalNote(new IdOrPos(data.selectedNotes.get(0), -1));
					}
				}
			}
			break;
		case KeyEvent.VK_M:
			metronome = !metronome;
			break;
		case KeyEvent.VK_N:
			if (e.isControlDown()) {
				newSong();
			}
			break;
		case KeyEvent.VK_O:
			if (e.isControlDown()) {
				open();
			}
			break;
		case KeyEvent.VK_Q:
			if (data.vocalsEditing) {
				data.toggleSelectedLyricConnected();
			}
			break;
		case KeyEvent.VK_R:
			if (e.isControlDown()) {
				data.redo();
			}
			break;
		case KeyEvent.VK_S:
			if (e.isControlDown()) {
				save();
			}
			break;
		case KeyEvent.VK_T:
			if (data.vocalsEditing) {
				data.toggleSelectedLyricToneless();
			} else if (data.currentInstrument.type != InstrumentType.KEYS) {
				if (ctrl) {
					data.changeTapSections();
				} else if ((data.my >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2)))
						&& (data.my <= (ChartPanel.lane0Y + ((ChartPanel.laneDistY * 9) / 2)))) {
					data.deselect();
					data.toggleNote(data.findClosestIdOrPosForX(data.mx), 0);
				}
			}
			break;
		case KeyEvent.VK_V:
			if (ctrl) {
				try {
					data.paste();
				} catch (final Exception exception) {
					Logger.error("Couldn't paste notes", exception);
				}
			}
			break;
		case KeyEvent.VK_W:
			if (!data.vocalsEditing && ctrl) {
				data.changeSPSections();
			} else if (data.vocalsEditing) {
				data.toggleSelectedVocalsWordPart();
			}
			break;
		case KeyEvent.VK_Z:
			if (ctrl) {
				data.undo();
			}
			break;
		case KeyEvent.VK_Y:
			if (!data.vocalsEditing && ctrl) {
				data.changeSoloSections();
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
		final int x = e.getX();
		final int y = e.getY();
		if (e.getButton() == MouseEvent.BUTTON1) {
			if ((y > (ChartPanel.sectionNamesY - 5)) && (y < ChartPanel.spY)) {
				stopMusic();
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
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if (data.vocalsEditing) {
				final IdOrPos idOrPos = data.findClosestVocalIdOrPosForX(x);
				if (idOrPos.isId()) {
					data.removeVocalNote(idOrPos.id);
				} else {
					editVocalNote(idOrPos);
				}
			}
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO moving notes
		data.mx = e.getX();
		data.my = e.getY();

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
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		data.mx = e.getX();
		data.my = e.getY();
	}

	@Override
	public void mousePressed(final MouseEvent e) {// TODO start note drag
		data.mx = e.getX();
		data.my = e.getY();

		final int x = e.getX();
		final int y = e.getY();
		if (e.getButton() == MouseEvent.BUTTON1) {
			data.mousePressX = -1;
			data.mousePressY = -1;
			if (ChartPanel.isInTempos(y)) {
				final Object[] tempoData = data.s.tempoMap.findOrCreateClosestTempo(data.xToTime(x));
				if (tempoData != null) {
					data.startTempoDrag((Tempo) tempoData[0], (Tempo) tempoData[1], (Tempo) tempoData[2],
							(boolean) tempoData[3]);
				}
				return;
			} else if (ChartPanel.isInNotes(y)) {
				stopMusic();
				final IdOrPos idOrPos = data.vocalsEditing ? data.findClosestVocalIdOrPosForX(x)
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
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if ((y >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) && (y <= (ChartPanel.lane0Y
					+ ((ChartPanel.laneDistY * 9) / 2)))) {
				if (!data.vocalsEditing) {
					stopMusic();
					data.selectedNotes.clear();
					data.lastSelectedNote = null;
					data.startNoteAdding(x, y);
				}
			}
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		data.mx = e.getX();
		data.my = e.getY();
		// TODO end note drag

		switch (e.getButton()) {
		case MouseEvent.BUTTON1:
			if (data.draggedTempo != null) {
				data.stopTempoDrag();
			}
			break;
		case MouseEvent.BUTTON3:
			data.endNoteAdding();
			break;
		}
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		final int rot = e.getWheelRotation();
		if (ctrl) {
			data.addZoom(rot * (shift ? 10 : 1));
		} else {
			if (data.vocalsEditing) {
				data.changeLyricLength(rot);
			} else {
				data.changeNoteLength(rot);
			}
		}
		e.consume();
	}

	public void newSong() {
		final JFileChooser chooser = new JFileChooser(new File(Config.musicPath));
		chooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				final String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".mp3") || name.endsWith(".ogg");
			}

			@Override
			public String getDescription() {
				return "Mp3 (.mp3) or Ogg (.ogg) file";
			}
		});

		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			final File songFile = chooser.getSelectedFile();
			final String songName = songFile.getName();
			final int dotIndex = songName.lastIndexOf('.');
			final String extension = songName.substring(dotIndex + 1).toLowerCase();
			if (!extension.equals("mp3") && !extension.equals("ogg")) {
				showPopup("Not an Mp3 or Ogg file!");
				return;
			}
			String folderName = songName.substring(0, songName.lastIndexOf('.'));

			folderName = JOptionPane.showInputDialog(frame, "Choose folder name", folderName);
			if (folderName == null) {
				return;
			}

			File f = new File(Config.songsPath + "/" + folderName);
			while (f.exists()) {
				folderName = JOptionPane.showInputDialog(frame, "Given folder already exists, choose different name",
						folderName);
				if (folderName == null) {
					return;
				}
				f = new File(Config.songsPath + "/" + folderName);
			}
			f.mkdir();
			final String songDir = f.getAbsolutePath();
			RW.writeB(songDir + "/guitar." + extension, RW.readB(songFile));

			final MusicData musicData = MusicData.readSongFile(f.getAbsolutePath());
			if (musicData == null) {
				showPopup("Music file (song.mp3 or song.ogg) not found in song folder");
				return;
			}

			data.setSong(songDir, new Song(), new IniData(), musicData);
			data.ini.charter = Config.charter;
			save();
		}
	}

	private void numberPressed(final int num) {
		if (gPressed) {
			data.gridSize = num;
			data.useGrid = true;
		} else if (isInTempos(data.my)) {
			final Object[] tempoData = data.s.tempoMap.findOrCreateClosestTempo(data.xToTime(data.mx));
			if (tempoData != null) {
				data.changeTempoBeatsInMeasure((Tempo) tempoData[1], (boolean) tempoData[3], num);
			}
		}
	}

	public void open() {
		final JFileChooser chooser = new JFileChooser(new File(data.path));
		chooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				final String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".mid") || name.endsWith(".chart") || name.endsWith(".lcf");
			}

			@Override
			public String getDescription() {
				return "Midi (.mid), Chart (.chart) or Log Charter (.lcf) file";
			}
		});

		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			final File f = chooser.getSelectedFile();
			final String dirPath = f.getParent();
			final String name = f.getName().toLowerCase();

			final Song s;
			if (name.endsWith(".mid")) {
				s = MidiReader.readMidi(f.getAbsolutePath());
			} else if (name.endsWith(".chart")) {
				s = null;// TODO
				showPopup("This file type is not supported cos I didn't finish it (remind Lordszynencja)");
				error("TODO chart song");
				return;
			} else if (name.endsWith(".lcf")) {
				s = null;// TODO
				showPopup("This file type is not supported cos I didn't finish it (remind Lordszynencja)");
				error("TODO lcf song");
				return;
			} else {
				s = null;
				showPopup("This file type is not supported");
				error("unsupported file: " + f.getName());
				return;
			}

			final MusicData musicData = MusicData.readSongFile(dirPath);
			if (musicData == null) {
				showPopup("Music file (song.mp3 or song.ogg) not found in song folder");
				return;
			}

			final File iniFile = new File(dirPath + "/song.ini");
			final IniData iniData;
			if (iniFile.exists()) {
				iniData = new IniData(iniFile);
			} else {
				iniData = new IniData();
				error("No ini file found on path " + iniFile.getAbsolutePath());
			}

			if ((s != null) && (musicData != null) && (iniData != null)) {
				Config.lastPath = dirPath;
				Config.save();
				data.setSong(dirPath, s, iniData, musicData);
			}
		}
	}

	private void playMusic() {
		player = SoundPlayer.play(data.music, data.t);
		playStartT = data.t;
	}

	public void save() {
		if (data.isEmpty) {
			return;
		}
		MidiWriter.writeMidi(data.path + "/notes.mid", data.s);
		// TODO save chart, lcf notes
		IniWriter.write(data.path + "/song.ini", data.ini);
		Config.save();
	}

	public void saveAs() {
		if (data.isEmpty) {
			return;
		}
		Logger.error("saveAs not implemented!");// TODO
		Config.save();
	}

	public void setNextTime(final double t) {
		if ((frame != null) && (frame.scrollBar != null)) {
			final int songLength = data.music.msLength();
			final double songPart = songLength == 0 ? 0 : t / songLength;
			frame.scrollBar.setValue((int) (songPart * frame.scrollBar.getMaximum()));
		}
		data.nextT = t;
		if (data.nextT < 0) {
			data.nextT = 0;
		}
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

	@Override
	public void windowGainedFocus(final WindowEvent e) {
	}

	@Override
	public void windowLostFocus(final WindowEvent e) {
		ctrl = false;
		alt = false;
		shift = false;
		left = false;
		right = false;
		gPressed = false;
		data.mousePressX = -1;
		data.mousePressY = -1;
		data.mx = -1;
		data.my = -1;
	}

}
