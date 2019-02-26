package log.charter.song;

public class Note extends Event {
	/**
	 * 0 -> open note<br/>
	 * notes & 1 -> Green<br/>
	 * notes & 2 -> Red<br/>
	 * notes & 4 -> Yellow<br/>
	 * notes & 8 -> Blue<br/>
	 * notes & 16 -> Orange
	 */
	public byte notes;

	public boolean forced = false;
	public boolean hopo = false;
	public boolean tap = false;
	public boolean crazy = false;

	public Note(final long pos) {
		super(pos);
	}

	public Note(final Note n) {
		super(n);
		notes = n.notes;
		tap = n.tap;
		hopo = n.hopo;
		crazy = n.crazy;
		forced = n.forced;
	}

	@Override
	public String toString() {
		return "Note{notes: "//
				+ (notes & 1) + ((notes >> 1) & 1) + ((notes >> 2) & 1) + ((notes >> 3) & 1) + ((notes >> 4) & 1)//
				+ ", pos: " + pos//
				+ ", length: " + length//
				+ ", hopo: " + (hopo ? "T" : "F")//
				+ ", tap: " + (tap ? "T" : "F")//
				+ ", crazy: " + (crazy ? "T" : "F") + "}";
	}
}
