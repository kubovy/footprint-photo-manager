package com.poterion.footprint.manager.utils

import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.DebugUtils")

fun <R> measureTime(message: String, function: () -> R): R {
	val start = System.currentTimeMillis()
	val result = function()
	LOGGER.info("${message} in ${(System.currentTimeMillis() - start).toDisplayDuration()}"
					.replace("%s", result.toString()))
	return result
}