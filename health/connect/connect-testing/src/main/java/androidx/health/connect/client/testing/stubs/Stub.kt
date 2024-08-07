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
 * Represents a stub mapping from a request [T] to a response [R].
 *
 * Stubs are usually created with the [stub] function or the [MutableStub] class.
 *
 * There are three possible outcomes of a stub:
 * - A value of type [R] is returned meaning that the request has succeeded and the value should be
 *   returned to the caller.
 * - An exception is thrown meaning that the request has failed and the error should be returned to
 *   the caller.
 * - Null (`null`) is returned meaning that the request has not been processed by this stub, in
 *   which case the client may call another stub or somehow fallback to a default behaviour.
 *
 * Example Stub where output depends on input:
 *
 * @sample androidx.health.connect.testing.samples.stub_mapping
 * @see stub
 * @see MutableStub
 */
@ExperimentalTestingApi
public fun interface Stub<in T, out R : Any> {
    /** Returns the next item. */
    public fun next(request: T): R?
}

/**
 * Creates a stub that always returns a default value.
 *
 * Example Stub where the output is fixed:
 *
 * @sample androidx.health.connect.testing.samples.stub_defaultOnly
 * @param default A response [R] that will be used if a response is requested
 * @see Stub
 */
@ExperimentalTestingApi
public fun <R : Any> stub(default: R?): Stub<Any?, R> =
    stub(queue = emptyList(), defaultHandler = { default })

/**
 * Creates a stub that returns elements from a queue or produces it using the default handler.
 *
 * Example Stub where the output is fixed:
 *
 * @sample androidx.health.connect.testing.samples.stub_defaultOnly
 *
 * Example Stub with 2 elements in a queue that once consumed returns a fixed response :
 *
 * @sample androidx.health.connect.testing.samples.stub_defaultAndQueue
 *
 * Example Stub that throws an exception when used:
 *
 * @sample androidx.health.connect.testing.samples.stub_defaultException
 *
 * Example Stub with 1 element in a queue that once consumed throws an exception when used:
 *
 * @sample androidx.health.connect.testing.samples.stub_queueAndDefaultException
 * @param queue a queue of items that has precedence over the default handler *
 * @param defaultHandler Function that produces a response [R]. Used when a response is requested
 *   and the queue is empty.
 */
@JvmOverloads
@ExperimentalTestingApi
public inline fun <R : Any> stub(
    queue: Iterable<R> = emptyList(),
    crossinline defaultHandler: () -> R? = { null }
): Stub<Any?, R> {
    val internalQueue = queue.toCollection(ArrayDeque())

    return Stub { internalQueue.removeFirstOrNull() ?: defaultHandler() }
}

/**
 * Throws if the next item in this [Stub] is a throwable, otherwise it doesn't do anything.
 *
 * Used when a [Stub] is only meant to throw exceptions and not to return values.
 */
internal fun <T> Stub<T, Nothing>.throwOrContinue(value: T): Nothing? = next(value)
