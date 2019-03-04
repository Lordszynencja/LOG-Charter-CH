package log.charter.song;

public class Event extends Position {

	/**
	 * length in ms
	 */
	public double length = 0;

	public Event(final double pos) {
		super(pos);
	}

	public Event(final Event e) {
		super(e);
		length = e.length;
	}

	@Override
	public String toString() {
		return "Event{pos: " + pos + ", length: " + length + "}";
	}
}
