package com.poterion.footprint.manager.data

import com.poterion.footprint.manager.data.BaseItem.Companion.COLUMN_NAME
import com.poterion.footprint.manager.data.MetadataTag.Companion.COLUMN_MEDIA_ITEM_ID
import com.poterion.footprint.manager.enums.TagValueType
import java.util.*
import javax.persistence.*

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