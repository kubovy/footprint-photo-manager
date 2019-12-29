package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.enums.DeviceType

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
val Device.isAvailable: Boolean
	get() = when (type) {
		DeviceType.LOCAL -> device?.toFileObject()?.exists() == true
		DeviceType.REMOVABLE -> device?.toFileObject()?.exists() == true
		DeviceType.SMB -> device?.toFileObject()?.smbFile?.exists() == true
	}