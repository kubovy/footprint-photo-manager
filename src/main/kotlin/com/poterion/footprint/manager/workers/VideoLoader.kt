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