package com.poterion.footprint.manager.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import javax.xml.bind.DatatypeConverter

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.IOUtils")

fun String.fileNameWithoutExtension(): String = substringBeforeLast(".")

fun Path.fileNameWithoutExtension(): String = fileName.toString().fileNameWithoutExtension()

fun String.fileExtension(): String = substringAfterLast(".", "").takeIf { it.length < 5 } ?: ""

fun String.ensureSuffix(suffix: String, ignoreCase: Boolean = false) =
	if (endsWith(suffix, ignoreCase)) this else "${this}${suffix}"

fun File.calculateHash(algorithm: String) = FileInputStream(this).calculateHash(algorithm)

fun ByteArray.calculateHash(algorithm: String): String {
	val digest = MessageDigest.getInstance(algorithm)
	digest.update(this)
	return DatatypeConverter.printHexBinary(digest.digest())
}

fun InputStream.calculateHash(algorithm: String): String = BufferedInputStream(this).use { inputStream ->
	val digest = MessageDigest.getInstance(algorithm)
	val block = ByteArray(8192)
	var length: Int
	while (inputStream.read(block).also { length = it } > 0) {
		digest.update(block, 0, length)
	}
	return DatatypeConverter.printHexBinary(digest.digest())
}

fun InputStream.gzipped() = GZIPInputStream(BufferedInputStream(this))

fun InputStream.tar() = TarArchiveInputStream(this)

fun InputStream.unTarTo(destinationPath: Path) = tar().extractTo(destinationPath)

fun InputStream.unGzipTarTo(destinationPath: Path) = gzipped().unTarTo(destinationPath)

fun TarArchiveInputStream.extractTo(destinationPath: Path) = use {
	try {
		var tarEntry: TarArchiveEntry?
		while (nextTarEntry.also { tarEntry = it } != null) {
			if (tarEntry?.isDirectory != false) continue
			val outputFile = destinationPath.resolve(tarEntry!!.name).toFile()
				.also { it.parentFile.mkdirs() }
			IOUtils.copy(this, FileOutputStream(outputFile))
		}
	} catch (t: Throwable) {
		LOGGER.error(t.message, t)
	}
}