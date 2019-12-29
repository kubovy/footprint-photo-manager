package com.poterion.footprint.manager.utils

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
fun Number.formatDurationMillis(): String {
	val second: Long = toLong() / 1000 % 60
	val minute: Long = toLong() / (1000 * 60) % 60
	val hour: Long = toLong() / (1000 * 60 * 60) % 24

	return String.format("%d:%02d:%02d", hour, minute, second)
}