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

import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.model.FileObject
import com.poterion.utils.kotlin.uriDecode
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
		FileObject(SmbFile("${resolve("/")}${path.uriDecode().removePrefix("/")}",
						   device?.cifsContext ?: SingletonContext.getInstance()))
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}
}

fun FileObject.toMediaItemOrNull(): MediaItem? = uri?.toMediaItemOrNull()