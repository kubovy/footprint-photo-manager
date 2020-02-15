package com.poterion.footprint.manager.workers

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.add
import com.poterion.footprint.manager.utils.isAvailable
import com.poterion.footprint.manager.utils.mediaItems
import javafx.scene.control.TreeItem

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class GenerateTreeWorker(arg: Collection<Device>?) :
	Worker<Collection<Device>?, TreeItem<UriItem>, List<TreeItem<UriItem>>>(arg) {
	override fun doWork(arg: Collection<Device>?): List<TreeItem<UriItem>>? {
		val devices = arg?.takeIf { it.isNotEmpty() } ?: Database.list(Device::class)
		val rootItems = mutableListOf<TreeItem<UriItem>>()
		//for (device in devices) {
		//	val rootItem = TreeItem(device as UriItem)
		//	update(rootItem)
		//}
		for (device in devices) {
			if (rootItems.none { it.value.id == device.id }) {
				val rootItem = TreeItem(device as UriItem)
				//val rootItem = rootTreeItems[device.id] ?: TreeItem(device as UriItem).also { update(it) }

				if (device.type != DeviceType.REMOVABLE || device.isAvailable) for (mediaItem in device.mediaItems) {
					rootItem.add(mediaItem)
				}

				rootItems.add(rootItem)
			}
		}
		return rootItems
	}
}