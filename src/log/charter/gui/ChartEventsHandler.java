package log.charter.gui;

import static log.charter.io.Logger.error;

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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import log.charter.gui.ChartData.IdOrPos;
import log.charter.io.IniWriter;
import log.charter.io.Logger;
import log.charter.io.midi.reader.MidiReader;
import log.charter.io.midi.writer.MidiWriter;
import log.charter.song.IniData;
import log.charter.song.Section;
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
	}

	@Override
	public void componentShown(final ComponentEvent e) {
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
			data.nextT = (playStartT + (((System.nanoTime() - player.startTime) * data.music.slowMultiplier()) / 1000000))
					- Config.delay;

			while ((nextNoteId != -1) && (data.currentNotes.get(nextNoteId).pos < (data.nextT - Config.delay))) {
				nextNoteId++;
				if (nextNoteId >= data.currentNotes.size()) {
					nextNoteId = -1;
				}
				if (claps) {
					SoundPlayer.play(note, 0);
				}
			}

			while ((nextTempoTime >= 0) && (nextTempoTime < (data.nextT - Config.delay))) {
				nextTempoTime = data.s.tempoMap.findNextBeatTime((int) (data.nextT - Config.delay));
				if (metronome) {
					SoundPlayer.play(tick, 0);
				}
			}
		} else {
			final int speed = (FL * (shift ? 10 : 2)) / (ctrl ? 10 : 1);
			data.nextT += (left ? -speed : 0) + (right ? speed : 0);
			if (data.nextT < 0) {
				data.nextT = 0;
			}
		}
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			if (!data.isEmpty && (player == null) && !left && !right) {
				nextNoteId = data.findClosestNoteForTime(data.nextT);
				if ((nextNoteId > 0) && (nextNoteId < data.currentNotes.size()) //
						&& (data.currentNotes.get(nextNoteId).pos < data.nextT)) {
					nextNoteId++;
				}
				if (nextNoteId >= data.currentNotes.size()) {
					nextNoteId = -1;
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
			stopMusic();
			data.nextT = ctrl ? (int) (data.currentNotes.isEmpty() ? 0 : data.currentNotes.get(0).pos)
					: 0;
			break;
		case KeyEvent.VK_END:
			stopMusic();
			data.nextT = ctrl ? (int) (data.currentNotes.isEmpty() ? 0
					: data.currentNotes.get(data.currentNotes.size() - 1).pos) : data.music.msLength();
			break;
		case KeyEvent.VK_LEFT:
			stopMusic();
			if (alt) {
				data.t = (int) data.s.tempoMap.findBeatTime(data.t - 1);
			} else {
				left = true;
			}
			break;
		case KeyEvent.VK_RIGHT:
			stopMusic();
			if (alt) {
				data.t = (int) data.s.tempoMap.findNextBeatTime(data.t);
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
			if (gPressed) {
				data.gridSize = 1;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_2:
			if (gPressed) {
				data.gridSize = 2;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_3:
			if (gPressed) {
				data.gridSize = 3;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_4:
			if (gPressed) {
				data.gridSize = 4;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_5:
			if (gPressed) {
				data.gridSize = 5;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_6:
			if (gPressed) {
				data.gridSize = 6;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_7:
			if (gPressed) {
				data.gridSize = 7;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_8:
			if (gPressed) {
				data.gridSize = 8;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_9:
			if (gPressed) {
				data.gridSize = 9;
				data.useGrid = true;
			}
			break;
		case KeyEvent.VK_C:
			claps = !claps;
			break;
		case KeyEvent.VK_G:
			data.useGrid = !data.useGrid;
			gPressed = true;
			break;
		case KeyEvent.VK_H:
			for (final int id : data.selectedNotes) {
				data.currentNotes.get(id).hopo ^= true;
			}
			break;
		case KeyEvent.VK_M:
			metronome = !metronome;
			break;
		case KeyEvent.VK_N:
			stopMusic();
			if (e.isControlDown()) {
				newSong();
			}
			break;
		case KeyEvent.VK_O:
			stopMusic();
			if (e.isControlDown()) {
				open();
			}
			break;
		case KeyEvent.VK_R:
			stopMusic();
			if (e.isControlDown()) {
				data.redo();
			}
			break;
		case KeyEvent.VK_S:
			stopMusic();
			if (e.isControlDown()) {
				save();
			}
			break;
		case KeyEvent.VK_T:
			if ((player == null) && (data.my >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2)))
					&& (data.my <= (ChartPanel.lane0Y
							+ ((ChartPanel.laneDistY * 9) / 2)))) {
				data.toggleNote(data.findClosestIdOrPosForX(data.mx), 0);
			}
			break;
		case KeyEvent.VK_Z:
			stopMusic();
			if (e.isControlDown()) {
				data.undo();
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
		gPressed = false;
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
			if (y < (ChartPanel.sectionNamesY - 5)) {
				return;
			} else if (y < ChartPanel.spY) {
				stopMusic();
				final Section s = data.findOrCreateSectionCloseTo(data.s.tempoMap.findBeatTime(data.xToTime(x + 10)));
				final String newSectionName = JOptionPane.showInputDialog(frame, "Section name:", s.name);
				if ((newSectionName == null) || newSectionName.trim().equals("")) {
					data.s.sections.remove(s);
				} else {
					s.name = newSectionName;
				}
			} else if (y < (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) {
				return;
			}
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO moving notes / adding notes
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
			if (y < ChartPanel.spY) {
				return;
			} else if (y < (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) {
				final Object[] tempoData = data.s.tempoMap.findOrCreateClosestTempo(data.xToTime(x));
				if (tempoData != null) {
					data.startTempoDrag((Tempo) tempoData[0], (Tempo) tempoData[1], (Tempo) tempoData[2],
							(boolean) tempoData[3]);
				}
				return;
			} else if (y < (ChartPanel.lane0Y + ((ChartPanel.laneDistY * 9) / 2))) {
				stopMusic();
				final IdOrPos idOrPos = data.findClosestIdOrPosForX(x);
				if (shift) {
					// TODO select note streak from data.lastNoteSelected to this
					// note
				} else if (ctrl) {
					if (idOrPos.isId()) {
						if (!data.selectedNotes.remove((Integer) idOrPos.id)) {
							data.lastSelectedNote = idOrPos.id;
							data.selectedNotes.add(idOrPos.id);
							data.selectedNotes.sort(null);
						}
					}
				} else {
					data.selectedNotes.clear();
					data.lastSelectedNote = null;
					if (idOrPos.isId()) {
						data.lastSelectedNote = idOrPos.id;
						data.selectedNotes.add(idOrPos.id);
					}
				}
			}
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if ((y >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) && (y <= (ChartPanel.lane0Y
					+ ((ChartPanel.laneDistY * 9) / 2)))) {
				stopMusic();
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
		data.stopTempoDrag();
		data.endNoteAdding();
		// TODO end drag
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		final int rot = e.getWheelRotation();
		if (ctrl) {
			data.addZoom(rot * (shift ? 10 : 1));
		} else {
			data.changeNoteLength(rot);
		}
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

			folderName = JOptionPane.showInputDialog(frame, "Choose folder name",
					folderName);
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
			save();
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
		Logger.info("saveAs");// TODO
		Config.save();
	}

	public void showPopup(final String msg) {
		JOptionPane.showMessageDialog(frame, msg);
	}

	private void stopMusic() {
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
