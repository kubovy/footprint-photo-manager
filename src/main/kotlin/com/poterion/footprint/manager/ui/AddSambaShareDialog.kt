package com.poterion.footprint.manager.ui

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.enums.Icons
import com.poterion.footprint.manager.utils.*
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.util.Callback
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
			?.map { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
			?.let { (username, password) -> "${username}:${password}" }
	private val folder: String
		get() = "${textFolder.text}${textFolder.text.takeUnless { it.endsWith("/") }?.let { "/" } ?: ""}"
			.split("/")
			.map { URLEncoder.encode(it, "UTF-8") }
			.joinToString("/")
	private val displayName: String
		get() = "${textFolder.text.replace("^/?(\\w+(/\\w+)*)/?$".toRegex(), "$1")} (${textAddress.text})"
	private val uri: URI?
		get() = URI("smb", textAddress.text, folder, null)

	private val error: String?
		get() = when {
			!textAddress.text.matches("((\\d+\\.\\d+\\.\\d+\\.\\d+)|(\\w+(\\.\\w+)))".toRegex()) -> "Wrong address"
			!textFolder.text.matches("(/\\w+)+".toRegex()) -> "Wrong folder"
			Database.list(Device::class)
				.filterNot { it == editDevice }
				.any { it.uri == uri.toString() } -> "Shared folder already exists"
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
		dialogPane.buttonTypes.addAll(saveButtonType, ButtonType.CANCEL)
		buttonSave = (dialogPane.lookupButton(saveButtonType) as Button).apply { isDisable = true }

		buttonTest.setOnAction {
			val url = uri?.toString()
			if (url != null) {
				val cifsContext = auth
					?.let { (user, password) -> NtlmPasswordAuthenticator("", user, password) }
					?.let { SingletonContext.getInstance().withCredentials(it) }
					?: SingletonContext.getInstance()
				val smbFile = SmbFile(url, cifsContext)
				try {
					labelError.text = if (smbFile.listFiles() != null) "OK" else "Failed!"
				} catch (t: Throwable) {
					LOGGER.error(t.message, t)
					labelError.text = "Failed: ${t.message}!"
				}
			}
		}
		textAddress.textProperty().addListener(listener)
		textFolder.textProperty().addListener(listener)

		dialogPane.content = GridPane()
			.apply {
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
				?.let { editDevice ?: Device(type = DeviceType.SMB) }
				?.also { device ->
					device.name = displayName
					device.auth = authString?.encrypt()
					device.uri = uri.toString()
				}
		}
	}
}