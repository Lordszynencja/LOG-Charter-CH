package log.charter.io.midi;

import static java.lang.Math.round;
import static java.util.Arrays.copyOfRange;
import static log.charter.io.Logger.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

		@Override
		public String toString() {
			return "MidEvent{t: " + t + ", msg: " + Arrays.toString(msg) + "}";
		}
	}

	public static enum TrackType {
		GUITAR, GUITAR_COOP, GUITAR_RHYTHM, BASS, KEYS, VOCALS, EVENTS, TEMPO, UNKNOWN;

		public static TrackType from(final String s) {
			if (s == null) {
				return UNKNOWN;
			}
			switch (s) {
			case "EVENTS":
				return EVENTS;
			case "PART GUITAR":
				return GUITAR;
			case "PART GUITAR COOP":
				return GUITAR_COOP;
			case "PART RHYTHM":
				return GUITAR_RHYTHM;
			case "PART BASS":
				return BASS;
			case "PART KEYS":
				return KEYS;
			case "PART VOCALS":
				return VOCALS;
			default:
				return UNKNOWN;
			}
		}

	}

	public final TrackType type;
	public final List<MidEvent> events;

	public MidTrack(final Track t, final boolean isTempo, final double scaler) {
		TrackType trackType = null;
		events = new ArrayList<>(t.size());

		for (int i = 0; i < t.size(); i++) {
			final MidiEvent e = t.get(i);
			final byte[] msg = e.getMessage().getMessage();

			debug(e.getTick() + ", " + Arrays.toString(msg));
			if ((msg[0] == -1) && (msg[1] == 3)) {
				trackType = TrackType.from(new String(copyOfRange(msg, 3, msg.length)));
			} else if ((msg[0] != -1) || (msg[1] != 47) || (msg[2] != 0)) {
				events.add(new MidEvent(ms(e, scaler), msg));
			}
		}
		type = isTempo ? TrackType.TEMPO : trackType;
		debug("Track " + type + " ended");
	}

	public MidTrack(final TrackType type, final List<MidEvent> events) {
		this.type = type;
		this.events = events;
	}

	private long ms(final MidiEvent e, final double scaler) {
		return round(e.getTick() * scaler);
	}
}
