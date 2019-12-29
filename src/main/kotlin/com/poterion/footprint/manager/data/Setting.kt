package com.poterion.footprint.manager.data

import java.util.*
import javax.persistence.*

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