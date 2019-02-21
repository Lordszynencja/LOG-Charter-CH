package log.song;

public class Tempo {
	public long t;
	public int bpm;
	public boolean sync;

	public Tempo(final long t, final int bpm, final boolean sync) {
		this.t = t;
		this.bpm = bpm;
		this.sync = sync;
	}

	@Override
	public String toString() {
		return "Tempo{t: " + t//
				+ ", bpm: " + bpm//
				+ ", sync: " + (sync ? "T" : "F") + "}";
	}
}
