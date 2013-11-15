package com.poterion.potpho.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExifTag {
	protected static class Format {
		protected final int value;
		private final int bytesPerComponent;
		private final String description;

		private Format(int value, int bytesPerComponent, String description) {
			this.value = value;
			this.bytesPerComponent = bytesPerComponent;
			this.description = description;
		}
	}
	
	protected static class Number {
		protected final int number;
		private final Format format;
		private final int components;
		private final String name;

		private Number(int number, Format format, int components, String name) {
			this.number = number;
			this.format = format;
			this.components = components;
			this.name = name;
		}
		
		private final byte[] getNumberBytes() {
			byte[] out = new byte[2];
			byte[] sizeBytes = ByteBuffer.allocate(4).putInt(number).array();
			for(int i=2; i < 4; i++) {
				out[i-2] = sizeBytes[i];
			}
			return out;
		}
	}

	public final static Format UNSIGNED_BYTE = new Format( 1, 1, "Unsigned Byte");
	public final static Format ASCII_STRING = new Format( 2, 1, "ASCII String");
	public final static Format UNSIGNED_SHORT = new Format( 3, 2, "Unsigned Short");
	public final static Format UNSIGNED_LONG= new Format( 4, 4, "Unsigned Long");
	public final static Format UNSIGNED_RATIONAL = new Format( 5, 8, "Unsigned Rational");
	public final static Format SIGNED_BYTE = new Format( 6, 1, "Signed Byte");
	public final static Format UNDEFINED = new Format( 7, 1, "Undefined");
	public final static Format SIGNED_SHORT = new Format( 8, 2, "Signed Short");
	public final static Format SIGNED_LONG = new Format( 9, 4, "Signed Long");
	public final static Format SIGNED_RATIONAL = new Format(10, 8, "Signed Rational");
	public final static Format SINGLE_FLOAT = new Format(11, 4, "Single Float");
	public final static Format DOUBLE_FLOAT = new Format(12, 8, "Double Float");
	
	public final static Format[] FORMATS = new Format[] {
		null,
		UNSIGNED_BYTE, ASCII_STRING, UNSIGNED_SHORT, UNSIGNED_LONG, UNSIGNED_RATIONAL,
		SIGNED_BYTE, UNDEFINED, SIGNED_SHORT, SIGNED_LONG, SIGNED_RATIONAL,
		SINGLE_FLOAT, DOUBLE_FLOAT
	};

	public final static int EXIF_OFFSET = 0x8787;
	public final static Map<Integer, Number> NUMBERS = new HashMap<Integer, Number>();
	static {
		// Tags used by IFD0 (main image)
		NUMBERS.put(0x010E, new Number(0x010E, ASCII_STRING, -1, "ImageDescription"));
		NUMBERS.put(0x010F, new Number(0x010F, ASCII_STRING, -1, "Make"));
		NUMBERS.put(0x0110, new Number(0x0110, ASCII_STRING, -1, "Model"));
		NUMBERS.put(0x0112, new Number(0x0112, UNSIGNED_SHORT, 1, "Orientation"));
		NUMBERS.put(0x011A, new Number(0x011A, UNSIGNED_RATIONAL, 1, "XResolution"));
		NUMBERS.put(0x011B, new Number(0x011B, UNSIGNED_RATIONAL, 1, "YResolution"));
		NUMBERS.put(0x0128, new Number(0x0128, UNSIGNED_SHORT, 1, "ResolutionUnit"));
		NUMBERS.put(0x0131, new Number(0x0131, ASCII_STRING, -1, "Software"));
		NUMBERS.put(0x0132, new Number(0x0132, ASCII_STRING, 20, "DateTime"));
		NUMBERS.put(0x013E, new Number(0x013E, UNSIGNED_RATIONAL, 2, "WhitePoint"));
		NUMBERS.put(0x013F, new Number(0x013F, UNSIGNED_RATIONAL, 6, "PrimaryChromaticities"));
		NUMBERS.put(0x0211, new Number(0x0211, UNSIGNED_RATIONAL, 3, "YCbCrCoefficients"));
		NUMBERS.put(0x0213, new Number(0x0213, UNSIGNED_SHORT, 1, "YCbCrPositioning"));
		NUMBERS.put(0x0214, new Number(0x0214, UNSIGNED_RATIONAL, 6, "ReferenceBlackWhite"));
		NUMBERS.put(0x8298, new Number(0x8298, ASCII_STRING, -1, "Copyright"));
		NUMBERS.put(0x8769, new Number(0x8787, UNSIGNED_LONG, 1, "ExifOffset"));
		// Tags used by Exif SubIFD
		NUMBERS.put(0x829A, new Number(0x829A, UNSIGNED_RATIONAL, 1, "ExposureTime"));
		NUMBERS.put(0x829D, new Number(0x829D, UNSIGNED_RATIONAL, 1, "FNumber"));
		NUMBERS.put(0x8822, new Number(0x8822, UNSIGNED_SHORT, 1, "ExposureProgram"));
		NUMBERS.put(0x8827, new Number(0x8827, UNSIGNED_SHORT, 2, "ISOSpeedRatings"));
		NUMBERS.put(0x9000, new Number(0x9000, UNDEFINED, 4, "ExifVersion"));
		NUMBERS.put(0x9003, new Number(0x9003, ASCII_STRING, 20, "DateTimeOriginal"));
		NUMBERS.put(0x9004, new Number(0x9004, ASCII_STRING, 20, "DateTimeDigitized"));
		NUMBERS.put(0x9101, new Number(0x9101, UNDEFINED, -1, "ComponentConfiguration"));
		NUMBERS.put(0x9102, new Number(0x9102, UNSIGNED_RATIONAL, 1, "CompressedBitsPerPixel"));
		NUMBERS.put(0x9201, new Number(0x9201, SIGNED_RATIONAL, 1, "ShutterSpeedValue"));
		NUMBERS.put(0x9202, new Number(0x9202, UNSIGNED_RATIONAL, 1, "ApertureValue"));
		NUMBERS.put(0x9203, new Number(0x9203, SIGNED_RATIONAL, 1, "BrightnessValue"));
		NUMBERS.put(0x9204, new Number(0x9204, SIGNED_RATIONAL, 1, "ExposureBiasValue"));
		NUMBERS.put(0x9205, new Number(0x9205, UNSIGNED_RATIONAL, 1, "MaxApertureValue"));
		NUMBERS.put(0x9206, new Number(0x9206, SIGNED_RATIONAL, 1, "SubjectDistance"));
		NUMBERS.put(0x9207, new Number(0x9207, UNSIGNED_SHORT, 1, "MeteringMode"));
		NUMBERS.put(0x9208, new Number(0x9208, UNSIGNED_SHORT, 1, "LightSource"));
		NUMBERS.put(0x9209, new Number(0x9209, UNSIGNED_SHORT, 1, "Flash"));
		NUMBERS.put(0x920A, new Number(0x920A, UNSIGNED_RATIONAL, 1, "FocalLength"));
		NUMBERS.put(0x927C, new Number(0x927C, UNDEFINED, -1, "MakerNote"));
		NUMBERS.put(0x9286, new Number(0x9286, UNDEFINED, -1, "UserComment"));
		NUMBERS.put(0xA000, new Number(0xA000, UNDEFINED, 4, "FlashPixVersion"));
		NUMBERS.put(0xA001, new Number(0xA001, UNSIGNED_SHORT, 1, "ColorSpace"));
		NUMBERS.put(0xA002, new Number(0xA002, UNSIGNED_LONG, 1, "ExifImageWidth"));
		NUMBERS.put(0xA003, new Number(0xA003, UNSIGNED_LONG, 1, "ExifImageHeight"));
		NUMBERS.put(0xA004, new Number(0xA004, ASCII_STRING, -1, "RelatedSoundFile"));
		NUMBERS.put(0xA005, new Number(0xA005, UNSIGNED_LONG, 1, "ExifInteroperabilityOffset"));
		NUMBERS.put(0xA20E, new Number(0xA20E, UNSIGNED_RATIONAL, 1, "FocalPlaneXResolution"));
		NUMBERS.put(0xA20F, new Number(0xA20F, UNSIGNED_RATIONAL, 1, "FocalPlaneYResolution"));
		NUMBERS.put(0xA210, new Number(0xA210, UNSIGNED_SHORT, 1, "FocalPlaneResolutionUnit"));
		NUMBERS.put(0xA217, new Number(0xA217, UNSIGNED_SHORT, 1, "SensingMethod"));
		NUMBERS.put(0xA300, new Number(0xA300, UNDEFINED, 1, "FileSource"));
		NUMBERS.put(0xA301, new Number(0xA301, UNDEFINED, 1, "SceneType"));
		// Tags used by IFD1 (thumbnail image)
		NUMBERS.put(0x0100, new Number(0x0100, UNSIGNED_LONG, 1, "ImageWidth"));
		NUMBERS.put(0x0101, new Number(0x0101, UNSIGNED_LONG, 1, "ImageLength"));
		NUMBERS.put(0x0102, new Number(0x0102, UNSIGNED_SHORT, 3, "BitsPerSample"));
		NUMBERS.put(0x0103, new Number(0x0103, UNSIGNED_SHORT, 1, "Compression"));
		NUMBERS.put(0x0106, new Number(0x0106, UNSIGNED_SHORT, 1, "PhotometricInterpretation"));
		NUMBERS.put(0x0111, new Number(0x0111, UNSIGNED_LONG, -1, "StripOffsets"));
		NUMBERS.put(0x0115, new Number(0x0115, UNSIGNED_SHORT, 1, "SamplesPerPixel"));
		NUMBERS.put(0x0116, new Number(0x0116, UNSIGNED_LONG, 1, "RowsPerStrip"));
		NUMBERS.put(0x0117, new Number(0x0117, UNSIGNED_LONG, -1, "StripByteConunts"));
		NUMBERS.put(0x011A, new Number(0x011A, UNSIGNED_RATIONAL, 1, "XResolution"));
		NUMBERS.put(0x011B, new Number(0x011B, UNSIGNED_RATIONAL, 1, "YResolution"));
		NUMBERS.put(0x011C, new Number(0x011C, UNSIGNED_SHORT, 1, "PlanarConfiguration"));
		NUMBERS.put(0x0128, new Number(0x0128, UNSIGNED_SHORT, 1, "ResolutionUnit"));
		NUMBERS.put(0x0201, new Number(0x0201, UNSIGNED_LONG, 1, "JpegIFOffset"));
		NUMBERS.put(0x0202, new Number(0x0202, UNSIGNED_LONG, 1, "JpegIFByteCount"));
		NUMBERS.put(0x0211, new Number(0x0211, UNSIGNED_RATIONAL, 3, "YCbCrCoefficients"));
		NUMBERS.put(0x0212, new Number(0x0212, UNSIGNED_SHORT, 2, "YCbCrSubSampling"));
		NUMBERS.put(0x0213, new Number(0x0213, UNSIGNED_SHORT, 1, "YCbCrPositioning"));
		NUMBERS.put(0x0214, new Number(0x0214, UNSIGNED_RATIONAL, 6, "ReferenceBlackWhite"));
		// Misc Tags
		NUMBERS.put(0x00FE, new Number(0x00FE, UNSIGNED_LONG, 1, "SubfileType"));
		NUMBERS.put(0x00FF, new Number(0x00FF, UNSIGNED_SHORT, 1, "SubfileType"));
		NUMBERS.put(0x012D, new Number(0x012D, UNSIGNED_SHORT, 3, "TransferFunction"));
		NUMBERS.put(0x013B, new Number(0x013B, ASCII_STRING, -1, "Artist"));
		NUMBERS.put(0x013D, new Number(0x013D, UNSIGNED_SHORT, 1, "Predictor"));
		NUMBERS.put(0x0142, new Number(0x0142, UNSIGNED_SHORT, 1, "TileWidth"));
		NUMBERS.put(0x0143, new Number(0x0143, UNSIGNED_SHORT, 1, "TileLength"));
		NUMBERS.put(0x0144, new Number(0x0144, UNSIGNED_LONG, -1, "TileOffsets"));
		NUMBERS.put(0x0145, new Number(0x0145, UNSIGNED_SHORT, -1, "TileByteCounts"));
		NUMBERS.put(0x014A, new Number(0x014A, UNSIGNED_LONG, -1, "SubIFDs"));
		NUMBERS.put(0x015B, new Number(0x015B, UNDEFINED, -1, "JPEGTables"));
		NUMBERS.put(0x828D, new Number(0x828D, UNSIGNED_SHORT, 2, "CFARepeatPatternDim"));
		NUMBERS.put(0x828E, new Number(0x828E, UNSIGNED_BYTE, -1, "CFAPattern"));
		NUMBERS.put(0x828F, new Number(0x828F, UNSIGNED_RATIONAL, 1, "BatteryLevel"));
		NUMBERS.put(0x83BB, new Number(0x83BB, UNSIGNED_LONG, -1, "IPTC/NAA "));
		NUMBERS.put(0x8773, new Number(0x8773, UNDEFINED, -1, "InterColorProfile"));
		NUMBERS.put(0x8824, new Number(0x8824, ASCII_STRING, -1, "SpectralSensitivity"));
		NUMBERS.put(0x8825, new Number(0x8825, UNSIGNED_LONG, 1, "GPSInfo"));
		NUMBERS.put(0x8828, new Number(0x8828, UNDEFINED, -1, "OECF"));
		NUMBERS.put(0x8829, new Number(0x8829, UNSIGNED_SHORT, 1, "Interlace"));
		NUMBERS.put(0x882A, new Number(0x882a, SIGNED_SHORT, 1, "TimeZoneOffset"));
		NUMBERS.put(0x882B, new Number(0x882B, UNSIGNED_SHORT, 1, "SelfTimerMode"));
		NUMBERS.put(0x920B, new Number(0x920B, UNSIGNED_RATIONAL, 1, "FlashEnergy"));
		NUMBERS.put(0x920C, new Number(0x920C, UNDEFINED, -1, "SpatialFrequencyResponse"));
		NUMBERS.put(0x920D, new Number(0x920D, UNDEFINED, -1, "Noise"));
		NUMBERS.put(0x9211, new Number(0x9211, UNSIGNED_LONG, 1, "ImageNumber"));
		NUMBERS.put(0x9212, new Number(0x9212, ASCII_STRING, 1, "SecurityClassification"));
		NUMBERS.put(0x9213, new Number(0x9213, ASCII_STRING, -1, "ImageHistory"));
		NUMBERS.put(0x9214, new Number(0x9214, UNSIGNED_SHORT, 4, "SubjectLocation"));
		NUMBERS.put(0x9215, new Number(0x9215, UNSIGNED_RATIONAL, 1, "ExposureIndex"));
		NUMBERS.put(0x9216, new Number(0x9216, UNSIGNED_BYTE, 4, "TIFF/EPStandardID "));
		NUMBERS.put(0x9290, new Number(0x9290, ASCII_STRING, -1, "SubSecTime"));
		NUMBERS.put(0x9291, new Number(0x9291, ASCII_STRING, -1, "SubSecTimeOriginal"));
		NUMBERS.put(0x9292, new Number(0x9292, ASCII_STRING, -1, "SubSecTimeDigitized"));
		NUMBERS.put(0xA20B, new Number(0xA20B, UNSIGNED_RATIONAL, 1, "FlashEnergy"));
		NUMBERS.put(0xA20C, new Number(0xA20C, UNSIGNED_SHORT, 1, "SpatialFrequencyResponse"));
		NUMBERS.put(0xA214, new Number(0xA214, UNSIGNED_SHORT, 1, "SubjectLocation"));
		NUMBERS.put(0xA215, new Number(0xA215, UNSIGNED_RATIONAL, 1, "ExposureIndex"));
		NUMBERS.put(0xA302, new Number(0xA302, UNDEFINED, 1, "CFAPattern"));
		//
		NUMBERS.put(0x8828, new Number(0x8828, UNDEFINED, 1, "OECF"));
		NUMBERS.put(0x9214, new Number(0x9214, UNDEFINED, 1, "SubjectArea"));
		NUMBERS.put(0x9290, new Number(0x9290, UNDEFINED, 1, "SubsecTime"));
		NUMBERS.put(0x9291, new Number(0x9291, UNDEFINED, 1, "SubsecTimeOriginal"));
		NUMBERS.put(0x9292, new Number(0x9292, UNDEFINED, 1, "SubsecTimeDigitized"));
		NUMBERS.put(0xA401, new Number(0xA401, UNDEFINED, 1, "CustomRendered"));
		NUMBERS.put(0xA402, new Number(0xA402, UNDEFINED, 1, "ExposureMode"));
		NUMBERS.put(0xA403, new Number(0xA403, UNDEFINED, 1, "WhiteBalance"));
		NUMBERS.put(0xA404, new Number(0xA404, UNDEFINED, 1, "DigitalZoomRatio"));
		NUMBERS.put(0xA405, new Number(0xA405, UNDEFINED, 1, "FocalLengthIn35mmFilm"));
		NUMBERS.put(0xA406, new Number(0xA406, UNDEFINED, 1, "SceneCaptureType"));
		NUMBERS.put(0xA407, new Number(0xA407, UNDEFINED, 1, "GainControl"));
		NUMBERS.put(0xA408, new Number(0xA408, UNDEFINED, 1, "Contrast"));
		NUMBERS.put(0xA409, new Number(0xA409, UNDEFINED, 1, "Saturation"));
		NUMBERS.put(0xA40A, new Number(0xA40A, UNDEFINED, 1, "Sharpness"));
		NUMBERS.put(0xA40B, new Number(0xA40B, UNDEFINED, 1, "DeviceSettingDescription"));
		NUMBERS.put(0xA40C, new Number(0xA40C, UNDEFINED, 1, "SubjectDistanceRange"));
		NUMBERS.put(0xA420, new Number(0xA420, UNDEFINED, 1, "ImageUniqueID"));
	};
	
	private final Number number;
	private final Format format;
	private final int components;
	private final int dataOffset;
	private final byte[] data;
	private final int alignment;

	public ExifTag(byte[] content, int offset, int alignment) {
		int number = ExifSegment.getInteger(content, offset, 2, alignment);
		this.number = NUMBERS.containsKey(number)
				? NUMBERS.get(number)
				: new Number(number, UNDEFINED, 1, "UNDEFINED"); 
		this.format = FORMATS[ExifSegment.getInteger(content, offset+2, 2, alignment)];
		this.components = ExifSegment.getInteger(content, offset+4, 4, alignment);
		this.alignment = alignment;
		int totalBytes = this.components * format.bytesPerComponent;
		if (totalBytes > 4) {
			this.dataOffset = ExifSegment.getInteger(content, offset+8, 4, alignment);
			this.data = ExifSegment.getBytes(content, this.dataOffset, totalBytes);
		} else {
			this.dataOffset = -1;
			this.data = ExifSegment.getBytes(content, offset+8, totalBytes);
		}

		String formatStr = (this.number.format.equals(format)
				? this.format.description
				: String.format("Format: %s/Number format: %s", this.format.description, this.number.format.description));
		System.out.println(String.format(
				"IFD TAG: %s [0x%04X]: \"%s\" (%s; Components: %d, Bytes per component: %d, Total: %d)",
				this.number.name, this.number.number, getData(), formatStr, this.components, this.format.bytesPerComponent, totalBytes));
	}

	public final Number getNumber() {
		return number;
	}
	
	public final Format getFormat() {
		return format;
	}

	public final int getDataOffset() {
		return dataOffset;
	}
	
	public final byte[] getData() {
		return data;
	}
	
	private final List<Long> getLongs() {
		boolean signed = (format.equals(SIGNED_BYTE) || format.equals(SIGNED_SHORT) || format.equals(SIGNED_LONG));
		List<Long> out = new ArrayList<Long>();
		for(int i=0; i < components; i++) {
			Long number = ExifSegment.getNumber(data, i * format.bytesPerComponent, format.bytesPerComponent, alignment, signed);
			out.add(number);
		}
		return out;
	}
	
	public final Long getLong() {
		return getLongs().get(0);
	}

	public final Object getObject() {
		if (format.equals(ASCII_STRING)) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i < data.length; i++) {
				if (data[i] != 0x00) { 
					sb.append((char) data[i]);
				} else {
					return sb.toString();
				}
			}
			return sb.toString();
		} else if (format.equals(SIGNED_BYTE)
				|| format.equals(UNSIGNED_BYTE)
				|| format.equals(SIGNED_SHORT)
				|| format.equals(UNSIGNED_SHORT)
				|| format.equals(SIGNED_LONG)
				|| format.equals(UNSIGNED_LONG)) {
			List<Long> out = getLongs();
			if (out.size() == 1) {
				return out.get(0);
			} else {
				return out;
			}
		} else if (format.equals(SIGNED_RATIONAL)) {
		} else if (format.equals(UNSIGNED_RATIONAL)) {
		} else if (format.equals(SINGLE_FLOAT)) {
		} else if (format.equals(DOUBLE_FLOAT)) {
		} else if (format.equals(UNDEFINED)) {
		} else {
		}
		return data;
	}
}
