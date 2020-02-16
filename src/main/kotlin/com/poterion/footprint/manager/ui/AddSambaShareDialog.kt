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
import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.model.FileObject
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.toUriOrNull
import com.poterion.footprint.manager.utils.usernamePassword
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.encrypt
import com.poterion.utils.kotlin.uriEncode
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.util.Callback
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class AddSambaShareDialog(private val editDevice: Device? = null) : Dialog<Device>() {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(AddSambaShareDialog::class.java)
	}

	private val saveButtonType = ButtonType(if (editDevice == null) "Add" else "Save", ButtonBar.ButtonData.OK_DONE)

	private val textAddress = TextField()
		.apply { promptText = "nas.intra or 192.168.x.x" }
		.apply { text = editDevice?.toUriOrNull()?.host ?: "" }
	private val textFolder = TextField()
		.apply { promptText = "/FolderName" }
		.apply { text = editDevice?.toUriOrNull()?.path?.removeSuffix("/") ?: "" }
	private val textUsername = TextField()
		.apply { promptText = "Username" }
		.apply { text = editDevice?.usernamePassword?.first ?: "" }
	private val textPassword = PasswordField()
		.apply { promptText = "Password" }
		.apply { text = editDevice?.usernamePassword?.second ?: "" }

	private val buttonTest = Button("Test")
	private val labelError = Label("")

	private lateinit var buttonSave: Button

	private val auth: Pair<String, String>?
		get() = textUsername.text.trim().takeUnless { it.isEmpty() }?.let { it to textPassword.text }

	private val authString: String?
		get() = auth
			?.toList()
			?.map { it.uriEncode() }
			?.let { (username, password) -> "${username}:${password}" }

	private val folder: String
		get() = "${textFolder.text}${textFolder.text.takeUnless { it.endsWith("/") }?.let { "/" } ?: ""}"

	private val displayName: String
		get() = "${textFolder.text.replace("^/?([^/]+(/[^/]+)*)/?$".toRegex(), "$1")} (${textAddress.text})"

	private val smbFile: SmbFile
		get() = SmbFile("smb://${textAddress.text}${folder}", auth
			?.let { (user, password) -> NtlmPasswordAuthenticator("", user, password) }
			?.let { SingletonContext.getInstance().withCredentials(it) }
			?: SingletonContext.getInstance())

	private val uri: URI?
		get() = FileObject(smbFile).uri

	private val error: String?
		get() = when {
			!textAddress.text.matches("((\\d+\\.\\d+\\.\\d+\\.\\d+)|(\\w+(\\.\\w+)))".toRegex()) -> "Wrong address"
			!textFolder.text.matches("(/.+)+".toRegex()) -> "Wrong folder"
			Database.list(Device::class)
				.filterNot { it == editDevice }
				.any { it.uri == uri?.toString() } -> "Shared folder already exists"
			else -> null
		}

	private val listener = { _: ObservableValue<out String>, _: String, _: String ->
		val e = error
		labelError.text = e ?: ""
		buttonTest.isDisable = e != null
		buttonSave.isDisable = e != null
	}

	init {
		title = if (editDevice == null) "Add shared folder" else "Edit shared folder"
		graphic = Icons.NAS.toImageView(64, 64)
		isResizable = true
		dialogPane.buttonTypes.addAll(saveButtonType, ButtonType.CANCEL)
		buttonSave = (dialogPane.lookupButton(saveButtonType) as Button).apply { isDisable = true }

		buttonTest.setOnAction {
			try {
				labelError.text = if (smbFile.listFiles() != null) "OK" else "Failed!"
			} catch (t: Throwable) {
				LOGGER.error(t.message, t)
				labelError.text = "Failed: ${t.message}!"
			}
		}
		textAddress.textProperty().addListener(listener)
		textFolder.textProperty().addListener(listener)

		dialogPane.content = GridPane().apply {
			hgap = 10.0
			vgap = 10.0
			padding = Insets(20.0, 150.0, 10.0, 10.0)

			add(Label("Address:"), 0, 0)
			add(textAddress, 1, 0)

			add(Label("Folder:"), 0, 1)
			add(textFolder, 1, 1)

			add(Label("Username:"), 0, 2)
			add(textUsername, 1, 2)

			add(Label("Password:"), 0, 3)
			add(textPassword, 1, 3)

			add(buttonTest, 0, 4)
			add(labelError, 1, 4)
		}

		Platform.runLater { textAddress.requestFocus() }

		resultConverter = Callback<ButtonType, Device> { dialogButton ->
			uri.takeIf { dialogButton == saveButtonType && error == null }
				?.let { it to (editDevice ?: Device(type = DeviceType.SMB)) }
				?.also { (uri, device) ->
					device.name = displayName
					device.auth = authString?.encrypt()
					device.uri = uri.toString()
				}
				?.second
		}
	}
}