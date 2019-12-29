package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.Main
import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.Icons
import com.poterion.footprint.manager.model.VirtualItem
import com.poterion.footprint.manager.xuggle.ImageSnapListener
import com.xuggle.mediatool.IMediaReader
import com.xuggle.mediatool.ToolFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Paths
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.ImageUtils")
const val THUMBNAIL_BBOX = 500

fun InputStream.toImage(width: Int = 0, height: Int = 0) = use {
	Image(it, width.toDouble(), height.toDouble(), true, true)
}

fun File.toImage(width: Int = 0, height: Int = 0) = FileInputStream(this)
	.use { it.toImage(width, height) }

val Icons.inputStream: InputStream
	get() = Icons::class.java
		.getResourceAsStream("/com/poterion/footprint/manager/icons/${name.toLowerCase()}.png")

fun Icons.toImage(width: Int = 16, height: Int = 16): Image =
	inputStream.toImage(width, height)

fun Icons.toImageView(width: Int = 16, height: Int = 16): ImageView =
	ImageView(toImage(width, height))

fun UriItem.icon() = when {
	this is Device -> when (type) {
		DeviceType.LOCAL -> Icons.HDD
		DeviceType.REMOVABLE -> Icons.SD
		DeviceType.SMB -> Icons.NAS
	}
	this is VirtualItem -> icon
	this is MediaItem -> when {
		imageFormat != null -> when {
			isPanorama -> Icons.PANORAMA
			else -> Icons.PICTURE
		}
		videoFormat != null -> when {
			resolution?.first == 3840 || resolution?.first == 4096 -> Icons.VIDEO_4K
			resolution?.second == 1080 -> Icons.VIDEO_1080
			resolution?.second == 720 -> Icons.VIDEO_720
			resolution?.second == 480 -> Icons.VIDEO_480
			else -> Icons.VIDEO
		}
		else -> null
	}
	else -> null
}

private fun ImageReader.calculateWidth(requestedWidth: Int = 0, requestedHeight: Int = 0) = when {
	requestedHeight > 0 -> requestedWidth
	requestedWidth == 0 -> getWidth(0)
	getWidth(0) >= getHeight(0) -> requestedWidth
	else -> getWidth(0) * requestedWidth / getHeight(0)
}

private fun ImageReader.calculateHeight(requestedWidth: Int = 0, requestedHeight: Int = 0) = when {
	requestedHeight > 0 -> requestedHeight
	requestedWidth == 0 -> getHeight(0)
	getHeight(0) >= getWidth(0) -> requestedWidth
	else -> getHeight(0) * requestedWidth / getWidth(0)
}

private fun ImageReader.calculateDimmensions(requestedWidth: Int = 0, requestedHeight: Int = 0) =
	Dimension(calculateWidth(requestedWidth, requestedHeight), calculateHeight(requestedWidth, requestedHeight))

private fun MediaItem.writeThumbnail(thumbnailFile: File,
									 extension: String,
									 requestedWidth: Int = 0,
									 requestedHeight: Int = 0): Boolean {
	if (thumbnailFile.exists() && thumbnailFile.length() > 0) return true

	val imageReaderIterator = ImageIO.getImageReadersBySuffix(extension)
	val reader = imageReaderIterator.next()
	var retry = 0
	while (retry < 4) {
		retry++
		inputStream()?.use { inputStream ->
			ImageIO.createImageInputStream(inputStream).use { imageInputStream ->
				try {
					reader.setInput(imageInputStream, false, false)
					val thumbnailAvailable = try {
						reader.readerSupportsThumbnails()
								&& reader.hasThumbnails(0)
								&& !listOf("jpg", "png", "bmp").contains(extension)
					} catch (t: Throwable) {
						LOGGER.error(t.message, t)
						false
					}
					val width = reader.calculateWidth(requestedWidth, requestedHeight)
					val height = reader.calculateHeight(requestedWidth, requestedHeight)

					if (retry % 2 == 1 && !thumbnailAvailable) retry++
					val inputImage: BufferedImage = if (retry % 2 == 1) reader.readThumbnail(0, 0) else reader.read(0)

					if (retry <= 2) try {
						val thumbnailsBuilder = try {
							Thumbnails.of(inputImage)
						} catch (t: Throwable) {
							LOGGER.error("${this} (${retry}): ${t.message}", t)
							null
						}

						thumbnailsBuilder
							?.size(width, height)
							?.keepAspectRatio(true)
							?.outputQuality(0.9)
							?.toFile(thumbnailFile)

						if (thumbnailsBuilder != null) return true
					} catch (t: Throwable) {
						LOGGER.error("${this} (${retry}): ${t.message}", t)
					}
					else {
						// Try to read thumbnail if possible, eead the raw image if no thumbnail exists
						try {
							val imageMetadata = reader.getImageMetadata(0)

							val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
							outputImage.createGraphics().drawImage(inputImage, 0, 0, width, height, null)

							val imageWriterIterator = ImageIO.getImageWritersByMIMEType("image/jpeg")
							val writer = imageWriterIterator.next()
							val imageWriteParam = writer.defaultWriteParam
							imageWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
							imageWriteParam.compressionQuality = 0.9f

							thumbnailFile.outputStream().buffered().use { outputStream ->
								val imageOutputStream = MemoryCacheImageOutputStream(outputStream)
								writer.output = imageOutputStream
								val iioImage = IIOImage(outputImage, null, imageMetadata)

								writer.write(null, iioImage, imageWriteParam)
								writer.dispose()
							}
							return true
						} catch (e: IndexOutOfBoundsException) {
							LOGGER.error("${this} (${retry}): ${e.message}", e)
						} catch (t: Throwable) {
							LOGGER.error("${this} (${retry}): ${t.message}", t)
						}
					}
				} catch (t: Throwable) {
					LOGGER.error("${this} (${retry}): ${t.message}", t)
				}
			}
		}
	}
	return false
}

//fun File.generateThumbnail(bbox: Int = 500): Image {
//	val outputStream = ByteArrayOutputStream()
//	writeThumbnail(outputStream, bbox)
//	return Image(ByteArrayInputStream(outputStream.toByteArray()))
//}

private fun MediaItem.getCachedFileInternal(): File = Main.CACHE_PATH.resolve("${id}.${uri.fileExtension()}").toFile()

fun MediaItem.getCachedFile(): File = when (deviceType) {
	DeviceType.LOCAL,
	DeviceType.REMOVABLE -> toUriOrNull()?.let { Paths.get(it) }?.toFile()
		?: throw RuntimeException("Invalid URI: ${uri}")
	DeviceType.SMB -> getCachedFileInternal().also { cachedFile ->
		if (!cachedFile.parentFile.exists()) cachedFile.parentFile.mkdirs()
		cachedFile.takeIf { !it.exists() }?.outputStream()?.buffered()?.use { outputStream ->
			inputStream(false)?.buffered()?.use { inputStream ->
				inputStream.copyTo(outputStream)
			}
		}
	}
}

fun MediaItem.deleteCachedFile() = getCachedFileInternal().takeIf { it.exists() }?.delete()

fun MediaItem.getCachedImage(width: Int = 0, height: Int = 0): File {
	if (width == 0 && height == 0
		&& deviceType in listOf(DeviceType.LOCAL, DeviceType.REMOVABLE)
		&& listOf("jpg", "jpeg").contains(toFileObject()?.extension?.toLowerCase())
	) {
		val cachedFile = toFileObject()?.file
		if (cachedFile?.exists() == true) return cachedFile
	}

	val folder = if (width == 0 && height == 0) "original"
	else if (height == 0) "${width}"
	else "${width}x${height}"

	val cachedFile = Main.CACHE_PATH.resolve(folder).resolve("${id}.jpg").toFile()
	if (!cachedFile.parentFile.exists()) cachedFile.parentFile.mkdirs()
	return cachedFile
}

fun MediaItem.getImageThumbnail(width: Int = 0, height: Int = 0): File =
	getCachedImage(width, height).let { cachedFile ->
		if (!cachedFile.exists() || cachedFile.length() == 0L) measureTime("Thumbnail ${cachedFile} generated") {
			writeThumbnail(cachedFile, toFileObject()?.extension?.toLowerCase() ?: "", width, height)
		}
		cachedFile
	}

fun MediaItem.getVideoThumbnail(width: Int = 0, height: Int = 0): File {
	val cacheFile = getCachedImage(width, height)
	if (!cacheFile.exists() || cacheFile.length() == 0L) uri.also { url ->
		try {
			val mediaReader: IMediaReader = ToolFactory.makeReader(url)
			mediaReader.bufferedImageTypeToGenerate = BufferedImage.TYPE_3BYTE_BGR
			mediaReader.addListener(ImageSnapListener(cacheFile, width, height))
			while (mediaReader.readPacket() == null && (!cacheFile.exists() || cacheFile.length() == 0L));
			mediaReader.close()
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}
	}
	return cacheFile
}

fun MediaItem.removeCache() {
	Main.CACHE_PATH.toFile().listFiles { file -> file.isDirectory }?.forEach { directory ->
		directory.toPath().resolve("${id}.jpg").toFile().takeIf { it.exists() }?.delete()
	}
	Main.CACHE_PATH.resolve("${id}.jpg").toFile().takeIf { it.exists() }?.delete()
}
