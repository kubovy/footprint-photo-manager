package com.poterion.potpho.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpegMarker {
	private final static String[][] MARKERS = new String[][] {
		{ "00", "INV", "INVALID" },
		{ "01", "TEM*", "For temporary private use in arithmetic coding" },
		// Reserved
		{ "02", "RES", "Reserved" },
		{ "03", "RES", "Reserved" },
		{ "04", "RES", "Reserved" },
		{ "05", "RES", "Reserved" },
		{ "06", "RES", "Reserved" },
		{ "07", "RES", "Reserved" },
		{ "08", "RES", "Reserved" },
		{ "09", "RES", "Reserved" },
		{ "0A", "RES", "Reserved" },
		{ "0B", "RES", "Reserved" },
		{ "0C", "RES", "Reserved" },
		{ "0D", "RES", "Reserved" },
		{ "0E", "RES", "Reserved" },
		{ "0F", "RES", "Reserved" },
		{ "10", "RES", "Reserved" },
		{ "11", "RES", "Reserved" },
		{ "12", "RES", "Reserved" },
		{ "13", "RES", "Reserved" },
		{ "14", "RES", "Reserved" },
		{ "15", "RES", "Reserved" },
		{ "16", "RES", "Reserved" },
		{ "17", "RES", "Reserved" },
		{ "18", "RES", "Reserved" },
		{ "19", "RES", "Reserved" },
		{ "1A", "RES", "Reserved" },
		{ "1B", "RES", "Reserved" },
		{ "1C", "RES", "Reserved" },
		{ "1D", "RES", "Reserved" },
		{ "1E", "RES", "Reserved" },
		{ "1F", "RES", "Reserved" },
		{ "20", "RES", "Reserved" },
		{ "21", "RES", "Reserved" },
		{ "22", "RES", "Reserved" },
		{ "23", "RES", "Reserved" },
		{ "24", "RES", "Reserved" },
		{ "25", "RES", "Reserved" },
		{ "26", "RES", "Reserved" },
		{ "27", "RES", "Reserved" },
		{ "28", "RES", "Reserved" },
		{ "29", "RES", "Reserved" },
		{ "2A", "RES", "Reserved" },
		{ "2B", "RES", "Reserved" },
		{ "2C", "RES", "Reserved" },
		{ "2D", "RES", "Reserved" },
		{ "2E", "RES", "Reserved" },
		{ "2F", "RES", "Reserved" },
		{ "30", "RES", "Reserved" },
		{ "31", "RES", "Reserved" },
		{ "32", "RES", "Reserved" },
		{ "33", "RES", "Reserved" },
		{ "34", "RES", "Reserved" },
		{ "35", "RES", "Reserved" },
		{ "36", "RES", "Reserved" },
		{ "37", "RES", "Reserved" },
		{ "38", "RES", "Reserved" },
		{ "39", "RES", "Reserved" },
		{ "3A", "RES", "Reserved" },
		{ "3B", "RES", "Reserved" },
		{ "3C", "RES", "Reserved" },
		{ "3D", "RES", "Reserved" },
		{ "3E", "RES", "Reserved" },
		{ "3F", "RES", "Reserved" },
		{ "40", "RES", "Reserved" },
		{ "41", "RES", "Reserved" },
		{ "42", "RES", "Reserved" },
		{ "43", "RES", "Reserved" },
		{ "44", "RES", "Reserved" },
		{ "45", "RES", "Reserved" },
		{ "46", "RES", "Reserved" },
		{ "47", "RES", "Reserved" },
		{ "48", "RES", "Reserved" },
		{ "49", "RES", "Reserved" },
		{ "4A", "RES", "Reserved" },
		{ "4B", "RES", "Reserved" },
		{ "4C", "RES", "Reserved" },
		{ "4D", "RES", "Reserved" },
		{ "4E", "RES", "Reserved" },
		{ "4F", "RES", "Reserved" },
		{ "50", "RES", "Reserved" },
		{ "51", "RES", "Reserved" },
		{ "52", "RES", "Reserved" },
		{ "53", "RES", "Reserved" },
		{ "54", "RES", "Reserved" },
		{ "55", "RES", "Reserved" },
		{ "56", "RES", "Reserved" },
		{ "57", "RES", "Reserved" },
		{ "58", "RES", "Reserved" },
		{ "59", "RES", "Reserved" },
		{ "5A", "RES", "Reserved" },
		{ "5B", "RES", "Reserved" },
		{ "5C", "RES", "Reserved" },
		{ "5D", "RES", "Reserved" },
		{ "5E", "RES", "Reserved" },
		{ "5F", "RES", "Reserved" },
		{ "60", "RES", "Reserved" },
		{ "61", "RES", "Reserved" },
		{ "62", "RES", "Reserved" },
		{ "63", "RES", "Reserved" },
		{ "64", "RES", "Reserved" },
		{ "65", "RES", "Reserved" },
		{ "66", "RES", "Reserved" },
		{ "67", "RES", "Reserved" },
		{ "68", "RES", "Reserved" },
		{ "69", "RES", "Reserved" },
		{ "6A", "RES", "Reserved" },
		{ "6B", "RES", "Reserved" },
		{ "6C", "RES", "Reserved" },
		{ "6D", "RES", "Reserved" },
		{ "6E", "RES", "Reserved" },
		{ "6F", "RES", "Reserved" },
		{ "70", "RES", "Reserved" },
		{ "71", "RES", "Reserved" },
		{ "72", "RES", "Reserved" },
		{ "73", "RES", "Reserved" },
		{ "74", "RES", "Reserved" },
		{ "75", "RES", "Reserved" },
		{ "76", "RES", "Reserved" },
		{ "77", "RES", "Reserved" },
		{ "78", "RES", "Reserved" },
		{ "79", "RES", "Reserved" },
		{ "7A", "RES", "Reserved" },
		{ "7B", "RES", "Reserved" },
		{ "7C", "RES", "Reserved" },
		{ "7D", "RES", "Reserved" },
		{ "7E", "RES", "Reserved" },
		{ "7F", "RES", "Reserved" },
		{ "80", "RES", "Reserved" },
		{ "81", "RES", "Reserved" },
		{ "82", "RES", "Reserved" },
		{ "83", "RES", "Reserved" },
		{ "84", "RES", "Reserved" },
		{ "85", "RES", "Reserved" },
		{ "86", "RES", "Reserved" },
		{ "87", "RES", "Reserved" },
		{ "88", "RES", "Reserved" },
		{ "89", "RES", "Reserved" },
		{ "8A", "RES", "Reserved" },
		{ "8B", "RES", "Reserved" },
		{ "8C", "RES", "Reserved" },
		{ "8D", "RES", "Reserved" },
		{ "8E", "RES", "Reserved" },
		{ "8F", "RES", "Reserved" },
		{ "90", "RES", "Reserved" },
		{ "91", "RES", "Reserved" },
		{ "92", "RES", "Reserved" },
		{ "93", "RES", "Reserved" },
		{ "94", "RES", "Reserved" },
		{ "95", "RES", "Reserved" },
		{ "96", "RES", "Reserved" },
		{ "97", "RES", "Reserved" },
		{ "98", "RES", "Reserved" },
		{ "99", "RES", "Reserved" },
		{ "9A", "RES", "Reserved" },
		{ "9B", "RES", "Reserved" },
		{ "9C", "RES", "Reserved" },
		{ "9D", "RES", "Reserved" },
		{ "9E", "RES", "Reserved" },
		{ "9F", "RES", "Reserved" },
		{ "A0", "RES", "Reserved" },
		{ "A1", "RES", "Reserved" },
		{ "A2", "RES", "Reserved" },
		{ "A3", "RES", "Reserved" },
		{ "A4", "RES", "Reserved" },
		{ "A5", "RES", "Reserved" },
		{ "A6", "RES", "Reserved" },
		{ "A7", "RES", "Reserved" },
		{ "A8", "RES", "Reserved" },
		{ "A9", "RES", "Reserved" },
		{ "AA", "RES", "Reserved" },
		{ "AB", "RES", "Reserved" },
		{ "AC", "RES", "Reserved" },
		{ "AD", "RES", "Reserved" },
		{ "AE", "RES", "Reserved" },
		{ "AF", "RES", "Reserved" },
		{ "B0", "RES", "Reserved" },
		{ "B1", "RES", "Reserved" },
		{ "B2", "RES", "Reserved" },
		{ "B3", "RES", "Reserved" },
		{ "B4", "RES", "Reserved" },
		{ "B5", "RES", "Reserved" },
		{ "B6", "RES", "Reserved" },
		{ "B7", "RES", "Reserved" },
		{ "B8", "RES", "Reserved" },
		{ "B9", "RES", "Reserved" },
		{ "BA", "RES", "Reserved" },
		{ "BB", "RES", "Reserved" },
		{ "BC", "RES", "Reserved" },
		{ "BD", "RES", "Reserved" },
		{ "BE", "RES", "Reserved" },
		{ "BF", "RES", "Reserved" },
		// Start of frame markers, non-differential, Huffman coding
		{ "C0", "SOF0", "Baseline DCT" },
		{ "C1", "SOF1", "Extended sequential DCT" },
		{ "C2", "SOF2", "Progressive DCT" },
		{ "C3", "SOF3", "Lossless (sequential)" },
		// Huffman table specification
		{ "C4", "DHT", "Define Huffman table(s)" },
		// Start of frame markers, differential, Huffman coding
		{ "C5", "SOF5", "Differential sequential DCT" },
		{ "C6", "SOF6", "Differential progressive DCT" },
		{ "C7", "SOF7", "Differential lossless (sequential)" },
		// Start of frame markers, non-differential, arithmetic coding
		{ "C8", "JPG", "Reserved for JPEG extensions" },
		{ "C9", "SOF9", "Extended sequential DCT" },
		{ "CA", "SOF10", "Progressive DCT" },
		{ "CB", "SOF11", "Lossless (sequential)" },
		// Arithmetic coding conditioning specification
		{ "CC", "DAC", "Default arithmetic coding conditioning(s)" },
		// Start Of Frame markers, differential, arithmetic coding
		{ "CD", "SOF13", "Differential sequential DCT" },
		{ "CE", "SOF14", "Differential progressive DCT" },
		{ "CF", "SOF15", "Differential lossless (sequential)" },
		// Restart interval termination
		{ "D0", "RST0*", "Restart with modulo 8 count 0" },
		{ "D1", "RST1*", "Restart with modulo 8 count 1" },
		{ "D2", "RST2*", "Restart with modulo 8 count 2" },
		{ "D3", "RST3*", "Restart with modulo 8 count 3" },
		{ "D4", "RST4*", "Restart with modulo 8 count 4" },
		{ "D5", "RST5*", "Restart with modulo 8 count 5" },
		{ "D6", "RST6*", "Restart with modulo 8 count 6" },
		{ "D7", "RST7*", "Restart with modulo 8 count 7" },
		// Other markers
		{ "D8", "SOI*", "Start of image" },
		{ "D9", "EOI*", "End of image" },
		{ "DA", "SOS", "Start of scan" },
		{ "DB", "DQT", "Define quantization table(s)" },
		{ "DC", "DNL", "Define number of lines" },
		{ "DD", "DRI", "Define restart interval" },
		{ "DE", "DHP", "Define hierarchical progression" },
		{ "DF", "EXP", "Exp and reference component(s)" },

		{ "E0", "APP00", "Reserverd for application segment 0" },
		{ "E1", "APP01", "Reserverd for application segment 1" },
		{ "E2", "APP02", "Reserverd for application segment 2" },
		{ "E3", "APP03", "Reserverd for application segment 3" },
		{ "E4", "APP04", "Reserverd for application segment 4" },
		{ "E5", "APP05", "Reserverd for application segment 5" },
		{ "E6", "APP06", "Reserverd for application segment 6" },
		{ "E7", "APP07", "Reserverd for application segment 7" },
		{ "E8", "APP08", "Reserverd for application segment 8" },
		{ "E9", "APP09", "Reserverd for application segment 9" },
		{ "EA", "APP10", "Reserverd for application segment 10" },
		{ "EB", "APP11", "Reserverd for application segment 11" },
		{ "EC", "APP12", "Reserverd for application segment 12" },
		{ "ED", "APP13", "Reserverd for application segment 13" },
		{ "EE", "APP14", "Reserverd for application segment 14" },
		{ "EF", "APP15", "Reserverd for application segment 15" },

		{ "F0", "JPG00", "Reserved for JPEG extension 0" },
		{ "F1", "JPG01", "Reserved for JPEG extension 1" },
		{ "F2", "JPG02", "Reserved for JPEG extension 2" },
		{ "F3", "JPG03", "Reserved for JPEG extension 3" },
		{ "F4", "JPG04", "Reserved for JPEG extension 4" },
		{ "F5", "JPG05", "Reserved for JPEG extension 5" },
		{ "F6", "JPG06", "Reserved for JPEG extension 6" },
		{ "F7", "JPG07", "Reserved for JPEG extension 7" },
		{ "F8", "JPG08", "Reserved for JPEG extension 8" },
		{ "F9", "JPG09", "Reserved for JPEG extension 9" },
		{ "FA", "JPG10", "Reserved for JPEG extension 10" },
		{ "FB", "JPG11", "Reserved for JPEG extension 11" },
		{ "FC", "JPG12", "Reserved for JPEG extension 12" },
		{ "FD", "JPG13", "Reserved for JPEG extension 13" },

		{ "FE", "COM", "Comment" },
		{ "FF", "INV", "INVALID" },
	};

	private final static List<Byte> ALONE = Arrays.asList(new Byte[] { (byte) 0xD0, (byte) 0xD1, (byte) 0xD2,
			(byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9 });

	private final static Map<String, String> mAbbreviation = new HashMap<String, String>(MARKERS.length);
	private final static Map<String, String> mDescription = new HashMap<String, String>(MARKERS.length);
	private final static Map<Byte, JpegMarker> instances = new HashMap<Byte, JpegMarker>(256);

	static {
		for (String[] m : MARKERS) {
			mAbbreviation.put(m[0], m[1]);
			mDescription.put(m[0], m[2]);
		}
	}

	public final static JpegMarker EOI = JpegMarker.getInstance((byte) 0xD9);
	public final static JpegMarker SOS = JpegMarker.getInstance((byte) 0xDA);

	public static JpegMarker getInstance(String abbrev) {
		byte b = 0;
		for (String[] marker : MARKERS) {
			if (marker[1].equals(abbrev)) {
				return getInstance(b);
			}
			b = (byte) (b + 1);
		}
		return null;
	}

	public static JpegMarker getInstance(byte b) {
		if (!instances.containsKey(b)) {
			instances.put(b, new JpegMarker(b));
		}
		return instances.get(b);
	}

	public final static String getAbbreviation(String marker) {
		return mAbbreviation.get(marker.toUpperCase());
	}

	public final static String getAbbreviation(byte marker) {
		return getAbbreviation(byte2String(marker));
	}

	public final static String getDescription(String marker) {
		return mDescription.get(marker.toUpperCase());
	}

	public final static String getDescription(byte marker) {
		return getDescription(byte2String(marker));
	}

	public final static boolean isAlone(JpegMarker marker) {
		return ALONE.contains(marker.getMarker());
	}

	private static String byte2String(byte b) {
		return String.format("%02X", b);
	}

	private final Byte b;

	private JpegMarker(byte b) {
		this.b = b;
	}

	public final byte getMarker() {
		return b;
	}

	public final String getAbbreviation() {
		return getAbbreviation(b);
	}

	public final String getDescription() {
		return getDescription(b);
	}

	public final byte[] get() {
		return new byte[] { (byte) 0xFF, b };
	}

	@Override
	public int hashCode() {
		return b.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JpegMarker) {
			return getMarker() == ((JpegMarker) obj).getMarker();
		}
		return false;
	}

	@Override
	public String toString() {
		return getAbbreviation() + " - " + getDescription();
	}
}
