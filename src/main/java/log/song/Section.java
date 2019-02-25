package log.song;

public class Section extends Position {

	/**
	 * Section name
	 */
	public final String name;

	public Section(final Section s) {
		super(s);
		name = s.name;
	}

	public Section(final String name, final long pos) {
		super(pos);
		this.name = name;
	}

	@Override
	public String toString() {
		return "Section{name: " + name + ", pos: " + pos + "}";
	}
}
