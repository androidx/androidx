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

package androidx.xr.compose.platform

import androidx.compose.runtime.RememberObserver
import kotlin.reflect.KProperty

/**
 * A [value] holder that will automatically call [value.close] when exiting the composition.
 *
 * This should be used in a remember block.
 */
internal fun <T : AutoCloseable> disposableValueOf(value: T): DisposableValue<T> =
    DisposableValue(value) { it.close() }

/**
 * A [value] holder that will automatically call [disposeBlock] when exiting the composition.
 *
 * This should be used in a remember block.
 */
internal fun <T> disposableValueOf(value: T, disposeBlock: (T) -> Unit): DisposableValue<T> =
    DisposableValue(value, disposeBlock)

/**
 * A [value] holder that will automatically call [disposeBlock] when exiting the composition.
 *
 * This should be used in a remember block.
 */
internal class DisposableValue<out T>(public val value: T, private val disposeBlock: (T) -> Unit) :
    RememberObserver {
    override fun onAbandoned() {
        disposeBlock(value)
    }

    override fun onForgotten() {
        disposeBlock(value)
    }

    override fun onRemembered() {}
}

internal operator fun <T> DisposableValue<T>.getValue(thisObj: Any?, property: KProperty<*>): T =
    value
