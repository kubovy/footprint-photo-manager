package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.Notification
import com.poterion.footprint.manager.enums.NotificationType
import io.reactivex.subjects.BehaviorSubject

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Notifications {
	private val _notifications = mutableListOf<Notification>()

	val notifications: Collection<Notification>
		get() = _notifications

	val subject: BehaviorSubject<Collection<Notification>> = BehaviorSubject.create<Collection<Notification>>()

	init {
		_notifications.addAll(Database.list(Notification::class))
		subject.onNext(_notifications)
	}

	fun notify(value: String,
			   type: NotificationType,
			   name: String = "",
			   deviceId: String? = null,
			   mediaItemId: String? = null,
			   metadataTagId: String? = null,
			   context: String? = null) {

		val notification = Notification(
				deviceId = deviceId,
				mediaItemId = mediaItemId,
				metadataTagId = metadataTagId,
				type = type,
				name = name,
				value = value,
				context = context)

		if ((deviceId == null && mediaItemId == null && metadataTagId == null)
			|| !_notifications.contains(notification)
		) {
			if (type.isPersistent) Database.save(notification)
			_notifications.add(notification)
			subject.onNext(_notifications)
		}
	}

	fun notifyAll(notifications: Collection<Notification>) {
		val relevant = notifications
			.filter {
				(it.deviceId == null && it.mediaItemId == null && it.metadataTagId == null) || !Notifications._notifications.contains(
						it)
			}

		val persistent = relevant.filter { it.type.isPersistent }
		Database.saveAll(persistent)

		this._notifications.addAll(relevant)
		subject.onNext(this._notifications)
	}

	fun dismiss(notification: Notification) {
		if (notification.type.isPersistent) Database.delete(notification)
		_notifications.remove(notification)
		subject.onNext(_notifications)
	}

	fun dismissAll(notifications: Collection<Notification>) {
		Database.deleteAll(notifications.filter { it.type.isPersistent })
		this._notifications.removeAll(notifications)
		this._notifications.removeAll { notification -> notifications.map { it.id }.contains(notification.id) }
		subject.onNext(this._notifications)
	}
}