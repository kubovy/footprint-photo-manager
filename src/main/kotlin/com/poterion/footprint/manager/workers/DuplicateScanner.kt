package com.poterion.footprint.manager.workers

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.Notification
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.footprint.manager.model.Progress
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.Notifications
import com.poterion.footprint.manager.utils.device
import com.poterion.footprint.manager.utils.mediaItems
import com.poterion.utils.kotlin.uriDecode

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DuplicateScanner : Worker<Void?, Pair<Progress, Int>, Int>(null) {
	private val progress = Progress()

	override fun doWork(arg: Void?): Int {
		update(progress to 0)
		val oldNotifications = Database.list(Notification::class).filter { it.type == NotificationType.DUPLICATE }
		Notifications.dismissAll(oldNotifications)

		val cache = mutableMapOf<String, Collection<String>>()
		val mediaItems = Database.list(Device::class).find { it.isPrimary }?.mediaItems ?: emptyList()
		update(progress.setTotal(mediaItems.size) to 0)
		var count = 0
		val notifications = mutableListOf<Notification>()

		for (mediaItem in mediaItems) if (!cache.containsKey(mediaItem.id) && mediaItem.hash != null) {
			val duplicates = mediaItems.filter { it.hash == mediaItem.hash }
			if (duplicates.size > 1) {
				cache.putAll(duplicates.mapNotNull { d -> d.id?.let { id -> id to duplicates.mapNotNull { it.id } } }
								 .toMap()
								 .toMutableMap())

				for (duplicate in duplicates) {
					notifications.add(Notification(
							value = "${duplicates.size} duplicates",
							type = NotificationType.DUPLICATE,
							name = duplicate.uri
								.removePrefix(duplicate.device?.uri ?: "")
								.removePrefix("/")
								.uriDecode(),
							deviceId = duplicate.deviceId,
							mediaItemId = duplicate.id,
							context = "${duplicate.hash}|${duplicates.joinToString(",")}"))
				}
				count++
			}
			update(progress.incrementAndGet() to count)
		}

		Notifications.notifyAll(notifications)

		update(progress.finish() to count)
		return count
	}
}