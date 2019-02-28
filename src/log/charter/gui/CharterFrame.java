package log.charter.gui;

import javax.swing.JFrame;

public class CharterFrame extends JFrame {
	private static final long serialVersionUID = 3603305480386377813L;

	public final ChartEventsHandler handler;

	public CharterFrame() {
		super("LOG Charter");
		handler = new ChartEventsHandler(this);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setVisible(true);
		this.setSize(800, 600);
		setJMenuBar(new CharterMenuBar(handler));
		getContentPane().add(new ChartPanel(handler));

		addKeyListener(handler);
		addMouseWheelListener(handler);
		addWindowFocusListener(handler);
		validate();
	}

}
