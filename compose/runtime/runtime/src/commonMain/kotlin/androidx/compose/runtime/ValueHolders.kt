/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime

internal interface ValueHolder<T> {
    fun readValue(map: PersistentCompositionLocalMap): T

    fun toProvided(local: CompositionLocal<T>): ProvidedValue<T>
}

/** A StaticValueHolder holds a value that will never change. */
internal data class StaticValueHolder<T>(val value: T) : ValueHolder<T> {
    override fun readValue(map: PersistentCompositionLocalMap): T = value

    override fun toProvided(local: CompositionLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = local,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = false
        )
}

/**
 * A lazy value holder is static value holder for which the value is produced by the valueProducer
 * parameter which is called once and the result is remembered for the life of LazyValueHolder.
 */
internal class LazyValueHolder<T>(valueProducer: () -> T) : ValueHolder<T> {
    private val current by lazy(valueProducer)

    override fun readValue(map: PersistentCompositionLocalMap): T = current

    override fun toProvided(local: CompositionLocal<T>): ProvidedValue<T> =
        composeRuntimeError("Cannot produce a provider from a lazy value holder")
}

internal data class ComputedValueHolder<T>(val compute: CompositionLocalAccessorScope.() -> T) :
    ValueHolder<T> {
    override fun readValue(map: PersistentCompositionLocalMap): T = map.compute()

    override fun toProvided(local: CompositionLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = local,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = null,
            compute = compute,
            isDynamic = false
        )
}

internal data class DynamicValueHolder<T>(val state: MutableState<T>) : ValueHolder<T> {
    override fun readValue(map: PersistentCompositionLocalMap): T = state.value

    override fun toProvided(local: CompositionLocal<T>): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = local,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = state,
            compute = null,
            isDynamic = true
        )
}
