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

import com.poterion.footprint.manager.data.Setting
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.Settings
import com.poterion.utils.kotlin.intermediate
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.stage.Modality
import javafx.stage.Stage

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SettingsController {
	companion object {
		fun showAndWait(): Collection<Setting>? {
			val fxmlLoader =
					FXMLLoader(SettingsController::class.java.getResource("/com/poterion/footprint/manager/settings.fxml"))
			val root = fxmlLoader.load<Pane>()
			val controller = fxmlLoader.getController<SettingsController>() as SettingsController
			controller.root = root

			Stage()
				.apply {
					title = "Settings"
					initModality(Modality.NONE)
					scene = Scene(root, 700.0, 650.0)
					//initStyle(StageStyle.UTILITY)
					isResizable = false
				}
				.showAndWait()
			return controller.result
		}
	}

	lateinit var root: Parent
	@FXML private lateinit var textFolderPattern: TextField
	@FXML private lateinit var labelFolderPatternExplanation: Label
	@FXML private lateinit var checkboxAutoplayVideos: CheckBox
	private var result: Collection<Setting>? = null

	@FXML
	fun initialize() {
		labelFolderPatternExplanation.text = """
			Folder pattern from device root to be followed.
			(Applies only to primary device.)
			
			Variables:
			  DATESPAN(y, m, d, [type]) 
			        - Date span between oldest and newest media in the collection
			          y    - Year pattern (see below)
			          m    - Month pattern (see below)
			          d    - Day pattern (see below)
			          type - Type of date span, one of: "full", "condensed" (optional)
			                 Default is "condensed", e.g.
			                 Full: 2019-12-04-2019-12-28
			                 Condensed: 2019-12-04-28, 2019-11-26-12-28
		
			  TITLE  - Collection title
			  NUM(x) - Media item sequence number (x digits, e.g. 0004)
			  EXT    - Uppercase extension (e.g. JPG)
			  ext    - Lowercase extension (e.g. jpg)
			  yyyy   - Year (4 digits, e.g. 2019)
			  yy     - Year (2 digits, e.g. 19)
			  mm     - Month (2 digits, e.g. 02, 11)
			  m      - Month (1 digit, e.g. 2, 11)
			  dd     - Day (2 digits, e.g. 05, 23)
			  d      - Day (1 digit, e.g. 5, 23)
			  HH     - Hour (24 hours, 2 digits, e.g. 01, 13)
			  H      - Hour (24 hours, 1 digit, e.g. 1, 13)
			  hh     - Hour (12 hours, 2 digits, e.g. 02, 10)
			  h      - Hour (12 hours, 1 digit, e.g. 2, 10)
			  MM     - Minutes (2 digits, e.g. 06, 45)
			  M      - Minutes (1 digits, e.g. 6, 45)
			  SS     - Seconds (2 digits, e.g. 04, 51)
			  S      - Seconds (1 digits, e.g. 4, 51)
			  a      - AM/PM
			
		""".trimIndent()

		textFolderPattern.text = Settings.folderPattern
		checkboxAutoplayVideos.isSelected = Database.list(Setting::class)
			.find { it.name == Setting.AUTOPLAY_VIDEOS }
			?.value == "true"
	}

	@FXML
	fun onApply(event: ActionEvent) {
		result = listOf(Setting.FOLDER_PATTERN, Setting.AUTOPLAY_VIDEOS)
			.map { name -> Database.list(Setting::class).find { it.name == name } ?: Setting(name = name) }
			.intermediate { setting ->
				setting.value = when (setting.name) {
					Setting.FOLDER_PATTERN -> textFolderPattern.text
					Setting.AUTOPLAY_VIDEOS -> checkboxAutoplayVideos.isSelected.let { if (it) "true" else "false" }
					else -> null
				}
			}
			.filter { it.value != null }
			.also { Database.saveAll(it) }
		closeStage(event)
	}

	fun onCancel(event: ActionEvent) {
		closeStage(event)
	}

	private fun closeStage(event: ActionEvent) {
		val source: Node = event.source as Node
		val stage = source.scene.window as Stage
		stage.close()
	}
}
