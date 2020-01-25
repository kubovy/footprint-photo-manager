package com.poterion.footprint.manager.data

import com.poterion.footprint.manager.data.BaseItem.Companion.COLUMN_NAME
import com.poterion.footprint.manager.data.Device.Companion.COLUMN_PRIMARY
import com.poterion.footprint.manager.enums.DeviceType
import java.time.Instant
import java.util.*
import javax.persistence.*

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