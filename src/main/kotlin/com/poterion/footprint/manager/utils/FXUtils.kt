package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.*
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.Icons
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.footprint.manager.model.VirtualItem
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.*
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.util.Callback
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

fun <T> TreeView<T>.cell(factory: TreeCell<T>.(TreeItem<T>?, T?, Boolean) -> Unit) {
	cellFactory = Callback<TreeView<T>, TreeCell<T>> {
		object : TreeCell<T>() {
			override fun updateItem(item: T, empty: Boolean) {
				super.updateItem(item, empty)
				factory(treeItem, item, empty)
			}
		}
	}
}

fun <T> TreeItem<T>.expand(predicate: (TreeItem<T>) -> Boolean = { true }) {
	if (!isLeaf) {
		isExpanded = true
		for (child in children) child.expand(predicate)
	}
}

fun <T> TreeItem<T>.find(predicate: (TreeItem<T>) -> Boolean): TreeItem<T>? = findAll(predicate)
	.firstOrNull()

fun <T> TreeItem<T>.findAll(predicate: (TreeItem<T>) -> Boolean): Collection<TreeItem<T>> {
	val result = mutableListOf<TreeItem<T>>()
	if (!isLeaf) {
		for (child in children) result.addAll(child.findAll(predicate))
	}
	if (predicate(this)) result.add(this)
	return result
}

private fun <T> TreeItem<T>.monitorExpansion(key: T.() -> String?) = apply {
	isExpanded = Database.list(Setting::class)
		.any { it.name == Setting.EXPANDED && it.value == value.key() }

	expandedProperty().addListener { observable, _, expanded ->
		(((observable as? BooleanProperty)?.bean as? TreeItem<*>)?.value as? T)?.key()?.also { key ->
			val previous = Database.list(Setting::class)
				.find { it.name == Setting.EXPANDED && it.value == key }

			if (expanded) (previous ?: Setting(name = Setting.EXPANDED))
				.apply { value = key }
				.also { Database.save(it) }
			else previous
				?.also { Database.delete(it) }
		}
	}
}

private fun <S, T> TreeTableColumn<S, T>.cellFactoryInternal(factory: (TreeTableCell<S, T>.(TreeItem<S>?, S?, T?, Boolean) -> Unit)? = null) {
	if (factory != null) cellFactory = Callback<TreeTableColumn<S, T>, TreeTableCell<S, T>> {
		object : TreeTableCell<S, T>() {
			override fun updateItem(item: T?, empty: Boolean) {
				super.updateItem(item, empty)
				this.factory(treeTableRow.treeItem, treeTableRow.item, item, empty)
			}
		}
	}
}

fun <S, T> TreeTableColumn<S, T>.cell(property: String,
									  factory: (TreeTableCell<S, T>.(TreeItem<S>?, S?, T?, Boolean) -> Unit)? = null) {
	cellValueFactory = TreeItemPropertyValueFactory<S, T>(property)
	cellFactoryInternal(factory)
}

fun <S, T> TreeTableColumn<S, T>.cell(getter: TreeTableColumn<S, T>.(TreeTableColumn.CellDataFeatures<S, T>?) -> T?) {
	cellValueFactory = Callback<TreeTableColumn.CellDataFeatures<S, T>, ObservableValue<T>> { param ->
		this.getter(param)?.let { SimpleObjectProperty(it) }
	}
}

fun <S, T> TreeTableColumn<S, T>.cell(getter: TreeTableColumn<S, T>.(TreeTableColumn.CellDataFeatures<S, T>?) -> T?,
									  factory: (TreeTableCell<S, T>.(TreeItem<S>?, S?, T?, Boolean) -> Unit)? = null) {
	cellValueFactory = Callback<TreeTableColumn.CellDataFeatures<S, T>, ObservableValue<T>> { param ->
		this.getter(param)?.let { SimpleObjectProperty(it) }
	}
	cellFactoryInternal(factory)
}

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