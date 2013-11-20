package com.poterion.potpho.test;

import java.io.IOException;

public class WriteExifMetadataExample {
	private final static String image1 = "C:\\Users\\jkubovy\\Downloads\\Temp\\2013-11-01 - Budapest\\2013-11-01 - 0002.JPG";
	private final static String image2 = "C:\\Users\\jkubovy\\Downloads\\Temp\\2013-11-01 - Budapest\\2013-11-01 - 0002-out.JPG";

	public static void main(String... args) throws IOException {
		JpegImage image = new JpegImage(image1);
		image.read();
		// ExifSegment exif = image.getExifSegment();
		image.write(image2);
	}
}
