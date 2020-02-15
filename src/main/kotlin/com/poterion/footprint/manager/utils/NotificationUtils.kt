/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.Icons
import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.data.Notification
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.utils.kotlin.uriDecode

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
val Notification.device: Device?
	get() = deviceId?.let { Database.get(Device::class, it) }

val Notification.mediaItem: MediaItem?
	get() = mediaItemId?.let { Database.get(MediaItem::class, it) }

val Notification.metadataTag: MetadataTag?
	get() = metadataTagId?.let { Database.get(MetadataTag::class, it) }

fun Notification.toIcon(): Icons = type.toIcon()

fun NotificationType.toIcon(): Icons = when (this) {
	NotificationType.MISSING_IN_PRIMARY -> Icons.MISSING_IN_PRIMARY
	NotificationType.MISSING_LOCATION -> Icons.MISSING_LOCATION
	NotificationType.DUPLICATE -> Icons.DUPLICATE
	NotificationType.INVALID_DATE -> Icons.INVALID_DATE
	NotificationType.PROCESSING_PROBLEM -> Icons.PROCESS
	NotificationType.SCAN_PROBLEM -> Icons.SCAN_PROBLEM
	NotificationType.METADATA_ERROR -> Icons.METADATA
	NotificationType.WRONG_FOLDER -> Icons.FOLDER_WRONG
	NotificationType.UNKNOWN -> Icons.UNKNOWN
}

val Notification.displayName: String
	get() = when (type) {
		NotificationType.DUPLICATE -> name
		else -> name + (listOfNotNull(
				deviceId
					?.let { Database.get(Device::class, it) }
					?.name
					?.let { "device: ${it}" },
				mediaItemId
					?.let { Database.get(MediaItem::class, it) }
					?.let { it.uri.removePrefix(it.device?.uri ?: "") }
					?.uriDecode()
					?.removePrefix("file://")
					?.let { "file: ${it}" },
				metadataTagId
					?.let { Database.get(MetadataTag::class, it) }
					?.let { "tag: [${it.directory}] ${it.name} = ${it.description}" })
			.takeIf { it.isNotEmpty() }?.joinToString(", ", " (", ")")
			?: "")
	}

val NotificationType.displayName: String
	get() = when (this) {
		NotificationType.MISSING_IN_PRIMARY -> "Missing items in primary storage"
		NotificationType.MISSING_LOCATION -> "Items without location metadata"
		NotificationType.DUPLICATE -> "Multiple duplicates in primary storage"
		NotificationType.INVALID_DATE -> "Items with invalid dates"
		NotificationType.PROCESSING_PROBLEM -> "Processing problems"
		NotificationType.SCAN_PROBLEM -> "Scan problems"
		NotificationType.METADATA_ERROR -> "Metadata errors"
		NotificationType.WRONG_FOLDER -> "Items in wrong folder"
		NotificationType.UNKNOWN -> "Unknown"
	}
