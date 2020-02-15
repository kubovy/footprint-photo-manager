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
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.enums.TagValueType
import com.poterion.footprint.manager.model.FileObject
import com.poterion.footprint.manager.model.VirtualItem
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI
import java.time.temporal.ChronoField
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
private val LOGGER = LoggerFactory.getLogger("com.poterion.footprint.manager.utils.model.MediaItemUtils")

val URI.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.uri.startsWith(toString()) }

val VirtualItem.mediaItems: Collection<MediaItem>
	get() = Database.list(MediaItem::class).filter { it.uri.startsWith(uri) }

fun URI.toMediaItemOrNull(): MediaItem? = Database.list(MediaItem::class).find { it.uri == toString() }

fun File.toMediaItemOrNull(): MediaItem? = FileObject(this).toMediaItemOrNull()

fun SmbFile.toMediaItemOrNull(): MediaItem? = FileObject(this).toMediaItemOrNull()

val MediaItem.metadata: Collection<MetadataTag>
	get() = Database.find(MetadataTag::class) { builder, root ->
		listOf(builder.equal(root.get<MetadataTag>("mediaItemId"),
							 id))
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

private fun <R : Any> List<List<MetadataTag>?>.findMetadata(transformation: (MetadataTag) -> R?,
															chooser: (List<List<R>>) -> List<R>?): List<R>? =
		filterNotNull()
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
			val (latDeg, latMin, latSec) = lat.toRationalList().map { it?.toDouble() ?: 0.0 } + listOf(0.0, 0.0, 0.0)
			val (lngDeg, lngMin, lngSec) = lng.toRationalList().map { it?.toDouble() ?: 0.0 } + listOf(0.0, 0.0, 0.0)
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
							mediaItemSiblingsCacheLock.write {
								mediaItemSiblingsCache?.remove(deletedItem.id)
							}
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
