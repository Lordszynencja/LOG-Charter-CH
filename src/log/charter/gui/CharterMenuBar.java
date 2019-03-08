package log.charter.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import log.charter.song.Instrument.InstrumentType;

public class CharterMenuBar extends JMenuBar {

	private static final long serialVersionUID = -5784270027920161709L;

	private static JMenuItem createItem(final String name, final ActionListener listener) {
		final JMenuItem item = new JMenuItem(name);
		item.addActionListener(listener);
		return item;
	}

	private final ChartEventsHandler handler;

	public CharterMenuBar(final ChartEventsHandler handler) {
		super();
		this.handler = handler;
		final Dimension size = new Dimension(100, 20);
		setMinimumSize(size);
		this.setSize(size);
		setMaximumSize(size);

		this.add(prepareFileMenu());
		this.add(prepareConfigMenu());
		this.add(prepareInstrumentMenu());
	}

	private JMenu prepareConfigMenu() {
		final JMenu menu = new JMenu("Config");
		menu.add(createItem("Options", e -> new ConfigPane(handler.frame)));
		menu.add(createItem("Song options", e -> new SongOptionsPane(handler.frame)));

		return menu;
	}

	private JMenu prepareFileMenu() {
		final JMenu menu = new JMenu("File");
		menu.add(createItem("New", e -> handler.newSong()));
		menu.add(createItem("Open", e -> handler.open()));
		menu.add(createItem("Save", e -> handler.save()));
		menu.add(createItem("Save as...", e -> handler.saveAs()));
		menu.add(createItem("Exit", e -> handler.exit()));

		return menu;
	}

	private JMenu prepareInstrumentMenu() {
		final JMenu menu = new JMenu("Instrument");
		menu.add(createItem("Easy", e -> handler.data.changeDifficulty(0)));
		menu.add(createItem("Medium", e -> handler.data.changeDifficulty(1)));
		menu.add(createItem("Hard", e -> handler.data.changeDifficulty(2)));
		menu.add(createItem("Expert", e -> handler.data.changeDifficulty(3)));
		menu.addSeparator();
		menu.add(createItem("Guitar", e -> handler.data.changeInstrument(InstrumentType.GUITAR)));
		menu.add(createItem("Coop Guitar", e -> handler.data.changeInstrument(InstrumentType.GUITAR_COOP)));
		menu.add(createItem("Rhytm Guitar", e -> handler.data.changeInstrument(InstrumentType.GUITAR_RHYTHM)));
		menu.add(createItem("Bass", e -> handler.data.changeInstrument(InstrumentType.BASS)));
		menu.add(createItem("Keys (TODO better notes)", e -> handler.data.changeInstrument(InstrumentType.KEYS)));
		menu.add(createItem("Vocals (TODO drawing)", e -> handler.data.editVocals()));

		return menu;
	}

}
