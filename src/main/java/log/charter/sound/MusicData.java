package log.charter.sound;

import static log.charter.sound.SoundPlayer.slow;
import static log.charter.sound.SoundPlayer.toBytes;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

public class MusicData {

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
	public byte[] preparedData;
	public int slow = 1;

	public MusicData(final byte[] b, final float rate) {
		preparedData = b;
		data = splitAudio(b);
		outFormat = new AudioFormat(Encoding.PCM_SIGNED, rate, 16, 2, 4, rate, false);
	}

	public void setSlow(final int newSlow) {
		if (newSlow == 0) {
			setSlow(1);
			return;
		}
		if (slow != newSlow) {
			slow = newSlow;
			preparedData = toBytes(slow(data, slow));
		}
	}
}