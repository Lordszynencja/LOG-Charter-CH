package log.charter.song;

public class Section extends Position {

	/**
	 * Section name
	 */
	public String name;

	public Section(final Section s) {
		super(s);
		name = s.name;
	}

	public Section(final String name, final double pos) {
		super(pos);
		this.name = name;
	}

	@Override
	public String toString() {
		return "Section{name: " + name + ", pos: " + pos + "}";
	}
}
