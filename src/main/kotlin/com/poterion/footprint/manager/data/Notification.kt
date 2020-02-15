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
package com.poterion.footprint.manager.data

import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_DEVICE_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_MEDIA_ITEM_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_METADATA_TAG_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_TYPE
import com.poterion.footprint.manager.enums.NotificationType
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.Table

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@Entity
@Table(name = "NOTIFICATIONS", indexes = [
	Index(columnList = BaseItem.COLUMN_NAME),
	Index(columnList = COLUMN_DEVICE_ID),
	Index(columnList = COLUMN_MEDIA_ITEM_ID),
	Index(columnList = COLUMN_METADATA_TAG_ID),
	Index(columnList = COLUMN_TYPE)])
data class Notification(
		@Id
		@Column(updatable = false, nullable = false)
		override var id: String? = UUID.randomUUID().toString(),

		@Column(name = COLUMN_DEVICE_ID)
		var deviceId: String? = null,

		@Column(name = COLUMN_MEDIA_ITEM_ID)
		var mediaItemId: String? = null,

		@Column(name = COLUMN_METADATA_TAG_ID)
		var metadataTagId: String? = null,

		@Column(name = COLUMN_TYPE, nullable = false)
		@Enumerated(EnumType.STRING)
		var type: NotificationType = NotificationType.UNKNOWN,

		@Column(name = BaseItem.COLUMN_NAME, nullable = false)
		override var name: String = "",

		@Column(columnDefinition = "TEXT")
		var value: String? = null,

		@Column(columnDefinition = "TEXT")
		var context: String? = null) : CacheableItem {

	companion object {
		const val COLUMN_DEVICE_ID = "DEVICE_ID"
		const val COLUMN_MEDIA_ITEM_ID = "MEDIA_ITEM_ID"
		const val COLUMN_METADATA_TAG_ID = "METADATA_TAG_ID"
		const val COLUMN_TYPE = "TYPE"
		val ROOT = Notification(name = "ROOT")
	}
}