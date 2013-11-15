package com.poterion.potpho.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExifSegment {
	public static final boolean isExif(byte[] content) {
		return content.length > 6
				&& content[0] == (byte) 0x45//E
				&& content[1] == (byte) 0x78//x
				&& content[2] == (byte) 0x69//i
				&& content[3] == (byte) 0x66//f
				&& content[4] == (byte) 0x00
				&& content[5] == (byte) 0x00;
	}
	
	public static final int getAlign(byte[] content) {
		if (isExif(content)) {
			if (content[6] == (byte) 0x49 && content[7] == (byte) 0x49 // II
					&& content[8] == (byte) 0x2A && content[9] == (byte) 0x00) { // 0x002A in  Intel alignment
				return INTEL;
			}
			if (content[6] == (byte) 0x4d && content[7] == (byte) 0x4d // MM
					&& content[8] == (byte) 0x00 && content[9] == (byte) 0x2A) { // 0x002A in  Motorola alignment
				return MOTOROLA;
			}
		}
		return -1;
	}

	public static final ExifSegment getInstance(byte[] content) {
		int alignment;
		if (isExif(content) && (alignment = getAlign(content)) != -1) {
			byte[] newContent = new byte[content.length-6];
			for (int i=6; i<content.length; i++) {
				newContent[i-6] = content[i];
			}
			return new ExifSegment(alignment, newContent);
		}
		return null;
	}

	protected static final byte[] getBytes(byte[] content, int offset, int length) {
		return getBytes(content, offset, length, MOTOROLA);
	}

	private static final byte[] int2byte(int integer, int count, int alignment) {
		byte[] bytes = ByteBuffer.allocate(4).putInt(integer).array();
		byte[] out = new byte[count];
		for(int i=(4-count); i < 4; i++) {
			out[i-(4-count)] = bytes[i];
		}
		if (alignment == INTEL) {
			for(int i=0; i<out.length/2; i++) {
				if (i != out.length - i - 1) {
					out[i] = (byte) (out[i] + out[out.length-i-1]);
					out[out.length-i-1] = (byte) (out[i] - out[out.length-i-1]);
					out[i] = (byte) (out[i] - out[out.length-i-1]);
				}
			}
		}
		return out;
	}

	protected static final byte[] getBytes(byte[] content, int offset, int length, int alignment) {
		byte[] out = new byte[length];
		for (int i=0; i<length; i++) {
			out[i] = content[i + offset];
		}
		if (alignment == INTEL) {
			for(int i=0; i<out.length/2; i++) {
				if (i != out.length - i - 1) {
					out[i] = (byte) (out[i] + out[out.length-i-1]);
					out[out.length-i-1] = (byte) (out[i] - out[out.length-i-1]);
					out[i] = (byte) (out[i] - out[out.length-i-1]);
				}
			}
		}
		return out;
	}

	protected static final int getInteger(byte[] content, int offset, int length, int alignment) {
		int number = 0;
		byte[] out = getBytes(content, offset, length, alignment);
		for (int i=0; i < length; i++) {
			number = number | (out[i] & 0xFF) << ((length-i-1)*8);
		}
		return number;
	}

	protected static final long getNumber(byte[] content, int offset, int length, int alignment, boolean signed) {
		long number = 0;
		byte[] out = getBytes(content, offset, length, alignment);
		for (int i=0; i < length; i++) {
			number = number | (out[i] & (signed ? 0x00 : 0xFF)) << ((length-i-1)*8);
		}
		return number;
	}

	private final static int INTEL = 0;
	private final static int MOTOROLA = 1;
	private final int alignment;
	private final byte[] content;
	private final List<ExifImageFileDirectory> ifds = new ArrayList<ExifImageFileDirectory>(2);
	private final ExifImageFileDirectory exifSubIdf;

	private ExifSegment(int alignment, byte[] content) {
		this.alignment = alignment;
		this.content = content;
		int ifdOffset = getInteger(this.content, 4, 4, this.alignment);

		while(ifdOffset > 0) {
			System.out.println("IFD Offset: " + ifdOffset);
			ExifImageFileDirectory ifd =  new ExifImageFileDirectory(this.content, ifdOffset, this.alignment);
			ifdOffset = ifd.getNextIdfOffset();
			ifds.add(ifd);
		}
		Long offset = ifds.get(0).getTag(ExifTag.EXIF_OFFSET).getLong();
		System.out.println("Exif SubIFD offset: " + offset);
		exifSubIdf = new ExifImageFileDirectory(this.content, offset.intValue(), this.alignment);
	}
	
	public final byte[] getBytes() {
		List<Byte> data = new ArrayList<Byte>(Arrays.asList(new Byte[]{
				0x45, 0x78, 0x69, 0x66, 0x00, 0x00, // Exif..
				(byte) (this.alignment == INTEL ? 0x49 : 0x4d), // I or M
				(byte) (this.alignment == INTEL ? 0x49 : 0x4d), // I or M
				(byte) (this.alignment == INTEL ? 0x2A : 0x00), // 0x002A
				(byte) (this.alignment == INTEL ? 0x00 : 0x2A),
				(byte) (this.alignment == INTEL ? 0x08 : 0x00), // Offset
				0x00, 0x00,
				(byte) (this.alignment == INTEL ? 0x00 : 0x08),
		}));
		int position = 8;
		
		for(ExifImageFileDirectory ifd : new ExifImageFileDirectory[]{ifds.get(0), exifSubIdf, ifds.get(1)}) {
			ExifTag[] tags = ifd.getTags();
			int dataAreaOffset = position + 6 + (tags.length * 12);

			for(byte b : int2byte(tags.length, 2, alignment)) {
				data.add(b);
				position++;
			}

			List<Byte> dataArea = new ArrayList<Byte>();
			for (ExifTag tag : tags) {
				// Number
				for(byte b : int2byte(tag.getNumber().number, 2, alignment)) {
					data.add(b);
					position++;
				}
				// Format
				for(byte b : int2byte(tag.getFormat().value, 2, alignment)) {
					data.add(b);
					position++;
				}
				// Components
				for(byte b : int2byte(tag.getFormat().value, 4, alignment)) {
					data.add(b);
					position++;
				}
				// Data Value
				byte[] value;
				if (tag.getDataOffset() == -1) {
					value = tag.getData();
				} else {
					value = int2byte(tag.getDataOffset(), 4, alignment); // TODO Update offset
					for(byte b : tag.getData()) {
						dataArea.add(b);
						dataAreaOffset++;
					}
				}
				for(byte b : value) {
					data.add(b);
					position++;
				}
			}
			
			for(byte b : int2byte(ifd.getNextIdfOffset(), 4, alignment)) {
				data.add(b);
				position++;
			}
			
			data.addAll(dataArea);
			position += dataArea.size();
		}
		
		byte[] out = new byte[data.size()+4];
		out[0] = (byte) 0xFF;
		out[1] = (byte) 0xE1;
		byte[] size = int2byte(data.size(), 2, alignment);
		for(int i=0; i < size.length; i++) {
			out[i+2] = size[i];
		}
		for(int i=0; i < data.size(); i++) {
			out[i+4] = data.get(i);
		}
		return out;
	}
}
