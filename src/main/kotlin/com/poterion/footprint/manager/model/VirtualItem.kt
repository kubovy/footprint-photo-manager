package com.poterion.footprint.manager.model

import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.enums.Icons

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class VirtualItem(override var name: String,
					   var icon: Icons = Icons.FOLDER,
					   override var uri: String = "") : UriItem {

	companion object {
		val ROOT = VirtualItem(name = "ROOT")
	}

	override val id: String? = uri
}