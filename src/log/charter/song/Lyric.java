package log.charter.song;

import java.util.List;

public class Lyric extends Event {
	public static String joinLyrics(final List<Lyric> lyrics) {
		final StringBuilder b = new StringBuilder();

		for (final Lyric l : lyrics) {
			if (l.lyric == null) {
				continue;
			}
			b.append(l.lyric);
			if (!l.wordPart) {
				b.append(" ");
			}
		}

		return b.toString();
	}

	public int tone;
	public String lyric;
	public boolean toneless = false;
	public boolean wordPart = false;
	public boolean connected = false;

	public Lyric(final double pos, final int tone) {
		super(pos);
		this.tone = tone;
		lyric = null;
	}

	public Lyric(final double pos, final int tone, final String lyric, final boolean toneless, final boolean wordPart,
			final boolean connected) {
		super(pos);
		this.tone = tone;
		this.lyric = lyric;
		this.toneless = toneless;
		this.wordPart = wordPart;
		this.connected = connected;
	}

	public Lyric(final double pos, final String lyric) {
		super(pos);
		tone = -1;
		this.lyric = lyric;
	}

	public Lyric(final Lyric l) {
		super(l);
		tone = l.tone;
		lyric = l.lyric;
		toneless = l.toneless;
		wordPart = l.wordPart;
		connected = l.connected;
	}

	@Override
	public String toString() {
		return "Lyric{pos: " + pos//
				+ ", length: " + getLength()//
				+ ", tone: " + tone//
				+ ", lyric: " + lyric + "}";
	}
}
