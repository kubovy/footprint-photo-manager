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
package com.poterion.footprint.manager.xuggle

import com.poterion.footprint.manager.utils.cifsContext
import com.poterion.footprint.manager.utils.device
import com.xuggle.xuggler.io.IURLProtocolHandler
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SmbFileProtocolHandler : IURLProtocolHandler {
	var file: SmbFile?
	var stream: SmbRandomAccessFile? = null
	private val cifsContext: CIFSContext
	private val LOGGER = LoggerFactory.getLogger(SmbFileProtocolHandler::class.java)

	constructor(file: SmbFile) {
		LOGGER.debug("Initializing file protocol handler: {}", file)
		cifsContext = file.context
		this.file = file
	}

	constructor(uri: String?) {
		LOGGER.debug("Initializing file protocol handler: {}", uri)
		cifsContext = uri?.let { URI(it) }?.device?.cifsContext ?: SingletonContext.getInstance()
		file = uri?.let { SmbFile(it, cifsContext) }
	}

	constructor(uri: String?, context: CIFSContext) {
		LOGGER.debug("Initializing file protocol handler: {}", uri)
		cifsContext = context
		file = uri?.let { SmbFile(it, cifsContext) }
	}

	override fun close(): Int {
		LOGGER.debug("Closing file: {}", file)
		try {
			stream!!.close()
		} catch (var2: IOException) {
			LOGGER.error("Error closing file: {}", file)
			var2.printStackTrace()
			return -1
		}
		LOGGER.debug("Succesfully closed file: {}", file)
		return 0
	}

	override fun open(url: String?, flags: Int): Int {
		LOGGER.debug("attempting to open {} with flags {}", url ?: file, flags)
		if (stream != null) close()

		if (file == null && url != null) file = SmbFile(url, cifsContext)

		LOGGER.debug("Opening file: {}", file)
		val mode: String = when (flags) {
			0 -> "r"
			1 -> "rw"
			2 -> "rw"
			else -> {
				LOGGER.error("Invalid flag passed to open: {}", file)
				return -1
			}
		}

		LOGGER.debug("read mode \"{}\" for file: {}", mode, file)
		return try {
			stream = SmbRandomAccessFile(file, mode)
			LOGGER.debug("Opened file: {}", file)
			0
		} catch (var6: Exception) {
			LOGGER.error("Could not find file: {}; ex: {}", file, var6)
			-1
		}
	}

	override fun read(buf: ByteArray, size: Int): Int {
		return try {
			stream!!.read(buf, 0, size)
		} catch (var4: IOException) {
			LOGGER.error("Got IO exception reading from file: {}", file)
			var4.printStackTrace()
			-1
		}
	}

	override fun seek(offset: Long, whence: Int): Long {
		return try {
			val seek: Long = when (whence) {
				0 -> offset
				1 -> stream!!.filePointer + offset
				else -> {
					if (whence != 2) {
						if (whence == 65536) {
							return stream!!.length().toInt().toLong()
						}
						LOGGER.error("invalid seek value \"{}\" for file: {}", whence, file)
						return -1L
					}
					stream!!.length() + offset
				}
			}
			stream!!.seek(seek)
			LOGGER.debug("seeking to \"{}\" in: {}", seek, file)
			seek
		} catch (var6: IOException) {
			LOGGER.error("got io exception \"{}\" while seeking in: {}", var6.message, file)
			var6.printStackTrace()
			-1L
		}
	}

	override fun write(buf: ByteArray, size: Int): Int {
		return try {
			stream!!.write(buf, 0, size)
			size
		} catch (var4: IOException) {
			LOGGER.error("Got error writing to file: {}", file)
			var4.printStackTrace()
			-1
		}
	}

	override fun isStreamed(url: String, flags: Int): Boolean {
		return false
	}
}
