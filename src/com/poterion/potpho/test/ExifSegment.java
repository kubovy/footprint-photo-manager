package com.poterion.potpho.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExifSegment {
	public static final boolean isExif(byte[] content) {
		return content.length > 6 && content[0] == (byte) 0x45// E
				&& content[1] == (byte) 0x78// x
				&& content[2] == (byte) 0x69// i
				&& content[3] == (byte) 0x66// f
				&& content[4] == (byte) 0x00 && content[5] == (byte) 0x00;
	}

	public static final int getAlign(byte[] content) {
		if (isExif(content)) {
			// II, 0x002A in Intel alignment
			if (content[6] == (byte) 0x49 && content[7] == (byte) 0x49 && content[8] == (byte) 0x2A
					&& content[9] == (byte) 0x00) {
				return JpegLib.INTEL;
			}
			// MM, // 0x002A in Motorola alignment
			if (content[6] == (byte) 0x4d && content[7] == (byte) 0x4d && content[8] == (byte) 0x00
					&& content[9] == (byte) 0x2A) {
				return JpegLib.MOTOROLA;
			}
		}
		return -1;
	}

	public static final ExifSegment getInstance(byte[] content) {
		int alignment;
		if (isExif(content) && (alignment = getAlign(content)) != -1) {
			byte[] newContent = new byte[content.length - 6];
			for (int i = 6; i < content.length; i++) {
				newContent[i - 6] = content[i];
			}
			return new ExifSegment(alignment, newContent);
		}
		return null;
	}

	private final int alignment;
	private final byte[] content;
	private final List<ExifImageFileDirectory> ifds = new ArrayList<ExifImageFileDirectory>(2);
	private final ExifImageFileDirectory exifSubIdf;
	private final Long exifSubIfdOffset;
	private final byte[] thumbnail;

	private ExifSegment(int alignment, byte[] content) {
		this.alignment = alignment;
		this.content = content;
		int exifOffset = JpegLib.byte2int(this.content, 4, 4, this.alignment);
		int ifdOffset = exifOffset;
		int offset = -1;

		while (ifdOffset > 0) {
			ExifImageFileDirectory ifd = new ExifImageFileDirectory(this.content, ifdOffset, this.alignment);
			offset = ifdOffset + ifd.getBytes().length;
			ifdOffset = ifd.getNextIdfOffset();
			ifds.add(ifd);
		}

		// Add data area length of IFD1
		for (ExifTag t : ifds.get(1).getTags()) {
			if (t.getDataOffset() > 0) {
				offset += t.getDataAreaBytes().length;
			}
		}

		exifSubIfdOffset = ifds.get(0).getTag(ExifTag.EXIF_OFFSET).getLong();
		exifSubIdf = new ExifImageFileDirectory(this.content, exifSubIfdOffset.intValue(), this.alignment);
		thumbnail = new byte[content.length - offset];

		for (int i = 0; i < (content.length - offset); i++) {
			thumbnail[i] = this.content[offset + i];
		}
	}

	public final byte[] getBytes() {
		// Exif.., II/MM, 0x002a, Offset (0x00000008)
		List<Byte> data = new ArrayList<Byte>(Arrays.asList(new Byte[] { 0x45, 0x78, 0x69, 0x66, 0x00, 0x00,
				(byte) (this.alignment == JpegLib.INTEL ? 0x49 : 0x4d),
				(byte) (this.alignment == JpegLib.INTEL ? 0x49 : 0x4d),
				(byte) (this.alignment == JpegLib.INTEL ? 0x2A : 0x00),
				(byte) (this.alignment == JpegLib.INTEL ? 0x00 : 0x2A),
				(byte) (this.alignment == JpegLib.INTEL ? 0x08 : 0x00), 0x00, 0x00,
				(byte) (this.alignment == JpegLib.INTEL ? 0x00 : 0x08), }));

		int idx = 8;
		// IFD0
		for (byte b : ifds.get(0).getBytes()) {
			data.add(6 + idx++, b);
		}
		// Padding till ExifSubIFD
		while (idx < exifSubIfdOffset) {
			data.add(6 + idx++, (byte) 0x00);
		}
		// IFD0 Data Area
		for (int t = 0; t < ifds.get(0).getTags().length; t++) {
			ExifTag tag = ifds.get(0).getTags()[t];
			if (tag.getDataOffset() > 0) {
				byte[] tagBytes = tag.getDataAreaBytes();
				for (int i = 0; i < tagBytes.length; i++) {
					data.remove(6 + tag.getDataOffset() + i);
					data.add(6 + tag.getDataOffset() + i, tagBytes[i]);
				}
			}
		}
		// ExifSubIFD
		for (byte b : exifSubIdf.getBytes()) {
			data.add(6 + idx++, b);
		}
		// Padding till IFD1
		while (idx < ifds.get(0).getNextIdfOffset()) {
			data.add(6 + idx++, (byte) 0x00);
		}
		// ExifSubIFD Data Area
		for (int t = 0; t < exifSubIdf.getTags().length; t++) {
			ExifTag tag = exifSubIdf.getTags()[t];
			if (tag.getDataOffset() > 0) {
				byte[] tagBytes = tag.getDataAreaBytes();
				for (int i = 0; i < tagBytes.length; i++) {
					data.remove(6 + tag.getDataOffset() + i);
					data.add(6 + tag.getDataOffset() + i, tagBytes[i]);
				}
			}
		}
		// IFD1
		for (byte b : ifds.get(1).getBytes()) {
			data.add(6 + idx++, b);
		}
		// IFD1 Data Area
		for (int t = 0; t < ifds.get(1).getTags().length; t++) {
			ExifTag tag = ifds.get(1).getTags()[t];
			if (tag.getDataOffset() > 0) {
				byte[] tagBytes = tag.getDataAreaBytes();
				for (int i = 0; i < tagBytes.length; i++) {
					int index = 6 + tag.getDataOffset() + i;
					if (data.size() > index) {
						data.remove(index);
					}
					data.add(index, tagBytes[i]);
				}
			}
		}

		idx = 0;
		byte[] out = new byte[4 + data.size() + thumbnail.length];
		out[idx++] = (byte) 0xFF;
		out[idx++] = (byte) 0xE1;
		byte[] size = JpegLib.int2byte(2 + data.size() + thumbnail.length, 2);

		for (int i = 0; i < size.length; i++) {
			out[idx++] = size[i];
		}

		for (int i = 0; i < data.size(); i++) {
			out[idx++] = data.get(i);
		}

		for (int i = 0; i < thumbnail.length; i++) {
			out[idx++] = thumbnail[i];
		}

		return out;
	}
}
