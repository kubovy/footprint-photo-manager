package com.poterion.footprint.manager.data

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*


//@Entity
//@Table(indexes = [
//	Index(columnList = COLUMN_DEVICE_CONNECTOR_ID),
//	Index(columnList = COLUMN_USERNAME)])
//data class BasicAuth(
//	@Id
//	@GeneratedValue(generator = "UUID")
//	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
//	@Column(columnDefinition = "UUID", updatable = false, nullable = false)
//	var id: UUID = UUID.randomUUID(),
//
//	@ManyToOne
//	@JoinColumn(name = COLUMN_DEVICE_CONNECTOR_ID, nullable = false, updatable = false)
//	var deviceConnector: DeviceConnector? = null,
//
//	@Column(name = COLUMN_USERNAME, nullable = false)
//	var username: String = "",
//
//	@Column(nullable = false)
//	var password: String = "") : Auth {
//
//	companion object {
//		const val COLUMN_DEVICE_CONNECTOR_ID = "DEVICE_CONNECTOR_ID"
//		const val COLUMN_USERNAME = "USERNAME"
//	}
//}