package com.poterion.footprint.manager.workers

import com.drew.imaging.ImageMetadataReader
import com.poterion.footprint.manager.Main
import com.poterion.footprint.manager.data.BaseItem
import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.footprint.manager.enums.PhotoFormat
import com.poterion.footprint.manager.enums.VideoFormat
import com.poterion.footprint.manager.model.FileObject
import com.poterion.footprint.manager.model.Progress
import com.poterion.footprint.manager.utils.*
import jcifs.smb.SmbAuthException
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ScanWorker(arg: Pair<URI, Boolean>) :
	Worker<Pair<URI, Boolean>, Pair<Progress, Any?>, Collection<MediaItem>>(arg) {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(ScanWorker::class.java)
		private val LISTING_PARALLELISM = Runtime.getRuntime().availableProcessors() - 1
		private val TRANSFORMING_PARALLELISM = Runtime.getRuntime().availableProcessors() - 1
		private val PROCESSING_PARALLELISM = Runtime.getRuntime().availableProcessors() - 1
		private const val CHUNK_SIZE = 100
	}

	private val photoExtensions = PhotoFormat.values().map { it.ext }.toTypedArray()
	private val videoExtensions = VideoFormat.values().map { it.ext }.toTypedArray()
	private val mediaExtensions = photoExtensions + videoExtensions
	private lateinit var mediaItemsUriMap: MutableMap<String, MediaItem>
	private val onSaveListener = { item: BaseItem -> if (item is MediaItem) mediaItemsUriMap[item.uri] = item }
	private var progress = Progress()
	private val mediaItemDirtinessMap = mutableMapOf<String, Boolean>()
	private var smbFileList = mutableMapOf<String, List<String>>()


	override fun doWork(arg: Pair<URI, Boolean>): List<MediaItem>? {
		val (uri, force) = arg
		val device = Database.list(Device::class)
			.find { uri.toString().startsWith(it.uri) }
		if (device != null) try {
			device.lastSeenAt = Instant.now()
			Database.save(device)
			mediaItemsUriMap = device.mediaItems.map { it.uri to it }.toMap().toMutableMap()
			Database.addOnSaveListener(onSaveListener)
			return uri.scan(force)
		} finally {
			Database.removeOnSaveListener(onSaveListener)
		}
		return null
	}

	private fun URI.scan(force: Boolean): List<MediaItem> {
		LOGGER.info("Scanning: ${this}")

		val parent = toFileObject()
		if (parent?.isDirectory == true) {
			update(Progress.INDETERMINATE to null)

			val found = when {
				parent.file != null -> parent.file.listFileObjects()
				parent.smbFile != null -> parent.smbFile.listFileObjects()
				else -> throw NotImplementedError()
			}.sortedWith(compareBy({ it.parent }, { it.name }))
			LOGGER.info("Found ${found.size} files in ${this}")
			progress.total.set(found.size)

			device?.scanContext().use {
				val all = found.chunked(CHUNK_SIZE)
					.parallelStreamMap(TRANSFORMING_PARALLELISM) { files ->
						files
							.mapNotNull { it.toMediaItem(this) }
							.also { items ->
								items.filter { (_, new) -> new }
									.takeIf { it.isNotEmpty() }
									?.map { (item, _) -> item }
									?.also { Database.saveAll(it) }
							}
							.map { (item, _) -> item }
							.also { update(progress.apply { progress.addAndGet(CHUNK_SIZE) } to it) }
					}
					.flatten()

				update(Progress.INDETERMINATE to null)
				progress.progress.set(0)

				val scanned = all.filter { !it.isUnscanned }
				val unscanned = all.filter { it.isUnscanned }

				unscanned
					.chunked(CHUNK_SIZE)
					.parallelStreamProcess(PROCESSING_PARALLELISM) { mediaItems ->
						for (mediaItem in mediaItems) {
							update(progress.apply { progress.getAndIncrement() } to mediaItem)
							mediaItem.useSambaCacheThumbnail()
							mediaItem.process(force)
						}
					}

				scanned
					.chunked(CHUNK_SIZE)
					.parallelStreamProcess { mediaItems ->
						for (mediaItem in mediaItems) {
							update(progress.apply { progress.getAndIncrement() } to mediaItem)
							mediaItem.process(force)
						}
					}

				all.also { cleanOrphans(it) }
					.also { update(progress.apply { progress.set(total.get()) } to null) }
			}
		} else {
			update(Progress.NOTHING_TODO to null)
		}
		return emptyList()
	}

	private fun Device.scanContext(): AutoCloseable = object : AutoCloseable {
		private var closeables = mutableListOf<AutoCloseable>()

		init {
			if (type == DeviceType.SMB) {
				buildSambaFileCache()
				closeables.add(downloadSambaCache("metadata"))
				closeables.add(downloadSambaCache("thumbnails"))
			}
		}

		override fun close() = closeables.forEach { it.close() }
	}

	private fun Device.buildSambaFileCache() = takeIf { type == DeviceType.SMB }
		?.toUriOrNull()
		?.resolve(".footprint/list")
		?.toFileObject()
		?.smbFile
		?.takeIf { it.exists() }
		?.inputStream
		?.buffered()
		?.use { inputStream ->
			val scanner = Scanner(inputStream)
			scanner.useDelimiter("\n")

			while (scanner.hasNext()) {
				val line = scanner.next().split("|")
				if (line.size >= 5) {
					val uri = device?.toUriOrNull()?.resolve(line.first())?.toString()
					if (uri != null) smbFileList[uri] = line
				}
			}
		}

	private val Device.smbCachePath: Path?
		get() = try {
			Main.CACHE_PATH.resolve("smb-${id}")
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			null
		}

	private fun Device.downloadSambaCache(type: String): AutoCloseable = object : AutoCloseable {
		init {
			clear()
			this@downloadSambaCache
				.takeIf { it.type == DeviceType.SMB }
				?.toUriOrNull()
				?.resolve(".footprint/${type}.tar.gz")
				?.toFileObject()
				?.smbFile
				?.takeIf { it.exists() }
				?.let { smbFile -> smbCachePath?.let { smbFile to it } }
				?.let { (smbFile, cachePath) ->
					try {
						smbFile to cachePath.resolve(type)
					} catch (t: Throwable) {
						LOGGER.error(t.message, t)
						null
					}
				}
				?.let { (smbFile, cacheFile) -> smbFile.inputStream.use { it.unGzipTarTo(cacheFile) } }
		}

		private fun clear() = try {
			this@downloadSambaCache
				.smbCachePath
				?.resolve(type)
				?.toFile()
				?.takeIf { it.exists() }
				?.deleteRecursively()
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}

		override fun close() {
			clear()
		}
	}

	private fun MediaItem.findSambaCacheFile(type: String, extension: String): File? =
		takeIf { deviceType == DeviceType.SMB }
			?.toUriOrNull()
			?.let { device?.toUriOrNull()?.relativize(it) }
			?.path
			?.let { device?.smbCachePath?.resolve(type)?.resolve(it) }
			?.let { it.parent.resolve("${it.fileNameWithoutExtension()}.${extension}") }
			?.toFile()

	private fun MediaItem.findSambaCacheMetadata(): List<MetadataTag> = findSambaCacheFile("metadata", "txt")
		?.inputStream()
		?.use { inputStream ->
			val scanner = Scanner(inputStream)
			scanner.useDelimiter("\n")
			val results = mutableListOf<MatchResult?>()
			while (scanner.hasNext()) {
				results.add("\\[([\\w:]+)]\\s*(\\d+|-)\\s*(\\w+)\\s*:\\s(.*)".toRegex().matchEntire(scanner.next()))
			}
			results
		}
		?.filterNotNull()
		?.map { it.groupValues }
		?.map {
			MetadataTag(
					mediaItemId = id,
					directory = it.getOrNull(1) ?: "",
					tagType = it.getOrNull(2)?.toIntOrNull() ?: 0,
					name = it.getOrNull(3) ?: "",
					description = it.getOrNull(4))
		}
		?: emptyList()

	private fun MediaItem.useSambaCacheThumbnail() = getCachedImage(width = THUMBNAIL_BBOX)
		.takeIf { !it.exists() || it.length() == 0L }
		?.let { cacheFile -> findSambaCacheFile("thumbnails", "jpg")?.let { it to cacheFile } }
		?.also { (smbFile, cacheFile) -> smbFile.renameTo(cacheFile) }

	private fun URI.cleanOrphans(found: List<MediaItem>) =
		mediaItems // Orphans
			.filter { mediaItem -> found.none { it.id == mediaItem.id } }
			.map { it.apply { deletedAt = Instant.now() } }
			.also { orphans ->
				LOGGER.info("Cleaning ${orphans.size} orphans")
				if (orphans.any { it.device?.isPrimary == true }) orphans
					.map { it.apply { deletedAt = Instant.now() } }
					.also { Database.saveAll(it) }
				else Database.deleteAll(orphans)
			}

	private val FileObject.isRelevant: Boolean
		get() = !name.startsWith(".") && (isDirectory == true || mediaExtensions.any { name.endsWith(".${it}", true) })

	private fun File.listFileObjects(): List<FileObject> = try {
		update(Progress.INDETERMINATE to absolutePath)
		listFiles { file -> FileObject(file).isRelevant }
			.also { if (it == null) LOGGER.error("NULL files in ${parent}") }
			?.toList()
			?.parallelStreamMap(LISTING_PARALLELISM) { file ->
				if (file.isDirectory) file.listFileObjects()
				else listOf(FileObject(file))
			}
			?.flatten()
			?: emptyList()
	} catch (t: Throwable) {
		LOGGER.error("${this}: ${t.message}", t)
		val mediaItem = getMediaItem()
		Notifications.notify(value = t.message ?: "${t::class.java.simpleName} occurred while scanning.",
							 type = NotificationType.SCAN_PROBLEM,
							 name = "${this}",
							 deviceId = mediaItem?.deviceId,
							 mediaItemId = mediaItem?.id)
		emptyList()
	}

	private fun SmbFile.listFileObjects(): List<FileObject> {
		update(Progress.INDETERMINATE to path)
		var retries = 0
		var result: List<FileObject>? = null
		var lastError: Throwable? = null
		while (retries < 5 && result == null) try {
			connectTimeout = 5_000
			readTimeout = 20_000
			result = listFiles { file -> FileObject(file).isRelevant }
				.also { if (it == null) LOGGER.error("NULL files in ${parent}") }
				?.toList()
				?.parallelStreamMap(LISTING_PARALLELISM) { file ->
					if (file.isDirectory) file.listFileObjects() // FIXME
					else listOf(FileObject(file))
				}
				?.flatten()
				?: emptyList()
		} catch (e: SmbAuthException) {
			LOGGER.error("${this}: ${e.message}", e)
			lastError = e
			retries = Int.MAX_VALUE
		} catch (t: Throwable) {
			LOGGER.error("${this}: ${t.message}", t)
			lastError = t
			retries++
		}
		if (result == null) {
			val mediaItem = getMediaItem()
			Notifications.notify(
					value = lastError?.message ?: "${lastError?.javaClass?.simpleName
						?: "Unknown error"} while scanning.",
					type = NotificationType.SCAN_PROBLEM,
					name = "${this}",
					deviceId = mediaItem?.deviceId,
					mediaItemId = mediaItem?.id)
		}
		return result ?: emptyList()
	}

	private fun FileObject.toMediaItem(rootUri: URI): Pair<MediaItem, Boolean>? =
		uri?.toString()?.let { fileObjectUri ->
			mediaItemsUriMap[fileObjectUri]?.let { it to false }
				?: MediaItem(deviceId = rootUri.device?.id,
							 name = name,
							 uri = fileObjectUri,
							 imageFormat = PhotoFormat.values().find { it.ext.equals(extension, true) },
							 videoFormat = VideoFormat.values().find { it.ext.equals(extension, true) },
							 audioCodingFormat = null, // TODO
							 videoCodingFormat = null) to true // TODO
		}

	private fun MediaItem.process(force: Boolean) {
		val start = System.currentTimeMillis()
		if (!isDirty && !force) return

		val hash = takeUnless { isDirty || hash == null }?.hash
			?: smbFileList[uri]?.get(4)?.toUpperCase()
			?: measureTime("Generated hash %s ?= ${smbFileList[uri]?.get(4)} for ${this.uri}") {
				inputStream()?.calculateHash("SHA-256")
			}
			?: ""

//		if (update || contentHash == null) inputStream()?.use { inputStream ->
//			try {
//				val imageReaderIterator = ImageIO.getImageReadersBySuffix(extension.toLowerCase())
//				val reader = imageReaderIterator.next()
//				reader.setInput(inputStream, false, false)
//				val image = reader.read(0)
//				val bos = ByteArrayOutputStream()
//				ImageIO.write(image, extension.toLowerCase(), bos)
//				contentHash = bos.toByteArray().calculateHash("SHA-512")
//				LOGGER.info("Generated content hash ${contentHash} for ${this.uri}")
//			} catch (t: Throwable) {
//				LOGGER.error("Error while processing ${this.uri}: ${t.message}", t)
//				Notifications.notify(value = e.message ?: "${t.javaClass.simpleName} occurred while calculating content hash.",
//									 type = NotificationType.PROCESSING_PROBLEM,
//									 mediaItemId = id)
//			}
//		}

		if (isDirty) also {
			it.length = smbFileList[uri]?.get(1)?.toLongOrNull() ?: toFileObject()?.length ?: 0
			it.hash = hash
			//it.contentHash = contentHash
			it.createdAt =
				Instant.ofEpochMilli(smbFileList[uri]?.get(2)?.toLongOrNull() ?: toFileObject()?.createdAt ?: 0)
			it.updatedAt =
				Instant.ofEpochMilli(smbFileList[uri]?.get(3)?.toLongOrNull() ?: toFileObject()?.updatedAt ?: 0)
		}

		Database.deleteAll(this.metadata)
		val entities = mutableListOf<BaseItem>(this)
		inputStream()
			?.let { BufferedInputStream(it) }
			?.use { inputStream ->
				val cachedMetadata: List<MetadataTag> = when (deviceType) {
					DeviceType.SMB -> findSambaCacheMetadata()
					else -> emptyList()
				}
				LOGGER.info("Found ${cachedMetadata.size} cached metadata tags")

				try {
					val fileMetadata = ImageMetadataReader.readMetadata(inputStream)
					for (directory in fileMetadata.directories) {
						for (tag in directory.tags) {
							if (directory.isRelevant(tag.tagType)) {
								var metadataTag: MetadataTag? = null
								try {
									metadataTag = this.metadata
										.find {
											it.directory == directory.name
													&& it.name == tag.tagName
													&& it.tagType == tag.tagType
													&& it.valueType == directory.getTagValueType(tag.tagType)
										}
										?: MetadataTag(
												mediaItemId = id,
												directory = directory.name,
												name = tag.tagName,
												tagType = tag.tagType,
												valueType = directory.getTagValueType(tag.tagType))

									metadataTag.apply {
										raw = directory.getString(tag.tagType)
										description = tag.description
									}

									entities.add(metadataTag)
								} catch (t: Throwable) {
									LOGGER.error("Error while processing metadata of ${this.uri}: ${t.message}", t)
									Notifications.notify(value = t.message
										?: "${t.javaClass.simpleName} occurred while processing metadata.",
														 type = NotificationType.METADATA_ERROR,
														 name = directory.name,
														 deviceId = deviceId,
														 mediaItemId = id,
														 metadataTagId = metadataTag?.id)
								}
							}
						}
						if (directory.hasErrors()) {
							for (error in directory.errors) {
								LOGGER.error("ERROR: ${error}")
								Notifications.notify(value = error,
													 type = NotificationType.METADATA_ERROR,
													 name = directory.name,
													 deviceId = deviceId,
													 mediaItemId = id)
							}
						}
					}

					Database.saveAll(entities)
					LOGGER.info("Processed ${this.uri} with ${entities.size - 1} tags in ${System.currentTimeMillis() - start}ms")
				} catch (t: Throwable) {
					LOGGER.error("Error while processing ${this.uri}: ${t.message}", t)
					Notifications.notify(
							value = t.message ?: "${t.javaClass.simpleName} occurred while processing metadata.",
							type = NotificationType.PROCESSING_PROBLEM,
							deviceId = deviceId,
							mediaItemId = id)
				}
			}

//		if (deviceType.remote) {
//			if (imageFormat != null) getImageThumbnail(width = CACHE_BBOX)
//			else if (videoFormat != null) getVideoThumbnail(width = CACHE_BBOX)
//		}

		deleteCachedFile()
	}

	private val MediaItem.isUnscanned: Boolean
		get() = updatedAt == Instant.EPOCH
				|| hash == null
				|| length == 0L

	private val MediaItem.isDirty: Boolean
		get() = id?.let {
			mediaItemDirtinessMap.getOrPut(it, {
				isUnscanned
						//|| (imageFormat != null && contentHash == null)
						|| updatedAt.toEpochMilli() != toFileObject()?.updatedAt
						|| length != toFileObject()?.length
			})
		} ?: false
}
