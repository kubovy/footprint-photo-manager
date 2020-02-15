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
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class PasswordDialog(private val confirmation: Boolean = false) : Dialog<String>() {
	private val password = PasswordField().apply {
		promptText = "Password"
	}
	private val passwordConfirmation = PasswordField().apply {
		promptText = "Password Confirmation"
	}
	private val messageLabel = Label("")
	private lateinit var okButton: Node

	private val error: String?
		get() = when {
			confirmation && password.text.length < 8 -> "Password too short"
			confirmation && !password.text.matches(".*[a-z].*".toRegex()) -> "At least one lower-case character missing"
			confirmation && !password.text.matches(".*[A-Z].*".toRegex()) -> "At least one upper-case character missing"
			confirmation && !password.text.matches(".*[0-9].*".toRegex()) -> "At least one number missing"
			confirmation && !password.text.matches(".*[`~!@#$%^&*()_{}\"|<>?\\[\\]'\\\\,./].*".toRegex()) -> "At least one special character missing"
			confirmation && password.text != passwordConfirmation.text -> "Passwords do not match"
			else -> null
		}

	private val listener = { _: ObservableValue<out String>, _: String, _: String ->
		messageLabel.text = error
		okButton.isDisable = error != null
	}

	init {
		title = Main.APP_TITLE
		headerText = if (confirmation) "Create a new master password" else "Enter master password"
		graphic = Icons.LOCK.toImageView(32, 32)

		dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
		okButton = dialogPane.lookupButton(ButtonType.OK)
		okButton.isDisable = true

		// Create the username and password labels and fields.
		val grid = GridPane()
		HBox.setHgrow(grid, Priority.ALWAYS)
		grid.hgap = 10.0
		grid.vgap = 10.0
		grid.padding = Insets(20.0, 150.0, 10.0, 10.0)

		grid.add(Label("Password:"), 0, 0)
		grid.add(password, 1, 0)
		if (confirmation) {
			grid.add(Label("Confirmation:"), 0, 1)
			grid.add(passwordConfirmation, 1, 1)
		}
		grid.add(messageLabel, 0, if (confirmation) 2 else 1)
		GridPane.setColumnSpan(messageLabel, GridPane.REMAINING)

		dialogPane.content = grid

		password.textProperty().addListener(listener)
		passwordConfirmation.textProperty().addListener(listener)

		Platform.runLater { password.requestFocus() }

		resultConverter = Callback<ButtonType, String> { button: ButtonType ->
			if (button == ButtonType.OK) password.text else null
		}
	}
}


