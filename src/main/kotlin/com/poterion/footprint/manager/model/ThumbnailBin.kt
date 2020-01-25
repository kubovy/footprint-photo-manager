package com.poterion.footprint.manager.model

import com.poterion.footprint.manager.data.MediaItem
import javafx.scene.control.CheckBox
import javafx.scene.shape.Rectangle

data class ThumbnailBin(
	val mediaItem: MediaItem,
	val background: Rectangle,
	val checkBox: CheckBox)