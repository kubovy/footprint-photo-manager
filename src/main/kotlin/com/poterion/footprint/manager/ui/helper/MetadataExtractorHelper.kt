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
package com.poterion.footprint.manager.ui.helper

import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.utils.metadata
import com.poterion.footprint.manager.workers.MetadataExtractorWorker
import com.poterion.utils.javafx.expandTree
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object MetadataExtractorHelper {
	private val LOGGER = LoggerFactory.getLogger(MetadataExtractorWorker::class.java.simpleName)
	private var metadataLoaderExecutor: ExecutorService? = null

	internal fun loadMetadataInBackground(mediaItem: MediaItem, tableMetadata: TreeTableView<MetadataTag>) {
		metadataLoaderExecutor?.shutdownNow()
		metadataLoaderExecutor?.awaitTermination(500, TimeUnit.MILLISECONDS)
		metadataLoaderExecutor = Executors.newSingleThreadExecutor()
		val extractor = MetadataExtractorWorker(mediaItem)
			.onCancel { }
			.onError { }
			.onSuccess { result ->
				LOGGER.debug("Metadata for ${mediaItem.uri} (${mediaItem.id}): " + (result
					?.joinToString("\n  - ", "\n  - ") {
						"[${it.directory}]: ${it.tagType} \"${it.name}\" = (${it.valueType}) ${it.description} / ${it.raw}"
					} ?: ""))
				if (result != null) {
					val metadataItems = (mediaItem.metadata + result)
						.distinct()
						.sortedWith(compareBy({ it.directory }, { it.tagType }))
						.groupBy { it.directory }
						//.mapValues { (_, value) -> value.sortedBy { it.name } }
						.mapValues { (_, value) -> value.map { TreeItem(it) } }
						.mapKeys { (directory, _) -> TreeItem(MetadataTag(id = null, name = directory)) }
						.mapKeys { (directory, value) -> directory.also { it.children.addAll(value) } }
						.keys
						.sortedBy { it.value.name }
					tableMetadata.root.children.setAll(metadataItems)
					tableMetadata.root.expandTree()
				}
			}
			.onFinished { }
		metadataLoaderExecutor?.submit(extractor)
	}
}