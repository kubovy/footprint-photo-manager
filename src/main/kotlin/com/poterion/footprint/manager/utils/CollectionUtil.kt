package com.poterion.footprint.manager.utils

import java.util.*
import java.util.concurrent.*
import java.util.stream.Stream
import kotlin.collections.ArrayList
import kotlin.streams.toList

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
inline fun <T> Iterable<T>.process(action: (T) -> Unit): Iterable<T> {
	for (element in this) action(element)
	return this
}

inline fun <T> Sequence<T>.process(crossinline action: (T) -> Unit): Sequence<T> = map {
	action(it)
	it
}

inline fun <T> Stream<T>.process(crossinline action: (T) -> Unit): Stream<T> = map {
	action(it)
	it
}

inline fun <T> Iterable<T>.processIf(predicate: (T) -> Boolean, action: (T) -> Unit): Iterable<T> {
	for (element in this) if (predicate(element)) action(element)
	return this
}

inline fun <T> Sequence<T>.processIf(crossinline predicate: (T) -> Boolean,
									 crossinline action: (T) -> Unit): Sequence<T> = map {
	if (predicate(it)) action(it)
	it
}

fun <T> Collection<T>.parallelStreamProcess(
	parallelism: Int = kotlin.math.max(3, Runtime.getRuntime().availableProcessors() - 2),
	action: (T) -> Unit): Collection<T> = ForkJoinPool(parallelism)
	.submit(Callable<Collection<T>> { parallelStream().process(action).toList() })
	.get() as Collection<T>

fun <T, R> Collection<T>.parallelStreamMap(
	parallelism: Int = kotlin.math.max(3, Runtime.getRuntime().availableProcessors() - 2),
	transform: (T) -> R): Collection<R> = ForkJoinPool(parallelism)
	.submit(Callable<Collection<R>> { parallelStream().map(transform).toList() })
	.get() as Collection<R>

fun <T> Iterable<T>.parallelFilter(
	parallelism: Int = kotlin.math.max(3, Runtime.getRuntime().availableProcessors() - 2),
	executor: ExecutorService = Executors.newFixedThreadPool(parallelism),
	predicate: (T) -> Boolean): List<T> {

	// default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
	val defaultSize = if (this is Collection<*>) this.size else 10
	val destination = Collections.synchronizedList(ArrayList<T>(defaultSize))

	for (item in this) {
		executor.submit { if (predicate(item)) destination.add(item) }
	}

	executor.shutdown()
	executor.awaitTermination(1, TimeUnit.DAYS)

	return ArrayList<T>(destination)
}

fun <T, R> Iterable<T>.parallelMap(
	parallelism: Int = kotlin.math.max(3, Runtime.getRuntime().availableProcessors() - 2),
	executor: ExecutorService = Executors.newFixedThreadPool(parallelism),
	transform: (T) -> R): List<R> {

	// default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
	val defaultSize = if (this is Collection<*>) this.size else 10
	val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

	for (item in this) {
		executor.submit { destination.add(transform(item)) }
	}

	executor.shutdown()
	executor.awaitTermination(1, TimeUnit.DAYS)

	return ArrayList<R>(destination)
}