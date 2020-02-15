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

import com.poterion.footprint.manager.data.BaseItem.Companion.COLUMN_NAME
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_CONTENT_HASH
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_CREATED_AT
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_DELETED_AT
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_DEVICE_ID
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_HASH
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_LENGTH
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_LOCKED
import com.poterion.footprint.manager.data.MediaItem.Companion.COLUMN_UPDATED_AT
import com.poterion.footprint.manager.enums.AudioCodingFormat
import com.poterion.footprint.manager.enums.PhotoFormat
import com.poterion.footprint.manager.enums.VideoCodingFormat
import com.poterion.footprint.manager.enums.VideoFormat
import java.time.Instant
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
@Table(name = "MEDIA", indexes = [
	Index(columnList = COLUMN_DEVICE_ID),
	Index(columnList = COLUMN_NAME),
	Index(columnList = COLUMN_LENGTH),
	Index(columnList = COLUMN_HASH),
	Index(columnList = COLUMN_CONTENT_HASH),
	Index(columnList = COLUMN_LOCKED),
	Index(columnList = COLUMN_CREATED_AT),
	Index(columnList = COLUMN_UPDATED_AT),
	Index(columnList = COLUMN_DELETED_AT)])
data class MediaItem(
		@Id
		@Column(updatable = false, nullable = false)
		override var id: String? = UUID.randomUUID().toString(),

		@Column(name = COLUMN_DEVICE_ID, columnDefinition = "UUID", nullable = true, updatable = false)
		var deviceId: String? = null,

		@Column(name = COLUMN_NAME, nullable = false)
		override var name: String = "",

		@Column(nullable = false)
		override var uri: String = "",

		@Column(name = COLUMN_LENGTH, nullable = false)
		var length: Long = 0L,

		@Column(name = COLUMN_HASH)
		var hash: String? = null,

		@Column(name = COLUMN_CONTENT_HASH)
		var contentHash: String? = null,

		@Column
		@Enumerated(value = EnumType.STRING)
		var imageFormat: PhotoFormat? = null,

		@Column
		@Enumerated(value = EnumType.STRING)
		var videoFormat: VideoFormat? = null,

		@Column
		@Enumerated(value = EnumType.STRING)
		var videoCodingFormat: VideoCodingFormat? = null,

		@Column
		@Enumerated(value = EnumType.STRING)
		var audioCodingFormat: AudioCodingFormat? = null,

		@Column(name = COLUMN_LOCKED, nullable = false)
		var locked: Boolean = false,

		@Column(name = COLUMN_CREATED_AT, nullable = false)
		var createdAt: Instant = Instant.EPOCH,

		@Column(name = COLUMN_UPDATED_AT, nullable = false)
		var updatedAt: Instant = Instant.EPOCH,

		@Column(name = COLUMN_DELETED_AT)
		var deletedAt: Instant? = null) : UriItem, CacheableItem {

	companion object {
		const val COLUMN_DEVICE_ID = "DEVICE_ID"
		const val COLUMN_LENGTH = "LENGTH"
		const val COLUMN_HASH = "HASH"
		const val COLUMN_CONTENT_HASH = "CONTENT_HASH"
		const val COLUMN_LOCKED = "LOCKED"
		const val COLUMN_CREATED_AT = "CREATED_AT"
		const val COLUMN_UPDATED_AT = "UPDATED_AT"
		const val COLUMN_DELETED_AT = "DELETED_AT"
	}
}