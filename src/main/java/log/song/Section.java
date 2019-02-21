package log.song;

public class Section {

	/**
	 * Section name
	 */
	String name;

	/**
	 * Section start in ms
	 */
	long pos;

	public Section(final String name, final long pos) {
		this.name = name;
		this.pos = pos;
	}

	@Override
	public String toString() {
		return "Section{name: " + name//
				+ ", pos: " + pos + "}";
	}
}
