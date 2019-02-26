package log.charter.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import log.charter.song.Song;

public class CharterFrame extends JFrame {
	private static final long serialVersionUID = 3603305480386377813L;

	public boolean playing = false;
	public int t = 0;
	public Song s = null;
	public double zoom = 0.4;
	public int offset = 300;
	private boolean left = false;
	private boolean right = false;

	public CharterFrame() {
		super("Test");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setVisible(true);
		this.setSize(800, 600);
		setJMenuBar(new CharterMenuBar(this));
		final ChartPanel chartPanel = new ChartPanel(this);
		getContentPane().add(chartPanel);

		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					left = true;
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					right = true;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					playing = !playing;
				} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					left = false;
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					right = false;
				}
			}

			@Override
			public void keyTyped(final KeyEvent e) {
			}
		});
		validate();

		new Thread(() -> {
			try {
				while (true) {
					if (playing) {
						t += 20;
					}
					if (left) {
						t -= 20;
					}
					if (right) {
						t += 20;
					}
					Thread.sleep(20);
					chartPanel.repaint();
				}
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}
		}).start();
	}

}
