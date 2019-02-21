package log.song;

import java.util.ArrayList;
import java.util.List;

public class Song {

	public static void addNote(final List<List<Note>> list, final Note n, final int diff) {
		if (diff < 0 || diff > 255)
			return;

		while (list.size() < diff)
			list.add(new ArrayList<>());

		list.get(diff).add(n);
	}

	/**
	 * Guitar notes
	 */
	public List<List<Note>> g = new ArrayList<>();
	/**
	 * Guitar star power
	 */
	public List<Event> gsp = new ArrayList<>();
	/**
	 * Guitar tap sections
	 */
	public List<Event> gt = new ArrayList<>();

	/**
	 * Bass notes
	 */
	public List<List<Note>> b = new ArrayList<>();
	/**
	 * Bass star power
	 */
	public List<Event> bsp = new ArrayList<>();
	/**
	 * Guitar tap sections
	 */
	public List<Event> bt = new ArrayList<>();

	/**
	 * Keys notes
	 */
	public List<List<Note>> k = new ArrayList<>();
	/**
	 * Keys star power
	 */
	public List<Event> ksp = new ArrayList<>();

	/**
	 * Sections
	 */
	public List<Section> sections = new ArrayList<>();

	/**
	 * Tempos
	 */
	public List<Tempo> tempos = new ArrayList<>();

}
