package log.charter.song;

public class Event extends Position {

	/**
	 * length in ms
	 */
	public double length = -1;

	public Event(final Event e) {
		super(e);
		length = e.length;
	}

	public Event(final long pos) {
		super(pos);
	}

	@Override
	public String toString() {
		return "Event{pos: " + pos + ", length: " + length + "}";
	}
}
