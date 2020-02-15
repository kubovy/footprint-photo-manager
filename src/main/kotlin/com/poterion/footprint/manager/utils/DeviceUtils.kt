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
package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.utils.kotlin.decrypt
import com.poterion.utils.kotlin.encrypt
import com.poterion.utils.kotlin.uriDecode
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

fun getPrimaryDevice(): Device? = Database.list(Device::class).find { it.isPrimary }

fun getPrimaryMediaItems(): Collection<MediaItem>? = getPrimaryDevice()?.mediaItems

val URI.device: Device?
	get() = Database.list(Device::class).find { toString().startsWith(it.uri) }

val Path.device: Device?
	get() = toUri().device

val URI.deviceType: DeviceType
	get() = getDeviceType(toString())

val Device.isAvailable: Boolean
	get() = when (type) {
		DeviceType.LOCAL -> device?.toFileObject()?.exists() == true
		DeviceType.REMOVABLE -> device?.toFileObject()?.exists() == true
		DeviceType.SMB -> device?.toFileObject()?.smbFile?.exists() == true
	}

var Device.authString: String?
	get() = auth?.decrypt()
	set(value) {
		auth = value?.encrypt()
	}

val Device.usernamePassword: Pair<String, String>?
	get() = authString
		?.split(":".toRegex(), 2)
		?.takeIf { it.size == 2 }
		?.map { it.uriDecode() }
		?.let { (username, password) -> username to password }

private val Device.authenticator: NtlmPasswordAuthenticator?
	get() = usernamePassword?.let { (username, password) -> NtlmPasswordAuthenticator("", username, password) }

val Device.cifsContext: CIFSContext
	get() = SingletonContext.getInstance().withCredentials(authenticator)

val Device.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.deviceId == id }

private val deviceTypeCache: MutableMap<String, DeviceType> = mutableMapOf()
private val deviceTypeCacheLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

fun getDeviceType(uri: String): DeviceType {
	var type: DeviceType? = null
	deviceTypeCacheLock.read {
		type = deviceTypeCache[uri]
		if (type == null) {
			type = DeviceType.values().firstOrNull { uri.startsWith(it.protocol) } ?: DeviceType.LOCAL
			if (type == DeviceType.LOCAL) {
				val isRemovable = USBDeviceDetectorManager().removableDevices
					.map { it.rootDirectory.toPath().toUri().toString() }
					.any { uri.startsWith(it) }
				if (isRemovable) type = DeviceType.REMOVABLE
			}
			deviceTypeCacheLock.write { deviceTypeCache[uri] = type ?: DeviceType.LOCAL }
		}
	}
	return type ?: DeviceType.LOCAL
}
