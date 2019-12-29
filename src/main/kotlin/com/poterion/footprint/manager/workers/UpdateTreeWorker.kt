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