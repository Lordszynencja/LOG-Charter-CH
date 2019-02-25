package log.song;

public class Position implements Comparable<Position> {
	public double pos;

	public Position(final long pos) {
		this.pos = pos;
	}

	public Position(final Position p) {
		pos = p.pos;
	}

	@Override
	public int compareTo(final Position o) {
		return new Double(pos).compareTo(o.pos);
	}
}
