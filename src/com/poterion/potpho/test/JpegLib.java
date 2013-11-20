package com.poterion.potpho.test;

import java.nio.ByteBuffer;

public class JpegLib {
	public final static int INTEL = 0;
	public final static int MOTOROLA = 1;

	public static final byte[] int2byte(int integer, int count) {
		return int2byte(integer, count, MOTOROLA);
	}

	public static final byte[] int2byte(int integer, int count, int alignment) {
		byte[] bytes = ByteBuffer.allocate(4).putInt(integer).array();
		byte[] out = new byte[count];
		for (int i = (4 - count); i < 4; i++) {
			out[i - (4 - count)] = bytes[i];
		}
		if (alignment == INTEL) {
			for (int i = 0; i < out.length / 2; i++) {
				if (i != out.length - i - 1) {
					out[i] = (byte) (out[i] + out[out.length - i - 1]);
					out[out.length - i - 1] = (byte) (out[i] - out[out.length - i - 1]);
					out[i] = (byte) (out[i] - out[out.length - i - 1]);
				}
			}
		}
		return out;
	}

	public static final int byte2int(byte[] content, int offset, int length, int alignment) {
		int number = 0;
		byte[] out = bytes(content, offset, length, alignment);
		for (int i = 0; i < length; i++) {
			number = number | (out[i] & 0xFF) << ((length - i - 1) * 8);
		}
		return number;
	}

	public static final long byte2long(byte[] content, int offset, int length, int alignment, boolean signed) {
		long number = 0;
		byte[] out = bytes(content, offset, length, alignment);
		for (int i = 0; i < length; i++) {
			number = number | (out[i] & (signed ? 0x00 : 0xFF)) << ((length - i - 1) * 8);
		}
		return number;
	}

	public static final byte[] bytes(byte[] content, int offset, int length) {
		return bytes(content, offset, length, JpegLib.MOTOROLA);
	}

	public static final byte[] bytes(byte[] content, int offset, int length, int alignment) {
		byte[] out = new byte[length];
		for (int i = 0; i < length; i++) {
			out[i] = content[i + offset];
		}
		if (alignment == JpegLib.INTEL) {
			for (int i = 0; i < out.length / 2; i++) {
				if (i != out.length - i - 1) {
					out[i] = (byte) (out[i] + out[out.length - i - 1]);
					out[out.length - i - 1] = (byte) (out[i] - out[out.length - i - 1]);
					out[i] = (byte) (out[i] - out[out.length - i - 1]);
				}
			}
		}
		return out;
	}
}
