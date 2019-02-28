package log.charter.gui;

import static log.charter.io.Logger.error;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import log.charter.io.IniWriter;
import log.charter.io.Logger;
import log.charter.io.midi.reader.MidiReader;
import log.charter.io.midi.writer.MidiWriter;
import log.charter.song.IniData;
import log.charter.song.Section;
import log.charter.song.Song;
import log.charter.sound.MusicData;
import log.charter.sound.SoundPlayer;
import log.charter.sound.SoundPlayer.Player;
import log.charter.util.RW;

public class ChartEventsHandler implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	public static final int FL = 10;

	public final ChartData data;
	private final CharterFrame frame;

	private Player player = null;
	private long playStart = 0;
	private boolean ctrl = false;
	private boolean alt = false;
	private boolean shift = false;
	private boolean left = false;
	private boolean right = false;

	public ChartEventsHandler(final CharterFrame frame) {
		this.frame = frame;
		data = new ChartData();
		new Thread(() -> {
			try {
				while (true) {
					frame();
					frame.repaint();
					Thread.sleep(FL);
				}
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void frame() {
		playStart = System.currentTimeMillis();
		final int speed = (FL * (shift ? 10 : 2)) / (ctrl ? 10 : 1);
		data.t += (player != null ? FL : 0) + (left ? -speed : 0) + (right ? speed : 0);
		if (data.t < 0) {
			data.t = 0;
		}
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			if ((player == null) && !left && !right) {
				playMusic();
			} else {
				stopMusic();
			}
			break;
		case KeyEvent.VK_HOME:
			stopMusic();
			data.t = 0;
			break;
		case KeyEvent.VK_LEFT:
			stopMusic();
			if (alt) {
				data.t = (int) data.findBeatTime(data.t - 1);
			} else {
				left = true;
			}
			break;
		case KeyEvent.VK_RIGHT:
			stopMusic();
			if (alt) {
				data.t = (int) data.findNextBeatTime(data.t);
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
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit?", "Exit",
					JOptionPane.YES_NO_OPTION)) {
				frame.dispose();
			}
			break;
		case KeyEvent.VK_O:
			if (e.isControlDown()) {
				open();
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
			if ((data.my >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) && (data.my <= (ChartPanel.lane0Y
					+ ((ChartPanel.laneDistY * 9) / 2)))) {
				data.toggleNote(data.findClosestIdOrPosForX(data.mx), 0);
			}
			break;
		case KeyEvent.VK_N:
			if (e.isControlDown()) {
				newSong();
			}
			break;
		case KeyEvent.VK_Z:
			if (e.isControlDown()) {
				data.undo();
			}
			break;
		default:
			break;
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
				final Section s = data.findOrCreateSectionCloseTo(data.findBeatTime(data.xToTime(x + 10)));
				final String newSectionName = JOptionPane.showInputDialog(frame, "Section name:", s.name);
				if ((newSectionName == null) || newSectionName.trim().equals("")) {
					data.s.sections.remove(s);
					showPopup("Section deleted");
				} else {
					s.name = newSectionName;
				}
			} else if (y < (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) {
				return;
			} else if (y < (ChartPanel.lane0Y + ((ChartPanel.laneDistY * 9) / 2))) {
				if (shift) {
					// TODO select note streak from data.lastNoteSelected to this
					// note
				} else if (ctrl) {
					// TODO select single note
				} else {
					// TODO clear, select single note
				}
			}
		} else if (e.getButton() == MouseEvent.BUTTON2) {
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			if ((y >= (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) && (y <= (ChartPanel.lane0Y
					+ ((ChartPanel.laneDistY * 9) / 2)))) {
				final int color = ChartPanel.yToLane(y) + 1;
				data.toggleNote(data.findClosestIdOrPosForX(x), color);
			}
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		// TODO moving notes / adding notes
		data.mx = e.getX();
		data.my = e.getY();
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
	public void mousePressed(final MouseEvent e) {

		System.out.println("press");
		// TODO starting drag

	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		System.out.println("release");
		// TODO ending drag

	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		if (ctrl) {
			data.addZoom(e.getWheelRotation() * (shift ? 10 : 1));
		} else {
			// TODO add selected note length change
		}
	}

	public void newSong() {// TODO
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
			File f = new File(Config.songsPath + "/" + folderName);
			while (f.exists()) {
				folderName = JOptionPane.showInputDialog(frame, "Given folder already exists, choose different name",
						folderName);
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

			data.clear();
			data.path = songDir;
			data.music = musicData;
			save();
			Config.save();
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
				Logger.error("No ini file found on path " + iniFile.getAbsolutePath());
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
		playStart = System.currentTimeMillis();
	}

	public void save() {
		MidiWriter.writeMidi(data.path + "/notes.mid", data.s);
		// TODO save chart, lcf notes
		IniWriter.write(data.path + "/song.ini", data.ini);
		Config.save();
	}

	public void saveAs() {
		Logger.info("saveAs");// TODO
		Config.save();
	}

	public void showPopup(final String msg) {
		JOptionPane.showMessageDialog(frame, msg);
	}

	private void stopMusic() {
		if (player != null) {
			player.stop();
			data.t += (int) (playStart - System.currentTimeMillis());
			player = null;
		}
	}

}
