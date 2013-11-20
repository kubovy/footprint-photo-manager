package com.poterion.potpho.test;

import java.util.ArrayList;
import java.util.List;

public class ExifImageFileDirectory {
	private final List<ExifTag> tags;
	private final int nextIdfOffset;
	private final int alignment;

	public ExifImageFileDirectory(byte[] content, int offset, int alignment) {
		this.alignment = alignment;
		int number = JpegLib.byte2int(content, offset, 2, this.alignment);
		this.tags = new ArrayList<ExifTag>(number);
		int position = offset + 2;

		for (int i = 0; i < number; i++) {
			this.tags.add(new ExifTag(content, position, this.alignment));
			position += 12;
		}

		this.nextIdfOffset = JpegLib.byte2int(content, position, 4, this.alignment);
	}

	public final ExifTag[] getTags() {
		return tags.toArray(new ExifTag[] {});
	}

	public final ExifTag getTag(int number) {
		for (ExifTag tag : tags) {
			if (tag.getNumber().number == number) {
				return tag;
			}
		}
		return null;
	}

	public final int getNextIdfOffset() {
		return nextIdfOffset;
	}

	public final byte[] getBytes() {
		List<Byte> output = new ArrayList<Byte>();
		// Tag count
		byte[] tagCount = JpegLib.int2byte(this.tags.size(), 2, this.alignment);
		for (int i = 0; i < tagCount.length; i++) {
			output.add(tagCount[i]);
		}
		// Directory
		for (int t = 0; t < this.tags.size(); t++) {
			byte[] tagBytes = this.tags.get(t).getBytes();
			for (int i = 0; i < tagBytes.length; i++) {
				output.add(tagBytes[i]);
			}
		}
		// Link to next IFD
		byte[] next = JpegLib.int2byte(nextIdfOffset, 4, this.alignment);
		for (int i = 0; i < next.length; i++) {
			output.add(next[i]);
		}

		byte[] out = new byte[output.size()];
		for (int i = 0; i < output.size(); i++) {
			out[i] = output.get(i);
		}
		return out;
	}
}
