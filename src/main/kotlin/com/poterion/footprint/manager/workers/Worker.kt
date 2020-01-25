package com.poterion.footprint.manager.workers

import javafx.application.Platform
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class Worker<A : Any?, P : Any?, R : Any?>(private val arg: A) : Callable<R?> {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(Worker::class.java)
	}

	private var onStartCallback: (A) -> Unit = {}
	private var onUpdateCallback: (P) -> Unit = {}
	private var onSuccessCallback: (R?) -> Unit = {}
	private var onFinishedCallback: (R?) -> Unit = {}
	private var onCancelCallback: () -> Unit = {}
	private var onErrorCallback: (Throwable) -> Unit = {}

	final override fun call(): R? {
		Platform.runLater { onStartCallback(arg) }
		var result: R? = null
		try {
			result = doWork(arg)
			Platform.runLater { onSuccessCallback(result) }
		} catch (e: InterruptedException) {
			LOGGER.error(e.message, e)
			Platform.runLater { onCancelCallback() }
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
			Platform.runLater { onErrorCallback(t) }
		} finally {
			Platform.runLater { onFinishedCallback(result) }
			return result
		}
	}

	abstract fun doWork(arg: A): R?

	fun update(progress: P) {
		Platform.runLater { onUpdateCallback(progress) }
		if (Thread.currentThread().isInterrupted) throw InterruptedException()
	}

	fun onStart(callback: (A) -> Unit): Worker<A, P, R> = apply {
		onStartCallback = callback
	}

	fun onUpdate(callback: (P) -> Unit): Worker<A, P, R> = apply {
		onUpdateCallback = callback
	}

	fun onSuccess(callback: (R?) -> Unit): Worker<A, P, R> = apply {
		onSuccessCallback = callback
	}

	fun onFinished(callback: (R?) -> Unit): Worker<A, P, R> = apply {
		onFinishedCallback = callback
	}

	fun onCancel(callback: () -> Unit): Worker<A, P, R> = apply {
		onCancelCallback = callback
	}

	fun onError(callback: (Throwable) -> Unit): Worker<A, P, R> = apply {
		onErrorCallback = callback
	}
}