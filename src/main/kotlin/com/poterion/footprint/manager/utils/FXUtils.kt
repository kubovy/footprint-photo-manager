package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.Icons
import com.poterion.footprint.manager.data.*
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.footprint.manager.model.VirtualItem
import com.poterion.utils.javafx.monitorExpansion
import com.poterion.utils.kotlin.ensureSuffix
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

private fun <T> TreeItem<T>.monitorExpansion(key: T.() -> String?) = monitorExpansion(
		{
			Database.list(Setting::class)
				.any { it.name == Setting.EXPANDED && it.value == value.key() }
		},
		{ item, expanded ->
			if (item != null) {
				val previous = Database.list(Setting::class)
					.find { it.name == Setting.EXPANDED && it.value == item.key() }

				if (expanded) (previous ?: Setting(name = Setting.EXPANDED))
					.apply { value = item.key() }
					.also { Database.save(it) }
				else previous
					?.also { Database.delete(it) }
			}
		}
)

fun TreeItem<UriItem>.getParentItem(mediaItem: MediaItem): TreeItem<UriItem>? {
	var parentItem: TreeItem<UriItem>? = takeIf { it.value?.id == mediaItem.deviceId }
		?: takeIf { value == VirtualItem.ROOT }
			?.let { children.find { it.value?.id == mediaItem.deviceId } }
		?: takeIf { value == VirtualItem.ROOT }
			?.let { Database.list(Device::class) }
			?.find { it.id == mediaItem.deviceId }
			?.let { TreeItem(it as UriItem) }
			?.also { children.add(it) }
			?.also { children.sortWith(dataTreeComparator) }

	val device = parentItem?.value as? Device
	parentItem?.monitorExpansion { uri }

	if (parentItem != null && device != null) {
		val folders = mediaItem
			.uri
			.removePrefix(device.uri.ensureSuffix("/"))
			.split("/")
			.takeIf { it.isNotEmpty() }
			?.let { it.subList(0, it.size - 1) }
			?: emptyList()

		for (folder in folders) {
			var childItem = parentItem?.children?.find { it.value?.name == folder }
			if (childItem == null) {
				childItem = parentItem?.value
					?.uri
					?.ensureSuffix("/")
					?.let { it + folder }
					?.ensureSuffix("/")
					?.let { VirtualItem(name = folder, icon = Icons.FOLDER, uri = it) }
					?.let { TreeItem(it as UriItem) }
					?.monitorExpansion { uri }

				if (childItem != null) {
					parentItem?.children?.add(childItem)
					parentItem?.children?.sortBy { it.value.name }
				}
			}
			parentItem = childItem
		}
	}
	return parentItem
}

fun TreeTableView<UriItem>.add(mediaItem: MediaItem) = root.add(mediaItem)

fun TreeItem<UriItem>.add(mediaItem: MediaItem) {
	val parentItem = getParentItem(mediaItem)
	val child = parentItem?.children?.find { it.value.uri == mediaItem.uri }
	if (child == null) {
		val treeItem = TreeItem(mediaItem as UriItem)
		parentItem?.children?.add(treeItem)
		parentItem?.children?.sortWith(dataTreeComparator)
	} else {
		child.value = mediaItem
		parentItem.children?.sortWith(dataTreeComparator)
	}
}

fun TreeView<Notification>.addAll(notifications: Collection<Notification>) = notifications.forEach { root.add(it) }

fun TreeView<Notification>.add(notification: Notification) = root.add(notification)

private fun TreeItem<Notification>.add(notification: Notification) {
	val parentItem = getParentItem(notification)
	val child = parentItem?.children?.find { it.value == notification }
	if (child == null) {
		val treeItem = TreeItem(notification)
		parentItem?.children?.add(treeItem)
		parentItem?.children?.sortWith(notificationTreeComparator)
	} else {
		child.value = notification
		parentItem.children?.sortWith(notificationTreeComparator)
	}
}

private fun TreeItem<Notification>.getParentItem(notification: Notification): TreeItem<Notification>? {
	var parentItem: TreeItem<Notification>? = takeIf { it.value.type == notification.type }
		?: takeIf { value == Notification.ROOT }
			?.let { children.find { it.value?.type == notification.type } }
		?: takeIf { value == Notification.ROOT }
			?.let { TreeItem(Notification(name = notification.type.displayName, type = notification.type)) }
			?.also { it.monitorExpansion { "notification-${it.value.type}" } }
			?.also { children.add(it) }
			?.also { children.sortWith(notificationTreeComparator) }

	if (notification.type == NotificationType.DUPLICATE) {
		val hash = notification.context?.split("|")?.firstOrNull()
		val childItem = parentItem?.children
			?.find { it.value.context?.split("|")?.firstOrNull() == hash }
			?: TreeItem(notification.copy(id = UUID.randomUUID().toString(),
										  name = notification.value ?: "Duplicates",
										  deviceId = null,
										  mediaItemId = null,
										  value = null))
				.also { it.monitorExpansion { "notification-duplicate-${notification.mediaItemId}" } }
				.also { parentItem?.children?.add(it) }
				.also { parentItem?.children?.sortWith(notificationTreeComparator) }
		parentItem = childItem
	}

	parentItem?.monitorExpansion { id }
	return parentItem
}

val dataTreeComparator = Comparator<TreeItem<UriItem>> { o1, o2 ->
	when {
		o1 == null && o2 == null -> 0
		o1 == null && o2 != null -> 1
		o1 != null && o2 == null -> -1
		o1?.value is Device && o2?.value !is Device -> -1
		o1?.value !is Device && o2?.value is Device -> 1
		o1?.value is Device && o2?.value is Device -> {
			((o1.value as Device).type to (o2.value as Device).type).let { (t1, t2) ->
				when {
					t1 != DeviceType.REMOVABLE && t2 == DeviceType.REMOVABLE -> -1
					t1 == DeviceType.REMOVABLE && t2 != DeviceType.REMOVABLE -> 1
					else -> compareValues(o1.value?.name, o2.value?.name)
				}
			}
		}
		o1?.value is VirtualItem && o2?.value !is VirtualItem -> -1
		o1?.value !is VirtualItem && o2?.value is VirtualItem -> 1
		o1?.value is MediaItem && o2?.value !is MediaItem -> -1
		o1?.value !is MediaItem && o2?.value is MediaItem -> 1
		else -> compareValues(o1?.value?.name, o2?.value?.name)
	}
}

private val notificationTreeComparator = compareBy<TreeItem<Notification>> { it.value?.name }