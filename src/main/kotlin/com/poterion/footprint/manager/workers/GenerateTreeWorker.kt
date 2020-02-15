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