package com.poterion.potpho.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class JpegSegment {
	private final JpegMarker marker;
	private final List<Byte> segment = new ArrayList<Byte>();
	
	public JpegSegment(byte marker) {
		this.marker = JpegMarker.getInstance(marker);
	}
	
	public JpegSegment(JpegMarker marker) {
		this(marker.getMarker());
	}

	public final void add(byte b) {
		segment.add(b);
	}

	public final JpegMarker getMarker() {
		return marker;
	}

	public final int getSize() {
		return segment.size();
	}

	public final byte[] getData() {
		byte[] out = new byte[segment.size()];
		for (int i = 0; i < segment.size(); i++) {
			out[i] = segment.get(i);
		}
		return out;
	}

	public final byte[] get() {
		boolean alone = JpegMarker.isAlone(marker);
		int size = alone ? 2 : (4 + segment.size());
		byte[] out = new byte[size];

		for (int i = 0; i < 2; i++) {
			out[i] = marker.get()[i];
		}

		if (!alone) {
			byte[] sizeBytes = ByteBuffer.allocate(4).putInt(segment.size() + 2).array();
			for(int i=2; i < 4; i++) {
				out[i] = sizeBytes[i];
			}

			for (int i = 0; i < segment.size(); i++) {
				out[i+4] = segment.get(i);
			}
		}
		return out;
	}
}
