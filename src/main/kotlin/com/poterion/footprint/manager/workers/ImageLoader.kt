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
import com.poterion.footprint.manager.utils.exists
import com.poterion.footprint.manager.utils.getImageThumbnail
import com.poterion.footprint.manager.utils.getVideoThumbnail
import javafx.scene.Node
import javafx.scene.image.Image

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ImageLoader(arg: Pair<Int, Collection<Pair<Node, MediaItem>>>) :
		Worker<Pair<Int, Collection<Pair<Node, MediaItem>>>, Pair<Node, Image>, Boolean>(arg) {

	override fun doWork(arg: Pair<Int, Collection<Pair<Node, MediaItem>>>): Boolean? {
		val (bbox, imagesToLoad) = arg
		for ((node, media) in imagesToLoad) {
			if (!media.exists()) continue
			if (media.imageFormat != null) {
				update(node to media.getImageThumbnail(width = bbox).inputStream().use { Image(it) })
			} else if (media.videoFormat != null) {
				update(node to media.getVideoThumbnail(width = bbox).inputStream().use { Image(it) })
			}
		}
		return true
	}
}