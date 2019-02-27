package log.charter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RW {

	public static File getFile(final String filename) {
		final File f = new File(filename);
		if (!f.exists()) {
			final int split = filename.lastIndexOf("/");
			if (split != -1) {
				final String folder = filename.substring(0, split);
				try {
					Files.createDirectories(Paths.get(folder));
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Files.createFile(Paths.get(filename));
				return new File(filename);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}

	public static String read(final File f) {
		return new String(readB(f));
	}

	public static String read(final String filename) {
		return new String(readB(filename));
	}

	public static byte[] readB(final File f) {
		try {
			final FileInputStream input = new FileInputStream(f);
			final byte[] bytes = new byte[(int) f.length()];
			input.read(bytes);
			input.close();
			return bytes;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	public static byte[] readB(final String filename) {
		return readB(getFile(filename));
	}

	public static void write(final File f, final String content) {
		writeB(f, content.getBytes());
	}

	public static void write(final String filename, final String content) {
		writeB(filename, content.getBytes());
	}

	public static void writeB(final File f, final byte[] content) {
		try {
			final FileOutputStream output = new FileOutputStream(f);
			output.write(content);
			output.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeB(final String filename, final byte[] content) {
		final File f = getFile(filename);
		writeB(f, content);
	}
}
