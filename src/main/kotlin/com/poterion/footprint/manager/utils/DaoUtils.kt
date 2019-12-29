package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.*
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.TagValueType
import com.poterion.footprint.manager.model.FileObject
import com.poterion.footprint.manager.model.VirtualItem
import com.poterion.footprint.manager.xuggle.SmbFileProtocolHandler
import com.xuggle.xuggler.io.FileProtocolHandler
import com.xuggle.xuggler.io.IURLProtocolHandler
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.DaoUtils")

/* Common */

fun UriItem.exists(): Boolean = id?.let { Database.get(this::class, it) } != null

/* Device */

var Device.authString: String?
	get() = auth?.decrypt()
	set(value) {
		auth = value?.encrypt()
	}

val Device.usernamePassword: Pair<String, String>?
	get() = authString
		?.split(":".toRegex(), 2)
		?.takeIf { it.size == 2 }
		?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
		?.let { (username, password) -> username to password }

private val Device.authenticator: NtlmPasswordAuthenticator?
	get() = usernamePassword?.let { (username, password) -> NtlmPasswordAuthenticator("", username, password) }

val Device.cifsContext: CIFSContext
	get() = SingletonContext.getInstance().withCredentials(authenticator)

/* Device Relation */

val UriItem.device: Device?
	get() = when (this) {
		is MediaItem -> deviceId?.let { Database.get(Device::class, it) }
		else -> Database.list(Device::class).find { uri.startsWith(it.uri) }
	}

val URI.device: Device?
	get() = Database.list(Device::class).find { toString().startsWith(it.uri) }

val Path.device: Device?
	get() = toUri().device

private val deviceTypeCache: MutableMap<String, DeviceType> = mutableMapOf()
private val deviceTypeCacheLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

private fun getDeviceType(uri: String): DeviceType {
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

val UriItem.deviceType: DeviceType
	get() = getDeviceType(uri)

val URI.deviceType: DeviceType
	get() = getDeviceType(toString())

/* Media Relation */

fun getPrimaryDevice(): Device? = Database.list(Device::class).find { it.isPrimary }

fun getPrimaryMediaItems(): Collection<MediaItem>? = getPrimaryDevice()?.mediaItems

val Device.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.deviceId == id }

val Notification.device: Device?
	get() = deviceId?.let { Database.get(Device::class, it) }

val Notification.mediaItem: MediaItem?
	get() = mediaItemId?.let { Database.get(MediaItem::class, it) }

val Notification.metadataTag: MetadataTag?
	get() = metadataTagId?.let { Database.get(MetadataTag::class, it) }

val URI.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.uri.startsWith(toString()) }

val VirtualItem.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.uri.startsWith(uri) }

fun URI.getMediaItem(): MediaItem? = Database.list(MediaItem::class).find { it.uri == toString() }

fun FileObject.getMediaItem(): MediaItem? = uri?.getMediaItem()

fun File.getMediaItem(): MediaItem? = FileObject(this).getMediaItem()

fun SmbFile.getMediaItem(): MediaItem? = FileObject(this).getMediaItem()

/* Metadata Relation */

val MediaItem.metadata: Collection<MetadataTag>
	get() = Database.find(MetadataTag::class) { builder, root ->
		listOf(builder.equal(root.get<MetadataTag>("mediaItemId"), id))
	}

fun MediaItem.typedMetadata(directory: String,
							valueType: TagValueType,
							chooser: (List<MetadataTag>) -> MetadataTag?,
							vararg tagTypes: Int): List<MetadataTag>? =
	Database.find(MetadataTag::class) { builder, root ->
		listOf(builder.equal(root.get<MetadataTag>("mediaItemId"), id),
			   builder.equal(root.get<MetadataTag>("directory"), directory),
			   builder.equal(root.get<MetadataTag>("valueType"), valueType),
			   root.get<MetadataTag>("tagType").`in`(tagTypes.toList()))
	}.groupBy { it.tagType }
		.let { map -> tagTypes.map { map.getOrDefault(it, emptyList()).let(chooser) } }
		.filterNotNull()
		.takeIf { it.size == tagTypes.size }

fun MediaItem.namedMetadata(directory: String,
							valueType: TagValueType,
							chooser: (List<MetadataTag>) -> MetadataTag?,
							vararg names: String): List<MetadataTag>? =
	Database.find(MetadataTag::class) { builder, root ->
		listOf(builder.equal(root.get<MetadataTag>("mediaItemId"), id),
			   builder.equal(root.get<MetadataTag>("directory"), directory),
			   builder.equal(root.get<MetadataTag>("valueType"), valueType),
			   root.get<MetadataTag>("name").`in`(names.toList()))
	}.groupBy { it.name }
		.let { map -> names.map { map.getOrDefault(it, emptyList()).let(chooser) } }
		.filterNotNull()
		.takeIf { it.size == names.size }

fun MediaItem.metadata(directory: String,
					   chooser: (List<MetadataTag>) -> MetadataTag?,
					   vararg tagTypes: Int): List<MetadataTag>? =
	Database.find(MetadataTag::class) { builder, root ->
		listOf(builder.equal(root.get<MetadataTag>("mediaItemId"), id),
			   builder.equal(root.get<MetadataTag>("directory"), directory),
			   root.get<MetadataTag>("tagType").`in`(tagTypes.toList()))
	}.groupBy { it.tagType }
		.let { map -> tagTypes.map { map.getOrDefault(it, emptyList()).let(chooser) } }
		.filterNotNull()
		.takeIf { it.size == tagTypes.size }

fun <R : Any> List<List<MetadataTag>?>.findMetadata(transformation: (MetadataTag) -> R?,
													chooser: (List<List<R>>) -> List<R>?): List<R>? = filterNotNull()
	.mapNotNull { l -> l.map(transformation).takeIf { r -> r.none { it == null } }?.filterNotNull() }
	.let(chooser)

val MediaItem.resolution: Pair<Int, Int>?
	get() {
		val chooser = { l: Collection<MetadataTag> -> l.maxBy { it.raw?.toIntOrNull() ?: 0 } }
		return listOf(metadata("JPEG", chooser, 3, 1),
					  metadata("Exif IFD0", chooser, 256, 257),
					  metadata("Exif SubIFD", chooser, 256, 257),
					  metadata("Exif SubIFD", chooser, 40962, 40963),
					  metadata("MP4 Video", chooser, 204, 205))
			.findMetadata({ it.raw?.toIntOrNull() }, { results -> results.maxBy { it.sum() } })
			?.let { (w, h) -> w to h }
	}

val MediaItem.resolutionRation: Double?
	get() = resolution?.let { (width, height) -> width.toDouble() / height.toDouble() }

val MediaItem.isPanorama: Boolean
	get() = resolutionRation == 2.0 || (resolution?.first ?: 0) > 6000

val MediaItem.formattedResolution: String?
	get() = resolution?.formattedResolution

val Pair<Int, Int>.formattedResolution: String?
	get() = let { (width, height) -> "${width}x${height}" }

val MediaItem.location: Pair<Double, Double>?
	get() = metadata("GPS", { it.firstOrNull() }, 1, 2, 3, 4) // GPS
		?.mapNotNull { it.raw }
		?.takeIf { it.size == 4 }
		?.let { (ns, lat, ew, lng) ->
			Pair(
					lat.toRationalList()
						.map { it?.toDouble() ?: 0.0 }
						.reduceIndexed { index, acc, d -> acc + (d / max(1, 60 * index)) }
						.let { it * if (ns == "N") 1.0 else -1.0 },
					lng.toRationalList()
						.map { it?.toDouble() ?: 0.0 }
						.reduceIndexed { index, acc, d -> acc + (d / max(1, 60 * index)) }
						.let { it * if (ew == "E") 1.0 else -1.0 })
		}

val MediaItem.formattedLocation: String?
	get() = metadata("GPS", { it.firstOrNull() }, 1, 2, 3, 4) // GPS
		?.mapNotNull { it.raw }
		?.takeIf { it.size == 4 }
		?.let { (ns, lat, ew, lng) ->
			val (latDeg, latMin, latSec) = lat.toRationalList().map { it?.toDouble() ?: 0.0 } +
					listOf(0.0, 0.0, 0.0)
			val (lngDeg, lngMin, lngSec) = lng.toRationalList().map { it?.toDouble() ?: 0.0 } +
					listOf(0.0, 0.0, 0.0)
			"%s%02.0f°%02.0f'%02.3f\" %s%03.0f°%02.0f'%02.3f\""
				.format(ns, latDeg, latMin, latSec, ew, lngDeg, lngMin, lngSec)
		}

val MediaItem.isPrimary: Boolean
	get() = device?.isPrimary == true

val MediaItem.isPresentInPrimary: Boolean
	get() = isPrimary || hash != null && getPrimaryMediaItems()?.any { it.hash == hash } == true

private var mediaItemSiblingsCache: MutableMap<String, Collection<MediaItem>>? = null
private val mediaItemSiblingsCacheLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

val MediaItem.siblings: Collection<MediaItem>
	get() {
		var siblings: Collection<MediaItem>? = null
		mediaItemSiblingsCacheLock.read {
			if (mediaItemSiblingsCache == null) {
				mediaItemSiblingsCacheLock.write {
					if (mediaItemSiblingsCache == null) {
						mediaItemSiblingsCache = mutableMapOf()
						Database.addOnSaveListener { deletedItem ->
							mediaItemSiblingsCacheLock.write { mediaItemSiblingsCache?.remove(deletedItem.id) }
						}
					}
				}
			}
			val mediaItemId = id
			if (mediaItemId != null) {
				siblings = mediaItemSiblingsCache?.get(mediaItemId)
				if (siblings == null) {
					siblings = toFileObject()
						?.parent
						?.let { parent -> device?.mediaItems?.filter { it.toFileObject()?.parent == parent } }
						?: emptyList()
					mediaItemSiblingsCacheLock.write {
						if (mediaItemSiblingsCache?.containsKey(mediaItemId) == false && siblings != null) {
							mediaItemSiblingsCache?.put(mediaItemId, siblings!!)
						}
					}
				}
			}
		}
		return siblings ?: emptyList()
	}

val Collection<MediaItem>.dateSpan
	get() = (minBy { it.createdAt }?.createdAt to maxBy { it.createdAt }?.createdAt)
		.let { (begin, end) -> begin?.let { b -> end?.let { e -> b to e } } }

fun Pair<Instant, Instant>.formatDateSpan(yearPattern: String,
										  monthPattern: String,
										  dayPattern: String,
										  condensed: Boolean = true): String {
	val (begin, end) = this
	val beginYear = begin.get(ChronoField.YEAR).toString().let { if (yearPattern.length > 2) it else it.substring(2) }
	val endYear = end.get(ChronoField.YEAR).toString().let { if (yearPattern.length > 2) it else it.substring(2) }
	val beginMonth =
		begin.get(ChronoField.MONTH_OF_YEAR).let { if (monthPattern.length > 1) "%02d".format(it) else it.toString() }
	val endMonth =
		end.get(ChronoField.MONTH_OF_YEAR).let { if (monthPattern.length > 1) "%02d".format(it) else it.toString() }
	val beginDay =
		begin.get(ChronoField.DAY_OF_MONTH).let { if (dayPattern.length > 1) "%02d".format(it) else it.toString() }
	val endDay =
		end.get(ChronoField.DAY_OF_MONTH).let { if (dayPattern.length > 1) "%02d".format(it) else it.toString() }

	return if (condensed) {
		var result = "${beginYear}-${beginMonth}-${beginDay}"
		if (beginYear != endYear) result += "-${endYear}"
		if (beginMonth != endMonth) result += "-${endMonth}"
		if (beginDay != endDay) result += "-${endDay}"
		result
	} else {
		"${beginYear}-${beginMonth}-${beginDay}-${endYear}-${endMonth}-${endDay}"
	}
}

fun MediaItem.getTargetPath(title: String): String {
	var result = Settings.folderPattern
	result = "%DATESPAN\\( *(y+) *, *(m+) *, *(d+)(:? *, *(\\w+))? *\\)".toRegex()
		.find(result)
		?.groupValues
		?.takeIf { it.size == 6 }
		?.let { group ->
			group[0] to (siblings.dateSpan ?: (createdAt to createdAt))
				.formatDateSpan(group[1], group[2], group[3], group[5].toLowerCase() == "condensed")
		}
		?.let { (match, span) -> result.replace(match, span) }
		?: result

	result = "%NUM\\( *(\\d+) *\\)".toRegex()
		.find(result)
		?.groupValues
		?.takeIf { it.size == 2 }
		?.let { group ->
			group[0] to siblings
				.filter { it.createdAt.isBefore(createdAt) }
				.size
				.let { "%0${group[1]}d".format(it) }
		}
		?.let { (match, span) -> result.replace(match, span) }
		?: result

	return result.replace("%TITLE", title)
		.replace("%EXT", toFileObject()?.extension?.toUpperCase() ?: "")
		.replace("%ext", toFileObject()?.extension?.toLowerCase() ?: "")
		.replace("%yyyy", createdAt.get(ChronoField.YEAR).toString())
		.replace("%yy", createdAt.get(ChronoField.YEAR).toString().substring(2))
		.replace("%mm", createdAt.get(ChronoField.MONTH_OF_YEAR).let { "%02d".format(it) })
		.replace("%m", createdAt.get(ChronoField.MONTH_OF_YEAR).toString())
		.replace("%dd", createdAt.get(ChronoField.DAY_OF_MONTH).toString().let { "%02d".format(it) })
		.replace("%d", createdAt.get(ChronoField.DAY_OF_MONTH).toString())
		.replace("%HH", createdAt.get(ChronoField.HOUR_OF_DAY).toString().let { "%02d".format(it) })
		.replace("%H", createdAt.get(ChronoField.HOUR_OF_DAY).toString())
		.replace("%hh", createdAt.get(ChronoField.HOUR_OF_AMPM).toString().let { "%02d".format(it) })
		.replace("%h", createdAt.get(ChronoField.HOUR_OF_AMPM).toString())
		.replace("%MM", createdAt.get(ChronoField.MINUTE_OF_HOUR).toString().let { "%02d".format(it) })
		.replace("%M", createdAt.get(ChronoField.MINUTE_OF_HOUR).toString())
		.replace("%SS", createdAt.get(ChronoField.SECOND_OF_MINUTE).toString().let { "%02d".format(it) })
		.replace("%S", createdAt.get(ChronoField.SECOND_OF_MINUTE).toString())
		.replace("%a", createdAt.get(ChronoField.AMPM_OF_DAY).let { if (it == 0) "AM" else "PM" })
}

val MediaItem.isPathValid: Boolean
	get() = !isPrimary || toFileObject()
		?.let { fileObject -> device?.toFileObject()?.relativize(fileObject) }
		?.joinToString("/")
		?.let { getTargetPath(".*").toRegex().matches(it) } == true

val Pair<Double, Double>.formattedLocation: String
	get() {
		val lat = if (first > 0) "N" else "S"
		val latDeg = first
		val latMin = (first - latDeg) * 60
		val latSec = (((first - latDeg) * 60) - latMin) * 60

		val lng = if (second > 0) "E" else "W"
		val lngDeg = second
		val lngMin = (second - latDeg) * 60
		val lngSec = (((second - latDeg) * 60) - latMin) * 60

		return "%s%02.0f°%02.0f'%02.3f\" %s%03.0f°%02.0f'%02.3f\""
			.format(lat, latDeg, latMin, latSec, lng, lngDeg, lngMin, lngSec)
	}

/* Conversions */
private fun String.toUriOrNull() = try {
	URI(this)
} catch (t: Throwable) {
	LOGGER.error(t.message, t)
	null
}

fun UriItem.toUriOrNull(): URI? = uri.toUriOrNull()

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

/* I/O Streams */

fun MediaItem.inputStream(cached: Boolean = true): InputStream? = toFileObject()?.toFile()?.let { file ->
	try {
		when (file) {
			is File -> file.inputStream()
			is SmbFile -> if (cached) getCachedFile().inputStream() else file.inputStream
			else -> null
		}
	} catch (t: Throwable) {
		LOGGER.error("${file}: ${t.message}", t)
		null
	}
}

/* */

fun UriItem.fileProtocolHandler(): IURLProtocolHandler? = when (deviceType) {
	DeviceType.LOCAL,
	DeviceType.REMOVABLE -> toFileObject()?.file?.let { FileProtocolHandler(it) }
	DeviceType.SMB -> toFileObject()?.smbFile?.let { SmbFileProtocolHandler(it) }
}
