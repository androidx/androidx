/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.health.connect.client.testing.stubs

import androidx.health.connect.client.testing.ExperimentalTestingApi

/**
 * A [Stub] whose defaultHandler and queue can be mutated at any point.
 *
 * Example MutableStub with a default value and a new item enqueued after consumption:
 *
 * @sample androidx.health.connect.testing.samples.simpleMutableStub
 *
 * Example MutableStub with an item in the queue and a default value, alternative construction:
 *
 * @sample androidx.health.connect.testing.samples.simpleMutableStub2
 *
 * Example Mutable stub with 1 element in a queue that once consumed returns a fixed response which
 * depends on the input:
 *
 * @sample androidx.health.connect.testing.samples.fullMutableStub
 *
 * Example Mutable stub created with the [buildStub] builder:
 *
 * @sample androidx.health.connect.testing.samples.builderMutableStub
 * @property defaultHandler Function that maps a request [T] to a response [R]. Used when the queue
 *   is empty.
 * @see Stub
 */
@ExperimentalTestingApi
public class MutableStub<T, R : Any>(
    public var defaultHandler: (T) -> R? = { null },
) : Stub<T, R> {
    internal val queue = ArrayDeque<R>()

    override fun next(request: T): R? {
        return queue.removeFirstOrNull() ?: defaultHandler(request)
    }
}

/**
 * Creates a new [MutableStub] with a default return value used when the queue is empty.
 *
 * @param defaultResponse The default return value
 */
@ExperimentalTestingApi
public fun <R : Any> MutableStub(defaultResponse: R?): MutableStub<Any?, R> =
    MutableStub(defaultHandler = { defaultResponse })

/**
 * Creates a new [MutableStub] that throws an exception if the queue is empty.
 *
 * Consider passing a default handler instead, so the exception is not created until necessary:
 * ```
 * MutableStub { throw Exception() }
 * ```
 *
 * @param defaultError The exception to throw if the queue is empty.
 */
@ExperimentalTestingApi
public fun <R : Any> MutableStub(defaultError: Throwable): MutableStub<Any?, R> =
    MutableStub(defaultHandler = { throw defaultError })

/**
 * Builder to create [MutableStub]s.
 *
 * @sample androidx.health.connect.testing.samples.builderMutableStub
 */
@ExperimentalTestingApi
public inline fun <T, R : Any> buildStub(builder: MutableStub<T, R>.() -> Unit): Stub<T, R> {
    return MutableStub<T, R>().apply(builder)
}

/**
 * Adds a new item to the [MutableStub]'s queue.
 *
 * @sample androidx.health.connect.testing.samples.builderMutableStub
 * @sample androidx.health.connect.testing.samples.fullMutableStub
 */
@ExperimentalTestingApi
public fun <R : Any> MutableStub<*, R>.enqueue(values: Iterable<R>) {
    queue.addAll(values)
}

/**
 * Adds a new item to the [MutableStub]'s queue.
 *
 * @sample androidx.health.connect.testing.samples.builderMutableStub
 * @sample androidx.health.connect.testing.samples.fullMutableStub
 */
@ExperimentalTestingApi
public fun <R : Any> MutableStub<*, R>.enqueue(vararg values: R) {
    queue.addAll(values.asList())
}

/**
 * Adds a new item to the [MutableStub]'s queue.
 *
 * @sample androidx.health.connect.testing.samples.fullMutableStub
 */
@ExperimentalTestingApi
public operator fun <R : Any> MutableStub<*, R>.plusAssign(value: R) {
    enqueue(value)
}
