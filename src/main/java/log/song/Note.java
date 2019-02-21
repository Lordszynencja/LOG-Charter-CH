package log.song;

public class Note {
	/**
	 * 0 -> open note<br/>
	 * notes & 1 -> Green<br/>
	 * notes & 2 -> Red<br/>
	 * notes & 4 -> Yellow<br/>
	 * notes & 8 -> Blue<br/>
	 * notes & 16 -> Orange
	 */
	public int notes;

	/**
	 * start in ms
	 */
	public long pos;

	/**
	 * length in ms
	 */
	public long length;

	public boolean tap = false;
	public boolean hopo = false;
	public boolean crazy = false;

	@Override
	public String toString() {
		return "Note{notes: " + (notes & 1) + (notes & 2) + (notes & 4) + (notes & 8) + (notes & 16)//
				+ ", pos: " + pos//
				+ ", length: " + length//
				+ ", tap: " + (tap ? "T" : "F")//
				+ ", hopo: " + (hopo ? "T" : "F")//
				+ ", crazy: " + (crazy ? "T" : "F") + "}";
	}
}
