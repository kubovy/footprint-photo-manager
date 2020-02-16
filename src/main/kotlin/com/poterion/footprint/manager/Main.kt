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
package com.poterion.footprint.manager

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.Setting
import com.poterion.footprint.manager.enums.NotificationType
import com.poterion.footprint.manager.ui.PasswordDialog
import com.poterion.footprint.manager.ui.ProgressDialog
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.Notifications
import com.poterion.footprint.manager.xuggle.SmbFileProtocolHandlerFactory
import com.poterion.utils.kotlin.decrypt
import com.poterion.utils.kotlin.encrypt
import com.poterion.utils.kotlin.isPasswordForEncryptionSet
import com.poterion.utils.kotlin.setPasswordForEncryption
import com.xuggle.xuggler.io.URLProtocolManager
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Main : Application() {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(Main::class.java)
		const val APP_NAME = "Media Manager"
		const val APP_COPYRIGHT = "2019 (c) Poterion"
		const val APP_TITLE = "${APP_NAME} | ${APP_COPYRIGHT}"
		val CONFIG_PATH: Path = Paths
			.get(System.getProperty("user.home"), ".config", "footprint")
			.toAbsolutePath()
		val CACHE_PATH: Path = CONFIG_PATH.resolve("cache")
		var initialScan = true

		init {
			URLProtocolManager.getManager().registerFactory("smb", SmbFileProtocolHandlerFactory())
		}

		@JvmStatic
		fun main(args: Array<String>) {
			val password = Option.builder("p")
				.longOpt("password")
				.hasArg()
				.argName("PASSWORD")
				.desc("Vault password.")
				.build()
			val noScan = Option.builder()
				.longOpt("no-scan")
				.desc("Don't perform initial scan of local devices.")
				.build()
			val help = Option.builder("h")
				.longOpt("help")
				.desc("This screen.")
				.build()

			val options = Options()
			options.addOption(password)
			options.addOption(noScan)
			options.addOption(help)

			val parser: CommandLineParser = DefaultParser()
			try {
				val line = parser.parse(options, args)
				if (line.hasOption("help")) {
					val formatter = HelpFormatter()
					formatter.printHelp("footprint", options)
					exitProcess(0)
				}

				line.getOptionValue("password")?.also { setPasswordForEncryption(it) }
				initialScan = !line.hasOption("no-scan")

				launch(Main::class.java, *args)
			} catch (e: ParseException) { // oops, something went wrong
				System.err.println("Parsing failed.  Reason: " + e.message)
				val formatter = HelpFormatter()
				formatter.printHelp("poterion-monitor", options)
				exitProcess(0)
			}
		}
	}

	override fun start(primaryStage: Stage) {
		LOGGER.info("Starting...")

		val progressDialog = ProgressDialog("Loading collections").apply {
			isResizable = true
			setOnShown {
				Thread {
					Database.list(Device::class)
					Platform.runLater { dismiss() }
				}.start()
			}
		}
		progressDialog.showAndWait()

		val nonce = Database.list(Setting::class).find { it.name == "nonce" }
		if (nonce != null) {
			if (isPasswordForEncryptionSet() && nonce.value?.decrypt() != "DIY") {
				setPasswordForEncryption("")
			}
			var password: String? = null
			while (password == null && !isPasswordForEncryptionSet()) {
				password = PasswordDialog().showAndWait().orElse(null)
				if (password != null) try {
					if (nonce.value?.decrypt(password) == "DIY") {
						setPasswordForEncryption(password)
					} else {
						password = null
					}
				} catch (t: Throwable) {
					LOGGER.error(t.message, t)
					password = null
				} else {
					Platform.exit()
					exitProcess(0)
				}
			}
		} else {
			val password = PasswordDialog(true).showAndWait().orElse(null)
			if (password != null) {
				Database.save(Setting(name = "nonce", value = "DIY".encrypt(password)))
				setPasswordForEncryption(password)
			} else {
				Platform.exit()
				exitProcess(0)
			}
		}

		val oldNotifications = Notifications.notifications.filter { it.type == NotificationType.DUPLICATE }
		Notifications.dismissAll(oldNotifications)

		val width = Database.list(Setting::class).find { it.name == Setting.WINDOW_WIDTH }
			?: Setting(name = Setting.WINDOW_WIDTH, value = "${1600.0}")
		val height = Database.list(Setting::class).find { it.name == Setting.WINDOW_HEIGHT }
			?: Setting(name = Setting.WINDOW_HEIGHT, value = "${1200.0}")
		val maximized = Database.list(Setting::class).find { it.name == Setting.WINDOW_MAXIMIZED }
			?: Setting(name = Setting.WINDOW_MAXIMIZED, value = "${false}")

		val root = ManagerController.get(primaryStage, initialScan)
		primaryStage.title = APP_TITLE
		primaryStage.scene = Scene(root,
								   width.value?.toDoubleOrNull() ?: 1600.0,
								   height.value?.toDoubleOrNull() ?: 1200.0)
		primaryStage.isMaximized = maximized.value?.toBoolean() == true
		primaryStage.show()
		primaryStage.widthProperty().addListener { _, _, v -> Database.save(width.apply { value = "${v}" }) }
		primaryStage.heightProperty().addListener { _, _, v -> Database.save(height.apply { value = "${v}" }) }
		primaryStage.maximizedProperty().addListener { _, _, v -> Database.save(maximized.apply { value = "${v}" }) }
		primaryStage.setOnCloseRequest {
			primaryStage.close()
			Platform.exit()
			exitProcess(0)
		}
	}
}