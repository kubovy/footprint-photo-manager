package com.poterion.footprint.manager.model

import javafx.scene.control.ProgressIndicator
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Progress(progress: Number = 0, total: Number = 0) {
	private val progressAtomic = AtomicLong(progress.toLong())
	private val totalAtomic = AtomicLong(total.toLong())

	val progress: Number
		get() = progressAtomic.get()

	val total: Number
		get() = totalAtomic.get()

	companion object {
		val INDETERMINATE = Progress(-1, 0)
		val NOTHING_TODO = Progress(0, 0)
		val FINISHED = Progress(1, 1)
	}

	val indeterminate: Boolean
		get() = progressAtomic.get() < 0

	val finished: Boolean
		get() = progressAtomic.get() == totalAtomic.get()

	val value: Double
		get() = when {
			indeterminate -> ProgressIndicator.INDETERMINATE_PROGRESS
			totalAtomic.get() > 0 -> progressAtomic.get().toDouble() / totalAtomic.get().toDouble()
			else -> 1.0
		}

	fun setIndeterminate() = apply {
		progressAtomic.set(-1)
		totalAtomic.set(0)
	}

	fun set(number: Number) = apply { progressAtomic.set(number.toLong()) }

	fun reset() = set(0)

	fun finish() = apply { progressAtomic.set(totalAtomic.get()) }

	fun getAndIncrement() = apply { progressAtomic.getAndIncrement() }

	fun incrementAndGet() = apply { progressAtomic.incrementAndGet() }

	fun addAndGet(number: Number) = apply { progressAtomic.addAndGet(number.toLong()) }

	fun getAndAdd(number: Number) = apply { progressAtomic.getAndAdd(number.toLong()) }

	fun setTotal(number: Number) = apply {
		totalAtomic.set(number.toLong())
	}
}