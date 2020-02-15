package com.poterion.footprint.manager.model

import com.poterion.footprint.manager.utils.extension
import com.poterion.utils.kotlin.uriEncode
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class FileObject {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(FileObject::class.java)
	}

	val file: File?
	val smbFile: SmbFile?

	val uri: URI?
		get() = try {
			when {
				file != null -> file.absoluteFile.toPath().toUri()
				smbFile != null -> smbFile.url
					.let { url -> URL(url, url.path.split("/").joinToString("/") { it.uriEncode() }) }
					.toURI()
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	val parent: String?
		get() = try {
			when {
				file != null -> file.parentFile.absolutePath
				smbFile != null -> smbFile.parent
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	val name: String
		get() = when {
			file != null -> file.name
			smbFile != null -> smbFile.name
			else -> throw NotImplementedError()
		}

	val extension: String
		get() = when {
			file != null -> file.extension
			smbFile != null -> smbFile.extension
			else -> throw NotImplementedError()
		}

	val length: Long?
		get() = try {
			when {
				file != null -> file.length()
				smbFile != null -> smbFile.length()
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	val createdAt: Long?
		get() = try {
			when {
				file != null -> Files
					.readAttributes(file.absoluteFile.toPath(), BasicFileAttributes::class.java)
					.creationTime()
					.toMillis()
				smbFile != null -> smbFile.createTime()
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	val updatedAt: Long?
		get() = try {
			when {
				file != null -> Files
					.readAttributes(file.toPath(), BasicFileAttributes::class.java)
					.lastModifiedTime()
					.toMillis()
				smbFile != null -> smbFile.lastModified()
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	val isDirectory: Boolean?
		get() = try {
			when {
				file != null -> file.isDirectory
				smbFile != null -> smbFile.isDirectory
				else -> throw NotImplementedError()
			}
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	constructor(file: File) {
		this.file = file
		this.smbFile = null
	}

	constructor(smbFile: SmbFile) {
		this.file = null
		this.smbFile = smbFile
	}

	fun relativize(other: FileObject): List<String>? = try {
		when {
			file != null && other.file != null -> {
				file.toPath().relativize(other.file.toPath()).map { it.toString() }
			}
			smbFile != null && other.smbFile != null -> {
				smbFile.url.toURI().relativize(other.smbFile.url.toURI()).rawPath.split("/")
			}
			else -> throw NotImplementedError()
		}
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}

	fun resolve(other: String) = try {
		when {
			file != null -> FileObject(file.toPath().resolve(other).toFile())
			smbFile != null -> FileObject(smbFile.resolve(other) as SmbFile)
			else -> throw NotImplementedError()
		}
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}

	fun exists(): Boolean? = try {
		when {
			file != null -> file.exists()
			smbFile != null -> smbFile.exists()
			else -> throw NotImplementedError()
		}
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
		null
	}

	fun toFile(): Any? = file ?: smbFile
}