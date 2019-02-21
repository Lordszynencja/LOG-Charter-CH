package log.io;

import static java.util.Arrays.copyOfRange;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

public class MidTrack {
	public static class MidEvent {
		public final long t;
		public final byte[] msg;

		public MidEvent(final long t, final byte[] msg) {
			this.t = t;
			this.msg = msg;
		}
	}

	public static enum TrackType {
		GUITAR, BASS, KEYS, EVENTS, TEMPO, UNKNOWN;

		public static TrackType from(final String s) {
			if (s == null)
				return UNKNOWN;
			if (s.equals("EVENTS"))
				return EVENTS;
			if (s.equals("PART GUITAR"))
				return GUITAR;
			if (s.equals("PART BASS"))
				return BASS;
			if (s.equals("PART KEYS"))
				return KEYS;

			return UNKNOWN;
		}

	}

	public final TrackType type;
	public final MidEvent[] events;

	public MidTrack(final Track t, final boolean isTempo, final double scaler) {
		TrackType trackType = null;
		events = new MidEvent[t.size()];

		for (int i = 0; i < t.size(); i++) {
			final MidiEvent e = t.get(i);
			final byte[] msg = e.getMessage().getMessage();
			events[i] = new MidEvent(ms(e, scaler), msg);

			if (msg[0] == -1 && msg[1] == 3)
				trackType = TrackType.from(new String(copyOfRange(msg, 3, msg.length)));
		}
		type = isTempo ? TrackType.TEMPO : trackType;
	}

	private long ms(final MidiEvent e, final double scaler) {
		return (long) Math.floor(e.getTick() * scaler);
	}
}
