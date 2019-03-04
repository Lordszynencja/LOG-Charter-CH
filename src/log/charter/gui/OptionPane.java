package log.charter.gui;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class OptionPane extends JDialog {

	private static interface ValueSetter {
		void setValue(String val);
	};

	private static interface ValueValidator {
		String validateValue(String val);
	}

	private static final ValueValidator dirValidator = val -> {
		final File f = new File(val);
		if (!f.exists()) {
			return "directory doesn't exist";
		}
		if (!f.isDirectory()) {
			return "given path is not folder";
		}
		return null;
	};

	private static final ValueValidator intValidator = val -> {
		try {
			final int i = Integer.parseInt(val);
			if (i < 0) {
				return "value must be positive";
			}
			if (i > 1000000) {
				return "value must be less than 1000000";
			}
			return null;
		} catch (final Exception e) {
			return "number expected";
		}
	};

	private static final ValueValidator markerOffsetValidator = val -> {
		try {
			final int i = Integer.parseInt(val);
			if (i < 0) {
				return "value must be positive";
			}
			if (i > 1000) {
				return "value must be less than 1000";
			}
			return null;
		} catch (final Exception e) {
			return "number expected";
		}
	};

	private static final long serialVersionUID = -3193534671039163160L;;

	private static final int OPTIONS_LSPACE = 10;
	private static final int OPTIONS_USPACE = 10;
	private static final int OPTIONS_LABEL_WIDTH = 200;
	private static final int OPTIONS_HEIGHT = 25;
	private static final int OPTIONS_MAX_INPUT_WIDTH = 500;

	private String musicPath = Config.musicPath;
	private String songsPath = Config.songsPath;

	private int minNoteDistance = Config.minNoteDistance;
	private int minLongNoteDistance = Config.minLongNoteDistance;
	private int minTailLength = Config.minTailLength;
	private int delay = Config.delay;
	private int markerOffset = Config.markerOffset;

	public OptionPane(final CharterFrame frame) {
		super(frame, "Options", true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);
		setLocation(Config.windowPosX + 50, Config.windowPosY + 50);
		setSize(700, 400);
		setLayout(null);

		addConfigValue(0, "Music folder", musicPath, 300, dirValidator, val -> musicPath = val);
		addConfigValue(1, "Songs folder", songsPath, 300, dirValidator, val -> songsPath = val);

		addConfigValue(3, "Minimal note distance", minNoteDistance + "", 50, intValidator,
				val -> minNoteDistance = Integer.valueOf(val));
		addConfigValue(4, "Minimal distance between tail and next note", minLongNoteDistance + "", 50, intValidator,
				val -> minLongNoteDistance = Integer.valueOf(val));
		addConfigValue(5, "Minimal note tail length", minTailLength + "", 50, intValidator, val -> minTailLength = Integer
				.valueOf(val));
		addConfigValue(6, "Sound delay", delay + "", 50, intValidator, val -> delay = Integer.valueOf(val));
		addConfigValue(7, "Marker position", markerOffset + "", 50, markerOffsetValidator, val -> markerOffset = Integer
				.valueOf(
						val));

		final JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> {
			Config.musicPath = musicPath;
			Config.songsPath = songsPath;
			Config.minNoteDistance = minNoteDistance;
			Config.minLongNoteDistance = minLongNoteDistance;
			Config.minTailLength = minTailLength;
			Config.delay = delay;
			Config.markerOffset = markerOffset;

			Config.save();
			dispose();
		});
		add(saveButton, 200, OPTIONS_USPACE + (10 * OPTIONS_HEIGHT), 100, 25);
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			dispose();
		});
		add(cancelButton, 325, OPTIONS_USPACE + (10 * OPTIONS_HEIGHT), 100, 25);

		validate();
		setVisible(true);
	}

	private void add(final JComponent component, final int x, final int y, final int w,
			final int h) {
		component.setBounds(x, y, w, h);
		final Dimension size = new Dimension(w, h);
		component.setMinimumSize(size);
		component.setPreferredSize(size);
		component.setMaximumSize(size);

		add(component);
	}

	private void addConfigValue(final int id, final String name, final String val,
			final int inputLength, final ValueValidator validator, final ValueSetter setter) {
		final int y = OPTIONS_USPACE + (id * OPTIONS_HEIGHT);
		final JLabel label = new JLabel(name, SwingConstants.LEFT);
		add(label, OPTIONS_LSPACE, y, OPTIONS_LABEL_WIDTH, OPTIONS_HEIGHT);

		final int fieldX = OPTIONS_LSPACE + OPTIONS_LABEL_WIDTH + 3;
		final JTextField field = new JTextField(val, inputLength);
		field.getDocument().addDocumentListener(new DocumentListener() {
			JLabel error = null;

			@Override
			public void changedUpdate(final DocumentEvent e) {
				if (error != null) {
					remove(error);
				}

				error = null;
				final String val = field.getText();
				final String validation = validator.validateValue(val);
				if (validation == null) {
					setter.setValue(val);
				} else {
					error = new JLabel(validation);
					add(error, fieldX + field.getWidth(), y, OPTIONS_LABEL_WIDTH, OPTIONS_HEIGHT);
				}
				repaint();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				changedUpdate(e);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				changedUpdate(e);
			}

		});
		add(field, fieldX, y, inputLength > OPTIONS_MAX_INPUT_WIDTH ? OPTIONS_MAX_INPUT_WIDTH : inputLength,
				OPTIONS_HEIGHT);
	}
}
