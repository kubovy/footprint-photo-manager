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