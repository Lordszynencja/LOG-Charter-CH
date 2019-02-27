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
import log.charter.sound.Mp3Loader;
import log.charter.sound.MusicData;
import log.charter.sound.OggLoader;
import log.charter.sound.SoundPlayer;
import log.charter.sound.SoundPlayer.Player;

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
				System.out.println(data.t);
				data.t = (int) data.findBeatTime(data.t - 1);
				System.out.println(data.t);
			} else {
				left = true;
			}
			break;
		case KeyEvent.VK_RIGHT:
			stopMusic();
			if (alt) {
				System.out.println(data.t);
				data.t = (int) data.findNextBeatTime(data.t);
				System.out.println(data.t);
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
		case KeyEvent.VK_S:
			if (e.isControlDown()) {
				save();
			}
			break;
		case KeyEvent.VK_O:
			if (e.isControlDown()) {
				open();
			}
			break;
		case KeyEvent.VK_N:
			if (e.isControlDown()) {
				newSong();
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
		System.out.println("click");
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (data.my < (ChartPanel.sectionNamesY - 5)) {
				return;
			} else if (data.my < ChartPanel.spY) {
				final Section s = data.findOrCreateSectionCloseTo(data.findBeatTime(data.xToTime(data.mx + 10)));
				final String newSectionName = JOptionPane.showInputDialog(frame, "Section name:", s.name);
				if ((newSectionName == null) || newSectionName.trim().equals("")) {
					data.s.sections.remove(s);
					showPopup("Section deleted");
				} else {
					s.name = newSectionName;
					showPopup("New section name is " + newSectionName);
				}
			} else if (data.my < (ChartPanel.lane0Y - (ChartPanel.laneDistY / 2))) {
				return;
			} else if (data.my < (ChartPanel.lane0Y + ((ChartPanel.laneDistY * 9) / 2))) {
				// TODO notes editing
			}
			return;
			// TODO select
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			// TODO add note

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
			// TODO add selected note length
		}
	}

	public void newSong() {// TODO
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
				return "Midi (.mid), Chart (.chart) or Log Charter (.lcf) File";
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

			File musicFile = new File(dirPath + "/guitar.mp3");
			final MusicData musicData;
			if (musicFile.exists()) {
				musicData = Mp3Loader.load(musicFile.getAbsolutePath());
			} else {
				musicFile = new File(dirPath + "/guitar.ogg");
				if (musicFile.exists()) {
					musicData = OggLoader.load(musicFile.getAbsolutePath());// TODO
				} else {
					musicData = null;
					showPopup("Music file (song.mp3 or song.ogg) not found in song folder");
					return;
				}
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
				data.path = dirPath;
				data.s = s;
				data.currentInstrument = s.g;
				data.currentDiff = 3;
				data.ini = iniData;
				data.music = musicData;
				data.t = 0;
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
