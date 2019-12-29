package com.poterion.footprint.manager.utils

import java.time.Duration

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val KI = arrayOf("", "k", "M", "G", "T", "P")

fun Long.toDuration(): Duration = Duration.ofMillis(this)

fun Long.toDisplayDuration(): String = toDuration().toDisplayString()

fun Duration.toDisplayString(): String {
	val days = toDays()
		.takeIf { it > 0 }?.let { "${it} days " }
	val hours = toHours()
		.takeIf { days != null || it > 0 }
		?.let { "%02d:".format(it) }
	val minutes = toMinutes()
		.takeIf { hours != null || it > 0 }
		?.let { "%02d:".format(it) }
	val seconds = seconds.rem(60)
		.takeIf { minutes != null || it > 0 }
		?.let { "%0${if (minutes != null) 2 else 1}d.".format(it) }
	val millis = toMillis().rem(1000)
		.takeIf { seconds != null || it > 0 }
		?.let { "%0${if (seconds != null) 3 else 1}d".format(it) }
	val suffix = when {
		days == null && hours == null && minutes == null && seconds == null -> "ms"
		days == null && hours == null && minutes == null -> "s"
		else -> null
	}
	return listOfNotNull(days, hours, minutes, seconds, millis, suffix).joinToString("")
}

fun Long.toKI(suffix: String = "B", digitsAfterComma: Int = 2): String {
	var index = 0
	var value = this.toDouble()
	while (value > 1024 && KI.size > (index + 1)) {
		value /= 1024
		index++
	}
	return "%.${digitsAfterComma}f%s%s".format(value, KI[index], suffix)
}