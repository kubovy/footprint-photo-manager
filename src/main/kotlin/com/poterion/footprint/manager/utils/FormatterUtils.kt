package com.poterion.footprint.manager.utils

import java.time.Instant
import java.time.temporal.ChronoField

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

val Pair<Int, Int>.formattedResolution: String?
	get() = let { (width, height) -> "${width}x${height}" }

fun Pair<Instant, Instant>.formatDateSpan(yearPattern: String,
										  monthPattern: String,
										  dayPattern: String,
										  condensed: Boolean = true): String {
	val (begin, end) = this
	val beginYear = begin.get(ChronoField.YEAR).toString().let { if (yearPattern.length > 2) it else it.substring(2) }
	val endYear = end.get(ChronoField.YEAR).toString().let { if (yearPattern.length > 2) it else it.substring(2) }
	val beginMonth =
		begin.get(ChronoField.MONTH_OF_YEAR).let { if (monthPattern.length > 1) "%02d".format(it) else it.toString() }
	val endMonth =
		end.get(ChronoField.MONTH_OF_YEAR).let { if (monthPattern.length > 1) "%02d".format(it) else it.toString() }
	val beginDay =
		begin.get(ChronoField.DAY_OF_MONTH).let { if (dayPattern.length > 1) "%02d".format(it) else it.toString() }
	val endDay =
		end.get(ChronoField.DAY_OF_MONTH).let { if (dayPattern.length > 1) "%02d".format(it) else it.toString() }

	return if (condensed) {
		var result = "${beginYear}-${beginMonth}-${beginDay}"
		if (beginYear != endYear) result += "-${endYear}"
		if (beginMonth != endMonth) result += "-${endMonth}"
		if (beginDay != endDay) result += "-${endDay}"
		result
	} else {
		"${beginYear}-${beginMonth}-${beginDay}-${endYear}-${endMonth}-${endDay}"
	}
}

val Pair<Double, Double>.formattedLocation: String
	get() {
		val lat = if (first > 0) "N" else "S"
		val latDeg = first
		val latMin = (first - latDeg) * 60
		val latSec = (((first - latDeg) * 60) - latMin) * 60

		val lng = if (second > 0) "E" else "W"
		val lngDeg = second
		val lngMin = (second - latDeg) * 60
		val lngSec = (((second - latDeg) * 60) - latMin) * 60

		return "%s%02.0f°%02.0f'%02.3f\" %s%03.0f°%02.0f'%02.3f\""
			.format(lat, latDeg, latMin, latSec, lng, lngDeg, lngMin, lngSec)
	}

