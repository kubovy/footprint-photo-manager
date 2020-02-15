package com.poterion.footprint.manager.utils

import com.drew.lang.Rational
import com.drew.metadata.Directory
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.enums.TagValueType
import java.util.*
import kotlin.math.floor

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private fun String.detectRationalArray(defaultType: TagValueType) =
		if (contains("/")) TagValueType.RATIONAL_ARRAY else defaultType

fun Directory.getTagValueType(tagType: Int) = this.getDate(tagType)
	?.let { TagValueType.DATE }
	?: getObject(tagType).let { raw ->
		when {
			raw is Boolean -> TagValueType.BOOLEAN
			//raw is Boolean? -> TagValueType.BOOLEAN
			raw is ByteArray -> TagValueType.BYTE_ARRAY
			raw is Array<*> && raw.isArrayOf<Byte>() -> TagValueType.BYTE_ARRAY
			raw is Date -> TagValueType.DATE
			raw is Double -> TagValueType.DOUBLE
			//raw is Double? -> TagValueType.DOUBLE
			raw is Float -> TagValueType.FLOAT
			//raw is Float? -> TagValueType.FLOAT
			raw is Int -> TagValueType.INT
			//raw is Int? -> TagValueType.INT
			raw is IntArray -> TagValueType.INT_ARRAY
			raw is Array<*> && raw.isArrayOf<Int>() -> TagValueType.INT_ARRAY
			raw is Long -> TagValueType.LONG
			//raw is Long? -> TagValueType.LONG
			raw is Rational -> getString(tagType).detectRationalArray(TagValueType.DOUBLE_ARRAY)
			raw is DoubleArray -> getString(tagType).detectRationalArray(TagValueType.RATIONAL_ARRAY)
			raw is FloatArray -> getString(tagType).detectRationalArray(TagValueType.RATIONAL_ARRAY)
			raw is Array<*> && raw.isArrayOf<Double>() -> getString(tagType).detectRationalArray(TagValueType.DOUBLE_ARRAY)
			raw is Array<*> && raw.isArrayOf<Float>() -> getString(tagType).detectRationalArray(TagValueType.FLOAT_ARRAY)
			raw is Array<*> && raw.isArrayOf<Rational>() -> TagValueType.RATIONAL_ARRAY
			else -> TagValueType.STRING
		}
	}

/**
 * https://www.exiv2.org/tags.html
 */
fun isRelevant(directory: String, tagType: Int, valueType: TagValueType, raw: String?) = when (directory) {
	"AVI" -> when (tagType) {
		1 -> valueType.isNumeric && raw?.toLongOrNull() != null // Frames Per Second
		3 -> valueType == TagValueType.DATE && raw?.parseExifDurationOrNull() != null // Duration
		6 -> valueType.isInteger && raw?.toLongOrNull() != null // Width
		7 -> valueType.isInteger && raw?.toLongOrNull() != null // Height
		320 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Datetime Original
		else -> true
	}
	"Exif SubIFD" -> when (tagType) {
		256 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Width
		257 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Height
		36867 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Date/Time Original
		36868 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Date/Time Digitized
		37382 -> valueType.isNumeric && raw?.toDoubleOrNull() != null // Subject Distance
		37396 -> valueType == TagValueType.INT_ARRAY && raw?.split(" ")?.mapNotNull { it.toLongOrNull() }?.size == 4 // Subject Location
		40962 -> valueType.isInteger && raw?.toLongOrNull() != null // Exif Image Width
		40963 -> valueType.isInteger && raw?.toLongOrNull() != null // Exif Image Height
		41996 -> valueType.isInteger && raw?.toLongOrNull() != null // Subject Distance Range
		42016 -> true // Unique Image ID
		else -> false
	}
	"Exif IFD0" -> when (tagType) {
		256 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Width
		257 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Height
		274 -> valueType.isInteger && raw?.toLongOrNull() != null // Orientation
		282 -> valueType.isInteger && raw?.toLongOrNull() != null // X Resolution
		283 -> valueType.isInteger && raw?.toLongOrNull() != null // Y Resolution
		296 -> valueType.isInteger && raw?.toLongOrNull() != null // Resolution Unit
		306 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Date/Time
		315 -> valueType == TagValueType.STRING // Artist
		4097 -> valueType.isInteger && raw?.toLongOrNull() != null // Related Image Width
		4098 -> valueType.isInteger && raw?.toLongOrNull() != null // Related Image Height
		33432 -> valueType == TagValueType.STRING // Copyright
		36867 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Date/Time Original
		36868 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Date/Time Digitized
		37396 -> valueType == TagValueType.INT_ARRAY && raw?.split(" ")?.mapNotNull { it.toIntOrNull() }?.size == 4 // Subject Location
		41996 -> valueType.isInteger && raw?.toLongOrNull() != null // Subject Distance Range
		else -> false
	}
	"File Type" -> when (tagType) {
		1 -> valueType == TagValueType.STRING // Detected File Type Name
		2 -> valueType == TagValueType.STRING // Detected File Type Long Name
		3 -> valueType == TagValueType.STRING // Detected MIME Type
		else -> false
	}
	"GPS" -> when (tagType) {
		0 -> valueType == TagValueType.STRING
		1 -> valueType == TagValueType.STRING && listOf("S", "N").contains(raw) // GPS Latitude Ref
		2 -> valueType == TagValueType.RATIONAL_ARRAY && raw?.toRationalListNotNull()?.size == 3 // GPS Latitude
		3 -> valueType == TagValueType.STRING && listOf("W", "E").contains(raw) // GPS Longitude Ref
		4 -> valueType == TagValueType.RATIONAL_ARRAY && raw?.toRationalListNotNull()?.size == 3 // GPS Longitude
		5 -> valueType.isInteger && raw?.toIntOrNull() != null // GPS Altitude Ref
		6 -> valueType == TagValueType.RATIONAL && raw?.toRationalOrNull() != null // GPS Altitude
		7 -> valueType == TagValueType.RATIONAL_ARRAY && raw?.toRationalListNotNull()?.size == 3 // GPS Time-Stamp
		16 -> valueType == TagValueType.STRING && listOf("T", "M").contains(raw) // GPS Img Direction Ref
		17 -> valueType.isNumeric && raw?.toFloatingOrNull() != null // GPS Img Direction
		23 -> valueType == TagValueType.STRING && listOf("T", "M").contains(raw) // GPS Dest Bearing Ref
		24 -> valueType.isNumeric && raw?.toFloatingOrNull() != null // GPS Dest Bearing
		27 -> valueType == TagValueType.STRING || valueType == TagValueType.BYTE_ARRAY // GPS Processing Method
		29 -> valueType == TagValueType.DATE && raw?.parseExifDateOrNull() != null // GPS Date Stamp
		31 -> valueType.isNumeric && raw?.toFloatingOrNull() != null // GPS H Positioning Error
		else -> false
	}
	"ICC Profile" -> when (tagType) {
		24 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Profile Date/Time
		else -> false
	}
	"Interoperability" -> when (tagType) {
		4097 -> valueType.isInteger && raw?.toLongOrNull() != null // Related Image Width
		4098 -> valueType.isInteger && raw?.toLongOrNull() != null // Related Image Height
		else -> false
	}
	"IPTC" -> when (tagType) {
		567 -> valueType == TagValueType.DATE && raw?.parseExifDateOrNull() != null // Date Created
		572 -> valueType == TagValueType.DATE && raw?.parseExifTimeOrNull() != null // Time Created
		574 -> valueType == TagValueType.DATE && raw?.parseExifDateOrNull() != null // Digital Date Created
		575 -> valueType == TagValueType.DATE && raw?.parseExifTimeOrNull() != null // Digital Time Created
		else -> false
	}
	"JFIF" -> when (tagType) {
		7 -> valueType == TagValueType.INT && raw?.toIntOrNull() != null // Resolution Units
		8 -> valueType.isInteger && raw?.toLongOrNull() != null // X Resolution
		10 -> valueType.isInteger && raw?.toLongOrNull() != null // Y Resolution
		else -> false
	}
	"JPEG" -> when (tagType) {
		1 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Height
		3 -> valueType.isInteger && raw?.toLongOrNull() != null // Image Width
		else -> false
	}
	"MP4" -> when (tagType) {
		256 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Creation Time
		257 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Modification Time
		258 -> valueType.isInteger && raw?.toLongOrNull() != null // Media Time Scale
		259 -> valueType.isInteger && raw?.toLongOrNull() != null // Duration
		260 -> valueType.isNumeric && raw?.toFloatingOrNull() != null // Duration in Seconds
		512 -> valueType.isNumeric && raw?.toDoubleOrNull() != null // Rotation
		else -> false
	}
	"MP4 Metadata" -> when (tagType) {
		101 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Creation Time
		102 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Modification Time
		else -> false
	}
	"MP4 Sound" -> when (tagType) {
		101 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Creation Time
		102 -> valueType == TagValueType.DATE && raw?.parseExifDateTimeOrNull() != null // Modification Time
		else -> false
	}
	"MP4 Video" -> when (tagType) {
		101 -> valueType == TagValueType.STRING //&& raw?.parseExifDateOrNull() != null // Creation Time
		102 -> valueType == TagValueType.STRING //&& raw?.parseExifDateOrNull() != null // Modification Time
		204 -> valueType.isInteger && raw?.toLongOrNull() != null // Width
		205 -> valueType.isInteger && raw?.toLongOrNull() != null // Height
		206 -> valueType.isInteger && raw?.toLongOrNull() != null // Horizontal Resolution
		207 -> valueType.isInteger && raw?.toLongOrNull() != null // Vertical Resolution
		214 -> valueType.isNumeric && raw?.toDoubleOrNull() != null // Frame Rate
		else -> false
	}
	else -> false
}

fun Directory.isRelevant(tagType: Int) = isRelevant(name, tagType, getTagValueType(tagType), getString(tagType))

fun MetadataTag.isRelevant() = isRelevant(directory, tagType, valueType, raw)

fun List<String>.detectMetadataTag(): MetadataTag? = takeIf { it.size >= 5 }
	?.subList(1, 5)
	?.let { (directory, tag, _, value) ->
		val tagType = tag.toIntOrNull()
		if (tagType != null) when {
			//directory == "" && tagType == 1 -> MetadataTag(directory = "AVI", tagType = tagType, name = "Frames Per Second", valueType = TagValueType.DOUBLE, raw = value)
			//directory == "" && tagType == 3 -> MetadataTag(directory = "AVI", tagType = tagType, name = "Duration", valueType = TagValueType.DATE, raw = value)
			//directory == "" && tagType == 6 -> MetadataTag(directory = "AVI", tagType = tagType, name = "Width", valueType = TagValueType.INT, raw = value)
			//directory == "" && tagType == 7 -> MetadataTag(directory = "AVI", tagType = tagType, name = "Height", valueType = TagValueType.INT, raw = value)
			//directory == "" && tagType == 320 -> MetadataTag(directory = "AVI", tagType = tagType, name = "Datetime Original", valueType = TagValueType.STRING, raw = value)
			directory == "EXIF:SubIFD:Image:Main" && tagType == 256 -> value
				.toRaw("Exif SubIFD", tagType, "Image Width", TagValueType.INT)
			directory == "EXIF:SubIFD:Image:Main" && tagType == 257 -> value
				.toRaw("Exif SubIFD", tagType, "Image Height", TagValueType.INT)
			directory == "EXIF:ExifIFD:Time:Main" && tagType == 36867 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Original")
			directory == "EXIF:ExifIFD:Time:Main:Copy1" && tagType == 36867 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Original")
			directory == "EXIF:ExifIFD:Time:Doc1" && tagType == 36867 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Original")
			directory == "EXIF:ExifIFD:Time:Doc1:Copy1" && tagType == 36867 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Original")
			directory == "EXIF:ExifIFD:Time:Main" && tagType == 36868 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Digitized")
			directory == "EXIF:ExifIFD:Time:Main:Copy1" && tagType == 36868 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Digitized")
			directory == "EXIF:ExifIFD:Time:Doc1" && tagType == 36868 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Digitized")
			directory == "EXIF:ExifIFD:Time:Doc1:Copy1" && tagType == 36868 -> value
				.toDateTime("Exif SubIFD", tagType, "Date/Time Digitized")
			directory == "EXIF:ExifIFD:Camera:Main" && tagType == 37382 -> value
				.toMetres("Exif SubIFD", tagType, "Subject Distance")
			directory == "EXIF:ExifIFD:Camera:Main" && tagType == 37396 -> value
				.toIntArray("Exif SubIFD", tagType, "Subject Location")
			directory == "EXIF:ExifIFD:Image:Main" && tagType == 40962 -> value
				.toRawWithSuffix("Exif SubIFD", tagType, "Exif Image Width", TagValueType.INT, "pixels")
			directory == "EXIF:ExifIFD:Image:Main:Copy1" && tagType == 40962 -> value
				.toRawWithSuffix("Exif SubIFD", tagType, "Exif Image Width", TagValueType.INT, "pixels")
			directory == "EXIF:ExifIFD:Image:Main" && tagType == 40963 -> value
				.toRawWithSuffix("Exif SubIFD", tagType, "Exif Image Height", TagValueType.INT, "pixels")
			directory == "EXIF:ExifIFD:Image:Main:Copy1" && tagType == 40963 -> value
				.toRawWithSuffix("Exif SubIFD", tagType, "Exif Image Height", TagValueType.INT, "pixels")
			directory == "EXIF:ExifIFD:Camera:Main" && tagType == 41996 -> value
				.toMapping("Exif SubIFD", tagType, "Subject Distance Range", TagValueType.INT, "0",
						   Triple("macro", "1", "Macro"),
						   Triple("close", "2", "Close view"),
						   Triple("distant", "3", "Distant view"))
			directory == "EXIF:ExifIFD:Camera:Main:Copy1" && tagType == 41996 -> value
				.toMapping("Exif SubIFD", tagType, "Subject Distance Range", TagValueType.INT, "0",
						   Triple("macro", "1", "Macro"),
						   Triple("close", "2", "Close view"),
						   Triple("distant", "3", "Distant view"))
			directory == "EXIF:ExifIFD:Image:Main" && tagType == 42016 -> value
				.toRaw("Exif SubIFD", tagType, "Unique Image ID", TagValueType.STRING)
			directory == "EXIF:IFD0:Image:Main" && tagType == 256 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Width", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 256 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Width", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main:Copy2" && tagType == 256 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Width", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main" && tagType == 257 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Height", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 257 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Height", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main:Copy2" && tagType == 257 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Image Height", TagValueType.INT, "pixels")
			directory == "EXIF:IFD0:Image:Main" && tagType == 274 -> value
				.toMapping("Exif IFD0", tagType, "Orientation", TagValueType.INT, "0",
						   Triple("orientation horizontal", "1", "Top, left side (Horizontal / normal)"),
						   Triple("orientation rotate 180", "3", "Bottom, right side (Rotate 180)"),
						   Triple("orientation rotate 90 CW", "6", "Right side, top (Rotate 90 CW)"),
						   Triple("orientation rotate 270 CW", "8", "Left side, bottom (Rotate 270 CW)"))
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 274 -> value
				.toMapping("Exif IFD0", tagType, "Orientation", TagValueType.INT, "0",
						   Triple("orientation horizontal", "1", "Top, left side (Horizontal / normal)"),
						   Triple("orientation rotate 180", "3", "Bottom, right side (Rotate 180)"),
						   Triple("orientation rotate 90 CW", "6", "Right side, top (Rotate 90 CW)"),
						   Triple("orientation rotate 270 CW", "8", "Left side, bottom (Rotate 270 CW)"))
			directory == "EXIF:IFD0:Image:Main:Copy2" && tagType == 274 -> value
				.toMapping("Exif IFD0", tagType, "Orientation", TagValueType.INT, "0",
						   Triple("orientation horizontal", "1", "Top, left side (Horizontal / normal)"),
						   Triple("orientation rotate 180", "3", "Bottom, right side (Rotate 180)"),
						   Triple("orientation rotate 90 CW", "6", "Right side, top (Rotate 90 CW)"),
						   Triple("orientation rotate 270 CW", "8", "Left side, bottom (Rotate 270 CW)"))
			directory == "EXIF:IFD0:Image:Doc1" && tagType == 274 -> value
				.toMapping("Exif IFD0", tagType, "Orientation", TagValueType.INT, "0",
						   Triple("orientation horizontal", "1", "Top, left side (Horizontal / normal)"),
						   Triple("orientation rotate 180", "3", "Bottom, right side (Rotate 180)"),
						   Triple("orientation rotate 90 CW", "6", "Right side, top (Rotate 90 CW)"),
						   Triple("orientation rotate 270 CW", "8", "Left side, bottom (Rotate 270 CW)"))
			directory == "EXIF:IFD0:Image:Main" && tagType == 282 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "X Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD1:Image:Main:Copy1" && tagType == 282 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "X Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD1:Image:Main:Copy2" && tagType == 282 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "X Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Doc1:Copy1" && tagType == 282 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "X Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Doc2:Copy2" && tagType == 282 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "X Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Main" && tagType == 283 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Y Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 283 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Y Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Main:Copy2" && tagType == 283 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Y Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Doc1:Copy1" && tagType == 283 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Y Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Doc2:Copy2" && tagType == 283 -> value
				.toRawWithSuffix("Exif IFD0", tagType, "Y Resolution", TagValueType.INT, "dots per")
			directory == "EXIF:IFD0:Image:Main" && tagType == 296 -> value
				.toMapping("Exif IFD0", tagType, "Resolution Unit", TagValueType.INT, "0",
						   Triple("none", "1", "(No unit)"), Triple("inch", "2", "Inch"), Triple("cm", "3", "cm"))
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 296 -> value
				.toMapping("Exif IFD0", tagType, "Resolution Unit", TagValueType.INT, "0",
						   Triple("none", "1", "(No unit)"), Triple("inch", "2", "Inch"), Triple("cm", "3", "cm"))
			directory == "EXIF:IFD0:Image:Main:Copy2" && tagType == 296 -> value
				.toMapping("Exif IFD0", tagType, "Resolution Unit", TagValueType.INT, "0",
						   Triple("none", "1", "(No unit)"), Triple("inch", "2", "Inch"), Triple("cm", "3", "cm"))
			directory == "EXIF:IFD0:Image:Doc1" && tagType == 296 -> value
				.toMapping("Exif IFD0", tagType, "Resolution Unit", TagValueType.INT, "0",
						   Triple("none", "1", "(No unit)"), Triple("inch", "2", "Inch"), Triple("cm", "3", "cm"))
			directory == "EXIF:IFD0:Image:Doc2:Copy2" && tagType == 296 -> value
				.toMapping("Exif IFD0", tagType, "Resolution Unit", TagValueType.INT, "0",
						   Triple("none", "1", "(No unit)"), Triple("inch", "2", "Inch"), Triple("cm", "3", "cm"))
			directory == "EXIF:IFD0:Time:Main" && tagType == 306 -> value
				.toDateTime("Exif IFD0", tagType, "Date/Time")
			directory == "EXIF:IFD0:Time:Main:Copy1" && tagType == 306 -> value
				.toDateTime("Exif IFD0", tagType, "Date/Time")
			directory == "EXIF:IFD0:Time:Doc1:Copy1" && tagType == 306 -> value
				.toDateTime("Exif IFD0", tagType, "Date/Time")
			directory == "EXIF:IFD0:Author:Main" && tagType == 315 -> value
				.toRaw("Exif IFD0", tagType, "Artist", TagValueType.STRING)
			directory == "EXIF:IFD0:Author:Doc1" && tagType == 315 -> value
				.toRaw("Exif IFD0", tagType, "Artist", TagValueType.STRING)
			directory == "EXIF:IFD0:Image:Main" && tagType == 4097 -> value
				.toRaw("Exif IFD0", tagType, "Related Image Width", TagValueType.INT)
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 4097 -> value
				.toRaw("Exif IFD0", tagType, "Related Image Width", TagValueType.INT)
			directory == "EXIF:IFD0:Image:Main" && tagType == 4098 -> value
				.toRaw("Exif IFD0", tagType, "Related Image Height", TagValueType.INT)
			directory == "EXIF:IFD0:Image:Main:Copy1" && tagType == 4098 -> value
				.toRaw("Exif IFD0", tagType, "Related Image Height", TagValueType.INT)
			directory == "EXIF:IFD0:Author:Main" && tagType == 33432 -> value
				.toRaw("Exif IFD0", tagType, "Copyright", TagValueType.STRING)
			directory == "EXIF:IFD0:Author:Main:Copy1" && tagType == 33432 -> value
				.toRaw("Exif IFD0", tagType, "Copyright", TagValueType.STRING)
			directory == "EXIF:IFD0:Author:Doc1" && tagType == 33432 -> value
				.toRaw("Exif IFD0", tagType, "Copyright", TagValueType.STRING)
			directory == "EXIF:IFD0:Time:Main" && tagType == 36867 -> value
				.toDateTime("Exif IFD0", tagType, "Date/Time Original")
			directory == "EXIF:IFD0:Time:Main" && tagType == 36868 -> value
				.toDateTime("Exif IFD0", tagType, "Date/Time Digitized")
			directory == "EXIF:IFD0:Camera:Main" && tagType == 37396 -> value
				.toIntArray("Exif IFD0", tagType, "Subject Location")
			directory == "EXIF:IFD0:Camera:Main" && tagType == 41996 -> value
				.toMapping("Exif IFD0", tagType, "Subject Distance Range", TagValueType.INT, "0",
						   Triple("macro", "1", "Macro"),
						   Triple("close", "2", "Close view"),
						   Triple("distant", "3", "Distant view"))
			directory == "EXIF:GPS:Location:Main" && tagType == 0 -> value
				.toGpsVersion()
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 0 -> value
				.toGpsVersion()
			directory == "EXIF:GPS:Location:Main" && tagType == 1 -> value
				.toLatitudeRef(tagType)
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 1 -> value
				.toLatitudeRef(tagType)
			directory == "EXIF:GPS:Location:Main" && tagType == 2 -> value
				.toLocation(tagType, "GPS Latitude")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 2 -> value
				.toLocation(tagType, "GPS Latitude")
			directory == "EXIF:GPS:Location:Main" && tagType == 3 -> value
				.toLongitureRef(tagType)
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 3 -> value
				.toLongitureRef(tagType)
			directory == "EXIF:GPS:Location:Main" && tagType == 4 -> value
				.toLocation(tagType, "GPS Longitude")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 4 -> value
				.toLocation(tagType, "GPS Longitude")
			directory == "EXIF:GPS:Location:Main" && tagType == 5 -> value
				.toAltitudeRef(tagType)
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 5 -> value
				.toAltitudeRef(tagType)
			directory == "EXIF:GPS:Location:Main" && tagType == 6 -> value
				.toMetres("GPS", tagType, "GPS Altitude")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 6 -> value
				.toMetres("GPS", tagType, "GPS Altitude")
			directory == "EXIF:GPS:Time:Main" && tagType == 7 -> value
				.toRationalArrayTimeStamp("GPS", tagType, "GPS Time-Stamp")
			directory == "EXIF:GPS:Time:Main:Copy1" && tagType == 7 -> value
				.toRationalArrayTimeStamp("GPS", tagType, "GPS Time-Stamp")
			directory == "EXIF:GPS:Location:Main" && tagType == 16 -> value
				.toDirectionRef(tagType, "GPS Img Direction Ref")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 16 -> value
				.toDirectionRef(tagType, "GPS Img Direction Ref")
			directory == "EXIF:GPS:Location:Main" && tagType == 17 -> value
				.toBearingRationalArray("GPS", tagType, "GPS Img Direction")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 17 -> value
				.toBearingRationalArray("GPS", tagType, "GPS Img Direction")
			directory == "EXIF:GPS:Location:Main" && tagType == 23 -> value
				.toDirectionRef(tagType, "GPS Dest Bearing Ref")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 23 -> value
				.toDirectionRef(tagType, "GPS Dest Bearing Ref")
			directory == "EXIF:GPS:Location:Main" && tagType == 24 -> value
				.toBearingRationalArray("GPS", tagType, "GPS Dest Bearing")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 24 -> value
				.toBearingRationalArray("GPS", tagType, "GPS Dest Bearing")
			directory == "EXIF:GPS:Location:Main" && tagType == 27 -> value
				.toRaw("GPS", tagType, "GPS Processing Method", TagValueType.STRING)
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 27 -> value
				.toRaw("GPS", tagType, "GPS Processing Method", TagValueType.STRING)
			directory == "EXIF:GPS:Time:Main" && tagType == 29 -> value
				.toDateStamp("GPS", tagType, "GPS Date Stamp")
			directory == "EXIF:GPS:Time:Main:Copy1" && tagType == 29 -> value
				.toDateStamp("GPS", tagType, "GPS Date Stamp")
			directory == "EXIF:GPS:Location:Main" && tagType == 31 -> value
				.toMetres("GPS", tagType, "GPS H Positioning Error")
			directory == "EXIF:GPS:Location:Main:Copy1" && tagType == 31 -> value
				.toMetres("GPS", tagType, "GPS H Positioning Error")
			//directory == "" && tagType == 24 -> MetadataTag(directory = "ICC Profile", tagType = tagType, name = "Profile Date/Time", valueType = TagValueType.UNKNOWN, raw = value)
			directory == "EXIF:InteropIFD:Image:Main" && tagType == 4097 -> value
				.toRaw("Interoperability", tagType, "Related Image Width", TagValueType.INT)
			directory == "EXIF:InteropIFD:Image:Doc1" && tagType == 4097 -> value
				.toRaw("Interoperability", tagType, "Related Image Width", TagValueType.INT)
			directory == "EXIF:InteropIFD:Image:Main" && tagType == 4098 -> value
				.toRaw("Interoperability", tagType, "Related Image Height", TagValueType.INT)
			directory == "EXIF:InteropIFD:Image:Doc1" && tagType == 4098 -> value
				.toRaw("Interoperability", tagType, "Related Image Height", TagValueType.INT)
			//directory == "" && tagType == 567 -> MetadataTag(directory = "IPTC", tagType = tagType, name = "Date Created", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 572 -> MetadataTag(directory = "IPTC", tagType = tagType, name = "Time Created", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 574 -> MetadataTag(directory = "IPTC", tagType = tagType, name = "Digital Date Created", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 575 -> MetadataTag(directory = "IPTC", tagType = tagType, name = "Digital Time Created", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 7 -> MetadataTag(directory = "JFIF", tagType = tagType, name = "Resolution Units", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 8 -> MetadataTag(directory = "JFIF", tagType = tagType, name = "X Resolution", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 10 -> MetadataTag(directory = "JFIF", tagType = tagType, name = "Y Resolution", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 1 -> MetadataTag(directory = "JPEG", tagType = tagType, name = "Image Height", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 3 -> MetadataTag(directory = "JPEG", tagType = tagType, name = "Image Width", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 256 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Creation Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 257 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Modification Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 258 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Media Time Scale", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 259 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Duration", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 260 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Duration in Seconds", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 512 -> MetadataTag(directory = "MP4", tagType = tagType, name = "Rotation", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 101 -> MetadataTag(directory = "MP4 Metadata", tagType = tagType, name = "Creation Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 102 -> MetadataTag(directory = "MP4 Metadata", tagType = tagType, name = "Modification Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 101 -> MetadataTag(directory = "MP4 Sound", tagType = tagType, name = "Creation Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 102 -> MetadataTag(directory = "MP4 Sound", tagType = tagType, name = "Modification Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 101 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Creation Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 102 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Modification Time", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 204 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Width", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 205 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Height", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 206 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Horizontal Resolution", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 207 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Vertical Resolution", valueType = TagValueType.UNKNOWN, raw = value)
			//directory == "" && tagType == 214 -> MetadataTag(directory = "MP4 Video", tagType = tagType, name = "Frame Rate", valueType = TagValueType.UNKNOWN, raw = value)
			else -> null
		} else null
	}

private fun String.toRaw(directory: String, tagType: Int, name: String, tagValueType: TagValueType) = MetadataTag(
		directory = directory,
		tagType = tagType,
		name = name,
		valueType = tagValueType,
		raw = this,
		description = this)

private fun String.toRawWithSuffix(directory: String,
								   tagType: Int,
								   name: String,
								   tagValueType: TagValueType,
								   suffix: String) = MetadataTag(
		directory = directory,
		tagType = tagType,
		name = name,
		valueType = tagValueType,
		raw = this,
		description = "${this} ${suffix}")

private fun String.toIntArray(directory: String, tagType: Int, name: String) = split(" ")
	.map { it.toIntOrNull() ?: 0 }
	.joinToString(" ")
	.let { intArray ->
		MetadataTag(
				directory = directory,
				tagType = tagType,
				name = name,
				valueType = TagValueType.INT_ARRAY,
				raw = intArray,
				description = intArray)
	}

private fun String.toRationalArrayTimeStamp(directory: String, tagType: Int, name: String) = "(\\d+):(\\d+):(\\d+)"
	.toRegex()
	.matchEntire(this)
	?.groupValues
	?.mapNotNull { it.toIntOrNull() }
	?.takeIf { it.size == 3 }
	?.let { (h, m, s) -> "${h}/1 ${m}/1 ${s}/1" to "%02d:%02d:%02d.000 UTC" }
	?.let { (raw, description) ->
		MetadataTag(directory = directory,
					tagType = tagType,
					name = name,
					valueType = TagValueType.RATIONAL_ARRAY,
					raw = raw,
					description = description)
	}

private fun String.toDateStamp(directory: String, tagType: Int, name: String) =
		takeIf { "\\d+:\\d+:\\d+".toRegex().matches(it) }
			?.let { date ->
				MetadataTag(directory = directory,
							tagType = tagType,
							name = name,
							valueType = TagValueType.DATE,
							raw = date,
							description = date)
			}

private fun String.toDateTime(directory: String, tagType: Int, name: String) =
		takeIf { "\\d+:\\d+:\\d+ \\d+:\\d+:\\d+".toRegex().matches(it) }
			?.let { date ->
				MetadataTag(directory = directory,
							tagType = tagType,
							name = name,
							valueType = TagValueType.DATE,
							raw = date,
							description = date)
			}

private fun String.toGpsVersion() = MetadataTag(
		directory = "GPS",
		tagType = 0,
		name = "GPS Version ID",
		valueType = TagValueType.STRING,
		raw = split(".").joinToString(" "),
		description = "%.3f".format(split(".").joinToString("").toDouble() / 1000.0))

private fun String.toLatitudeRef(tagType: Int) = MetadataTag(
		directory = "GPS",
		tagType = tagType,
		name = "GPS Latitude Ref",
		valueType = TagValueType.STRING,
		raw = takeIf { it.startsWith("S") }?.let { "S" } ?: "N",
		description = takeIf { it.startsWith("S") }?.let { "S" } ?: "N")

private fun String.toLongitureRef(tagType: Int) = MetadataTag(
		directory = "GPS",
		tagType = tagType,
		name = "GPS Longitude Ref",
		valueType = TagValueType.STRING,
		raw = takeIf { it.startsWith("W") }?.let { "W" } ?: "E",
		description = takeIf { it.startsWith("W") }?.let { "W" } ?: "E")

private fun String.toLocation(tagType: Int, name: String) = MetadataTag(
		directory = "GPS",
		tagType = tagType,
		name = name,
		valueType = TagValueType.RATIONAL_ARRAY,
		raw = toRawLocation(),
		description = toFormattedLocation())

private fun String.toMapping(directory: String,
							 tagType: Int,
							 name: String,
							 valueType: TagValueType,
							 defaultRaw: String? = null,
							 vararg map: Triple<String, String, String>) = (map
	.find { (search, _, _) -> startsWith(search, false) }
	?.let { (_, raw, description) -> raw to description }
	?: (defaultRaw?.let { it to this }))
	?.let { (raw, description) ->
		MetadataTag(directory = directory,
					tagType = tagType,
					name = name,
					valueType = valueType,
					raw = raw,
					description = description)
	}

private fun String.toAltitudeRef(tagType: Int) = when {
	startsWith("above", false) -> "0" to "Sea level"
	startsWith("below", false) -> "1" to "Below sea level"
	"Unknown (\\d+\\.\\d+)".toRegex().matches(this) -> "Unknown (\\d+\\.\\d+)"
		.toRegex()
		.matchEntire(this)
		?.groupValues
		?.get(1) to this
	else -> "-1" to this
}.let { (raw, description) ->
	MetadataTag(directory = "GPS",
				tagType = tagType,
				name = "GPS Altitude Ref",
				valueType = TagValueType.INT,
				raw = raw,
				description = description)
}

private fun String.toMetres(directory: String, tagType: Int, name: String) = "(\\d+\\.?\\d*) m"
	.toRegex()
	.matchEntire(this)
	?.groupValues
	?.mapNotNull { it.toDoubleOrNull() }
	?.getOrNull(0)
	?.let { Triple((it - floor(it)) > 0.0, it, "${it} metres") }
	?.let { (isRational, raw, description) ->
		MetadataTag(directory = directory,
					tagType = tagType,
					name = name,
					valueType = if (isRational) TagValueType.RATIONAL_ARRAY else TagValueType.LONG,
					raw = if (isRational) raw.toRational().toString() else raw.toString(),
					description = description)
	}

private fun String.toDirectionRef(tagType: Int, name: String) = when {
	startsWith("M", false) -> "M" to "Magnetic direction"
	startsWith("T", false) -> "T" to "True direction"
	else -> "U" to this
}.let { (raw, description) ->
	MetadataTag(directory = "GPS",
				tagType = tagType,
				name = name,
				valueType = TagValueType.STRING,
				raw = raw,
				description = description)
}

private fun String.toBearingRationalArray(directory: String, tagType: Int, name: String) = toDoubleOrNull()
	?.let { it.toRational() to it }
	?.let { (raw, description) -> "${raw}" to "${description} degrees" }
	?.let { (raw, description) ->
		MetadataTag(directory = directory,
					tagType = tagType,
					name = name,
					valueType = TagValueType.RATIONAL_ARRAY,
					raw = raw,
					description = description)
	}
//private fun String.to
//fun List<String>.cachedMetadataRecordToMetadataTag() : MetadataTag? = takeIf { it.size == 4 }
//	.let {
//
//	}