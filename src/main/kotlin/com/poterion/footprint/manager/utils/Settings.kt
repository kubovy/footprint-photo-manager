package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.data.Setting

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Settings {
	val folderPattern: String
		get() = Database.list(Setting::class)
			.find { it.name == Setting.FOLDER_PATTERN }
			?.value
			?: "%yyyy/%DATESPAN(yyyy, mm, dd) - %TITLE/%yyyy-%mm-%dd-%NUM(4).%ext"
}