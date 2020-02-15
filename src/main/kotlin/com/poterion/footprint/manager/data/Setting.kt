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

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.Table

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@Entity
@Table(name = "SETTINGS", indexes = [
	Index(columnList = BaseItem.COLUMN_NAME)])
data class Setting(
		@Id
		@Column(updatable = false, nullable = false)
		override var id: String? = UUID.randomUUID().toString(),

		@Column(name = BaseItem.COLUMN_NAME, nullable = false)
		override var name: String = "",

		@Column(columnDefinition = "TEXT")
		var value: String? = null) : CacheableItem {

	companion object {
		const val EXPANDED = "expanded"
		const val FOLDER_PATTERN = "folderPattern"
		const val AUTOPLAY_VIDEOS = "autoplayVideos"
		const val WINDOW_WIDTH = "windowWidth"
		const val WINDOW_HEIGHT = "windowHeight"
		const val WINDOW_MAXIMIZED = "windowMaximized"
		const val COLUMN_DATA_NAME_WIDTH = "columnDataNameWidth"
		const val COLUMN_METADATA_NAME_WIDTH = "columnMetadataNameWidth"
		const val COLUMN_METADATA_VALUE_WIDTH = "columnMetadataValueWidth"
	}
}