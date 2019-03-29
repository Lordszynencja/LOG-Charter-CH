package log.charter.gui;

import log.charter.song.IniData;

public final class SongOptionsPane extends ParamsPane {
	private static final long serialVersionUID = -3193534671039163160L;;

	private String name;
	private String artist;
	private String album;
	private Integer track;
	private Integer year;
	private String genre;

	private String loadingPhrase;
	private String charter;

	private int diffG;
	private int diffGC;
	private int diffGR;
	private int diffB;
	private int diffK;

	public SongOptionsPane(final CharterFrame frame) {
		super(frame, "Options", 15);

		final IniData iniData = frame.handler.data.ini;
		name = iniData.name;
		artist = iniData.artist;
		album = iniData.album;
		track = iniData.track;
		year = iniData.year;
		genre = iniData.genre;

		loadingPhrase = iniData.loadingPhrase;
		charter = iniData.charter;

		diffG = iniData.diffG;
		diffGC = iniData.diffGC;
		diffGR = iniData.diffGR;
		diffB = iniData.diffB;
		diffK = iniData.diffK;

		addConfigValue(0, "Song name", name, 300, null, val -> name = val);
		addConfigValue(1, "Artist", artist, 300, null, val -> artist = val);
		addConfigValue(2, "Album", album, 300, null, val -> album = val);
		addConfigValue(3, "Track", track, 40, createIntValidator(0, 1000, true), //
				val -> track = (val == null) || val.isEmpty() ? null : Integer.valueOf(val));
		addConfigValue(4, "Year", year, 80, createIntValidator(Integer.MIN_VALUE, Integer.MAX_VALUE, true), //
				val -> year = (val == null) || val.isEmpty() ? null : Integer.valueOf(val));
		addConfigValue(5, "Genre", genre, 200, null, val -> genre = val);

		addConfigValue(6, "Loading phrase", loadingPhrase, 500, null, val -> loadingPhrase = val);
		addConfigValue(7, "Charter", charter, 200, null, val -> charter = val);

		addConfigValue(8, "Guitar difficulty", diffG, 40, createIntValidator(-1, 100, false), //
				val -> diffG = Integer.valueOf(val));
		addConfigValue(9, "Coop guitar difficulty", diffGC, 40, createIntValidator(-1, 100, false), //
				val -> diffGC = Integer.valueOf(val));
		addConfigValue(10, "Rhytm guitar difficulty", diffGR, 40, createIntValidator(-1, 100, false), //
				val -> diffGR = Integer.valueOf(val));
		addConfigValue(11, "Bass difficulty", diffB, 40, createIntValidator(-1, 100, false), //
				val -> diffB = Integer.valueOf(val));
		addConfigValue(12, "Keyboard difficulty", diffK, 40, createIntValidator(-1, 100, false), //
				val -> diffK = Integer.valueOf(val));

		addButtons(14, e -> {
			iniData.name = name;
			iniData.artist = artist;
			iniData.album = album;
			iniData.track = track;
			iniData.year = year;
			iniData.genre = genre;

			iniData.loadingPhrase = loadingPhrase;
			iniData.charter = charter;

			iniData.diffG = diffG;
			iniData.diffGC = diffGC;
			iniData.diffGR = diffGR;
			iniData.diffB = diffB;
			iniData.diffK = diffK;

			frame.handler.songFileHandler.save();

			dispose();
		});

		validate();
		setVisible(true);
	}
}
