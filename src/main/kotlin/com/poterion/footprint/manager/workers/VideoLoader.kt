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
import com.poterion.footprint.manager.utils.getVideoThumbnail
import com.poterion.utils.javafx.toImage
import javafx.scene.Node
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class VideoLoader(arg: Collection<Pair<Node, MediaItem>>) :
		Worker<Collection<Pair<Node, MediaItem>>, Pair<Node, Any>, Boolean>(arg) {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(VideoLoader::class.java)
	}

	override fun doWork(arg: Collection<Pair<Node, MediaItem>>): Boolean? {
		for ((node, media) in arg) {
			var item: Any? = try {
				media.toMediaPlayer()
			} catch (t: Throwable) {
				LOGGER.error(t.message, t)
				null
			}
			if (item == null) item = try {
				media.getVideoThumbnail().toImage()
			} catch (t: Throwable) {
				LOGGER.error(t.message, t)
				null
			}
			if (item != null) update(node to item)
		}
		return true
	}

	private fun MediaItem.toMediaPlayer() = this
		.takeIf { it.videoFormat != null }
		?.uri
		?.let { Media(it) }
		?.let { MediaPlayer(it) }
		?.also { it.isAutoPlay = false }
}