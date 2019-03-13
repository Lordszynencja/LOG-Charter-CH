package log.charter.gui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollBar;

public class CharterFrame extends JFrame {
	private static final long serialVersionUID = 3603305480386377813L;

	public final ChartEventsHandler handler;
	public final ChartPanel chartPanel;
	public final JScrollBar scrollBar;

	public CharterFrame() {
		super("LOG Charter");
		setLayout(null);
		handler = new ChartEventsHandler(this);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setVisible(true);
		setSize(Config.windowWidth, Config.windowHeight);
		setLocation(Config.windowPosX, Config.windowPosY);
		setJMenuBar(new CharterMenuBar(handler));
		chartPanel = new ChartPanel(handler);
		add(chartPanel, 0, Config.windowWidth, ChartPanel.HEIGHT);
		scrollBar = createScrollBar();
		add(scrollBar, ChartPanel.HEIGHT, Config.windowWidth, 20);

		addKeyListener(handler);
		addMouseWheelListener(handler);
		addWindowFocusListener(handler);
		addComponentListener(handler);
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

	private JScrollBar createScrollBar() {
		final JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, 10000);
		scrollBar.addAdjustmentListener(e -> {
			final double length = handler.data.music.msLength();
			handler.setNextTimeWithoutScrolling((length * e.getValue()) / scrollBar.getMaximum());
		});

		return scrollBar;
	}
}
