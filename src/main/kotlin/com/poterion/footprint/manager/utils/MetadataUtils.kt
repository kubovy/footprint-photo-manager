package com.poterion.footprint.manager.utils

import com.drew.lang.Rational
import com.drew.metadata.Directory
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.enums.TagValueType
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private fun String.detectRationalArray(defaultType: TagValueType) =
	if (contains("/")) TagValueType.RATIONAL_ARRAY else defaultType

fun Directory.getTagValueType(tagType: Int) = this.getDate(tagType)?.let { TagValueType.DATE }
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
		257 -> valueType == TagValueType.DATE && raw?.parseExifDateOrNull() != null
		567 -> valueType == TagValueType.DATE && raw?.parseExifDateOrNull() != null
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