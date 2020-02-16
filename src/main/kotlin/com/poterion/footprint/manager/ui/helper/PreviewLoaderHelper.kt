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

import com.poterion.footprint.manager.Icons
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.workers.ImageLoader
import com.poterion.footprint.manager.workers.VideoLoader
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.toFormattedDuration
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PreviewLoaderHelper(private val stackPanePreview: StackPane,
						  private val mediaView: MediaView,
						  private val imageView: ImageView,
						  private val hboxPreviewControls: HBox,
						  private val buttonPreviewPlayPause: Button,
						  private val sliderPreviewPosition: Slider,
						  private val labelPreviewStart: Label,
						  private val labelPreviewCurrent: Label,
						  private val labelPreviewEnd: Label) {
	private var mediaLoaderExecutor: ExecutorService? = null
	private var mediaPlayerSeekStatus: MediaPlayer.Status? = null

	internal fun loadPreviewInBackground(mediaItem: MediaItem, mediaPlayerProvider: (MediaPlayer?) -> Unit) {
		mediaLoaderExecutor?.shutdownNow()
		mediaLoaderExecutor?.awaitTermination(500, TimeUnit.MILLISECONDS)
		mediaLoaderExecutor = Executors.newSingleThreadExecutor()

		val loader = if (mediaItem.imageFormat != null) ImageLoader(0 to listOf(imageView to mediaItem))
			.onUpdate { (node, image) ->
				if (stackPanePreview.children.contains(mediaView)) stackPanePreview.children.remove(mediaView)
				if (stackPanePreview.children.contains(hboxPreviewControls)) stackPanePreview.children.remove(
						hboxPreviewControls)
				if (!stackPanePreview.children.contains(imageView)) stackPanePreview.children.add(imageView)
				mediaPlayerProvider(null)
				(node as? ImageView)?.image = image
			}
			.onCancel { }
			.onError { }
		else if (mediaItem.videoFormat != null) VideoLoader(listOf(mediaView to mediaItem))
			.onUpdate { (node, item) ->
				if (item is MediaPlayer) {
					if (stackPanePreview.children.contains(imageView)) stackPanePreview.children.remove(imageView)
					if (!stackPanePreview.children.contains(mediaView)) stackPanePreview.children.add(mediaView)
					if (!stackPanePreview.children.contains(hboxPreviewControls)) stackPanePreview.children.add(
							hboxPreviewControls)

					imageView.image = null
					mediaPlayerProvider(item)
					(node as? MediaView)?.mediaPlayer = item

					buttonPreviewPlayPause.text = null
					buttonPreviewPlayPause.graphic = Icons.PAUSE.toImageView(32, 32)
					sliderPreviewPosition.min = 0.0
					sliderPreviewPosition.value = 0.0
					sliderPreviewPosition.max = 0.0
					labelPreviewStart.text = 0.toFormattedDuration()
					labelPreviewCurrent.text = 0.toFormattedDuration()
					labelPreviewEnd.text = 0.toFormattedDuration()

					item.setOnReady {
						labelPreviewEnd.text = item.totalDuration?.toMillis()?.toFormattedDuration()
						sliderPreviewPosition.min = 0.0
						sliderPreviewPosition.value = 0.0
						sliderPreviewPosition.max = item.totalDuration?.toMillis() ?: 0.0
					}
					item.setOnPlaying { buttonPreviewPlayPause.graphic = Icons.PAUSE.toImageView(32, 32) }
					item.setOnPaused { buttonPreviewPlayPause.graphic = Icons.PLAY.toImageView(32, 32) }
					item.setOnStopped { buttonPreviewPlayPause.graphic = Icons.PLAY.toImageView(32, 32) }
					item.setOnHalted { buttonPreviewPlayPause.graphic = Icons.PLAY.toImageView(32, 32) }
					item.setOnEndOfMedia { item.stop() }

					item.currentTimeProperty()?.addListener { _, _, position ->
						sliderPreviewPosition.value = position.toMillis()
						labelPreviewCurrent.text = position.toMillis().toFormattedDuration()
					}

					sliderPreviewPosition.setOnMousePressed {
						mediaPlayerSeekStatus = item.status
						item.pause()
					}
					sliderPreviewPosition.setOnMouseReleased {
						item.seek(Duration(sliderPreviewPosition.value))
						if (mediaPlayerSeekStatus == MediaPlayer.Status.PLAYING) item.play()
						mediaPlayerSeekStatus = null
					}
					sliderPreviewPosition.valueProperty().addListener { _, _, position ->
						if (mediaPlayerSeekStatus != null) item.seek(Duration(position.toDouble()))
					}

					item.isAutoPlay = true
				} else if (item is Image) {
					if (stackPanePreview.children.contains(mediaView)) stackPanePreview.children.remove(mediaView)
					if (stackPanePreview.children.contains(hboxPreviewControls)) stackPanePreview.children.remove(
							hboxPreviewControls)
					if (!stackPanePreview.children.contains(imageView)) stackPanePreview.children.add(imageView)
					mediaPlayerProvider(null)
					imageView.image = item
				}
			}
			.onCancel { }
			.onError { }
		else null

		loader?.let { mediaLoaderExecutor?.submit(it) }
	}
}