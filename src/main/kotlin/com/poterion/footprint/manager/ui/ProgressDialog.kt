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
package com.poterion.footprint.manager.ui

import com.poterion.footprint.manager.Icons
import com.poterion.footprint.manager.Main
import com.poterion.utils.javafx.toImageView
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
		isResizable = true

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


