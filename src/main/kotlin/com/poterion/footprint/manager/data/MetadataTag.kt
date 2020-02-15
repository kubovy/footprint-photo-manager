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
import com.poterion.footprint.manager.data.MetadataTag.Companion.COLUMN_MEDIA_ITEM_ID
import com.poterion.footprint.manager.enums.TagValueType
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
@Table(name = "METADATA", indexes = [
	Index(columnList = COLUMN_MEDIA_ITEM_ID),
	Index(columnList = COLUMN_NAME)])
data class MetadataTag(
		@Id
		@Column(updatable = false, nullable = false)
		override var id: String? = UUID.randomUUID().toString(),

		@Column(name = COLUMN_MEDIA_ITEM_ID, columnDefinition = "UUID", nullable = true, updatable = false)
		var mediaItemId: String? = null,

		@Column(nullable = false, updatable = false)
		var directory: String = "",

		@Column(name = COLUMN_NAME, nullable = false, updatable = false)
		override var name: String = "",

		@Column(name = COLUMN_TAG_TYPE, nullable = false, updatable = false)
		var tagType: Int = 0,

		@Column(name = COLUMN_VALUE_TYPE, nullable = false)
		@Enumerated(value = EnumType.STRING)
		var valueType: TagValueType = TagValueType.UNKNOWN,

		@Column(columnDefinition = "TEXT")
		var raw: String? = null,

		@Column(columnDefinition = "TEXT")
		var description: String? = null) : BaseItem {

	companion object {
		const val COLUMN_MEDIA_ITEM_ID = "MEDIA_ITEM_ID"
		const val COLUMN_TAG_TYPE = "TAG_TYPE"
		const val COLUMN_VALUE_TYPE = "VALUE_TYPE"
	}
}