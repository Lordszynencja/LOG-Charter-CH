package log.charter.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import log.charter.main.LogCharterMain;
import log.charter.song.Instrument;
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
		this.add(prepareNotesMenu());
		this.add(prepareInfoMenu());
	}

	private JMenu prepareConfigMenu() {
		final JMenu menu = new JMenu("Config");
		menu.add(createItem("Options", e -> new ConfigPane(handler.frame)));
		menu.add(createItem("Song options", e -> new SongOptionsPane(handler.frame)));

		return menu;
	}

	private JMenu prepareFileMenu() {
		final JMenu menu = new JMenu("File");
		menu.add(createItem("New", e -> handler.songFileHandler.newSong()));
		menu.add(createItem("Open", e -> handler.songFileHandler.open()));
		menu.add(createItem("Save", e -> handler.songFileHandler.save()));
		menu.add(createItem("Save as... (TODO currently only saves)", e -> handler.songFileHandler.saveAs()));
		menu.add(createItem("Exit", e -> handler.exit()));

		return menu;
	}

	private JMenu prepareInfoMenu() {
		final JMenu menu = new JMenu("Info");

		final String infoText = "Lords of Games Charter\n"//
				+ "Created by Lordszynencja\n"//
				+ "Current version: " + LogCharterMain.VERSION + "\n\n"//
				+ "TODO:\n"//
				+ "working Save As...\n"//
				+ "own file type/saving song creation progress\n"//
				+ "more features of note editing/selection";

		menu.add(createItem("Version", e -> JOptionPane.showMessageDialog(handler.frame, infoText)));

		return menu;
	}

	private JMenu prepareInstrumentMenu() {
		final JMenu menu = new JMenu("Instrument");
		for (int i = 0; i < Instrument.diffNames.length; i++) {
			final int diff = i;
			menu.add(createItem(Instrument.diffNames[i], e -> handler.data.changeDifficulty(diff)));
		}
		menu.addSeparator();
		for (final InstrumentType type : InstrumentType.sortedValues()) {
			menu.add(createItem(type.name, e -> handler.data.changeInstrument(type)));
		}

		return menu;
	}

	private JMenu prepareNotesMenu() {
		final JMenu menu = new JMenu("Notes");
		final JMenu copyFromMenu = new JMenu("Copy from");

		for (final InstrumentType type : InstrumentType.sortedValues()) {
			final JMenu copyFromMenuInstr = new JMenu(type.name);
			for (int i = 0; i < Instrument.diffNames.length; i++) {
				final int diff = i;
				copyFromMenuInstr.add(createItem(Instrument.diffNames[i], e -> handler.copyFrom(type, diff)));
			}
			copyFromMenu.add(copyFromMenuInstr);
		}

		menu.add(copyFromMenu);

		return menu;
	}
}
