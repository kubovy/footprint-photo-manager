package com.poterion.footprint.manager.data

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*

//@Entity
//@Table(indexes = [
//	Index(columnList = COLUMN_DEVICE_ID)])
//data class DeviceConnector(
//	@Id
//	@GeneratedValue(generator = "UUID")
//	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
//	@Column(columnDefinition = "UUID", updatable = false, nullable = false)
//	var id: UUID = UUID.randomUUID(),
//
//	@ManyToOne
//	@JoinColumn(name = COLUMN_DEVICE_ID, nullable = false, updatable = false)
//	var device: Device? = null,
//
//	@Column(nullable = false)
//	var host: String = "",
//
//	@Column(nullable = false)
//	var local: Boolean = true,
//
//	@Column(nullable = false)
//	var uri: String = ""
//
//	//var auth: Auth? = null
//	) {
//
//	companion object {
//		const val COLUMN_DEVICE_ID = "DEVICE_ID"
//	}
//}