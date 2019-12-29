package com.poterion.footprint.manager.data

import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_DEVICE_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_MEDIA_ITEM_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_METADATA_TAG_ID
import com.poterion.footprint.manager.data.Notification.Companion.COLUMN_TYPE
import com.poterion.footprint.manager.enums.NotificationType
import java.util.*
import javax.persistence.*

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