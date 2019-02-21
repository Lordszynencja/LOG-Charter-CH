package log.song;

public class Event implements Comparable<Event> {

	/**
	 * start in ms
	 */
	public long pos;

	/**
	 * length in ms
	 */
	public long length;

	public Event(final long pos) {
		this.pos = pos;
	}

	@Override
	public int compareTo(final Event o) {
		return new Long(pos).compareTo(o.pos);
	}

	@Override
	public String toString() {
		return "Event{pos: " + pos//
				+ ", length: " + length + "}";
	}
}
