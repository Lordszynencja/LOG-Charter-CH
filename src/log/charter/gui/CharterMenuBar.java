package log.charter.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

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
	}

	private JMenu prepareConfigMenu() {
		final JMenu menu = new JMenu("Config");
		menu.add(createItem("Options", e -> new OptionPane(handler.frame)));

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

}
