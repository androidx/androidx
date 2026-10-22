/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.StateObject

internal class TransformState<T>
private constructor(
    private val delegate: MutableState<T>,
    private val dispose: () -> Unit,
) : DisposableState<T>, MutableState<T> by delegate, StateObject by delegate as StateObject {

    override var value: T
        get() = delegate.value
        set(value) {
            delegate.value = value
        }

    internal constructor(
        initialValue: T,
        dispose: () -> Unit,
    ) : this(mutableStateOf(initialValue), dispose)

    override fun dispose() {
        dispose.invoke()
    }
}
