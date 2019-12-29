package com.poterion.footprint.manager.utils

import com.drew.lang.Rational
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.ExifUtils")

fun String.toRationalOrNull() = split("/")
	.takeIf { it.size == 2 }
	?.mapNotNull { it.toLongOrNull() }
	?.takeIf { it.size == 2 }
	?.let { (numerator, denominator) -> Rational(numerator, denominator) }

fun String.toFloatingOrNull() = toRationalOrNull()?.toDouble() ?: toDoubleOrNull()

fun String.toRationalList() = split(" ").map { it.toRationalOrNull() }

fun String.toRationalListNotNull() = toRationalList().filterNotNull()

fun List<Rational>.toDegrees() = map { it.toDouble() }
	.mapIndexed { i, d -> d / (if (i == 0) 1 else (60 * i)) }
	.reduce { acc, d -> acc + d }

fun String.parseExifDurationOrNull() = try {
	DateTimeFormatter.ofPattern("HH:mm:ss").parse(this)
} catch (e: Exception) {
	LOGGER.error("Cannot parse ${this}: ${e.message}", e)
	null
}

fun String.parseTemporalOrNull(patterns: List<String>): TemporalAccessor? {
	var exception: Exception? = null
	for (pattern in patterns) {
		try {
			return DateTimeFormatter.ofPattern(pattern).parse(this)
		} catch (e: Exception) {
			exception = e
		}
	}

	val instant = toLongOrNull()
		?.takeIf { it > 1000000 }
		?.let { if (it < 9999999999) Instant.ofEpochSecond(it, 0) else Instant.ofEpochMilli(it) }
	if (instant != null) return instant

	if (exception != null) {
		LOGGER.error("Cannot parse ${this} (using ${patterns.joinToString(", ")}): ${exception.message}", exception)
	}
	return null
}

fun String.parseExifDateTimeOrNull() = parseTemporalOrNull(listOf(
		"EEE MMM dd HH:mm:ss zzz yyyy",
		"yyyy:MM:dd HH:mm:ss"))

fun String.parseExifDateOrNull() = parseTemporalOrNull(listOf(
		"yyyy:MM:dd",
		"yyyyMMdd"))

fun String.parseExifTimeOrNull() = parseTemporalOrNull(listOf(
		"HH:mm:ssZZZZZ",
		"HH:mm:ss",
		"HHmmssZZZZZ",
		"HHmmss"))
