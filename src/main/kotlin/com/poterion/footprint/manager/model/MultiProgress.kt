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
package com.poterion.footprint.manager.model

import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class MultiProgress(private val progressBar: ProgressBar? = null) {
	private var data = mutableMapOf<String, Progress>()

	val progress: Long
		get() = data.values.map { it.progress.toLong() }
			.filter { it >= 0 }
			.takeIf { it.isNotEmpty() }
			?.reduce { acc, l -> acc + l }
			?: (if (data.values.all { it.progress.toLong() == -1L }) -1L else 0)

	val total: Long
		get() = data.values
			.map { it.total.toLong() }
			.filter { it >= 0 }
			.takeIf { it.isNotEmpty() }
			?.reduce { acc, l -> acc + l }
			?: 0

	val indeterminate: Boolean
		get() = progress < 0

	val finished: Boolean
		get() = progress == total

	val value: Double
		get() = when {
			indeterminate -> ProgressIndicator.INDETERMINATE_PROGRESS
			total > 0 -> progress.toDouble() / total.toDouble()
			else -> 1.0
		}

	fun update(key: String, progress: Progress?) {
		if (progress != null) {
			data.getOrPut(key, { Progress(-1, 0) }).also {
				it.set(progress.progress)
				it.setTotal(progress.total)
			}
		} else {
			data.remove(key)
		}
		progressBar?.also { update(it) }
	}

	private fun update(progressBar: ProgressBar) {
		val determinate = data.values.filter { !it.indeterminate }
		when {
			determinate.isNotEmpty() -> progressBar.progress = progress.toDouble() / total.toDouble()
			data.isNotEmpty() -> progressBar.progress = ProgressIndicator.INDETERMINATE_PROGRESS
			else -> progressBar.progress = 0.0
		}
	}
}