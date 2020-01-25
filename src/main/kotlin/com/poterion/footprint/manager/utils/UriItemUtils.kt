package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.model.FileObject
import com.poterion.footprint.manager.xuggle.SmbFileProtocolHandler
import com.poterion.utils.kotlin.ensureSuffix
import com.poterion.utils.kotlin.toUriOrNull
import com.xuggle.xuggler.io.FileProtocolHandler
import com.xuggle.xuggler.io.IURLProtocolHandler
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.model.UriItemUtils")

fun UriItem.exists(): Boolean = id?.let { Database.get(this::class, it) } != null

fun UriItem.toUriOrNull(): URI? = uri.toUriOrNull()

val UriItem.device: Device?
	get() = when (this) {
		is MediaItem -> deviceId?.let { Database.get(Device::class, it) }
		else -> Database.list(Device::class).find { uri.startsWith(it.uri) }
	}


val UriItem.deviceType: DeviceType
	get() = getDeviceType(uri)

private val fileObjectCache = mutableMapOf<String, FileObject?>()

fun UriItem.toFileObject(): FileObject? = id?.let { id ->
	var result = fileObjectCache[id]
	if (result == null) {
		result = try {
			when (deviceType) {
				DeviceType.LOCAL,
				DeviceType.REMOVABLE -> toUriOrNull()
				DeviceType.SMB -> when (this) {
					is MediaItem -> toUriOrNull()
					else -> uri.ensureSuffix("/").toUriOrNull()
				}
			}?.toFileObject()
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}
		fileObjectCache[id] = result
	}
	result
}

fun UriItem.fileProtocolHandler(): IURLProtocolHandler? = when (deviceType) {
	DeviceType.LOCAL,
	DeviceType.REMOVABLE -> toFileObject()?.file?.let { FileProtocolHandler(it) }
	DeviceType.SMB -> toFileObject()?.smbFile?.let { SmbFileProtocolHandler(it) }
}

