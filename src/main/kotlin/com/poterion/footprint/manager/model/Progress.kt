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