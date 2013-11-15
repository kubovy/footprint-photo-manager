package com.poterion.potpho.test;

import java.util.ArrayList;
import java.util.List;

public class ExifImageFileDirectory {
	private final List<ExifTag> tags;
	private final int nextIdfOffset;

	public ExifImageFileDirectory(byte[] content, int offset, int alignment) {
		int number = ExifSegment.getInteger(content, offset, 2, alignment);
		System.out.println(String.format("IFD has %s tags.", number));
		tags = new ArrayList<ExifTag>(number);
		int position = offset + 2;
		for(int i=0; i < number; i++) {
			tags.add(new ExifTag(content, position, alignment));
			position += 12;
		}
		nextIdfOffset = ExifSegment.getInteger(content, position, 4, alignment);
	}

	public final ExifTag[] getTags() {
		return tags.toArray(new ExifTag[]{});
	}
	
	public final ExifTag getTag(int number) {
		for(ExifTag tag : tags) {
			if (tag.getNumber().number == number) {
				return tag;
			}
		}
		return null;
	}

	public final int getNextIdfOffset() {
		return nextIdfOffset;
	}
}
