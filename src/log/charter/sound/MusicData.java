package log.charter.sound;

import static log.charter.sound.SoundPlayer.slow;
import static log.charter.sound.SoundPlayer.toBytes;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

public class MusicData {
	private static final int DEF_RATE = 44100;

	public static MusicData generateSound(final double pitch, final double length, final double loudness) {
		final int[] data = new int[(int) (length * DEF_RATE)];
		for (int i = 0; i < data.length; i++) {
			data[i] = (int) (Math.pow(Math.sin((pitch * Math.PI * i) / DEF_RATE), 2) * loudness * 32768);
		}

		return new MusicData(new int[][] { data, data }, DEF_RATE);
	}

	public static MusicData readFile(final String path) {
		if (path.endsWith(".mp3")) {
			final File f = new File(path);
			if (f.exists()) {
				return Mp3Loader.load(f.getAbsolutePath());
			}
		} else if (path.endsWith(".ogg")) {
			final File f = new File(path);
			if (f.exists()) {
				return OggLoader.load(f.getAbsolutePath());
			}
		}
		return null;
	}

	public static MusicData readSongFile(final String dir) {
		File musicFile = new File(dir + "/guitar.mp3");
		if (musicFile.exists()) {
			return Mp3Loader.load(musicFile.getAbsolutePath());
		}
		musicFile = new File(dir + "/guitar.ogg");
		if (musicFile.exists()) {
			return OggLoader.load(musicFile.getAbsolutePath());
		}
		return null;
	}

	private static int[][] splitAudio(final byte[] b) {
		final int[][] d = new int[2][b.length / 4];
		for (int i = 0; i < b.length; i += 4) {
			d[0][i / 4] = b[i] + (b[i + 1] * 256);
			d[1][i / 4] = b[i + 2] + (b[i + 3] * 256);
		}

		return d;
	}

	public final int[][] data;
	public final AudioFormat outFormat;
	private byte[] preparedData;
	private int slow = 1;

	public MusicData(final byte[] b, final float rate) {
		preparedData = b;
		data = splitAudio(b);
		outFormat = new AudioFormat(Encoding.PCM_SIGNED, rate, 16, 2, 4, rate, false);
	}

	private MusicData(final int[][] data, final float rate) {
		preparedData = toBytes(data);
		this.data = data;
		outFormat = new AudioFormat(Encoding.PCM_SIGNED, rate, 16, 2, 4, rate, false);
	}

	public byte[] getData() {
		return preparedData;
	}

	public void setSlow(final int newSlow) {
		if (newSlow == 0) {
			return;
		}
		if (newSlow != slow) {
			slow = newSlow;
			preparedData = toBytes(slow(data, slow));
		}
	}

	public double slowMultiplier() {
		return slow > 0 ? 1.0 / slow : -slow / (-slow + 1.0);
	}
}