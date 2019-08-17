package log.charter.gui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;

import log.charter.data.Config;
import log.charter.gui.handlers.CharterFrameComponentListener;
import log.charter.gui.handlers.CharterFrameMouseWheelListener;
import log.charter.gui.handlers.CharterFrameWindowFocusListener;
import log.charter.gui.handlers.CharterFrameWindowListener;

public class CharterFrame extends JFrame {
	private static final long serialVersionUID = 3603305480386377813L;

	public static final String TITLE = "LOG Charter";
	public static final String TITLE_UNSAVED = "LOG Charter*";

	public final ChartEventsHandler handler;
	public final ChartPanel chartPanel;
	public final JScrollBar scrollBar;

	public CharterFrame() {
		super(TITLE);
		setLayout(null);
		handler = new ChartEventsHandler(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLocationByPlatform(true);
		setVisible(true);
		setSize(Config.windowWidth, Config.windowHeight);
		setLocation(Config.windowPosX, Config.windowPosY);
		setJMenuBar(new CharterMenuBar(handler));
		chartPanel = new ChartPanel(handler);
		add(chartPanel, 0, Config.windowWidth, ChartPanel.HEIGHT);
		scrollBar = createScrollBar();
		add(scrollBar, ChartPanel.HEIGHT, Config.windowWidth, 20);

		addHelp();

		addKeyListener(handler);
		addMouseWheelListener(new CharterFrameMouseWheelListener(handler));
		addWindowFocusListener(new CharterFrameWindowFocusListener(handler));
		addWindowListener(new CharterFrameWindowListener(handler));
		addComponentListener(new CharterFrameComponentListener(this));
		validate();
	}

	private void add(final JComponent component, final int y,
			final int w, final int h) {
		component.setBounds(0, y, w, h);
		final Dimension size = new Dimension(w, h);
		component.setMinimumSize(size);
		component.setPreferredSize(size);
		component.setMaximumSize(size);
		component.validate();

		add(component);
	}

	private void addHelp() {
		final JLabel helpLabel = new JLabel();
		helpLabel.setText("<html>G → toggle grid<br>"//
				+ "G, 1-9 → set grid size<br>"//
				+ "1-9 when mouse is on beat → set beats in measure<br>"//
				+ "Left press above tempo section → add/edit/remove song section<br>"//
				+ "T → place/toggle open note (guitar editing)<br>"//
				+ "Ctrl + W → toggle Star Power section (guitar editing)<br>"//
				+ "Ctrl + T → toggle Tap section (guitar editing)<br>"//
				+ "Ctrl + Y → toggle Solo section (guitar editing)<br>"//
				+ "U → toggle selected notes crazy<br>"//
				+ "H → toggle selected notes HOPO<br>"//
				+ "Ctrl + H → set auto-HOPO for selected notes (type max distance since previous note in ms to make it HOPO)<br>"//
				+ "Ctrl + L → place vocal line (vocals editing)<br>"//
				+ "L → edit vocal note (vocals editing)<br>"//
				+ "T → toggle note toneless (vocals editing)<br>"//
				+ "Q → toggle note connected (vocals editing)<br>"//
				+ "W → toggle note is word part (vocals editing)<br></html>");

		helpLabel.setVerticalAlignment(JLabel.TOP);
		add(helpLabel, ChartPanel.HEIGHT + 20, Config.windowWidth, 300);
	}

	private JScrollBar createScrollBar() {
		final JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, 10000);
		scrollBar.addAdjustmentListener(e -> {
			final double length = handler.data.music.msLength();
			handler.setNextTimeWithoutScrolling((length * e.getValue()) / scrollBar.getMaximum());
		});

		return scrollBar;
	}
}
