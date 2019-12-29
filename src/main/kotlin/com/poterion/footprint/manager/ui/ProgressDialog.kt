package com.poterion.footprint.manager.ui

import com.poterion.footprint.manager.Main
import com.poterion.footprint.manager.enums.Icons
import com.poterion.footprint.manager.utils.toImageView
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.ProgressBar
import javafx.stage.Modality
import javafx.util.Callback

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ProgressDialog(message: String, blocking: Boolean = true) : Dialog<Void>() {
	private var closeButton: Button

	init {
		title = Main.APP_TITLE
		headerText = message
		graphic = Icons.SYNCHRONIZE.toImageView(32, 32)

		dialogPane.buttonTypes.addAll(ButtonType.CLOSE)
		closeButton = (dialogPane.lookupButton(ButtonType.CLOSE) as Button).apply {
			isVisible = false
			text = ""
			minWidth = 0.0
			prefWidth = 0.0
			maxWidth = 0.0
			minHeight = 0.0
			prefHeight = 0.0
			maxHeight = 0.0
		}

		// Create the username and password labels and fields.
		dialogPane.content = ProgressBar(ProgressBar.INDETERMINATE_PROGRESS)
		initModality(if (blocking) Modality.APPLICATION_MODAL else Modality.NONE)

		resultConverter = Callback<ButtonType, Void> { null }
	}

	fun dismiss() {
		closeButton.fire()
	}
}


