package log.charter.io.chart.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import log.charter.io.Logger;
import log.charter.song.Song;
import log.charter.util.RW;
import log.charter.util.Splitter;

public class ChartReader {
	public static Song readChart(final String path) {
		if (path != null) {
			throw new IllegalArgumentException("Not finished");
		}
		try {
			return new ChartReader(path).read();
		} catch (final Exception e) {
			Logger.error("Couln't load chart file", e);
			return null;
		}
	}

	private ChartReader(final String path) throws IOException {
		final String[] lines = Splitter.split(RW.read(path), Splitter.endLineRegex);

		if (lines.length == 0) {
			throw new IllegalArgumentException("Chart file is empty");
		}

		if (!lines[0].equals("[Song]")) {
			throw new IllegalArgumentException("Chart file has wrong format, line[0] is not [Song]");
		}
		if (!lines[1].startsWith("{")) {
			throw new IllegalArgumentException("Chart file has wrong format, line[0] is not [Song]");
		}
		final Map<String, String> songParams = new HashMap<>();
		int i = 0;
		for (; i < lines.length; i++) {
			if (lines[i].startsWith("}")) {
				break;
			} // TODO load song info

		}
	}

	private Song read() {
		return null;
	}
}
