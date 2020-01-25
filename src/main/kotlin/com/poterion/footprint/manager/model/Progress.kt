package com.poterion.footprint.manager.model

import javafx.scene.control.ProgressIndicator
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Progress(progress: Int = 0, total: Int = 0) {
	val progress = AtomicInteger(progress)
	val total = AtomicInteger(total)

	companion object {
		val INDETERMINATE = Progress(-1, 0)
		val NOTHING_TODO = Progress(0, 0)
		val FINISHED = Progress(1, 1)
	}

	val indeterminate: Boolean
		get() = progress.get() < 0

	val finished: Boolean
		get() = progress.get() == total.get()

	val value: Double
		get() = when {
			indeterminate -> ProgressIndicator.INDETERMINATE_PROGRESS
			total.get() > 0 -> progress.get().toDouble() / total.get().toDouble()
			else -> 1.0
		}
}