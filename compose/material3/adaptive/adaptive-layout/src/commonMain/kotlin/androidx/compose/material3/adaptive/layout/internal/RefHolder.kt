/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.reflect.KProperty

/**
 * This is a helper function to remember a mutable value that the value change will not trigger
 * recomposition. For example we may want to remember a certain variable value of the last
 * composition to do some comparison. This can be a light-weight replacement of using states in
 * certain cases.
 */
@Composable
internal fun <T> rememberRef(initialValue: T): RefHolder<T> = remember { RefHolder(initialValue) }

internal class RefHolder<T>(var value: T)

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> RefHolder<T>.getValue(thisObj: Any?, property: KProperty<*>): T =
    value

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> RefHolder<T>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}
