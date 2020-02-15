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
import com.poterion.footprint.manager.data.Device.Companion.COLUMN_PRIMARY
import com.poterion.footprint.manager.enums.DeviceType
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
@Table(indexes = [
	Index(columnList = COLUMN_NAME),
	Index(columnList = COLUMN_PRIMARY)])
data class Device(
		@Id
		@Column(updatable = false, nullable = false)
		override var id: String? = UUID.randomUUID().toString(),

		@Column(name = COLUMN_NAME, unique = true, nullable = false)
		override var name: String = "",

		@Column(nullable = false)
		@Enumerated(EnumType.STRING)
		var type: DeviceType = DeviceType.LOCAL,

		@Column(columnDefinition = "TEXT")
		var auth: String? = null,

		@Column(nullable = false)
		override var uri: String = "",

		@Column(name = COLUMN_PRIMARY, nullable = false)
		var isPrimary: Boolean = false,

		@Column(name = COLUMN_CREATED_AT, nullable = false)
		var createdAt: Instant = Instant.now(),

		@Column(name = COLUMN_LAST_SEEN_AT, nullable = false)
		var lastSeenAt: Instant = Instant.now()) : UriItem, CacheableItem {

	companion object {
		const val COLUMN_PRIMARY = "IS_PRIMARY"
		const val COLUMN_CREATED_AT = "CREATED_AT"
		const val COLUMN_LAST_SEEN_AT = "LAST_SEEN_AT"
	}
}