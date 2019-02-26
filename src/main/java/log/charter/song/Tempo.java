package log.charter.song;

public class Tempo extends Position {
	public int id;
	public int kbpm;
	public boolean sync;

	public Tempo(final int id, final long pos, final int kbpm, final boolean sync) {
		super(pos);
		this.id = id;
		this.kbpm = kbpm;
		this.sync = sync;
	}

	public Tempo(final Tempo t) {
		super(t);
		id = t.id;
		kbpm = t.kbpm;
		sync = t.sync;
	}

	@Override
	public String toString() {
		return sync ? "TempoBPM{pos: " + pos + ", kbpm: " + kbpm + "}"
				: "TempoMeasure{pos: " + pos + ", measure: " + kbpm + "}";
	}
}
