package log.charter.song;

public class Lyric extends Event {
	public int tone;
	public String lyric;
	public boolean noTone = false;
	public boolean wordPart = false;
	public boolean connected = false;

	public Lyric(final double pos, final int tone) {
		super(pos);
		this.tone = tone;
		lyric = null;
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
		noTone = l.noTone;
		wordPart = l.wordPart;
		connected = l.connected;
	}

	@Override
	public String toString() {
		return "Lyric{pos: " + pos//
				+ ", length: " + length//
				+ ", tone: " + tone//
				+ ", lyric: " + lyric + "}";
	}
}