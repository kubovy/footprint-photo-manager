package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.model.FileObject
import jcifs.context.SingletonContext
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Paths

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.FileObjectUtils")

fun URI.toFileObject(): FileObject? = when (deviceType) {
	DeviceType.LOCAL,
	DeviceType.REMOVABLE -> try {
		FileObject(Paths.get(path).toFile())
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}
	DeviceType.SMB -> try {
		FileObject(SmbFile(toString(), device?.cifsContext ?: SingletonContext.getInstance()))
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}
}

fun FileObject.toMediaItemOrNull(): MediaItem? = uri?.toMediaItemOrNull()