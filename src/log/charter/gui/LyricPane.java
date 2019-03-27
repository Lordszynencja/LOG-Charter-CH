package log.charter.gui;

import log.charter.gui.ChartData.IdOrPos;
import log.charter.song.Lyric;

public class LyricPane extends ParamsPane {
	private static final long serialVersionUID = -4754359602173894487L;

	private String text;
	private boolean toneless;
	private boolean wordPart;
	private boolean connected;

	public LyricPane(final CharterFrame frame, final IdOrPos idOrPos) {
		super(frame, "Lyrics edit", 6);
		final Lyric l;
		if (idOrPos.isId()) {
			l = frame.handler.data.s.v.lyrics.get(idOrPos.id);
			text = l.lyric;
			toneless = l.toneless;
			wordPart = l.wordPart;
			connected = l.connected;
		} else {
			l = null;
			text = "";
			toneless = true;
			wordPart = false;
			connected = false;
		}

		addConfigValue(0, "Lyric", text, 200, null, val -> text = val);
		addConfigCheckbox(1, "Toneless", toneless, val -> toneless = val);
		addConfigCheckbox(2, "Word part", wordPart, val -> wordPart = val);
		addConfigCheckbox(3, "Connected", connected, val -> connected = val);

		addButtons(5, e -> {
			if (idOrPos.isId()) {
				if ("".equals(text)) {
					frame.handler.data.removeVocalNote(idOrPos.id);
				} else {
					l.lyric = text;
					l.toneless = toneless;
					l.wordPart = wordPart;
					l.connected = connected;
				}
			} else {
				if (!"".equals(text) && (text != null)) {
					frame.handler.data.addVocalNote(idOrPos.pos, 0, text, toneless, wordPart, connected);
				}
			}

			dispose();
		});

		validate();
		setVisible(true);
	}

}