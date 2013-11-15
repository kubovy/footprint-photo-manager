package com.poterion.potpho.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JpegImage {
	private final static int DEFAULT = 0;
	private final static int MARKER = 1;
	private final static int SIZE = 2;
	private final static int DATA = 3;
	private final static int STREAM = 4;

	private final String filename;
	private final List<JpegSegment> segments = new ArrayList<JpegSegment>();
	private final List<Byte> stream = new ArrayList<Byte>(5 * 1024 * 1024);

	public JpegImage(String filename) {
		this.filename = filename;
	}

	public final int getSegmentCount(JpegMarker marker) {
		return getSegment(marker).size();
	}
	
	public final JpegSegment getSegment(JpegMarker marker, int index) {
		return getSegment(marker).get(index);
	}
	
	public final List<JpegSegment> getSegment(JpegMarker marker) {
		List<JpegSegment> out = new ArrayList<JpegSegment>();
		for(JpegSegment segment : segments) {
			if (segment.getMarker().equals(marker)) {
				out.add(segment);
			}
		}
		return out;
	}

	public final ExifSegment getExifSegment() {
		List<JpegSegment> segments = getSegment(JpegMarker.getInstance("APP01"));
		for(JpegSegment segment : segments) {
			ExifSegment exif = ExifSegment.getInstance(segment.getData());
			if (exif != null) {
				return exif;
			}
		}
		return null;
	}
	
	public final void read() {
		File file = new File(filename);
		byte[] buffer = new byte[8196]; // buffer

		try {
			InputStream input = null;
			int state = DEFAULT;
			Byte[] sizeBytes = null;
			int size = 0;
			int dataSize = 0;
			JpegSegment segment = null;
			try {
				int bytesRead = 0;
				int totalBytesRead = 0;
				input = new BufferedInputStream(new FileInputStream(file));

				do {
					bytesRead = input.read(buffer);
					if (bytesRead > 0) {
						totalBytesRead = totalBytesRead + bytesRead;
					}
					for (int i = 0; i < bytesRead; i++) {
						if (state == DEFAULT && buffer[i] == (byte) 0xFF) {
							state = MARKER;
						} else if (state == MARKER) {
							if (buffer[i] == 0x00 || buffer[i] == (byte) 0xFF) {
								throw new RuntimeException("Fuck!");
							} else {
								segment = new JpegSegment(buffer[i]);
								if (JpegMarker.isAlone(segment.getMarker())) {
									segments.add(segment);
									state = DEFAULT;
								} else {
									sizeBytes = new Byte[]{ null, null };
									state = SIZE;
								}
							}
						} else if (state == SIZE) {
							if (sizeBytes[0] == null) {
								sizeBytes[0] = buffer[i];
							} else {
								sizeBytes[1] = buffer[i];
								size = (sizeBytes[0] & 0xFF) << 8 | (sizeBytes[1] & 0xFF);
								dataSize = 2;
								state = DATA;
							}
						} else if (state == DATA) {
							segment.add(buffer[i]);
							dataSize++;
							if (dataSize == size) {
								segments.add(segment);
								if (segment.getMarker().equals(JpegMarker.SOS)) {
									state = STREAM;
								} else {
									state = DEFAULT;
								}
							}
						} else if (state == STREAM) {
							stream.add(buffer[i]);
						} else {
							throw new RuntimeException("Invalid state " + state + " read: " + String.format("%02X", buffer[i]) + "!");
						}
					}
				} while (bytesRead >= 0);
				stream.remove(stream.size()-1);
				stream.remove(stream.size()-1);
				segments.add(new JpegSegment(JpegMarker.EOI));
			} finally {
				input.close();
			}
		} catch (FileNotFoundException ex) {
			System.err.println(String.format("File %s not found.", filename));
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	public final void write() {
		write(filename);
	}

	public final void write(String filename) {
		File file = new File(filename);
		OutputStream output = null;
		try {
			try {
				output = new BufferedOutputStream(new FileOutputStream(file));
				for (JpegSegment segment : segments) {
					if (ExifSegment.isExif(segment.getData())) {
						output.write(getExifSegment().getBytes());
					} else {
						output.write(segment.get());
					}
					if (segment.getMarker().equals(JpegMarker.SOS)) {
						for (byte b : stream) {
							output.write(new byte[]{b});
						}
					}
				}
			} finally {
				output.close();
			}
		} catch (FileNotFoundException ex) {
			System.err.println(String.format("File %s not found.", filename));
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
}
