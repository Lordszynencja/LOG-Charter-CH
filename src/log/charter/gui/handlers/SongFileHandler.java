package log.charter.gui.handlers;

import static log.charter.io.Logger.error;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import log.charter.gui.ChartEventsHandler;
import log.charter.gui.CharterFrame;
import log.charter.gui.Config;
import log.charter.io.IniWriter;
import log.charter.io.Logger;
import log.charter.io.midi.reader.MidiReader;
import log.charter.io.midi.writer.MidiWriter;
import log.charter.song.IniData;
import log.charter.song.Song;
import log.charter.sound.MusicData;
import log.charter.util.RW;

public class SongFileHandler {

	private final ChartEventsHandler handler;

	public SongFileHandler(final ChartEventsHandler handler) {
		this.handler = handler;
	}

	public void newSong() {
		if (!handler.checkChanged()) {
			return;
		}

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

		if (chooser.showOpenDialog(handler.frame) == JFileChooser.APPROVE_OPTION) {
			final File songFile = chooser.getSelectedFile();
			final String songName = songFile.getName();
			final int dotIndex = songName.lastIndexOf('.');
			final String extension = songName.substring(dotIndex + 1).toLowerCase();
			if (!extension.equals("mp3") && !extension.equals("ogg")) {
				handler.showPopup("Not an Mp3 or Ogg file!");
				return;
			}
			String folderName = songName.substring(0, songName.lastIndexOf('.'));

			folderName = JOptionPane.showInputDialog(handler.frame, "Choose folder name", folderName);
			if (folderName == null) {
				return;
			}

			File f = new File(Config.songsPath + "/" + folderName);
			while (f.exists()) {
				folderName = JOptionPane.showInputDialog(handler.frame,
						"Given folder already exists, choose different name",
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
				handler.showPopup("Music file (song.mp3 or song.ogg) not found in song folder");
				return;
			}

			handler.data.setSong(songDir, new Song(), new IniData(), musicData);
			handler.data.ini.charter = Config.charter;
			save();
		}
	}

	public void open() {
		if (!handler.checkChanged()) {
			return;
		}

		final JFileChooser chooser = new JFileChooser(new File(handler.data.path));
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

		if (chooser.showOpenDialog(handler.frame) == JFileChooser.APPROVE_OPTION) {
			final File f = chooser.getSelectedFile();
			final String dirPath = f.getParent();
			final String name = f.getName().toLowerCase();

			final Song s;
			if (name.endsWith(".mid")) {
				s = MidiReader.readMidi(f.getAbsolutePath());
			} else if (name.endsWith(".chart")) {
				s = null;// TODO
				handler.showPopup("This file type is not supported cos I didn't finish it (remind Lordszynencja)");
				error("TODO chart song");
				return;
			} else if (name.endsWith(".lcf")) {
				s = null;// TODO
				handler.showPopup("This file type is not supported cos I didn't finish it (remind Lordszynencja)");
				error("TODO lcf song");
				return;
			} else {
				s = null;
				handler.showPopup("This file type is not supported");
				error("unsupported file: " + f.getName());
				return;
			}

			final MusicData musicData = MusicData.readSongFile(dirPath);
			if (musicData == null) {
				handler.showPopup("Music file (song.mp3 or song.ogg) not found in song folder");
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
				handler.data.setSong(dirPath, s, iniData, musicData);
				handler.data.changed = false;
				handler.frame.setTitle(CharterFrame.TITLE);
			}
		}
	}

	public void save() {
		if (handler.data.isEmpty) {
			return;
		}
		MidiWriter.writeMidi(handler.data.path + "/notes.mid", handler.data.s);
		// TODO save .chart, .lcf notes
		IniWriter.write(handler.data.path + "/song.ini", handler.data.ini);
		Config.save();
		handler.data.changed = false;
		handler.frame.setTitle(CharterFrame.TITLE);
	}

	public void saveAs() {
		if (handler.data.isEmpty) {
			return;
		}
		Logger.error("saveAs not implemented, doing normal save!");// TODO
		save();
		Config.save();
	}
}
