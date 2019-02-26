package log.charter.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

public class CharterMenuBar extends JMenuBar {
	private static final long serialVersionUID = -5784270027920161709L;

	private static JMenuItem createItem(final String name, final ActionListener listener) {
		final JMenuItem item = new JMenuItem(name);
		item.addActionListener(listener);
		return item;
	}

	private final CharterFrame frame;

	public CharterMenuBar(final CharterFrame frame) {
		super();
		this.frame = frame;
		final Dimension size = new Dimension(100, 20);
		setMinimumSize(size);
		this.setSize(size);
		setMaximumSize(size);

		this.add(prepareFileMenu());
	}

	private void open() {
		final File dir = new File((frame.s == null) || (frame.s.path == null) ? "C:/" : frame.s.path);
		final JFileChooser chooser = new JFileChooser(dir);
		chooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				System.out.println(f.getName());
				return f.isDirectory() || f.getName().endsWith(".mid") || f.getName().endsWith(".lcf");
			}

			@Override
			public String getDescription() {
				return "Midi (.mid) or Log Charter (.lcf) File";
			}
		});

		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			final File f = chooser.getSelectedFile();

			System.out.print(f.getAbsolutePath());
		}
	}

	private JMenu prepareFileMenu() {
		final JMenu menu = new JMenu("File");
		menu.add(createItem("Open", e -> open()));
		menu.add(createItem("Save", e -> save()));
		menu.add(createItem("Save as...", e -> saveAs()));

		return menu;
	}

	private void save() {
		System.out.println("Save file");
	}

	private void saveAs() {
		System.out.println("Save file as");
	}

}
