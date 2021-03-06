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

import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.utils.add
import javafx.scene.control.TreeTableView

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class UpdateTreeWorker(arg: Pair<TreeTableView<UriItem>, Collection<MediaItem>>) :
		Worker<Pair<TreeTableView<UriItem>, Collection<MediaItem>>, Process, Boolean>(arg) {

	override fun doWork(arg: Pair<TreeTableView<UriItem>, Collection<MediaItem>>): Boolean {
		val (table, mediaItems) = arg
		for (mediaItem in mediaItems) {
			table.add(mediaItem)
		}
		return true
	}
}