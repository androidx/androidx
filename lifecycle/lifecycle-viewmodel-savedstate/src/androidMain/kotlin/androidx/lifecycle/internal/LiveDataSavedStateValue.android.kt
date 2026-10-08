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

package androidx.lifecycle.internal

import androidx.lifecycle.MutableLiveData
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateValue
import androidx.savedstate.read
import androidx.savedstate.savedState

internal class LiveDataSavedStateValue<T>(val liveData: MutableLiveData<T>) : SavedStateValue<T> {

    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = liveData.value as T
        set(value) {
            liveData.value = value
        }

    override fun saveState(): SavedState {
        return if (liveData.isInitialized) savedState(mapOf(SAVED_STATE_VALUE_KEY to value))
        else savedState()
    }

    override fun restoreState(savedState: SavedState?) {
        if (savedState != null && savedState.read { contains(SAVED_STATE_VALUE_KEY) }) {
            @Suppress("UNCHECKED_CAST")
            value = unwrapSavedStateValue(savedState) as T
        }
    }
}
