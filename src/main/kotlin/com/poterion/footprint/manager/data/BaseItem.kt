package com.poterion.footprint.manager.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface BaseItem {

	companion object {
		const val COLUMN_NAME = "NAME"
	}

	val id: String?
	val name: String
}