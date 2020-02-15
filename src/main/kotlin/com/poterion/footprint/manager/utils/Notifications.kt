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