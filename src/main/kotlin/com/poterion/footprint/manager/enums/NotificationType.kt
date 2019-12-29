package com.poterion.footprint.manager.enums

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class NotificationType(val isPersistent: Boolean) {
	MISSING_IN_PRIMARY(true),
	MISSING_LOCATION(true),
	DUPLICATE(true),
	INVALID_DATE(true),
	PROCESSING_PROBLEM(true),
	SCAN_PROBLEM(false),
	METADATA_ERROR(true),
	WRONG_FOLDER(true),
	UNKNOWN(false)
}