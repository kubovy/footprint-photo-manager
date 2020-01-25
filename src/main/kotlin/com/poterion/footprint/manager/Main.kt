package com.poterion.footprint.manager

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.Setting
import com.poterion.footprint.manager.ui.PasswordDialog
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.xuggle.SmbFileProtocolHandlerFactory
import com.poterion.utils.kotlin.decrypt
import com.poterion.utils.kotlin.encrypt
import com.poterion.utils.kotlin.setPasswordForEncryption
import com.xuggle.xuggler.io.URLProtocolManager
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage
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

		init {
			URLProtocolManager.getManager().registerFactory("smb", SmbFileProtocolHandlerFactory())
		}

		@JvmStatic
		fun main(args: Array<String>) {
			launch(Main::class.java)
		}
	}

	override fun start(primaryStage: Stage) {
		LOGGER.info("Starting...")
		Database.list(Device::class)

		val nonce = Database.list(Setting::class).find { it.name == "nonce" }
		if (nonce != null) {
			var password: String? = null
			while (password == null) {
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

		val width = Database.list(Setting::class).find { it.name == Setting.WINDOW_WIDTH }
			?: Setting(name = Setting.WINDOW_WIDTH, value = "${1600.0}")
		val height = Database.list(Setting::class).find { it.name == Setting.WINDOW_HEIGHT }
			?: Setting(name = Setting.WINDOW_HEIGHT, value = "${1200.0}")
		val maximized = Database.list(Setting::class).find { it.name == Setting.WINDOW_MAXIMIZED }
			?: Setting(name = Setting.WINDOW_MAXIMIZED, value = "${false}")


		val root = ManagerController.get(primaryStage)
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