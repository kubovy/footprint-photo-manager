package com.poterion.footprint.manager.model

import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class MultiProgress(private val progressBar: ProgressBar? = null) {
	private var data = mutableMapOf<String, Progress>()

	val progress: Long
		get() = data.values.map { it.progress.toLong() }.reduce { acc, l -> acc + l }

	val total: Long
		get() = data.values.map { it.total.toLong() }.reduce { acc, l -> acc + l }

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